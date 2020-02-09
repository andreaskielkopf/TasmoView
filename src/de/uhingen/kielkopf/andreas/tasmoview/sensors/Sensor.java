package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonList;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonString;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonValue;

public class Sensor implements Comparable<Sensor> {
   public static final String     STATUS_8      ="Status 8";
   private static final int       MAXWERTE      =10000;
   public static Instant          firstTimestamp=null;
   // private static Instant lastTimestamp =null;
   public static double           timescaleS    =5d/MAXWERTE;
   private final String           name;
   private final String           kennung;
   public final String            typ;
   public final Tasmota           tasmota;
   public Path2D.Double           path          =new Path2D.Double();
   public final TreeSet<Messwert> werte         =new TreeSet<Messwert>();
   private ArrayList<Messwert>    untersuchung  =new ArrayList<Messwert>();
   private double                 timescaleD    =5d/MAXWERTE;
   private double                 minwert;
   private double                 maxwert;
   public Sensor(Tasmota tasmota, String name, String kennung, String typ/* , JsonObject einheit, TreeMap<Date, Messwert> werte */) {
      super();
      name.isBlank();
      this.name=name;
      this.tasmota=tasmota;
      this.typ=typ;
      // System.out.println(typ);
      this.kennung=kennung;
   }
   public static void addSensors(Tasmota tasmota, JsonObject jo) {
      if (!(jo instanceof JsonList)) return; // StatusSNS
      Sensor s=null;
      addSensors: for (JsonObject j1:((JsonList) jo).list) { // AM23..
         if (j1 instanceof JsonList) {
            // System.out.println(j1);
            // String name=getName(tasmota, j); // AM23..
            for (JsonObject j2:((JsonList) j1).list) {
               if (j2 instanceof JsonValue) {
                  String name=Integer.toString(tasmota.ipPart)+"."+j1.name+"."+j2.name;
                  // 25...AM23... Temp
                  synchronized (Data.data.sensoren) {
                     for (Sensor sensor:Data.data.sensoren) if (name.equals(sensor.name)) continue addSensors;
                     s=new Sensor(tasmota, name, j1.name, j2.name);
                     Data.data.sensoren.add(s);
                     Data.data.sensorTypen.add(s.typ);
                     Vector<Sensor> sa=new Vector<Sensor>();
                     sa.addAll(Data.data.sensoren);
                     if (!sa.isEmpty()) {
                        Data.data.getSensorList().setListData(sa);
                        Data.data.getSensorList().revalidate();
                        Data.data.getSensorList().repaint(1000);
                        Data.data.getSensorGraphPanel().setSensors(Data.data.sensoren);
                     }
                  }
                  System.out.println(tasmota+s.name);
               }
            }
         }
      }
      if (s!=null) Data.data.tasmotasMitSensoren.add(tasmota);
   }
   @Override
   public int compareTo(Sensor o) {
      return name.compareTo(o.name);
   }
   public static void antwortAuswerten(Entry<CompletableFuture<HttpResponse<String>>, Tasmota> entry) {
      try {
         String     body=entry.getKey().get().body();
         Tasmota    tasm=entry.getValue();
         JsonObject json=JsonObject.interpret(body);
         if (json==null) return;
         Instant i=getInstant(json.getJsonObject("Time"));
         synchronized (Data.data.sensoren) {
            for (Sensor sensor:Data.data.sensoren) {
               if (sensor.tasmota!=tasm) continue;
               JsonObject j1=json.getJsonObject(sensor.kennung);
               if (j1==null) continue;
               sensor.addWert(i, j1);
               // System.out.println(sensor.tasmota.name.toString()+j1);
            }
         }
      } catch (InterruptedException|ExecutionException e) {
         e.printStackTrace();
      }
   }
   /** ein neuer Messwert dieses Sensors wird eingetragen */
   private synchronized void addWert(Instant i, JsonObject j) {
      if (firstTimestamp==null) firstTimestamp=i;
      Double y=j.getDoubleValue(typ);
      // JsonObject jy=j1.getJsonObject(typ);
      // if (jy instanceof JsonValue) {
      if (y!=null) {
         // double y=Double.valueOf(((JsonValue) jy).value);
         Messwert mwert           =new Messwert(i, j, y);
         boolean  recalculateSkala=true;
         double   x               =((double) ChronoUnit.SECONDS.between(firstTimestamp, i))*timescaleD;
         if (werte.isEmpty()) {
            path.moveTo(x, mwert.value);
            minwert=mwert.value;
            maxwert=mwert.value;
         } else {
            path.lineTo(x, mwert.value);
            if (mwert.value<minwert) minwert=mwert.value;
            else if (mwert.value>maxwert) maxwert=mwert.value;
            else recalculateSkala=false;
         }
         werte.add(mwert);
         if (werte.size()>MAXWERTE) {
            compressWerte();
            recalculatePath();
            recalculateSkala=true;
         } else if (timescaleD!=timescaleS) {
            recalculatePath();
            recalculateSkala=true;
         }
         if (recalculateSkala) Data.data.getSensorGraphPanel().recalculateSkala(this);
         Data.data.getSensorGraphPanel().repaint(1000);
      }
   }
   private void recalculatePath() {
      double timescaleT=5d/((double) ChronoUnit.SECONDS.between(firstTimestamp, Instant.now()));
      if (timescaleS>timescaleT*2) timescaleS=timescaleT;
      timescaleD=timescaleS;
      Path2D.Double p2     =new Path2D.Double();
      int           counter=0;
      for (Messwert mwert:werte) {
         double x=((double) ChronoUnit.SECONDS.between(firstTimestamp, mwert.instant))*timescaleD;
         if (0==counter++) p2.moveTo(x, mwert.value);
         else p2.lineTo(x, mwert.value);
      }
      path=p2;
   }
   private void compressWerte() {
      // Suche unnötige Zwischenwerte und entferne auch Ausreisser
      untersuchung.clear();
      untersuchung.addAll(werte);
      for (int index=1; index<untersuchung.size()-1; index++) {
         // if (untersuchung.get(index).value==untersuchung.get(index-1).value) {
         if (untersuchung.get(index-1).value==untersuchung.get(index+1).value) {
            werte.remove(untersuchung.get(index));
         } // }
      }
      if (werte.size()<MAXWERTE*0.9) return;
      untersuchung.retainAll(werte);
      for (int index=0; index<untersuchung.size()-MAXWERTE*0.1; index+=2) {
         werte.remove(untersuchung.get(index));
      } // TODO min und max neu berechnen
   }
   private static Instant getInstant(JsonObject jsonObject) {
      if ((jsonObject instanceof JsonString)) {
         JsonString js=(JsonString) jsonObject;
         if (js.name.equals("Time")) try {
            return Instant.parse(js.value+"Z");
         } catch (Exception e) {
            System.err.println("Unable to parse "+js.value+" : "+Instant.now());
            e.printStackTrace();
         }
      }
      return Instant.now();
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      if (tasmota.name!=null) {
         sb.append(tasmota.name.list.get(0));
         sb.append('.');
         sb.append(kennung);
         sb.append('.');
         sb.append(typ);
      } else {
         sb.append("Sensor");
         sb.append(name);
      }
      return sb.toString();
      // return "Sensor "+name;
   }
   public Color getColor() {
      if (typ.startsWith("T")) return Color.RED;
      if (typ.startsWith("H")) return Color.BLUE;
      return Color.MAGENTA;
   }
   public Path2D.Double getPath() {
      Path2D.Double np=(java.awt.geom.Path2D.Double) path.clone();
      return np;
   }
   public boolean hasWerte() {
      return !werte.isEmpty();
   }
   public double getMinWert() {
      return minwert;
   }
   public double getMaxWert() {
      return maxwert;
   }
}

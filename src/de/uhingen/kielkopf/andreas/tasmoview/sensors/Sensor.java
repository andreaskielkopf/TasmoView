package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.*;

import javax.swing.DefaultListModel;

import de.uhingen.kielkopf.andreas.beans.minijson.*;
import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

public class Sensor implements Comparable<Sensor> {
   public static Instant                        firstTimestamp=null;
   public static final int                      MAXWERTE      =1000;
   public static final String                   STATUS_8      ="Status 8";
   // static private Instant lastTimestamp =null;
   public static double                         timescaleS    =5d / MAXWERTE;
   static final double                          FARB_RADIUS   =0.7d / 2d;
   static final float                           FARB_BASIS    =0.95f;
   static final double                          FARB_STEP     =(2d * Math.PI) / 5.3d;
   private final String                         name;
   public final Tasmota                         tasmota;
   public final String                          kennung;
   public final String                          typ;
   public final Color                           color;
   private double                               maxwert;
   private double                               minwert;
   public Path                                  pfad          =null;
   public Path2D.Double                         path          =new Path2D.Double();
   private double                               timescaleD    =5d / MAXWERTE;
   private final ArrayList<Messwert>            untersuchung  =new ArrayList<>();
   public final ConcurrentSkipListSet<Messwert> werte         =new ConcurrentSkipListSet<>();
   public final ConcurrentSkipListSet<Messwert> saveWerte     =new ConcurrentSkipListSet<>();
   private int                                  countdown;
   // private final <Messwert> puffer =new <Messwert>();
   /**
    * @param tasmota1
    *           verbundenes Gerät z.B. mit namen "Thermostat"
    * @param name1
    *           netzwerktypischer Name
    * @param kennung1
    *           z.B. AM2301 oder AM2301-00
    * @param typ1
    *           z.B. Temperature oder Humidity
    */
   private Sensor(Tasmota tasmota1, String name1, String kennung1,
            String typ1/* , JsonObject einheit, TreeMap<Date, Messwert> werte */) {
      name=name1;
      tasmota=tasmota1;
      typ=typ1;
      kennung=kennung1;
      color=createcolor();
   }
   public static void addSensors(Tasmota tasmota, JsonObject j0) {
      if (tasmota == null)
         return; // StatusSNS
      if (j0 instanceof JsonContainer jc) {
         Sensor s=null;
         addSensors: for (final JsonObject j1:jc.list)
            if (j1 instanceof JsonContainer jc2) {
               for (final JsonObject j2:jc2.list)
                  if (j2 instanceof JsonValue jv) {
                     final String name=Integer.toString(tasmota.ipPart) + "." + j1.name + "." + jv.name;
                     // 25...AM23... Temp
                     for (final Sensor sensor:Data.getData().gesamtSensoren)
                        if (name.equals(sensor.name))
                           continue addSensors;
                     s=new Sensor(tasmota, name, j1.name, jv.name);
                     Data.getData().gesamtSensoren.add(s);
                     tasmota.lokaleSensoren.add(s);
                     ((DefaultListModel<Sensor>) Data.getData().getSensorJList().getModel()).addElement(s);
                     Data.getData().sensorTypen.add(s.typ);
                     // Vector<Sensor> sa=new Vector<Sensor>();// TODO brauchts das noch ?
                     // sa.addAll(Data.getData().sensoren);
                     // if (!sa.isEmpty()) {
                     // Data.getData().getSensorJList().setListData(sa);
                     // Data.getData().getSensorJList().revalidate();
                     // Data.getData().getSensorJList().repaint(1000);
                     Data.getData().getSensorGraphPanel().setSensors(Data.getData().gesamtSensoren);
                     // }
                     System.out.println(tasmota + s.name);
                  }
            }
         if (s != null)
            Data.getData().tasmotasMitSensoren.add(tasmota);
      }
   }
   public static void antwortAuswerten(ArrayList<String> sl, Tasmota tasm) {
      // Tasmota tasm=entry.getValue();
      // ArrayList<String> sl =entry.getKey().get();
      final String     s =sl.get(1);
      final JsonObject j0=JsonObject.convertToJson(s);
      if (j0 == null)
         return; // synchronized (Data.getData().sensoren) {
      for (final Sensor sensor:Data.getData().gesamtSensoren) {
         if (sensor.tasmota != tasm)
            continue;
         final JsonObject j1=j0.getJsonObject(sensor.kennung);
         if (j1 == null)
            continue;
         final Instant i=Messwert.getInstant(j0.getJsonObject("Time"));
         sensor.addWert(i, j1);
         // System.out.println(sensor.tasmota.name.toString()+j1);
      } // }
   }
   public static void antwortAuswerten(Entry<CompletableFuture<ArrayList<String>>, Tasmota> entry) {
      try {
         final Tasmota           tasm=entry.getValue();
         final ArrayList<String> sl  =entry.getKey().get();
         final String            s   =sl.get(1);
         final JsonObject        j0  =JsonObject.convertToJson(s);
         if (j0 == null)
            return; // synchronized (Data.getData().sensoren) {
         for (final Sensor sensor:Data.getData().gesamtSensoren) {
            if (sensor.tasmota != tasm)
               continue;
            final JsonObject j1=j0.getJsonObject(sensor.kennung);
            if (j1 == null)
               continue;
            final Instant i=Messwert.getInstant(j0.getJsonObject("Time"));
            sensor.addWert(i, j1);
            // System.out.println(sensor.tasmota.name.toString()+j1);
         } // }
      } catch (InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
   }
   /** ein neuer Messwert dieses Sensors wird eingetragen */
   public synchronized void addWert(Instant instant, JsonObject j0) {
      if (firstTimestamp == null)
         firstTimestamp=instant;
      final Double value=j0.getDoubleValue(typ);
      if (value == null)
         return;
      final Messwert mwert=new Messwert(instant, j0, value.doubleValue());
      /** dreifache Messwerte zu 2 zusammenfassen und Spitzen/Jitter entfernen */
      if (werte.size() > 1) {
         final Messwert tmp=werte.pollLast(); // letzten provisorisch entfernen
         if (werte.last().value != mwert.value)
            werte.add(tmp); // doch wieder einfügen
      }
      werte.add(mwert);
      {
         /**
          * Wenn zumindest 2 Werte da sind, - den letzten rausholen - den vorletzten mit dem aktuellen Wert vergleichen
          * - wenn sie ungleich sind, alle Werte erhalten - wenn sie gleich sind, den Zwischenwert entfernt lassen Das
          * ist entweder ein Ausreisser, oder Jitter, oder 3 Konstante Werte hintereinander
          */
         if (saveWerte.size() > 1) {
            final Messwert letzter=saveWerte.pollLast(); // letzten provisorisch entfernen
            if (saveWerte.last().value != mwert.value)
               saveWerte.add(letzter);
         }
         saveWerte.add(mwert);
      }
      boolean      recalculateSkala=true;
      final double x               =(ChronoUnit.SECONDS.between(firstTimestamp, instant)) * timescaleD;
      if (path.getCurrentPoint() == null) {
         path.moveTo(x, mwert.value);
         minwert=mwert.value;
         maxwert=mwert.value;
      } else {
         path.lineTo(x, mwert.value);
         if (mwert.value < minwert)
            minwert=mwert.value;
         else
            if (mwert.value > maxwert)
               maxwert=mwert.value;
            else
               recalculateSkala=false;
      }
      if (werte.size() > MAXWERTE) {
         compressWerte();
         recalculatePath();
         recalculateSkala=true;
      } else
         if (timescaleD != timescaleS) {
            recalculatePath();
            recalculateSkala=true;
         } else
            if (--countdown <= 0) {
               countdown=10;
               recalculatePath();
               recalculateSkala=true;
            }
      if (recalculateSkala)
         Data.getData().getSensorGraphPanel().recalculateSkala(this);
      Data.getData().getSensorGraphPanel().repaint(1000);
   }
   @Override
   public int compareTo(Sensor o) {
      return name.compareTo(o.name);
   }
   private void compressWerte() {
      // Suche unnötige Zwischenwerte und entferne auch Ausreisser
      // untersuchung.clear();
      untersuchung.addAll(werte); // TODO clean untersuchung sonst brauchen wir sehr viel Speicher
      /*
       * for (int index=1; index<untersuchung.size()-1; index++) { // if
       * (untersuchung.get(index).value==untersuchung.get(index-1).value) { if
       * (untersuchung.get(index-1).value==untersuchung.get(index+1).value) { werte.remove(untersuchung.get(index)); }
       * // } } if (werte.size()<MAXWERTE*0.9) return; untersuchung.retainAll(werte);
       */
      for (int index=0; index < (untersuchung.size() - (MAXWERTE * 0.1)); index+=5) {
         werte.remove(untersuchung.get(index));
      } // TODO min und max neu berechnen
      Messwert first=null;
      for (final Messwert messwert:werte) {
         if (first == null) {
            first=messwert;
         } else {
            final double   value     =(first.value + messwert.value) / 2d;
            final long     millis    =first.instant.until(messwert.instant, ChronoUnit.MILLIS);
            final Instant  next      =first.instant.plus(millis / 2, ChronoUnit.MILLIS);
            final Messwert mittelwert=new Messwert(next, first.json, value);
            werte.remove(first);
            werte.remove(messwert);
            first=null;
            werte.add(mittelwert);
         }
      }
   }
   private Color createcolor() {
      double winkel=0;
      for (final Sensor s:Data.getData().gesamtSensoren)
         if (s.typ.equals(typ))
            winkel+=FARB_STEP;
      System.out.println(typ + " Winkel= " + (((int) ((180 * winkel) / Math.PI)) % 360));
      final float sin=(float) (FARB_RADIUS * (Math.sin(winkel) + 1d));
      final float cos=(float) (FARB_RADIUS * (Math.cos(winkel) + 1d));
      switch (typ) {
      case "Temperature":
         return new Color(FARB_BASIS, sin, cos);// RED
      case "Humidity":
         return new Color(sin, cos, FARB_BASIS);// BLUE
      default:
         return new Color(cos, FARB_BASIS, sin);// GREEN
      }
   }
   public Color getColor() {
      if (typ.startsWith("T"))
         return Color.RED;
      if (typ.startsWith("H"))
         return Color.BLUE;
      return Color.MAGENTA;
   }
   public double getMaxWert() {
      return maxwert;
   }
   public double getMinWert() {
      return minwert;
   }
   public Path2D.Double getPath() {
      return (java.awt.geom.Path2D.Double) path.clone();
   }
   public boolean hasWerte() {
      return !werte.isEmpty();
   }
   private void recalculatePath() {
      final double timescaleT=5d / (ChronoUnit.SECONDS.between(firstTimestamp, Instant.now()));
      if (timescaleS > (timescaleT * 2))
         timescaleS=timescaleT;
      timescaleD=timescaleS;
      final Path2D.Double p2     =new Path2D.Double();
      int                 counter=0;
      for (final Messwert mwert:werte) {
         final double x=(ChronoUnit.SECONDS.between(firstTimestamp, mwert.instant)) * timescaleD;
         if (0 == counter++)
            p2.moveTo(x, mwert.value);
         else
            p2.lineTo(x, mwert.value);
      }
      path=p2;
   }
   @Override
   public String toString() {
      final StringBuilder sb=new StringBuilder();
      if (tasmota.deviceName != null) {
         sb.append(tasmota.deviceName);
         sb.append('.');
         sb.append(kennung);
         sb.append('.');
         sb.append(typ);
      } else {
         sb.append("Sensor");
         sb.append(name);
      }
      return sb.toString();
   }
}

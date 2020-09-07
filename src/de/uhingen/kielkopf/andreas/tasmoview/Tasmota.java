package de.uhingen.kielkopf.andreas.tasmoview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;

import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonArray;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

public class Tasmota implements Comparable<Tasmota> {
   /** Nummer des Gerätes */
   public final int                           ipPart;
   /** Text der IP des Gerätes */
   public /* final */ String                  hostaddress;
   /** Name des Gerätes nachdem es erkannt wurde */
   public JsonArray                           name;
   private JsonObject                         warning;
   public final ConcurrentSkipListSet<Sensor> sensoren   =new ConcurrentSkipListSet<>();
   /** unbeantwortete Anfragen */
   // private final ArrayList<CompletableFuture<Object>> incomplete =new ArrayList<CompletableFuture<Object>>();
   /** beantwortete Anfragen */
   // private final TreeMap<String, JsonObject> complete =new TreeMap<String, JsonObject>();
   /** verarbeitete Anfragen werden hier eingelagert (Anfragetext,Antwort als JSON) */
   public final TreeMap<String, JsonObject>   jsontree   =new TreeMap<String, JsonObject>();
   /** Alle Statusdaten auf einmal anfragen */
   public static final String                 SUCHANFRAGE="status 0";
   /** Gerätenamen erkennen */
   private static final String                SUCHKENNUNG="FriendlyName";
   /** Warnungen erkennen */
   private static final String                WARNING    ="WARNING";
   private static final String                SENSOREN   ="StatusSNS";
   /**
    * Lege ein Tasmota-Objekt probeweise an und fange an zu testen ob es antwortet
    * 
    * @throws IOException
    */
   public Tasmota(int i) {
      ipPart=i;
      byte[] nextIp=Data.data.myIp.getAddress();
      nextIp[3]=(byte) i;
      try {
         hostaddress=InetAddress.getByAddress(nextIp).getHostAddress();
         // incomplete.add(request(SUCHANFRAGE));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   /**
    * fordere eine bestimmte Übertragung an und gibt sie zurück sobald sie da ist
    * 
    * @return Stringliste mit Frage und Antwort oder nur der Frage
    */
   public final ArrayList<String> request(final String anfrage) {
      final StringBuilder sb=new StringBuilder();
      // Benutzername und Passwort einbinden
      /// http://192.168.178.181/cm?user=andreas&password=akf4sonoff&cmnd=status%200#
      sb.append("user=");
      sb.append(Data.data.getUserField().getText());
      sb.append("&password=");
      sb.append(Data.data.getPasswordField().getPassword());
      sb.append("&cmnd=");
      // Anfrage einbinden
      sb.append(anfrage);
      // return CompletableFuture.supplyAsync(() -> {
      ArrayList<String> sl=new ArrayList<String>();
      sl.add("&cmnd="+anfrage);
      try {
         URL               url   =new URI("http", hostaddress, "/cm", sb.toString(), "").toURL();
         HttpURLConnection client=(HttpURLConnection) url.openConnection();
         client.setRequestMethod("GET");
         client.setConnectTimeout(5*1000);// 5 Sekunden
         BufferedReader br=new BufferedReader(new InputStreamReader(client.getInputStream()));
         while (true) {
            String line=br.readLine();
            if (line==null) break;
            sl.add(line);
         }
      } catch (IOException|URISyntaxException ignore) {
         // e.printStackTrace(); // System.out.println("--"+sb.toString()); // return null;
      }
      return sl;
      // });
   }
   public final boolean process(ArrayList<String> response) {
      if (response.size()<2) return false;
      int i=response.get(0).lastIndexOf("&cmnd=");
      if (i<0) return false;
      String anfrage=response.get(0).substring(i+6);
      if (anfrage.length()<1) return false;
      JsonObject j=JsonObject.interpret(response.get(1));
      if (j==null) return false;
      if (SUCHANFRAGE.equals(anfrage)) {
         JsonObject jo=j.getJsonObject(SUCHKENNUNG);
         if (jo instanceof JsonArray) this.name=(JsonArray) jo;
         warning=j.getJsonObject(WARNING);
         // Tasmota-Gerät verlangt Passwort
         if (warning!=null) System.out.println(response.get(1));
      }
      jsontree.put(anfrage, j);
      // complete.put(anfrage, j);
      register(anfrage, j);
      // Data.data.dataModel.fireTableDataChanged();
      return true;
   }
   /** versuche die offenen Anfragen zu verarbeiten */
   /*
    * public final void process() { if (incomplete.isEmpty()) return; ArrayList<CompletableFuture<Object>> tocheck=new
    * ArrayList<CompletableFuture<Object>>(incomplete); for (CompletableFuture<Object> cf:tocheck) { try { if (!cf.isDone()) continue; incomplete.remove(cf); if
    * (cf.isCancelled()||cf.isCompletedExceptionally()) continue; Object response=cf.get(); System.out.println(response.getClass().getName());
    * System.out.println(response.toString()); String q =response.toString(); // request().uri().getQuery(); // System.out.println(q); int i
    * =q.lastIndexOf("&cmnd="); String anfrage=null; if (i>0) anfrage=q.substring(i+6); JsonObject j=JsonObject.interpret(response.toString()/* body() ); if
    * (j!=null) { if (SUCHANFRAGE.equals(anfrage)) { JsonObject jo=j.getJsonObject(SUCHKENNUNG); if (jo instanceof JsonArray) name=(JsonArray) jo;
    * warning=j.getJsonObject(WARNING); // Tasmota-Gerät verlangt Passwort if (warning!=null) System.out.println(this); } jsontree.put(anfrage, j);
    * complete.put(anfrage, j); register(anfrage, j); Data.data.dataModel.fireTableDataChanged(); } } catch (InterruptedException|ExecutionException e) {
    * e.printStackTrace(); } } }
    */
   /** Anfrage eintragen */
   final void register(String anfrage, JsonObject json) {
      boolean changed=false;
      if (!Data.data.tablenames.containsKey(anfrage)) {
         LinkedHashSet<String> kennungen=new LinkedHashSet<String>();
         Data.data.tablenames.put(anfrage, kennungen);
      }
      LinkedHashSet<String> k=Data.data.tablenames.get(anfrage);
      for (JsonObject jo:json.getAll()) {
         if (jo.name==null) continue;
         if (jo.name.equalsIgnoreCase(anfrage)) continue;
         if (jo.name.equalsIgnoreCase(SENSOREN)) Sensor.addSensors(this, jo);
         if (k.contains(jo.name)) continue;
         k.add(jo.name);
         changed=true;
      }
      if (changed) Data.data.dataModel.setTable(anfrage);
   }
   /** Ist das ein Tasmota-Gerät ? */
   /*
    * public Boolean isTasmota() { // Zeit um Antworten zu bearbeiten process(); if (name!=null) return true; if (incomplete.isEmpty()) return false; return
    * null; }
    */
   /** Hole Alle Antworten ! */
   ArrayList<JsonObject> getAll() {
      ArrayList<JsonObject> c=new ArrayList<JsonObject>();
      for (JsonObject response:jsontree.values())
         c.addAll(response.getAll());
      return c;
   }
   /** Hole Alle Antworten mit diesem Namen in einer Liste */
   public ArrayList<JsonObject> getAll(String name) {
      ArrayList<JsonObject> c=new ArrayList<JsonObject>();
      for (JsonObject response:jsontree.values()) {
         for (JsonObject o:response.getAll(name))
            c.add(o);
      }
      return c;
   }
   /** statische Methode um Strings in html umzurechnen */
   public static String toHtmlString(String s) {
      if (s==null) return s;
      String html=s.replaceAll(" ", "%20");
      return html.replaceAll(";", "%35");
   }
   /** Hole den Text der erste Antwort mit diesem Namen */
   public String getValue(String name) {
      ArrayList<JsonObject> l=getAll(name);
      if (l.isEmpty()) return "";
      String   s=l.get(0).toString();
      String[] a=s.split(":");
      if (a.length==2) return a[1];
      if (a.length==1) return a[0];
      return s.substring(a[0].length()+1);
   }
   @Override
   public int compareTo(Tasmota o) {
      return Integer.compare(this.ipPart, o.ipPart);
   }
   @Override
   public int hashCode() {
      final int prime =31;
      int       result=1;
      result=prime*result+ipPart;
      return result;
   }
   @Override
   public boolean equals(Object obj) {
      if (this==obj) return true;
      if (obj==null) return false;
      if (getClass()!=obj.getClass()) return false;
      Tasmota other=(Tasmota) obj;
      if (ipPart!=other.ipPart) return false;
      return true;
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      sb.append(this.getClass().getSimpleName());
      sb.append("[ip="+hostaddress);
      if (name!=null) {
         sb.append(",");
         sb.append(name);
      }
      sb.append("]");
      // if (!incomplete.isEmpty()) sb.append(" (waiting)");
      if (warning!=null) sb.append(warning);
      return sb.toString();
   }
}

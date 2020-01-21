package de.uhingen.kielkopf.andreas.tasmoview;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonArray;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;

public class Tasmota implements Comparable<Tasmota> {
   /** mehrfach verwendeter client */
   private final HttpClient                                         client     =HttpClient.newHttpClient();;
   /** Nummer des Gerätes */
   public final int                                                 ipPart;
   /** Text der IP des Gerätes */
   public final String                                              hostaddress;
   /** Name des Gerätes nachdem es erkannt wurde */
   private JsonArray                                                name;
   private JsonObject                                               warning;
   /** unbeantwortete Anfragen */
   private final ArrayList<CompletableFuture<HttpResponse<String>>> incomplete =new ArrayList<CompletableFuture<HttpResponse<String>>>();
   /** beantwortete Anfragen */
   private final TreeMap<String, JsonObject>                        complete   =new TreeMap<String, JsonObject>();
   /** verarbeitete Anfragen werden hier eingelagert (Anfragetext,Antwort als JSON) */
   public final TreeMap<String, JsonObject>                         jsontree   =new TreeMap<String, JsonObject>();
   /** Alle Statusdaten auf einmal anfragen */
   private static final String                                      SUCHANFRAGE="status 0";
   /** Gerätenamen erkennen */
   private static final String                                      SUCHKENNUNG="FriendlyName";
   /** Warnungen erkennen */
   private static final String                                      WARNING    ="WARNING";
   /** Lege ein Tasmota-Objekt probeweise an und fange an zu testen ob es antwortet */
   private Tasmota(int i) throws UnknownHostException, URISyntaxException {
      ipPart=i;
      byte[] nextIp=Data.data.myIp.getAddress();
      nextIp[3]=(byte) i;
      hostaddress=InetAddress.getByAddress(nextIp).getHostAddress();
      request(SUCHANFRAGE);
   }
   public static final void scanFor(int i) {
      try {
         Tasmota tasmota=new Tasmota(i);
         if (Data.data.tasmotas.contains(tasmota)) {
            // requests retten
            ArrayList<CompletableFuture<HttpResponse<String>>> r=tasmota.incomplete;
            // bisheriges Tasmota-Objekt wiederverwenden
            tasmota=Data.data.tasmotas.ceiling(tasmota);
            tasmota.incomplete.addAll(r);
         }
         // neue Anfragen sind unterwegs
         Data.data.unconfirmed.add(tasmota); // System.out.println(tasmota);
         Data.testUnconfirmed();
      } catch (UnknownHostException|URISyntaxException e) {
         e.printStackTrace();
      }
   }
   /** fordere eine bestimmte Übertragung an und setze sie zu den unbeantworteten */
   private final void request(String anfrage) throws URISyntaxException {
      StringBuilder sb=new StringBuilder();
      // Benutzername und Passwort einbinden
      sb.append("user=");
      sb.append(Data.data.getUserField().getText());
      sb.append("&password=");
      sb.append(Data.data.getPasswordField().getPassword());
      sb.append("&cmnd=");
      // Anfrage einbinden
      sb.append(anfrage);
      HttpRequest request=HttpRequest.newBuilder() //
               .uri(new URI("http", hostaddress, "/cm", sb.toString(), "")) //
               .timeout(Duration.ofMinutes(1)).build();
      // request absenden und asynchron bearbeiten
      incomplete.add(client.sendAsync(request, BodyHandlers.ofString()));
   }
   /** versuche die offenen Anfragen zu verarbeiten */
   public final void process() {
      if (incomplete.isEmpty()) return;
      ArrayList<CompletableFuture<HttpResponse<String>>> tocheck=new ArrayList<CompletableFuture<HttpResponse<String>>>(incomplete);
      for (CompletableFuture<HttpResponse<String>> cf:tocheck) {
         try {
            if (!cf.isDone()) continue;
            incomplete.remove(cf);
            if (cf.isCancelled()||cf.isCompletedExceptionally()) continue;
            HttpResponse<String> response=cf.get();
            String               q       =response.request().uri().getQuery(); // System.out.println(q);
            int                  i       =q.lastIndexOf("&cmnd=");
            String               anfrage =null;
            if (i>0) anfrage=q.substring(i+6);
            JsonObject j=JsonObject.interpret(response.body());
            if (j!=null) {
               if (SUCHANFRAGE.equals(anfrage)) {
                  JsonObject jo=j.getJsonObject(SUCHKENNUNG);
                  if (jo instanceof JsonArray) name=(JsonArray) jo;
                  warning=j.getJsonObject(WARNING);
                  System.out.println(this);
               }
               jsontree.put(anfrage, j);
               complete.put(anfrage, j);
               register(anfrage, j);
               Data.data.dataModel.fireTableDataChanged();
            }
         } catch (InterruptedException|ExecutionException e) {
            e.printStackTrace();
         }
      }
   }
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
         if (k.contains(jo.name)) continue;
         k.add(jo.name);
         changed=true;
      }
      if (changed) Data.data.dataModel.setTable(anfrage);
   }
   /** Ist das ein Tasmota-Gerät ? */
   public Boolean isTasmota() {
      // Zeit um Antworten zu bearbeiten
      process();
      if (name!=null) return true;
      if (incomplete.isEmpty()) return false;
      return null;
   }
   /** Hole Alle Antworten ! */
   ArrayList<JsonObject> getAll() {
      ArrayList<JsonObject> c=new ArrayList<JsonObject>();
      for (JsonObject response:complete.values()) c.addAll(response.getAll());
      return c;
   }
   /** Hole Alle Antworten mit diesem Namen in einer Liste */
   ArrayList<JsonObject> getAll(String name) {
      ArrayList<JsonObject> c=new ArrayList<JsonObject>();
      for (JsonObject response:complete.values()) {
         for (JsonObject o:response.getAll(name)) c.add(o);
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
   String getValue(String name) {
      ArrayList<JsonObject> l=getAll(name);
      if (l.isEmpty()) return "";
      String   s=l.get(0).toString();
      String[] a=s.split(":");
      if (a.length==2) return a[1];
      if (a.length==1) return a[0];
      return s.substring(a[0].length()+1);
   }
   /**
    * Das gibt normalerweise FriendlyName private String getName() { if (name==null) return " ? ? ? "; JsonObject fo=name.getJsonObject(0); if (!(fo instanceof
    * JsonString)) return " * * * "; return ((JsonString) fo).value; }
    */
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
      if (!incomplete.isEmpty()) sb.append(" (waiting)");
      if (warning!=null) sb.append(warning);
      return sb.toString();
   }
}

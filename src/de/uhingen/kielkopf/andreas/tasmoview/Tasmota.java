package de.uhingen.kielkopf.andreas.tasmoview;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;

import de.uhingen.kielkopf.andreas.beans.RecordParser;
import de.uhingen.kielkopf.andreas.beans.minijson.*;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * Repräsentation eines kompletten Geräts mit Tasmota Firmware
 */
public class Tasmota implements Comparable<Tasmota> {
   static private ConcurrentSkipListMap<Integer, Tasmota> unsicher       =new ConcurrentSkipListMap<>();
   // static private long CONNECT_TIMEOUT=60000L;
   // private IoSession session;
   /** IP-Nummer des Gerätes im lokalen Netzwerk (nur letzter Teil) */
   public final Integer                                   ipPart;
   /** IP des Gerätes */
   public InetAddress                                     hostaddress;
   /** Name des Gerätes nachdem es erkannt wurde (DeviceName oder anderer passender Text z.B. FriendlyName) */
   public String                                          deviceName;
   public String                                          moduleTyp;
   static private final String[]                          NAMENSSUCHE    = {"DeviceName", "FriendlyName", "Hostname",
            "Topic", "IPAddress", "Mac",};
   private JsonObject                                     warning;
   /** Liste aller Sensoren dieses Geräts */
   public final ConcurrentSkipListSet<Sensor>             lokaleSensoren =new ConcurrentSkipListSet<>();
   /**
    * verarbeitete Anfragen werden hier eingelagert (Anfragetext,Antwort als JSON) neuere Anfragen ersetzen jeweils alte
    * Anfragen
    */
   public final SortedMap<String, JsonObject>             jsontree       =new ConcurrentSkipListMap<>();
   private final Builder                                  requestBuilder;
   private final HttpClient                               client;
   // private boolean exists =false;
   // static public final ConcurrentSkipListMap<Integer, Tasmota> tasmotas=new ConcurrentSkipListMap<>();
   /** Alle Statusdaten auf einmal anfragen */
   public static final String                             SUCHANFRAGE    ="status 0";
   public static final String                             CMD_PREFIX     ="cmnd=";
   public static final String                             USER_PREFIX    ="user=";
   public static final String                             PASSWORD_PREFIX="password=";
   public static final String                             UND            ="&";
   public static final String[]                           ZUSATZ_FRAGEN  = {"module", "Gpio", "state", "template",
            "rule1", "Timer1", "timer2", "timer3", "timer4"};
   /** Warnungen erkennen */
   static private final String                            WARNING        ="WARNING";
   static private final String                            SENSOREN       ="StatusSNS";
   static private final String                            HTTP           ="http";
   /**
    * Lege ein Tasmota-Objekt probeweise an und fange an zu testen ob es antwortet
    * 
    * @throws UnknownHostException
    * @throws URISyntaxException
    */
   public Tasmota(Integer i) throws UnknownHostException, URISyntaxException {
      ipPart=i;
      byte[] nextIp=Data.getData().myIp.getAddress();
      nextIp[3]=ipPart.byteValue();
      hostaddress=InetAddress.getByAddress(nextIp);// .getHostAddress();
      client=HttpClient.newHttpClient();
      requestBuilder=HttpRequest.newBuilder().timeout(Duration.ofSeconds(5)).GET();
   }
   /**
    * fordere eine bestimmte Übertragung an und gibt die Antwort(en) in einer Liste zurück sobald sie da ist
    * 
    * @return Stringliste mit Frage und Antwort oder nur der Frage
    */
   public final List<HttpResponse<String>> requests(final String[] anfragen) {
      ArrayList<HttpResponse<String>> sl=new ArrayList<>();
      for (String anfrage:anfragen) {
         // Benutzername und Passwort einbinden
         /// http://192.168.178.181/cm?user=andreas&password=akf4sonoff&cmnd=status%200#
         StringBuilder sb=new StringBuilder(USER_PREFIX).append(Data.getData().getUserField().getText());
         sb.append(UND).append(PASSWORD_PREFIX).append(Data.getData().getPasswordField().getPassword());
         sb.append(UND).append(CMD_PREFIX).append(anfrage); // Anfrage einbinden
         // sl.add(UND + anfrage);
         try {
            HttpRequest request=requestBuilder.copy()
                     .uri(new URI(HTTP, hostaddress.getHostAddress(), "/cm", sb.toString(), "")).build();
            // URL uril=new URI("http", hostaddress.getHostAddress(), "/cm", sb.toString(), "").toURL();
            // HttpURLConnection client=(HttpURLConnection) url.openConnection();
            // client.setRequestMethod("GET");
            // client.setConnectTimeout(5 * 1000);// 5 Sekunden
            // BufferedReader br=new BufferedReader(new InputStreamReader(client.getInputStream()));
            // while (true) {
            // String line=br.readLine();
            // if (line == null)
            // break;
            // sl.add(line);
            // }
            HttpResponse<String> erg=client.send(request, BodyHandlers.ofString());
            sl.add(erg);
         } catch (IOException | URISyntaxException | InterruptedException ignore) {}
      }
      return sl;
   }
   private final Pattern ANFRAGE_PATTERN=Pattern.compile("(?:&cmnd=)(.+)");
   public final boolean process(List<HttpResponse<String>> erg) {
      if (erg.isEmpty())
         return false;
      for (HttpResponse<String> zeile:erg) {
         int rc=zeile.statusCode();
         switch (rc) {
            default:
            case 401:
            case 404:
               System.err.println("Responsecode for(" + hostaddress + ")=" + rc);
               return false;
            case 200: // Treffer
               break;
         }
      }
      String anfrage="";
      for (HttpResponse<String> zeile:erg) {
         if (zeile.body() == null)
            continue;
         if (zeile.body().isEmpty())
            continue;
         if (zeile.body().startsWith(UND)) {// anfrage ermittelt
            anfrage=zeile.body().substring(1);
         } else { // antwort verarbeiten
            anfrage= RecordParser.getString(ANFRAGE_PATTERN.matcher(zeile.uri().getQuery())) ;
            if (JsonObject.convertToJson(zeile.body()) instanceof JsonObject j) {
               jsontree.put(anfrage, j);
               register(anfrage, j);
               if (SUCHANFRAGE.equals(anfrage))
                  namensSuche(); // nur bei einmaliger statusabfrege "status 0"
               warning=j.getJsonObject(WARNING); // Tasmota-Gerät verlangt Passwort
               if (warning != null)
                  System.out.println(zeile);
            }
         }
      }
      return true;// (j != null);
   }
   /** Suche im vorhandenen Jsontree nach einem passenden deviceNamen und trage ihn als Devicename ein */
   void namensSuche() {
      JsonObject suchTree=jsontree.get(SUCHANFRAGE);
      if (suchTree == null)
         return;
      suchschleife: for (String kennung:NAMENSSUCHE) {// versuche einen Namen für das Gerät zu finden
         for (JsonObject jsonObject:suchTree.getAll(kennung))
            if (jsonObject instanceof JsonString js) {
               this.deviceName=js.value;
               if (deviceName == null)
                  continue;
               if (deviceName.isEmpty())
                  continue;
               break suchschleife;
            } else
               if (jsonObject instanceof JsonContainer jc) {
                  for (JsonObject jsonObject2:jc.list)
                     if (jsonObject2 instanceof JsonString js) {
                        this.deviceName=js.value;
                        if (deviceName == null)
                           continue;
                        if (deviceName.isEmpty())
                           continue;
                        break suchschleife;
                     }
               }
      }
   }
   /** Anfrage eintragen */
   final void register(String anfrage, JsonObject json) {
      boolean changed=false;
      if (SUCHANFRAGE.equals(anfrage)) {
         if (json instanceof JsonContainer jc)
            for (JsonObject subTabelle:jc.list)
               if (subTabelle instanceof JsonContainer jc2)
                  changed|=registerTabelle(jc2);
      } else {
         json.name=anfrage;
         // List("{\""+anfrage+"\":"+json.toString()+"");
         if (json instanceof JsonContainer jc)
            changed|=registerTabelle(jc);
      }
      if (changed) {
         DefaultListModel<String> dlm=(DefaultListModel<String>) Data.getData().tasmolist.getTableAuswahl().getModel();
         for (Entry<String, ConcurrentSkipListSet<String>> entry:Data.getData().tableNames.entrySet())
            if (!entry.getValue().isEmpty())
               if (!dlm.contains(entry.getKey()))
                  dlm.addElement(entry.getKey());
         Data.getData().dataModel.setTable(anfrage);
      }
   }
   static private final Pattern IS_NUMBER=Pattern.compile("[0-9]+");
   /** registriert die Empfangenen Elemente als Spaltennamen */
   final boolean registerTabelle(JsonContainer tabelle) {
      String name=tabelle.name;
      if (name == null)
         for (JsonObject j:tabelle.getAll()) {
            name=j.name;
            if (name != null)
               break;
         }
      boolean changed=false;
      if (name == null)
         return changed;
      ConcurrentSkipListMap<String, ConcurrentSkipListSet<String>> tabellen=Data.getData().tableNames;
      tabellen.putIfAbsent(name, new ConcurrentSkipListSet<String>(NUMMERN_SICHERER_COMPARATOR)); // neuen Typ von
                                                                                                  // Tabelle eintragen
                                                                                                  // falls erforderlich
      ConcurrentSkipListSet<String> listOfColumnames=tabellen.get(name);
      for (JsonObject s:tabelle.getAll(SENSOREN))
         Sensor.addSensors(this, s);
      for (JsonObject j:tabelle.list) {
         String n=j.name;
         if (n == null)
            continue;// Nullwerte überspringen
         // if (n.equalsIgnoreCase(SENSOREN)) Sensor.addSensors(this, j); // Sensoren eintragen
         if (n.equalsIgnoreCase(name))
            continue;// eigenen Eintrag überspringen
         if (IS_NUMBER.matcher(n).matches()) {
            System.out.println("Numerisch " + n);
            continue; // rein Numerische Überschriften unterdrücken
         }
         if (tabellen.containsKey(n)) {
            if (j instanceof JsonContainer jc)
               registerTabelle(jc); // System.out.println("Extra Tabelle "+j);
            continue;
         }
         if (listOfColumnames.add(n)) {
            changed=true;
            // System.out.println(name+">"+n);
         }
      }
      return changed;
   }
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
      for (JsonObject response:jsontree.values())
         for (JsonObject o:response.getAll(name))
            c.add(o);
      return c;
   }
   /** statische Methode um Strings in html umzurechnen */
   public static String toHtmlString(String s) {
      if (s == null)
         return s;
      String html=s.replaceAll(" ", "%20");
      return html.replaceAll(";", "%35");
   }
   /** Hole den Text der erste Antwort mit diesem Namen */
   public String getValue(String name) {
      ArrayList<JsonObject> l=getAll(name);
      if (l.isEmpty())
         return "";
      String[] a=l.getFirst().toString().split(":");
      if (a.length == 2)
         return a[1];
      if (a.length == 1)
         return a[0];
      return l.getFirst().toString().substring(a[0].length() + 1);
   }
   /*
    * private void connect() { NioSocketConnector connector=new NioSocketConnector();
    * connector.setConnectTimeoutMillis(Tasmota.CONNECT_TIMEOUT); // connector.getFilterChain().addLast("http", new
    * ProtocolCodecFilter(factory)); connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new
    * ObjectSerializationCodecFactory())); connector.getFilterChain().addLast("logger", new LoggingFilter());
    * connector.setHandler(new TasmotaClientSessionHandler(this)); int countdown=20; do { try { ConnectFuture
    * future=connector.connect(new InetSocketAddress(hostaddress, 80)); future.awaitUninterruptibly();
    * session=future.getSession(); break; } catch (RuntimeIoException e) { System.err.println("Failed to connect.");
    * e.printStackTrace(); try { Thread.sleep(5000); } catch (InterruptedException ignore) {} } } while (--countdown >
    * 0); session.getCloseFuture().awaitUninterruptibly(); connector.dispose(); }
    */
   @Override
   public int compareTo(Tasmota o) {
      return this.ipPart.compareTo(o.ipPart);
   }
   @Override
   public int hashCode() {
      // final int prime=31;
      // int result=1;
      // result=prime * result + ipPart.hashCode();
      // return result;
      return ipPart.hashCode();
   }
   @Override
   public boolean equals(Object obj) {
      return (this == obj) || (obj instanceof Tasmota other && ipPart.equals(other.ipPart));
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      if (deviceName != null) { // Wenn ein Name bekannt ist
         sb.append(deviceName);
         sb.append("(");
         sb.append(hostaddress);
         sb.append(")");
      } else { // Ansonsten Typ und IP
         sb.append(this.getClass().getSimpleName());
         sb.append("[ip=" + hostaddress);
         sb.append("]");
         if (warning != null)
            sb.append(warning);
      }
      return sb.toString();
   }
   static final Pattern                   ZAHLEN_AM_ENDE             =Pattern.compile("\\d+$");
   static public final Comparator<String> NUMMERN_SICHERER_COMPARATOR=                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            new Comparator<String>() {
               @Override
               public int compare(String o1, String o2) {
                  int normal=o1.compareTo(o2);
                  if (normal != 0)
                     if (o1.length() != o2.length()) {
                        Matcher m1=ZAHLEN_AM_ENDE.matcher(o1);
                        Matcher m2=ZAHLEN_AM_ENDE.matcher(o2);
                        if (m1.find() && m2.find()) {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        // beide
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             // enthalten
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             // Zahlen
                           try {
                              String r1="";
                              int i1=-1;
                              // if (m1.hitEnd()) {
                              i1=Integer.valueOf(m1.group()).intValue();
                              r1=m1.replaceFirst("");
                              // }
                              String r2="";
                              int i2=-1;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     //
                              m2.groupCount();
                              // if (m2.hitEnd()) {
                              i2=Integer.valueOf(m2.group()).intValue();
                              r2=m2.replaceFirst("");
                              // }
                              if (r1.equals(r2))
                                 return Integer.compare(i1, i2);
                           } catch (NumberFormatException e) {
                              System.out.println(o1 + ":" + o2);
                              e.printStackTrace();
                           }
                        }
                     }
                  return normal;
               }
            };
   /**
    * @param i
    * @return
    * @throws UnknownHostException
    * @throws URISyntaxException
    */
   public static Tasmota getTasmota(Integer i) throws UnknownHostException, URISyntaxException {
      if (Data.getData().tasmotasD.get(i) instanceof Tasmota tasmota)
         return tasmota;
      if (unsicher.get(i) instanceof Tasmota tasmota)
         return tasmota;
      Tasmota t=new Tasmota(i);
      unsicher.put(i, t);
      return t;
   }
   /**
    * @param b
    */
   // public void setExists(boolean b) {
   // exists=b;
   // }
}

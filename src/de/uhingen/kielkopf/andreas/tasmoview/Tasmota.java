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
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;

import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonContainer;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonString;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * Repräsentation eines kompletten Geräts mit Tasmota Firmware
 */
public class Tasmota implements Comparable<Tasmota> {
   /** IP-Nummer des Gerätes im lokalen Netzwerk (nur letzter Teil) */
   public final int                           ipPart;
   /** Text der kompletten IP des Gerätes */
   public /* final */ String                  hostaddress;
   /** Name des Gerätes nachdem es erkannt wurde (DeviceName oder anderer passender Text z.B. FriendlyName) */
   public String                              deviceName;
   public String                              moduleTyp;
   private static final String[]              NAMENSSUCHE    = {"DeviceName", "FriendlyName", "Hostname", "Topic", "IPAddress", "Mac",};
   private JsonObject                         warning;
   /** Liste aller Sensoren dieses Geräts */
   public final ConcurrentSkipListSet<Sensor> sensoren       =new ConcurrentSkipListSet<>();
   /** verarbeitete Anfragen werden hier eingelagert (Anfragetext,Antwort als JSON) neuere Anfragen ersetzen jeweils alte Anfragen */
   public final SortedMap<String, JsonObject> jsontree       =new ConcurrentSkipListMap<>();
   /** Alle Statusdaten auf einmal anfragen */
   public static final String                 SUCHANFRAGE    ="status 0";
   public static final String                 CMD_PREFIX     ="cmnd=";
   public static final String                 USER_PREFIX    ="user=";
   public static final String                 PASSWORD_PREFIX="password=";
   public static final String                 UND            ="&";
   public static final String[]               ZUSATZ_FRAGEN  = {"module", "Gpio", "state", "template", "rule1", "Timer1", "timer2", "timer3", "timer4"};
   /** Warnungen erkennen */
   private static final String                WARNING        ="WARNING";
   private static final String                SENSOREN       ="StatusSNS";
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
      } catch (IOException e) {}
   }
   /**
    * fordere eine bestimmte Übertragung an und gibt die Antwort(en) in einer Liste zurück sobald sie da ist
    * 
    * @return Stringliste mit Frage und Antwort oder nur der Frage
    */
   public final List<String> requests(final String[] anfragen) {
      ArrayList<String> sl=new ArrayList<String>();
      for (String anfrage:anfragen) {
         final StringBuilder sb=new StringBuilder();
         // Benutzername und Passwort einbinden
         /// http://192.168.178.181/cm?user=andreas&password=akf4sonoff&cmnd=status%200#
         sb.append(USER_PREFIX);
         sb.append(Data.data.getUserField().getText());
         sb.append(UND);
         sb.append(PASSWORD_PREFIX);
         sb.append(Data.data.getPasswordField().getPassword());
         sb.append(UND);
         sb.append(CMD_PREFIX);
         sb.append(anfrage); // Anfrage einbinden
         sl.add(UND+anfrage);
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
         } catch (IOException|URISyntaxException ignore) {}
      }
      return sl;
   }
   public final boolean process(List<String> response) {
      boolean hatInhalt=false;
      for (String zeile:response)
         if (!zeile.startsWith(UND)) {
            hatInhalt=true;
            break;
         }
      if (!hatInhalt) return false; // keine Antwort enthalten
      String     anfrage="";
      JsonObject j      =null;
      for (String zeile:response) {
         if (zeile==null) continue;
         if (zeile.isEmpty()) continue;
         if (zeile.startsWith(UND)) {// anfrage ermittelt
            anfrage=zeile.substring(1);
         } else { // antwort verarbeiten
            j=JsonObject.convertToJson(zeile);
            if (j==null) continue;
            jsontree.put(anfrage, j);
            register(anfrage, j);
            if (SUCHANFRAGE.equals(anfrage)) namensSuche(); // nur bei einmaliger statusabfrege "status 0"
            warning=j.getJsonObject(WARNING); // Tasmota-Gerät verlangt Passwort
            if (warning!=null) System.out.println(zeile);
         }
      }
      return (j!=null);
   }
   /** Suche im vorhandenen Jsontree nach einem passenden deviceNamen und trage ihn als Devicename ein */
   void namensSuche() {
      JsonObject suchTree=jsontree.get(SUCHANFRAGE);
      if (suchTree==null) return;
      suchschleife: for (String kennung:NAMENSSUCHE) {// versuche einen Namen für das Gerät zu finden
         for (JsonObject jsonObject:suchTree.getAll(kennung))
            if (jsonObject instanceof JsonString) {
               this.deviceName=((JsonString) jsonObject).value;
               if (deviceName==null) continue;
               if (deviceName.isEmpty()) continue;
               break suchschleife;
            } else
               if (jsonObject instanceof JsonContainer) {
                  for (JsonObject jsonObject2:((JsonContainer) jsonObject).list)
                     if (jsonObject2 instanceof JsonString) {
                        this.deviceName=((JsonString) jsonObject2).value;
                        if (deviceName==null) continue;
                        if (deviceName.isEmpty()) continue;
                        break suchschleife;
                     }
               }
      }
   }
   /** Anfrage eintragen */
   final void register(String anfrage, JsonObject json) {
      boolean changed=false;
      if (SUCHANFRAGE.equals(anfrage)) {
         if (json instanceof JsonContainer) for (JsonObject subTabelle:((JsonContainer) json).list)
            if (subTabelle instanceof JsonContainer) changed|=registerTabelle((JsonContainer) subTabelle);
      } else {
         json.name=anfrage;
         // List("{\""+anfrage+"\":"+json.toString()+"");
         if (json instanceof JsonContainer) changed|=registerTabelle((JsonContainer) json);
      }
      if (changed) {
         DefaultListModel<String> dlm=(DefaultListModel<String>) Data.data.tasmolist.getTableAuswahl().getModel();
         for (Entry<String, ConcurrentSkipListSet<String>> entry:Data.data.tableNames.entrySet())
            if (!entry.getValue().isEmpty()) if (!dlm.contains(entry.getKey())) dlm.addElement(entry.getKey());
         Data.data.dataModel.setTable(anfrage);
      }
   }
   static private final Pattern IS_NUMBER=Pattern.compile("[0-9]+");
   /** registriert die Empfangenen Elemente als Spaltennamen */
   final boolean registerTabelle(JsonContainer tabelle) {
      String name=tabelle.name;
      if (name==null) for (JsonObject j:tabelle.getAll()) {
         name=j.name;
         if (name!=null) break;
      }
      boolean changed=false;
      if (name==null) return changed;
      ConcurrentSkipListMap<String, ConcurrentSkipListSet<String>> tabellen=Data.data.tableNames;
      tabellen.putIfAbsent(name, new ConcurrentSkipListSet<String>(NUMMERN_SICHERER_COMPARATOR)); // neuen Typ von Tabelle eintragen falls erforderlich
      ConcurrentSkipListSet<String> listOfColumnames=tabellen.get(name);
      for (JsonObject j:tabelle.list) {
         String n=j.name;
         if (n==null) continue;// Nullwerte überspringen
         if (n.equalsIgnoreCase(SENSOREN)) Sensor.addSensors(this, j); // Sensoren eintragen
         if (n.equalsIgnoreCase(name)) continue;// eigenen Eintrag überspringen
         if (IS_NUMBER.matcher(n).matches()) {
            System.out.println("Numerisch "+n);
            continue; // Numerische Überschriften unterdrücken
         }
         if (tabellen.containsKey(n)) {
            if (j instanceof JsonContainer) {
               System.out.println("Extra Tabelle "+j);
               registerTabelle((JsonContainer) j);
            }
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
      if (deviceName!=null) { // Wenn ein Name bekannt ist
         sb.append(deviceName);
         sb.append("(");
         sb.append(hostaddress);
         sb.append(")");
      } else { // Ansonsten Typ und IP
         sb.append(this.getClass().getSimpleName());
         sb.append("[ip="+hostaddress);
         sb.append("]");
         if (warning!=null) sb.append(warning);
      }
      return sb.toString();
   }
   static final Pattern                   ZAHLEN_AM_ENDE             =Pattern.compile("\\d+$");
   static public final Comparator<String> NUMMERN_SICHERER_COMPARATOR=                         //
            new Comparator<String>() {
               @Override
               public int compare(String o1, String o2) {
                  int normal=o1.compareTo(o2);
                  if (normal!=0) if (o1.length()!=o2.length()) {
                     Matcher m1=ZAHLEN_AM_ENDE.matcher(o1);
                     Matcher m2=ZAHLEN_AM_ENDE.matcher(o2);
                     if (m1.find()&&m2.find()) {                                               // beide enthalten Zahlen
                        try {
                           String r1="";
                           int i1=-1;
                           // if (m1.hitEnd()) {
                           i1=Integer.valueOf(m1.group());
                           r1=m1.replaceFirst("");
                           // }
                           String r2="";
                           int i2=-1;   //
                           m2.groupCount();
                           // if (m2.hitEnd()) {
                           i2=Integer.valueOf(m2.group());
                           r2=m2.replaceFirst("");
                           // }
                           if (r1.equals(r2)) return Integer.compare(i1, i2);
                        } catch (NumberFormatException e) {
                           System.out.println(o1+":"+o2);
                           e.printStackTrace();
                        }
                     }
                  }
                  return normal;
               }
            };
}

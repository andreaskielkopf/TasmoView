package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.SensorGraphPanel;

/** Singleton um die Daten des Programms zentral zu halten */
public class Data {
   /** Singleton */
   public static final Data                                  data               =new Data();
   /** gemeinsam verwendete Passwortfeld */
   private JPasswordField                                    passwordField;
   /** gemeinsam verwendetes Feld für den Username */
   private JTextField                                        userField;
   private ScanPanel                                         scanPanel;
   TasmoList                                                 tasmolist;
   /** Liste der gefundenen Tasmotas mit ihren Daten */
   public final TreeSet<Tasmota>                             tasmotas           =new TreeSet<Tasmota>();
   /** Liste der bisher gefundenen Sensoren */
   public final TreeSet<Sensor>                              sensoren           =new TreeSet<Sensor>();
   public final TreeSet<String>                              sensorTypen        =new TreeSet<String>();
   public final TreeSet<Tasmota>                             tasmotasMitSensoren=new TreeSet<Tasmota>();
   // TODO lokal zwischenspeichern
   /** Liste der offenen Suche von Tasmotas oder der offenen Refreshs */
   public final TreeSet<Tasmota>                             unconfirmed        =new TreeSet<Tasmota>();
   /** Die eigene IP dieses Rechners */
   public InetAddress                                        myIp               =null;
   /** spezielles TableModel mit wechselnden Tabellen und Überschriften für die Infoseite */
   public TasmoTableModell                                   dataModel          =null;
   /** 2D-ArrayList mit Uberschriften für die benannten Tabellen */
   public final LinkedHashMap<String, LinkedHashSet<String>> tablenames         =new LinkedHashMap<String, LinkedHashSet<String>>();
   /** Bitset mit den gefundenen tasmotas als Bit (nicht nochmal nach denen suchen) */
   public final BitSet                                       found_tasmotas     =new BitSet(256);
   private JList<Sensor>                                     sensorList;
   private SensorGraphPanel                                  sensorGraphPanel;
   // TODO lokal zwischenspeichern und holen
   static final String                                       USER               ="user";
   static final String                                       PASSWORD           ="password";
   public final Preferences                                  prefs              =Preferences.userNodeForPackage(TasmoView.class);
   /** Im Konstruktor werden Die festgelegte Tabellen mit ihren Überschriften definiert und eingetragen */
   private Data() {
      String[][] init= { // {"Tabellenname", "Spalte2", "Spalte3", "Spalte4", ...}
               {"Home", "Module", "Power", "Color", "LoadAvg", "LinkCount", "Uptime"},
               {"Health", "Uptime", "BootCount", "RestartReason", "LoadAvg", "Sleep", "MqttCount", "LinkCount", "Downtime", "RSSI"},
               {"Firmware", "Version", "Core", "SDK", "ProgramSize", "Free", "OtaUrl"},
               {"Wifi_", "Hostname", "Mac", "IPAddress", "Gateway", "SSId", "BSSId", "Channel", "RSSI", "LinkCount", "Downtime"},
               {"MQTT", "Topic", "FullTopic", "CommandTopic", "StatTopic", "TeleTopic", "FallbackTopic", "GroupTopic"}//
      };
      for (String[] spalten:init) {
         LinkedHashSet<String> tabelle     =new LinkedHashSet<String>();
         String                tabellenname=null;
         for (String spalte:spalten) {
            if (tabellenname==null) tabellenname=spalte;
            else tabelle.add(spalte);
         } // eintragen
         tablenames.put(tabellenname, tabelle);
      }
   }
   /** Prüfe die Ausstehenden Tasmotas bis lle entweder erkannt oder verworfen sind */
   synchronized public static void testUnconfirmed() {
      ArrayList<Tasmota> totest=new ArrayList<Tasmota>(data.unconfirmed);
      for (Tasmota t:totest) {
         Boolean test=t.isTasmota();
         if (test==null) continue;
         data.unconfirmed.remove(t);
         if (test) {
            data.found_tasmotas.set(t.ipPart);
            data.tasmotas.add(t);
            // Tasmota-Gerät gefunden
            System.out.println(t);
         }
      }
   }
   /** Konstrukor für das gemeinsam genutzte Feld */
   JPasswordField getPasswordField() {
      if (passwordField==null) {
         System.out.println(prefs.get(PASSWORD, ""));
         passwordField=new JPasswordField(prefs.get(PASSWORD, ""));
         // TODO optional lokal zwischenspeichern und holen
         passwordField.setColumns(10);
      }
      return passwordField;
   }
   /** Konstrukor für das gemeinsam genutzte Feld */
   JTextField getUserField() {
      if (userField==null) {
         // TODO lokal zwischenspeichern und holen
         System.out.println(prefs.get(USER, "admin"));
         userField=new JTextField(prefs.get(USER, "admin"));
         userField.setColumns(10);
      }
      return userField;
   }
   public ScanPanel getScanPanel() {
      if (scanPanel==null) {
         scanPanel=new ScanPanel();
      }
      return scanPanel;
   }
   public TasmoList getTasmoList() {
      if (tasmolist==null) {
         tasmolist=new TasmoList();
      }
      return tasmolist;
   }
   public JList<Sensor> getSensorList() {
      if (sensorList==null) {
         sensorList=new JList<Sensor>();
         // sensorList.setVisibleRowCount(10);
         sensorList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
               List<Sensor> sl=getSensorList().getSelectedValuesList();
               getSensorGraphPanel().setSensors(sl);
            }
         });
      }
      return sensorList;
   }
   public SensorGraphPanel getSensorGraphPanel() {
      if (sensorGraphPanel==null) {
         sensorGraphPanel=new SensorGraphPanel();
         sensorGraphPanel.setLayout(new BorderLayout(0, 0));
      }
      return sensorGraphPanel;
   }
}

package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.net.InetAddress;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import de.uhingen.kielkopf.andreas.tasmoview.grafik.JPowerPane;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.SensorGraphPanel;
import de.uhingen.kielkopf.andreas.tasmoview.table.TasmoList;
import de.uhingen.kielkopf.andreas.tasmoview.table.TasmoTableModell;

/** Singleton um die Daten des Programms zentral zu halten */
public class Data {
   /** Singleton */
   public static final Data                                                  data               =new Data();
   // TODO lokal zwischenspeichern und holen
   static final String                                                       USER               ="user";
   static final String                                                       PASSWORD           ="password";
   /** gemeinsam verwendetes Passwortfeld */
   private JPasswordField                                                    passwordField;
   /** gemeinsam verwendetes Feld für den Username */
   private JTextField                                                        userField;
   private ScanPanel                                                         scanPanel;
   public TasmoList                                                          tasmolist;
   /** Liste der gefundenen Tasmotas mit ihren Daten */
   public final ConcurrentSkipListSet<Tasmota>                               tasmotas           =new ConcurrentSkipListSet<>();
   /** Liste der gerade noch laufenden Anfragen */
   public final ConcurrentSkipListSet<CompletableFuture<String>>             anfragen           =                              //
            new ConcurrentSkipListSet<>((o1, o2) -> Integer.compare(hashCode(), o1.hashCode()));
   /** Liste der bisher gefundenen Sensoren */
   public final ConcurrentSkipListSet<Sensor>                                gesamtSensoren     =new ConcurrentSkipListSet<>();
   public final ConcurrentSkipListSet<String>                                sensorTypen        =new ConcurrentSkipListSet<>();
   public final ConcurrentSkipListSet<Tasmota>                               tasmotasMitSensoren=new ConcurrentSkipListSet<>();
   /** Die eigene IP dieses Rechners */
   public InetAddress                                                        myIp               =null;
   /** spezielles TableModel mit wechselnden Tabellen und Überschriften für die Infoseite */
   public TasmoTableModell                                                   dataModel          =null;
   /** 2D-ArrayList mit Uberschriften für die benannten Tabellen aller Geräte */
   public final ConcurrentSkipListMap<String, ConcurrentSkipListSet<String>> tableNames         =new ConcurrentSkipListMap<>();
   /** Bitset mit den gefundenen tasmotas als Bit (nicht nochmal nach denen suchen) */
   public final BitSet                                                       found_tasmotas     =new BitSet(256);
   private JList<Sensor>                                                     sensorJList;
   private SensorGraphPanel                                                  sensorGraphPanel;
   public final Preferences                                                  prefs              =Preferences
            .userNodeForPackage(TasmoView.class);
   public JPowerPane                                                         powerpane;
   /** Im Konstruktor werden Die festgelegte Tabellen mit ihren Überschriften definiert und eingetragen */
   private Data() {
      // TasmoList.recalculateColumnames();
   }
   /** Prüfe die Ausstehenden Tasmotas bis alle entweder erkannt oder verworfen sind */
   /** Konstrukor für das gemeinsam genutzte Feld */
   public JPasswordField getPasswordField() {
      if (passwordField == null) {
         System.out.println(prefs.get(PASSWORD, ""));
         passwordField=new JPasswordField(prefs.get(PASSWORD, ""));
         // TODO optional lokal zwischenspeichern und holen
         passwordField.setColumns(10);
      }
      return passwordField;
   }
   public ScanPanel getScanPanel() {
      if (scanPanel == null)
         scanPanel=new ScanPanel();
      return scanPanel;
   }
   public SensorGraphPanel getSensorGraphPanel() {
      if (sensorGraphPanel == null) {
         sensorGraphPanel=new SensorGraphPanel();
         sensorGraphPanel.setLayout(new BorderLayout(0, 0));
      }
      return sensorGraphPanel;
   }
   public JList<Sensor> getSensorJList() {
      if (sensorJList == null) {
         sensorJList=new JList<>(new DefaultListModel<Sensor>());
         sensorJList.addListSelectionListener(e -> {
            final List<Sensor> sl=getSensorJList().getSelectedValuesList();
            getSensorGraphPanel().setSensors(sl);
         });
      }
      return sensorJList;
   }
   public TasmoList getTasmoList() {
      if (tasmolist == null)
         tasmolist=new TasmoList();
      return tasmolist;
   }
   /** Konstrukor für das gemeinsam genutzte Feld */
   public JTextField getUserField() {
      if (userField == null) {
         // TODO lokal zwischenspeichern und holen
         System.out.println(prefs.get(USER, "admin"));
         userField=new JTextField(prefs.get(USER, "admin"));
         userField.setColumns(10);
      }
      return userField;
   }
}

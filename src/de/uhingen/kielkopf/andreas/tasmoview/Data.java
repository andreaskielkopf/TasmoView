package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.net.InetAddress;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.prefs.Preferences;

import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uhingen.kielkopf.andreas.tasmoview.grafik.JPowerPane;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.SensorGraphPanel;
import de.uhingen.kielkopf.andreas.tasmoview.table.TasmoList;
import de.uhingen.kielkopf.andreas.tasmoview.table.TasmoTableModell;

/** Singleton um die Daten des Programms zentral zu halten */
public class Data {
   /** Singleton */
   public static final Data                                                  data               =new Data();
   /** gemeinsam verwendetes Passwortfeld */
   private JPasswordField                                                    passwordField;
   /** gemeinsam verwendetes Feld für den Username */
   private JTextField                                                        userField;
   private ScanPanel                                                         scanPanel;
   public TasmoList                                                          tasmolist;
   /** Liste der gefundenen Tasmotas mit ihren Daten */
   public final ConcurrentSkipListSet<Tasmota>                               tasmotas           =new ConcurrentSkipListSet<>();
   /** Liste der gerade noch laufenden Anfragen */
   public final ConcurrentSkipListSet<CompletableFuture<String>>             anfragen           =                                                                   //
            new ConcurrentSkipListSet<CompletableFuture<String>>(new Comparator<CompletableFuture<String>>() {
               @Override
               public int compare(CompletableFuture<String> o1, CompletableFuture<String> o2) {
                  return Integer.compare(hashCode(), o1.hashCode());
               }
            });
   /** Liste der bisher gefundenen Sensoren */
   public final ConcurrentSkipListSet<Sensor>                                sensoren           =new ConcurrentSkipListSet<>();
   public final ConcurrentSkipListSet<String>                                sensorTypen        =new ConcurrentSkipListSet<>();
   public final ConcurrentSkipListSet<Tasmota>                               tasmotasMitSensoren=new ConcurrentSkipListSet<>();
   /** Die eigene IP dieses Rechners */
   public InetAddress                                                        myIp               =null;
   /** spezielles TableModel mit wechselnden Tabellen und Überschriften für die Infoseite */
   public TasmoTableModell                                                   dataModel          =null;
   /** 2D-ArrayList mit Uberschriften für die benannten Tabellen aller Geräte */
   public final ConcurrentSkipListMap<String, ConcurrentSkipListSet<String>> tableNames         =new ConcurrentSkipListMap<String, ConcurrentSkipListSet<String>>();
   /** Bitset mit den gefundenen tasmotas als Bit (nicht nochmal nach denen suchen) */
   public final BitSet                                                       found_tasmotas     =new BitSet(256);
   private JList<Sensor>                                                     sensorList;
   private SensorGraphPanel                                                  sensorGraphPanel;
   // TODO lokal zwischenspeichern und holen
   static final String                                                       USER               ="user";
   static final String                                                       PASSWORD           ="password";
   public final Preferences                                                  prefs              =Preferences.userNodeForPackage(TasmoView.class);
   public JPowerPane                                                         powerpane;
   /** Im Konstruktor werden Die festgelegte Tabellen mit ihren Überschriften definiert und eingetragen */
   private Data() {
      // TasmoList.recalculateColumnames();
   }
   /** Prüfe die Ausstehenden Tasmotas bis alle entweder erkannt oder verworfen sind */
   /** Konstrukor für das gemeinsam genutzte Feld */
   public JPasswordField getPasswordField() {
      if (passwordField==null) {
         System.out.println(prefs.get(PASSWORD, ""));
         passwordField=new JPasswordField(prefs.get(PASSWORD, ""));
         // TODO optional lokal zwischenspeichern und holen
         passwordField.setColumns(10);
      }
      return passwordField;
   }
   /** Konstrukor für das gemeinsam genutzte Feld */
   public JTextField getUserField() {
      if (userField==null) {
         // TODO lokal zwischenspeichern und holen
         System.out.println(prefs.get(USER, "admin"));
         userField=new JTextField(prefs.get(USER, "admin"));
         userField.setColumns(10);
      }
      return userField;
   }
   public ScanPanel getScanPanel() {
      if (scanPanel==null) scanPanel=new ScanPanel();
      return scanPanel;
   }
   public JList<Sensor> getSensorList() {
      if (sensorList==null) {
         sensorList=new JList<Sensor>();
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
   public TasmoList getTasmoList() {
      if (tasmolist==null) tasmolist=new TasmoList();
      return tasmolist;
   }
}

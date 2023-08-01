package de.uhingen.kielkopf.andreas.tasmoview.table;

import java.util.Collections;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

/** Ein Tablemodell für die veränderbare Tabelle */
public class TasmoTableModell extends AbstractTableModel {
   private static final long             serialVersionUID      =-730406069158230203L;
   static public final String[][]        SPALTEN_UEBERSCHRIFTEN= {                                                      //
            {"Home", "Module", "Power", "Color", "LoadAvg", "LinkCount", "Uptime"},
            {"Health", "Uptime", "BootCount", "RestartReason", "LoadAvg", "Sleep", "MqttCount", "LinkCount", "Downtime",
                     "RSSI"},
            {"Firmware", "Version", "Core", "SDK", "ProgramSize", "Free", "OtaUrl"},
            // disabled {"Wifi_", "Hostname", "Mac", "IPAddress", "Gateway", "SSId", "BSSId", "Channel", "RSSI",
            // "LinkCount", "Downtime"},
            {"MQTT", "Topic", "FullTopic", "CommandTopic", "StatTopic", "TeleTopic", "FallbackTopic", "GroupTopic"},    //
            {"Timers", "Timer1", "Timer2", "Timer3", "Timer4"},                                                         //
            {"Timer1"},                                                                                                 //
            {"Wifi"},
            // {"GPIOs1"}, {"GPIOs2"}, {"GPIOs3"}
   };
   /** Dieser Eintrag kommt immer in die erste Spalte */
   static final String                   DEVICE_NAME           ="DeviceName";
   /** Aktuelle Liste der Columnnames */
   private ConcurrentSkipListSet<String> columnNames;
   /** Liste zum Programmstart */
   private final String[]                cn                    = {"Module", "Topic", "ButtonTopic", "Power",
            "PowerOnState", "LedState", "LedMask", "", ""};
   public TasmoTableModell() {
      columnNames=new ConcurrentSkipListSet<>();
      Collections.addAll(columnNames, cn);
      setTable(null);
   }
   static void berechneSpalten() {
      if (Data.getData().tasmolist != null) {
         final JTable table=Data.getData().tasmolist.getTable();
         if (table == null)
            return;
         for (int c=0; c < table.getColumnModel().getColumnCount(); c++) {
            int w=0; // getColumnName(c).length();
            int mw=w;
            final int rc=table.getRowCount();
            for (int r=0; r < rc; r++) {
               final Object v=table.getValueAt(r, c);
               if (v == null)
                  continue;
               final int l=v.toString().length();
               if (l > mw)
                  mw=l;
               w+=l;
            }
            /** Der Wert soll zwischen dem Mittelwert und dem Maxwert liegen */
            w=(int) (20f * (0.5f + ((0.5f * w) / (rc + 0.01f)) + (0.3f * mw)));
            if (c < table.getColumnModel().getColumnCount())
               table.getColumnModel().getColumn(c).setPreferredWidth(w);
            // System.out.print(c + ":" + w + " ");
         }
      }
   }
   @Override
   public int getColumnCount() {
      return columnNames.size() + 1;
   }
   /* Ermittle den Columnname */
   @Override
   public String getColumnName(int col) {
      final ConcurrentSkipListSet<String> cnames=columnNames;
      // erste Splate ist immer der Gerätename
      if (col == 0)
         return DEVICE_NAME;
      // Wenn zu hohe Spalten angefragt werden
      if (col > cnames.size())
         return "@";
      return (String) cnames.toArray()[col - 1];
   }
   @Override
   /** Je gefundenem Tasmota eine Reihe */
   public int getRowCount() {
      return Data.getData().tasmotasD.size();
   }
   public Tasmota getTasmota(int rowIndex) {
      if (rowIndex >= getRowCount())
         return null;
      return (Tasmota) Data.getData().tasmotasD.values().toArray()[rowIndex];
   }
   /** Inhalte live aus den Daten ermitteln */
   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex >= getRowCount())
         return "#";
      if (columnIndex == 0) { return getTasmota(rowIndex).deviceName; }
      final String cname=getColumnName(columnIndex);
      return getTasmota(rowIndex).getValue(cname);
   }
   /**
    * Es wurde eine andere Tabelle für die Anzeige der Daten der Tasmotas ausgewählt
    *
    * Umstellen der Tabelle auf andere Daten. Der key für die Daten is jeweils der Name der Spalte
    */
   public void setTable(String name) {
      int row=-1;
      /** Hier muss eine Rekursion vermieden werden ! ==> Wenn Tasmolist nicht exisitiert einfach ignorieren */
      if (Data.getData().tasmolist != null)
         if (Data.getData().tasmolist.getTable() != null)
            row=Data.getData().tasmolist.getTable().getSelectedRow();
      final String key=Tasmota.toHtmlString(name);
      if ((columnNames == null) || (key == null)) {
         final ConcurrentSkipListSet<String> c=new ConcurrentSkipListSet<>();
         Collections.addAll(c, cn);
         columnNames=c;
         if (key == null) {// Aufruf im Konstruktor
            fireTableStructureChanged();
            return;
         }
      }
      if (Data.getData().tableNames.containsKey(key)) {// Datensatz aktivieren
         columnNames=Data.getData().tableNames.get(key);
         fireTableStructureChanged();
         if (row != -1)
            Data.getData().getTasmoList().getTable().setRowSelectionInterval(row, row);
      }
      berechneSpalten();
   }
}

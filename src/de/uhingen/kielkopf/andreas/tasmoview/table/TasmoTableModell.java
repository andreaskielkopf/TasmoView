package de.uhingen.kielkopf.andreas.tasmoview.table;

import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

/** Ein Tablemodell für die veränderbare Tabelle */
public class TasmoTableModell extends AbstractTableModel {
   private static final long             serialVersionUID      =-730406069158230203L;
   static public final String[][]        SPALTEN_UEBERSCHRIFTEN= {                                                                                          //
            {"Home", "Module", "Power", "Color", "LoadAvg", "LinkCount", "Uptime"},
            {"Health", "Uptime", "BootCount", "RestartReason", "LoadAvg", "Sleep", "MqttCount", "LinkCount", "Downtime", "RSSI"},
            {"Firmware", "Version", "Core", "SDK", "ProgramSize", "Free", "OtaUrl"},
            // disabled {"Wifi_", "Hostname", "Mac", "IPAddress", "Gateway", "SSId", "BSSId", "Channel", "RSSI", "LinkCount", "Downtime"},
            {"MQTT", "Topic", "FullTopic", "CommandTopic", "StatTopic", "TeleTopic", "FallbackTopic", "GroupTopic"},                                        //
            {"Timers", "Timer1", "Timer2", "Timer3", "Timer4"},                                                                                             //
            {"Timer1"},                                                                                                                                     //
            {"Wifi"},
            // {"GPIOs1"}, {"GPIOs2"}, {"GPIOs3"}
   };
   /** Aktuelle Liste der Columnnames */
   private ConcurrentSkipListSet<String> columnNames;
   /** Liste zum Programmstart */
   private final String[]                cn                    = {"Module", "Topic", "ButtonTopic", "Power", "PowerOnState", "LedState", "LedMask", "", ""};
   /** Dieser Eintrag kommt immer in die erste Spalte */
   static final String                   DEVICE_NAME           ="DeviceName";
   public TasmoTableModell() {
      super();
      setTable(null);
   }
   /**
    * Es wurde eine andere Tabelle für die Anzeige der Daten der Tasmotas ausgewählt
    * 
    * Umstellen der Tabelle auf andere Daten. Der key für die Daten is jeweils der Name der Spalte
    */
   public void setTable(String name) {
      int row=-1;
      /** Hier muss eine Rekursion vermieden werden ! ==> Wenn Tasmolist nicht exisitiert einfach ignorieren */
      if (Data.data.tasmolist!=null) if (Data.data.tasmolist.getTable()!=null) row=Data.data.tasmolist.getTable().getSelectedRow();
      String key=Tasmota.toHtmlString(name);
      if ((columnNames==null)||(key==null)) {
         ConcurrentSkipListSet<String> c=new ConcurrentSkipListSet<>();
         for (String string:cn)
            c.add(string);
         columnNames=c;
         if (key==null) {// Aufruf im Konstruktor
            fireTableStructureChanged();
            return;
         }
      }
      if (Data.data.tableNames.containsKey(key)) {// Datensatz aktivieren
         columnNames=Data.data.tableNames.get(key);
         fireTableStructureChanged();
         if (row!=-1) Data.data.getTasmoList().getTable().setRowSelectionInterval(row, row);
      }
      berechneSpalten();
   }
   /* Ermittle den Columnname */
   public String getColumnName(int col) {
      ConcurrentSkipListSet<String> cnames=columnNames;
      // erste Splate ist immer der Gerätename
      if (col==0) return DEVICE_NAME;
      // Wenn zu hohe Spalten angefragt werden
      if (col>cnames.size()) return "@";
      return (String) cnames.toArray()[col-1];
   }
   @Override
   /** Je gefundenem Tasmota eine Reihe */
   public int getRowCount() {
      return Data.data.tasmotas.size();
   }
   @Override
   public int getColumnCount() {
      return columnNames.size()+1;
   }
   /** Inhalte live aus den Daten ermitteln */
   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex==0) { return getTasmota(rowIndex).deviceName; }
      String cname=getColumnName(columnIndex);
      String erg  =getTasmota(rowIndex).getValue(cname);
      return erg;
   }
   public Tasmota getTasmota(int rowIndex) {
      return (Tasmota) Data.data.tasmotas.toArray()[rowIndex];
   }
   void berechneSpalten() {
      if (Data.data.tasmolist!=null) {
         JTable table=Data.data.tasmolist.getTable();
         if (table==null) return;
         for (int c=0; c<table.getColumnCount(); c++) {
            int w =0;                  // getColumnName(c).length();
            int mw=w;
            int rc=table.getRowCount();
            for (int r=0; r<rc; r++) {
               Object v=table.getValueAt(r, c);
               if (v==null) continue;
               int l=v.toString().length();
               if (l>mw) mw=l;
               w+=l;
            }
            /** Der Wert soll zwischen dem Mittelwert und dem Maxwert liegen */
            w=(int) (20f*(0.5f+0.5f*w/(rc+0.01f)+0.3f*mw));
            // if (c<table.getColumnModel().getColumnCount())
            table.getColumnModel().getColumn(c).setPreferredWidth(w);
            // System.out.print(c+":"+w+" ");
         }
      }
   }
}

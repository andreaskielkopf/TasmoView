package de.uhingen.kielkopf.andreas.tasmoview.table;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonArray;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonList;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;

/** Ein Tablemodell für die veränderbare Tabelle */
public class TasmoTableModell extends AbstractTableModel {
   private static final long     serialVersionUID=-730406069158230203L;
   /** Aktuelle Liste der Columnnames */
   private LinkedHashSet<String> columnNames;
   /** Liste zum Programmstart */
   private final String[]        cn              = {"Module", "Topic", "ButtonTopic", "Power", "PowerOnState", "LedState", "LedMask", "", ""};
   /** Dieser Eintrag kommt immer in die erste Spalte */
   static final String           FRIENDLY_NAME   ="FriendlyName";
   public TasmoTableModell() {
      super();
      setTable(null);
   }
   /** Umstellen der Tabelle auf andere Daten */
   public void setTable(String name) {
      int row=-1;
      /** Hier muss eine Rekursion vermieden werden ! ==> Wenn Tasmolist nicht exisitiert einfach ignorieren */
      // try {
      if (Data.data.tasmolist!=null) if (Data.data.tasmolist.getTable()!=null) row=Data.data.tasmolist.getTable().getSelectedRow();
      // } catch (NullPointerException e) {
      // e.printStackTrace();
      // }
      String key=Tasmota.toHtmlString(name);
      if ((columnNames==null)||(key==null)) {
         LinkedHashSet<String> c=new LinkedHashSet<String>();
         for (String string:cn)
            c.add(string);
         columnNames=c;
         if (key==null) {// Aufruf im Konstruktor
            fireTableStructureChanged();
            return;
         }
      }
      if (!Data.data.tablenames.containsKey(key)) {// Datensatz vorbereiten
         ArrayList<JsonObject> jos=new ArrayList<JsonObject>();
         for (Tasmota t:Data.data.tasmotas)
            jos.addAll(t.getAll(key));
         createTableHeaders(key, jos);
      }
      if (Data.data.tablenames.containsKey(key)) {// Datensatz aktivieren
         columnNames=Data.data.tablenames.get(key);
         fireTableStructureChanged();
         if (row!=-1) Data.data.getTasmoList().getTable().setRowSelectionInterval(row, row);
      }
      berechneSpalten();
   }
   /* Ermittle den Columnname */
   public String getColumnName(int col) {
      LinkedHashSet<String> cnames=columnNames;
      // erste Splate ist immer der Gerätename
      if (col==0) return FRIENDLY_NAME;
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
      String cname=getColumnName(columnIndex);
      String erg  =getTasmota(rowIndex).getValue(cname);
      return erg;
   }
   private void createTableHeaders(String key, ArrayList<JsonObject> jos) {
      LinkedHashSet<String> names=new LinkedHashSet<String>();
      for (JsonObject jo:jos) //
         if ((jo instanceof JsonList)||(jo instanceof JsonArray))//
            for (JsonObject j:(jo instanceof JsonList) ? ((JsonList) jo).list : ((JsonArray) jo).list)//
            if (j.name!=null) names.add(j.name);
      if (!names.isEmpty()) Data.data.tablenames.put(key, names);
   }
   public Tasmota getTasmota(int rowIndex) {
      return (Tasmota) Data.data.tasmotas.toArray()[rowIndex];
   }
   void berechneSpalten() {
      if (Data.data.tasmolist!=null) {
         JTable table=Data.data.tasmolist.getTable();
         if (table==null) return;
         for (int c=0; c<getColumnCount(); c++) {
            int w =0;            // getColumnName(c).length();
            int mw=w;
            int rc=getRowCount();
            for (int r=0; r<rc; r++) {
               int l=getValueAt(r, c).toString().length();
               if (l>mw) mw=l;
               w+=l;
            }
            /** Der Wert soll zwischen dem Mittelwert und dem Maxwert liegen */
            w=(int) (20f*(0.5f+0.5f*w/(rc+0.01f)+0.3f*mw));
            table.getColumnModel().getColumn(c).setPreferredWidth(w);
            // System.out.print(c+":"+w+" ");
         }
      }
   }
}

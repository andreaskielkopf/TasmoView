package de.uhingen.kielkopf.andreas.tasmoview;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.table.AbstractTableModel;

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
      try {
         if (Data.data.tasmolist!=null) row=Data.data.getTasmoList().getTable().getSelectedRow();
      } catch (NullPointerException e) {
         e.printStackTrace();
      }
      String key=Tasmota.toHtmlString(name);
      if ((columnNames==null)||(key==null)) {
         LinkedHashSet<String> c=new LinkedHashSet<String>();
         for (String string:cn) c.add(string);
         columnNames=c;
         if (key==null) {// Aufruf im Konstruktor
            fireTableStructureChanged();
            return;
         }
      }
      if (!Data.data.tablenames.containsKey(key)) {// Datensatz vorbereiten
         ArrayList<JsonObject> jos=new ArrayList<JsonObject>();
         for (Tasmota t:Data.data.tasmotas) jos.addAll(t.getAll(key));
         createTableHeaders(key, jos);
      }
      if (Data.data.tablenames.containsKey(key)) {// Datensatz aktivieren
         columnNames=Data.data.tablenames.get(key);
         fireTableStructureChanged();
         if (row!=-1) Data.data.getTasmoList().getTable().setRowSelectionInterval(row, row);
      }
   }
   /* BErmittle den Columnname */
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
      return getTasmota(rowIndex).getValue(getColumnName(columnIndex));
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
}

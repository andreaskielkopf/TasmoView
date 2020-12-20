package de.uhingen.kielkopf.andreas.tasmoview.table;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

/**
 * 
 * @author andreas kielkopf
 * @date 2019
 * 
 *       Ein Panel mit der Liste einiger Tasmota-Geräte die bei einem Scan gefunden wurden
 * 
 *       Im oberen Teil findet sich eine Auswahl an möglichen Ansichten der Tabelle die als Knöpfe ausgebildet sind. Jede Ansicht zeigt unterschiedliche Daten
 *       der Geräte an.
 * 
 *       Die Ansichten sind nicht vorgegeben, sondern werden von dem bestimmt, was das jeweilige Gerät als JSON-Daten übermittelt. Daraus werden sowohl die
 *       Titel, alos auch die breiten der Spalten ermittelt
 * 
 *       Wenn eine Reihe ausgewählt ist, kann das betreffende Gerät auf Knopfdruck mit dem Browser geöffnet werden
 */
public class TasmoList extends JPanel {
   private static final long serialVersionUID=4606263020682918366L;
   // static public final String[] TABELLEN_AUSWAHL_NAMEN= { //
   // "Home", "Health", "Firmware", "Wifi_", "MQTT", "Wifi", "PWM"};
   private JScrollPane       scrollPane;
   private JTable            table;
   private JList<String>     tableAuswahlJList;
   private JPanel            panel;
   private JButton           browserButton;
   private JPanel            panel_1;
   private JPanel            panel_2;
   /** Eine Tabellemit auswöhlbaren Ansichten */
   public TasmoList() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel(), BorderLayout.NORTH);
      add(getPanel_2(), BorderLayout.CENTER);
      recalculateColumnames();
   }
   /** Scrollpane um die Tabelle herrum */
   private JScrollPane getScrollPane() {
      if (scrollPane==null) {
         scrollPane=new JScrollPane(getTable());
         scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      }
      return scrollPane;
   }
   /** Tabelle mit auswählbarem Dateninhalt */
   JTable getTable() {
      if (table==null) {
         Data.data.dataModel=new TasmoTableModell();
         table=new JTable();
         table.setPreferredScrollableViewportSize(new Dimension(900, 400));
         table.setFont(new Font("Dialog", Font.PLAIN, 19));
         FontMetrics fm=table.getFontMetrics(table.getFont());
         table.setRowHeight((int) (1.15f*fm.getHeight()));
         table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         table.setModel(Data.data.dataModel);
         table.setFillsViewportHeight(true);
         table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
               boolean b=(getTable().getSelectedRow()!=-1);
               getBrowserButton().setEnabled(b);
            }
         });
      }
      return table;
   }
   public JList<String> getTableAuswahl() {
      if (tableAuswahlJList==null) {
         tableAuswahlJList=new JList<String>(new DefaultListModel<String>());
         tableAuswahlJList.setFixedCellWidth(120);
         tableAuswahlJList.setFixedCellHeight(25);
         tableAuswahlJList.setBorder(new TitledBorder(null, "Select Report", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         tableAuswahlJList.setSize(new Dimension(100, 20));
         tableAuswahlJList.setFont(new Font("Dialog", Font.BOLD, 15));
         tableAuswahlJList.setVisibleRowCount(3);
         tableAuswahlJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         tableAuswahlJList.setLayoutOrientation(JList.VERTICAL_WRAP);
         tableAuswahlJList.setSelectedIndex(0);
         tableAuswahlJList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
               String key=getTableAuswahl().getSelectedValue();
               /// Es wurde eine andere Tabellenansicht gewählt. Übergeben wird die
               if (key!=null) Data.data.dataModel.setTable(key);
            }
         });
      }
      return tableAuswahlJList;
   }
   private JPanel getPanel() {
      if (panel==null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getTableAuswahl(), BorderLayout.CENTER);
         panel.add(getPanel_1(), BorderLayout.EAST);
      }
      return panel;
   }
   public JButton getBrowserButton() {
      if (browserButton==null) {
         browserButton=new JButton("Browser");
         browserButton.setHorizontalTextPosition(SwingConstants.LEFT);
         browserButton.setIcon(new ImageIcon(TasmoList.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/tag-places.png")));
         browserButton.setEnabled(false);
         browserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               int row=getTable().getSelectedRow();
               if (row!=-1) try {
                  Tasmota t=Data.data.dataModel.getTasmota(row);
                  openURL(t.hostaddress);
               } catch (InterruptedException|IOException|NullPointerException|URISyntaxException e1) {
                  e1.printStackTrace();
               }
            }
         });
      }
      return browserButton;
   }
   public static void openURL(String host) throws InterruptedException, IOException, URISyntaxException {
      if (Desktop.isDesktopSupported()) {
         StringBuilder unpw=new StringBuilder();
         unpw.append(Data.data.getUserField().getText());
         unpw.append(":");
         unpw.append(Data.data.getPasswordField().getPassword());
         URI uri=new URI("http", unpw.toString(), host, -1, null, null, null);
         Desktop.getDesktop().browse(uri);
         /// http://andreas:akf4sonoff@192.168.178.28
      } else {
         System.err.println("Desktop is not suportet. trying Runtime.exec");
         String  os=System.getProperty("os.name");
         Runtime rt=Runtime.getRuntime();
         if (os.contains("win")) {
            rt.exec("rundll32 url.dll,FileProtocolHandler "+("http://"+host)).waitFor();
         } else
            if (os.contains("mac")||os.contains("darwin")) {
               String[] cmd= {"open", "http://"+host};
               rt.exec(cmd).waitFor();
            } else
               if (os.contains("nix")||os.contains("nux")||os.contains("aix")) {
                  String[] cmd= {"xdg-open", "http://"+host};
                  rt.exec(cmd).waitFor();
               } else {
                  System.err.println("Browser-start not supported:"+os);
               }
      }
   }
   private JPanel getPanel_1() {
      if (panel_1==null) {
         panel_1=new JPanel();
         panel_1.setBorder(new TitledBorder(null, "put into Browser", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getBrowserButton(), BorderLayout.CENTER);
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2==null) {
         panel_2=new JPanel();
         panel_2.setLayout(new BorderLayout(0, 0));
         panel_2.setBorder(new TitledBorder(null, "Report", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_2.add(getScrollPane(), BorderLayout.CENTER);
      }
      return panel_2;
   }
   static public void recalculateColumnames() {
      if (Data.data==null) return;
      ConcurrentSkipListMap<String, ConcurrentSkipListSet<String>> tabellen=Data.data.tableNames;
      for (String[] spalten:TasmoTableModell.SPALTEN_UEBERSCHRIFTEN) {
         ConcurrentSkipListSet<String> tabelle=null;
         for (String spalte:spalten)
            if (tabelle==null) { // Der erste Eintrag ist der Tabellenname
               tabellen.putIfAbsent(spalte, new ConcurrentSkipListSet<String>(Tasmota.NUMMERN_SICHERER_COMPARATOR)); // neuen Typ von Tabelle eintragen falls
                                                                                                                     // erforderlich
               tabelle=tabellen.get(spalte);
            } else {
               tabelle.add(spalte);// Weitere Einträge sind die Überschriften der Spalten
            }
      }
   }
}

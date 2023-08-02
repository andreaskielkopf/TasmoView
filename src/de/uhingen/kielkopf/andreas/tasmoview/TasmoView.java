package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.tasmoview.grafik.JPowerPane;

public class TasmoView {
   private JFrame                 frame;
   private JLabel                 cLabel;
   private JPanel                 panelTable;
   private JPanel                 panelScan;
   private JTabbedPane            tabbedPane;
   private JPanel                 panel;
   private JPanel                 panel_1;
   private JPowerPane             panel_2;
   private SensorPanel            sensorPanel;
   private JPanel                 panel_3;
   static public volatile boolean keepRunning=true;
   /**
    * Launch the application.
    * 
    * @wbp.parser.entryPoint
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            TasmoView window=new TasmoView();
            Data.getData().getScanPanel().getScanButton().doClick();
            window.frame.setVisible(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }
   /** Create the application. */
   public TasmoView() {
      initialize();
   }
   /**
    * Initialize the contents of the frame.
    * 
    * @wbp.parser.entryPoint
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(2100, 100, 1200, 800);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(getLabelC(), BorderLayout.SOUTH);
      frame.getContentPane().add(getTabbedPane(), BorderLayout.CENTER);
   }
   private JLabel getLabelC() {
      if (cLabel == null) {
         cLabel=new JLabel(
                  "TasmoView 0.5  ©2023 by Andreas Kielkopf (All source is included in JAR-file)   https://github.com/andreaskielkopf/TasmoView ");
      }
      return cLabel;
   }
   private JPanel getPanelTable() {
      if (panelTable == null) {
         panelTable=new JPanel();
         panelTable.setBorder(
                  new TitledBorder(null, "Found Devices", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelTable.setLayout(new BorderLayout(0, 0));
         panelTable.add(Data.getData().getTasmoList(), BorderLayout.CENTER);
      }
      return panelTable;
   }
   private JPanel getPanelScan() {
      if (panelScan == null) {
         panelScan=new JPanel();
         panelScan.setBorder(
                  new TitledBorder(null, "Scan for Devices", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelScan.setLayout(new BorderLayout(0, 0));
         panelScan.add(Data.getData().getScanPanel(), BorderLayout.NORTH);
      }
      return panelScan;
   }
   private JTabbedPane getTabbedPane() {
      if (tabbedPane == null) {
         tabbedPane=new JTabbedPane(SwingConstants.BOTTOM);
         tabbedPane.addTab("Scan", null, getPanel(), null);
         tabbedPane.setEnabledAt(0, true);
         tabbedPane.addTab("Sensors", null, getPanel_1(), null);
         tabbedPane.setEnabledAt(1, true);
         tabbedPane.addTab("Power", null, getPanel_2(), null);
         tabbedPane.setEnabledAt(2, true);
         tabbedPane.addTab("New tab", null, getPanel_3(), null);
         tabbedPane.setEnabledAt(3, false);
      }
      return tabbedPane;
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getPanelTable(), BorderLayout.CENTER);
         panel.add(getPanelScan(), BorderLayout.NORTH);
      }
      return panel;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getSensorPanel(), BorderLayout.CENTER);
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPowerPane();
         Data.getData().powerpane=panel_2;
         // panel_2.setLayout(new BorderLayout(0, 0));
      }
      return panel_2;
   }
   private JPanel getPanel_3() {
      if (panel_3 == null) {
         panel_3=new JPanel();
         panel_3.setLayout(new BorderLayout(0, 0));
      }
      return panel_3;
   }
   private SensorPanel getSensorPanel() {
      if (sensorPanel == null) {
         sensorPanel=new SensorPanel();
      }
      return sensorPanel;
   }
}

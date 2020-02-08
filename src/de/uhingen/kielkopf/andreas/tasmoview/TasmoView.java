package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

public class TasmoView {
   private JFrame      frame;
   private JLabel      cLabel;
   private JPanel      panelT;
   private JPanel      panelS;
   private JTabbedPane tabbedPane;
   private JPanel      panel;
   private JPanel      panel_1;
   private JPanel      panel_2;
   private SensorPanel sensorPanel;
   /** Launch the application. */
   public static void main(String[] args) {
      EventQueue.invokeLater(new Runnable() {
         public void run() {
            try {
               TasmoView window=new TasmoView();
               Data.data.getScanPanel().getScanButton().doClick();
               window.frame.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }
   /** Create the application. */
   public TasmoView() {
      initialize();
   }
   /** Initialize the contents of the frame. */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 1200, 800);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(getLabelC(), BorderLayout.SOUTH);
      frame.getContentPane().add(getTabbedPane(), BorderLayout.CENTER);
   }
   private JLabel getLabelC() {
      if (cLabel==null) {
         cLabel=new JLabel("TasmoView 0.1   ©2020 by Andreas Kielkopf (All source is included in JAR-file)   https://github.com/andreaskielkopf/TasmoView ");
      }
      return cLabel;
   }
   private JPanel getPanelT() {
      if (panelT==null) {
         panelT=new JPanel();
         panelT.setBorder(new TitledBorder(null, "Found Devices", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelT.setLayout(new BorderLayout(0, 0));
         panelT.add(Data.data.getTasmoList(), BorderLayout.CENTER);
      }
      return panelT;
   }
   private JPanel getPanelS() {
      if (panelS==null) {
         panelS=new JPanel();
         panelS.setBorder(new TitledBorder(null, "Scan for Devices", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelS.setLayout(new BorderLayout(0, 0));
         panelS.add(Data.data.getScanPanel(), BorderLayout.NORTH);
      }
      return panelS;
   }
   private JTabbedPane getTabbedPane() {
      if (tabbedPane==null) {
         tabbedPane=new JTabbedPane(JTabbedPane.BOTTOM);
         tabbedPane.addTab("Scan", null, getPanel(), null);
         tabbedPane.setEnabledAt(0, true);
         tabbedPane.addTab("Sensors", null, getPanel_1(), null);
         tabbedPane.setEnabledAt(1, true);
         tabbedPane.addTab("New tab", null, getPanel_2(), null);
         tabbedPane.setEnabledAt(2, false);
      }
      return tabbedPane;
   }
   private JPanel getPanel() {
      if (panel==null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getPanelT(), BorderLayout.CENTER);
         panel.add(getPanelS(), BorderLayout.NORTH);
      }
      return panel;
   }
   private JPanel getPanel_1() {
      if (panel_1==null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getSensorPanel(), BorderLayout.CENTER);
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2==null) {
         panel_2=new JPanel();
         panel_2.setLayout(new BorderLayout(0, 0));
      }
      return panel_2;
   }
   private SensorPanel getSensorPanel() {
      if (sensorPanel==null) {
         sensorPanel=new SensorPanel();
      }
      return sensorPanel;
   }
}

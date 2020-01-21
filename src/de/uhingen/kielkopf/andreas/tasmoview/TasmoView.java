package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TasmoView {
   private JFrame frame;
   private JLabel cLabel;
   private JPanel panelT;
   private JPanel panelS;
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
      frame.setBounds(100, 100, 1000, 500);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(getLabelC(), BorderLayout.SOUTH);
      frame.getContentPane().add(getPanelT(), BorderLayout.CENTER);
      frame.getContentPane().add(getPanelS(), BorderLayout.NORTH);
   }
   private JLabel getLabelC() {
      if (cLabel==null) {
         cLabel=new JLabel("©2020 by Andreas Kielkopf (All source is included in JAR-file)");
      }
      return cLabel;
   }
   private JPanel getPanelT() {
      if (panelT==null) {
         panelT=new JPanel();
         panelT.setLayout(new BorderLayout(0, 0));
         panelT.add(Data.data.getTasmoList(), BorderLayout.CENTER);
      }
      return panelT;
   }
   private JPanel getPanelS() {
      if (panelS==null) {
         panelS=new JPanel();
         panelS.setLayout(new BorderLayout(0, 0));
         panelS.add(Data.data.getScanPanel(), BorderLayout.NORTH);
      }
      return panelS;
   }
}

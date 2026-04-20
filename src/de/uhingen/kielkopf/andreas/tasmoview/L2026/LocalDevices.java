/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026;

import java.awt.*;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.uhingen.kielkopf.andreas.beans.gui.FrameHelper;

/**
 * @author Andreas Kielkopf
 *
 */
public class LocalDevices extends JFrame {
   private static final long    serialVersionUID=-7051259598258605188L;
   private DevicePanel          panel;
   final private NetworkScanner ns;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            var window=new LocalDevices();
            window.setVisible(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }
   /**
    * Create the application.
    */
   public LocalDevices() {
      ns=new NetworkScanner(getClass()).logOutput();
      initialize();
      FrameHelper.restore(this);
   }
   /**
    * Initialize the contents of the frame.
    */
   private void initialize() {
      setBounds(100, 100, 800, 650);
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      getContentPane().add(getPanel(), BorderLayout.CENTER);
   }
   private DevicePanel getPanel() {
      if (panel == null) {
         panel=new DevicePanel(ns);
         panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
      }
      return panel;
   }
}

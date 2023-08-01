package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.*;
import java.net.*;
import java.util.prefs.BackingStoreException;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.tasmoview.tasks.TasmoScanner;

/** Grafisches Element zur Steuerung und Kontrolle des Scans nach Tasmota-Geröten im localen Netzwerk */
public class ScanPanel extends JPanel {
   private static final long serialVersionUID=4194405156596011563L;
   /** Knopf zum start/stop des scans nach neuen Geräten */
   private JToggleButton     scanButton;
   /** Anzeige der eigenen IP */
   private JLabel            ipLabel;
   /** Platz für den Username und das Passwort */
   private JLabel            userLabel;
   private JLabel            passwordLabel;
   /** Knopf zum Refresh der vorhandenen Daten aller bekannten Tasmotas */
   private JToggleButton     refreshButton;
   /** Fortschrittsbalken des Scan oder Refresh */
   private JProgressBar      progressBar;
   /** Das gesamte ScanPanel */
   private JPanel            scanPanel;
   private JButton           btnStorePWD;
   protected TasmoScanner    tasmoScanner;
   public ScanPanel() {
      setLayout(new BorderLayout(0, 0));
      add(getProgressBar(), BorderLayout.CENTER);
      add(getScanPanel(), BorderLayout.NORTH);
   }
   private JButton getBtnStorePWD() {
      if (btnStorePWD == null) {
         btnStorePWD=new JButton("store User and Password");
         btnStorePWD.addActionListener(e -> {
            try {
               Data.getData().prefs.put(Data.USER, Data.getData().getUserField().getText());
               /**
                * Das ist nicht 100% sicher weil der String im Speicher bleibt bis das Programm endet. Aber da das
                * Passwort sowieso unverschlßsselt in den Preferences landet ist das nicht schlimm. Das Passwort kann eh
                * auch aus den HTML-Anfragen extrahiert werden
                */
               Data.getData().prefs.put(Data.PASSWORD, new String(Data.getData().getPasswordField().getPassword()));
               Data.getData().prefs.flush();
               System.out.println("Username and Password stored");
            } catch (final BackingStoreException e1) {
               e1.printStackTrace();
            }
         });
      }
      return btnStorePWD;
   }
   /** Ermittle die lokale IP und trage sie zum Nutzen für alle ein */
   private JLabel getIPLabel() {
      if (ipLabel == null) {
         ipLabel=new JLabel();
         try (final DatagramSocket socket=new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.1"), 10003);
            Data.getData().myIp=socket.getLocalAddress();
            if (Data.getData().myIp != null)
               ipLabel.setText("from " + Data.getData().myIp.getHostAddress());
         } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
         }
         if (Data.getData().myIp == null)
            System.err.println("Die IP konnte nicht ermittelt werden");
      }
      return ipLabel;
   }
   private JLabel getPasswordLabel() {
      if (passwordLabel == null) {
         passwordLabel=new JLabel("HTML-password");
      }
      return passwordLabel;
   }
   /** Fortschrittsbalken für San und Refresh */
   private JProgressBar getProgressBar() {
      if (progressBar == null) {
         progressBar=new JProgressBar();
         progressBar.setEnabled(false);
         progressBar.setMaximum(255);
         progressBar.setStringPainted(true);
      }
      return progressBar;
   }
   /** Knopf um einen Refresh einzuleiten oder abzubrechen */
   private JToggleButton getRefreshButton() {
      if (refreshButton == null) {
         refreshButton=new JToggleButton("Refresh");
         refreshButton.setToolTipText("Refresh Data on already found Devices");
         refreshButton.setSelectedIcon(new ImageIcon(
                  ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/process-stop.png")));
         refreshButton.setIcon(
                  new ImageIcon(ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/edit-find.png")));
         refreshButton.setPreferredSize(new Dimension(140, 40));
         refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
         refreshButton.addActionListener(e -> {
            Data.getData().getTasmoList().getBrowserButton().setEnabled(false);
            // final JToggleButton btn=(JToggleButton) e.getSource();
            getScanButton().setEnabled(!refreshButton.isSelected());
            // if ((tasmoScanner == null) || tasmoScanner.isDone() || tasmoScanner.isCancelled()) {
            scan(true);
            refreshButton.setSelected(true);
            // } else {
            // tasmoScanner.cancel(true);
            // refreshButtonbtn.setSelected(false);
            // }
         });
      }
      return refreshButton;
   }
   /** Knopf um den Scan einzuleiten oder ihn abzubrechen */
   public JToggleButton getScanButton() {
      if (scanButton == null) {
         scanButton=new JToggleButton("Scan");
         scanButton.setToolTipText("Scan for new Devices");
         scanButton.setIcon(
                  new ImageIcon(ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/edit-find.png")));
         scanButton.setSelectedIcon(new ImageIcon(
                  ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/process-stop.png")));
         scanButton.setPreferredSize(new Dimension(120, 40));
         scanButton.setAlignmentX(Component.CENTER_ALIGNMENT);
         scanButton.addActionListener(e -> {
            Data.getData().getTasmoList().getBrowserButton().setEnabled(false);
            // final JToggleButton btn=(JToggleButton) e.getSource();
            getRefreshButton().setEnabled(!scanButton.isSelected());
            // if (tasmoScanner == null) || tasmoScanner.isDone() || tasmoScanner.isCancelled()) {
            scan(false);
            scanButton.setSelected(true);
            // } else {
            // tasmoScanner.cancel(true);
            // btn.setSelected(false);
            // }
         });
      }
      return scanButton;
   }
   private JPanel getScanPanel() {
      if (scanPanel == null) {
         scanPanel=new JPanel();
         final FlowLayout fl_scanPanel=new FlowLayout(FlowLayout.LEFT, 5, 5);
         scanPanel.add(getIPLabel());
         scanPanel.setLayout(fl_scanPanel);
         scanPanel.add(getScanButton());
         scanPanel.add(getRefreshButton());
         scanPanel.add(getUserLabel());
         scanPanel.add(Data.getData().getUserField());
         scanPanel.add(getPasswordLabel());
         scanPanel.add(Data.getData().getPasswordField());
         scanPanel.add(getBtnStorePWD());
      }
      return scanPanel;
   }
   private JLabel getUserLabel() {
      if (userLabel == null) {
         userLabel=new JLabel("HTML-user");
      }
      return userLabel;
   }
   /**
    * Der eigentliche SCAn lauft in einem extra thread
    */
   protected void scan(boolean rescan) {
      tasmoScanner=TasmoScanner.getTasmoScanner(rescan, getProgressBar(), getScanButton(), getRefreshButton());
   }
}

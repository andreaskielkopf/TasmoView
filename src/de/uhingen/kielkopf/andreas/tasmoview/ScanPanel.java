package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;

/** Grafisches Element zur Steuerung und Kontrolle des Scans nach Tasmota-Geröten im localen Netzwerk */
public class ScanPanel extends JPanel {
   private static final long serialVersionUID=4194405156596011563L;
   /** Zeitabstand eim Scan */
   private static final int  ABSTAND_IN_MS   =100;
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
   /** Hilfsvariablen während der Suche */
   private BitSet            tosearch        =new BitSet(256);
   private BitSet            posible         =new BitSet(256);     // Hinweise aus der letzten Suche
   /** Timer der die Aktionen während der Suche oder dem Refresh abwickelt */
   private Timer             scanTimer       =null;
   /** Das gesamte ScanPanel */
   private JPanel            scanPanel;
   public ScanPanel() {
      setLayout(new BorderLayout(0, 0));
      add(getProgressBar(), BorderLayout.SOUTH);
      add(getScanPanel(), BorderLayout.CENTER);
   }
   /** Ermittle die lokale IP und trage sie zum Nutzen für alle ein */
   private JLabel getIPLabel() {
      if (ipLabel==null) {
         ipLabel=new JLabel();
         try (final DatagramSocket socket=new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.1"), 10003);
            Data.data.myIp=socket.getLocalAddress();
            if (Data.data.myIp!=null) ipLabel.setText("from "+Data.data.myIp.getHostAddress());
         } catch (SocketException|UnknownHostException e) {
            e.printStackTrace();
         }
         if (Data.data.myIp==null) System.err.println("Die IP konnte nicht ermittelt werden");
      }
      return ipLabel;
   }
   /** Knopf um den Scan einzuleiten oder ihn abzubrechen */
   public JToggleButton getScanButton() {
      if (scanButton==null) {
         scanButton=new JToggleButton("Scan");
         scanButton.setIcon(new ImageIcon(ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/edit-find.png")));
         scanButton.setSelectedIcon(new ImageIcon(ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/process-stop.png")));
         scanButton.setPreferredSize(new Dimension(120, 40));
         scanButton.setAlignmentX(Component.CENTER_ALIGNMENT);
         scanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               Data.data.getTasmoList().getBrowserButton().setEnabled(false);
               JToggleButton btn=(JToggleButton) e.getSource();
               getRefreshButton().setEnabled(!btn.isSelected());
               if (tosearch.isEmpty()) {
                  tosearch.set(0, 256);
                  if (Data.data.myIp!=null) tosearch.clear(Data.data.myIp.getAddress()[3]);
               }
               BitSet firstbits=(BitSet) posible.clone();
               firstbits.and(tosearch); // nur was nicht schon bearbeitet wurde
               BitSet secondbits=(BitSet) tosearch.clone();
               secondbits.andNot(firstbits); // Rest
               List<Integer> first=firstbits.stream().boxed().collect(Collectors.toList());
               Collections.shuffle(first);
               List<Integer> second=secondbits.stream().boxed().collect(Collectors.toList());
               Collections.shuffle(second);
               first.addAll(second);
               scan(btn, first);
            }
         });
      }
      return scanButton;
   }
   /** Knopf um einen Refresh einzuleiten oder abzubrechen */
   private JToggleButton getRefreshButton() {
      if (refreshButton==null) {
         refreshButton=new JToggleButton("Refresh");
         refreshButton.setSelectedIcon(new ImageIcon(ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/process-stop.png")));
         refreshButton.setIcon(new ImageIcon(ScanPanel.class.getResource("/de/uhingen/kielkopf/andreas/tasmoview/edit-find.png")));
         refreshButton.setPreferredSize(new Dimension(140, 40));
         refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
         refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               Data.data.getTasmoList().getBrowserButton().setEnabled(false);
               JToggleButton btn=(JToggleButton) e.getSource();
               getScanButton().setEnabled(!btn.isSelected());
               if (tosearch.isEmpty()) {
                  tosearch.set(0, 256);
                  if (Data.data.myIp!=null) tosearch.clear(Data.data.myIp.getAddress()[3]);
               }
               BitSet        firstbits=(BitSet) Data.data.found_tasmotas.clone();
               List<Integer> first    =firstbits.stream().boxed().collect(Collectors.toList());
               Collections.shuffle(first);
               scan(btn, first);
            }
         });
      }
      return refreshButton;
   }
   /** Der eigentliche Scanvargang wird in einem Timer organisiert */
   protected void scan(final JToggleButton btn, List<Integer> list) {
      if (scanTimer!=null) scanTimer.cancel();
      if (!btn.isSelected()) { return; }
      scanTimer=new Timer("Scan4Tasmota");
      int offset=0;
      getProgressBar().setMaximum(list.size());
      for (Integer i:list) { // Suche ein bestimmtes Gerät
         final int o=++offset;
         scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
               // System.out.println(i);
               getProgressBar().setString(Integer.toString(i));
               getProgressBar().setValue(o);
               if (Data.data.myIp!=null) {
                  Tasmota.scanFor(i);
                  tosearch.clear(i);
               }
            }
         }, ABSTAND_IN_MS*offset);
      }
      // Schließe die Suche ab, indem gewartet wird, bis die Antworten da sind um sie auszuwerten
      scanTimer.schedule(new TimerTask() {
         @Override
         public void run() {
            getProgressBar().setIndeterminate(true);
            getProgressBar().setString("please wait up to 1 minute");
            while (!Data.data.unconfirmed.isEmpty()) {
               Data.testUnconfirmed();
               try {
                  Thread.sleep(ABSTAND_IN_MS);
               } catch (InterruptedException e) {}
            }
            getProgressBar().setIndeterminate(false);
            getProgressBar().setValue(getProgressBar().getMaximum());
            btn.setSelected(false);
            getScanButton().setEnabled(true);
            getRefreshButton().setEnabled(true);
            for (Tasmota t:Data.data.tasmotas) {
               System.out.println(t);
            }
            getProgressBar().setString("ready");
         }
      }, ABSTAND_IN_MS*++offset);
   }
   /** Fortschrittsbalken für San und Refresh */
   private JProgressBar getProgressBar() {
      if (progressBar==null) {
         progressBar=new JProgressBar();
         progressBar.setEnabled(false);
         progressBar.setMaximum(255);
         progressBar.setStringPainted(true);
      }
      return progressBar;
   }
   private JPanel getScanPanel() {
      if (scanPanel==null) {
         scanPanel=new JPanel();
         scanPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5));
         scanPanel.add(getScanButton());
         scanPanel.add(getIPLabel());
         scanPanel.add(getUserLabel());
         scanPanel.add(Data.data.getUserField());
         scanPanel.add(getPasswordLabel());
         scanPanel.add(Data.data.getPasswordField());
         scanPanel.add(getRefreshButton());
      }
      return scanPanel;
   }
   private JLabel getUserLabel() {
      if (userLabel==null) {
         userLabel=new JLabel("HTML-user");
      }
      return userLabel;
   }
   private JLabel getPasswordLabel() {
      if (passwordLabel==null) {
         passwordLabel=new JLabel("HTML-password");
      }
      return passwordLabel;
   }
}

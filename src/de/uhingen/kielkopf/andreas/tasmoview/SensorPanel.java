package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.tasmoview.tasks.DataLogger;
import de.uhingen.kielkopf.andreas.tasmoview.tasks.SensorScanner;
import de.uhingen.kielkopf.andreas.tasmoview.tasks.TasmoScanner;

public class SensorPanel extends JPanel {
   private static final long serialVersionUID=-349511026033574886L;
   TreeSet<Tasmota>          tasmotasPoll    =new TreeSet<Tasmota>();
   private JPanel            selectionPanel;
   private JPanel            refreshPanel;
   private JLabel            refreshLabel;
   private JSpinner          refreshSpinner;
   private SensorScanner     sensorscanner;
   /** Liste der offenen Suche von Tasmotas oder der offenen Refreshs */
   private JLabel            lblSekunden;
   private JLabel            saveLabel;
   private JLabel            lblMinuten;
   private JSpinner          saveSpinner;
   private DataLogger        datalogger;
   private JLabel            lblLastRead;
   private JLabel            lblLastSaved;
   /**
    * Create the panel.
    */
   public SensorPanel() {
      setLayout(new BorderLayout());
      add(getSensorSelectPanel(), BorderLayout.WEST);
      add(getRefreshPanel(), BorderLayout.NORTH);
      add(Data.data.getSensorGraphPanel(), BorderLayout.CENTER);
      sensorscanner=new SensorScanner(getRefreshSpinner(), getLblLastRead());
      TasmoScanner.pool.submit(sensorscanner);
      datalogger=new DataLogger(getSaveSpinner(), getLblLastSaved());
      TasmoScanner.pool.submit(datalogger);
   }
   private JPanel getSensorSelectPanel() {
      if (selectionPanel==null) {
         selectionPanel=new JPanel();
         selectionPanel.setName("selectionPanel");
         selectionPanel.setBorder(new TitledBorder(null, "Display", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         selectionPanel.setLayout(new BorderLayout(0, 0));
         selectionPanel.add(Data.data.getSensorJList(), BorderLayout.CENTER);
      }
      return selectionPanel;
   }
   private JPanel getRefreshPanel() {
      if (refreshPanel==null) {
         refreshPanel=new JPanel();
         refreshPanel.setBorder(
                  new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Refresh", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
         GridBagLayout gbl_refreshPanel=new GridBagLayout();
         gbl_refreshPanel.columnWidths=new int[] {73, 73, 73, 73, 73, 73, 73, 73, 0};
         gbl_refreshPanel.rowHeights=new int[] {20, 0};
         gbl_refreshPanel.columnWeights=new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
         gbl_refreshPanel.rowWeights=new double[] {0.0, Double.MIN_VALUE};
         refreshPanel.setLayout(gbl_refreshPanel);
         GridBagConstraints gbc_lblLastRead=new GridBagConstraints();
         gbc_lblLastRead.fill=GridBagConstraints.BOTH;
         gbc_lblLastRead.insets=new Insets(0, 0, 0, 5);
         gbc_lblLastRead.gridx=0;
         gbc_lblLastRead.gridy=0;
         refreshPanel.add(getLblLastRead(), gbc_lblLastRead);
         GridBagConstraints gbc_refreshLabel=new GridBagConstraints();
         gbc_refreshLabel.fill=GridBagConstraints.BOTH;
         gbc_refreshLabel.insets=new Insets(0, 0, 0, 5);
         gbc_refreshLabel.gridx=1;
         gbc_refreshLabel.gridy=0;
         refreshPanel.add(getRefreshLabel(), gbc_refreshLabel);
         GridBagConstraints gbc_refreshSpinner=new GridBagConstraints();
         gbc_refreshSpinner.fill=GridBagConstraints.BOTH;
         gbc_refreshSpinner.insets=new Insets(0, 0, 0, 5);
         gbc_refreshSpinner.gridx=2;
         gbc_refreshSpinner.gridy=0;
         refreshPanel.add(getRefreshSpinner(), gbc_refreshSpinner);
         GridBagConstraints gbc_lblSekunden=new GridBagConstraints();
         gbc_lblSekunden.fill=GridBagConstraints.BOTH;
         gbc_lblSekunden.insets=new Insets(0, 0, 0, 5);
         gbc_lblSekunden.gridx=3;
         gbc_lblSekunden.gridy=0;
         refreshPanel.add(getLblSekunden(), gbc_lblSekunden);
         GridBagConstraints gbc_lblLastSaved=new GridBagConstraints();
         gbc_lblLastSaved.fill=GridBagConstraints.BOTH;
         gbc_lblLastSaved.insets=new Insets(0, 0, 0, 5);
         gbc_lblLastSaved.gridx=4;
         gbc_lblLastSaved.gridy=0;
         refreshPanel.add(getLblLastSaved(), gbc_lblLastSaved);
         GridBagConstraints gbc_saveLabel=new GridBagConstraints();
         gbc_saveLabel.fill=GridBagConstraints.BOTH;
         gbc_saveLabel.insets=new Insets(0, 0, 0, 5);
         gbc_saveLabel.gridx=5;
         gbc_saveLabel.gridy=0;
         refreshPanel.add(getSaveLabel(), gbc_saveLabel);
         GridBagConstraints gbc_saveSpinner=new GridBagConstraints();
         gbc_saveSpinner.fill=GridBagConstraints.BOTH;
         gbc_saveSpinner.insets=new Insets(0, 0, 0, 5);
         gbc_saveSpinner.gridx=6;
         gbc_saveSpinner.gridy=0;
         refreshPanel.add(getSaveSpinner(), gbc_saveSpinner);
         GridBagConstraints gbc_lblMinuten=new GridBagConstraints();
         gbc_lblMinuten.fill=GridBagConstraints.BOTH;
         gbc_lblMinuten.gridx=7;
         gbc_lblMinuten.gridy=0;
         refreshPanel.add(getLblMinuten(), gbc_lblMinuten);
      }
      return refreshPanel;
   }
   private JLabel getRefreshLabel() {
      if (refreshLabel==null) {
         refreshLabel=new JLabel("Refresh Sensordata every");
      }
      return refreshLabel;
   }
   private JSpinner getRefreshSpinner() {
      if (refreshSpinner==null) {
         refreshSpinner=new JSpinner();
         refreshSpinner.setAlignmentX(Component.RIGHT_ALIGNMENT);
         refreshSpinner.setModel(new SpinnerNumberModel(10, 1, 120, 1));
      }
      return refreshSpinner;
   }
   private JSpinner getSaveSpinner() {
      if (saveSpinner==null) {
         saveSpinner=new JSpinner();
         saveSpinner.setModel(new SpinnerNumberModel(5, 1, 60, 1));
      }
      return saveSpinner;
   }
   private JLabel getLblSekunden() {
      if (lblSekunden==null) {
         lblSekunden=new JLabel("s  ");
         lblSekunden.setName("lblSekunden");
      }
      return lblSekunden;
   }
   private JLabel getSaveLabel() {
      if (saveLabel==null) {
         saveLabel=new JLabel("Save Sensordata every");
         saveLabel.setName("saveLabel");
      }
      return saveLabel;
   }
   private JLabel getLblMinuten() {
      if (lblMinuten==null) {
         lblMinuten=new JLabel("min  ");
         lblMinuten.setName("lblMinuten");
      }
      return lblMinuten;
   }
   private JLabel getLblLastRead() {
      if (lblLastRead==null) {
         lblLastRead=new JLabel(" NOT read yet");
         lblLastRead.setName("lblLastRead");
      }
      return lblLastRead;
   }
   private JLabel getLblLastSaved() {
      if (lblLastSaved==null) {
         lblLastSaved=new JLabel("NOT saved yet");
         lblLastSaved.setName("lblLastSaved");
      }
      return lblLastSaved;
   }
}

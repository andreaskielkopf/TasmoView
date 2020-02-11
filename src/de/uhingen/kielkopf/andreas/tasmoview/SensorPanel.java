package de.uhingen.kielkopf.andreas.tasmoview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

public class SensorPanel extends JPanel {
   private static final long                                             serialVersionUID=-349511026033574886L;
   TreeSet<Tasmota>                                                      tasmotasPoll    =new TreeSet<Tasmota>();
   private Timer                                                         sensorTimer     =new Timer("SensorTimer");
   private JPanel                                                        panel;
   private JPanel                                                        refreshPanel;
   private JLabel                                                        refreshLabel;
   private JSpinner                                                      spinner;
   /** Liste der offenen Suche von Tasmotas oder der offenen Refreshs */
   final LinkedHashMap<CompletableFuture<HttpResponse<String>>, Tasmota> requests        =                         //
            new LinkedHashMap<CompletableFuture<HttpResponse<String>>, Tasmota>();
   private JLabel                                                        lblSekunden;
   /**
    * Create the panel.
    */
   public SensorPanel() {
      setLayout(new BorderLayout());
      add(getPanel(), BorderLayout.WEST);
      add(getRefreshPanel(), BorderLayout.NORTH);
      add(Data.data.getSensorGraphPanel(), BorderLayout.CENTER);
      sensorTimer.schedule(new Abfrage(), 5*1000);
      sensorTimer.schedule(new Auswertung(), 5*1000+100);
   }
   private JPanel getPanel() {
      if (panel==null) {
         panel=new JPanel();
         panel.setBorder(new TitledBorder(null, "Display", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(Data.data.getSensorList(), BorderLayout.CENTER);
      }
      return panel;
   }
   private JPanel getRefreshPanel() {
      if (refreshPanel==null) {
         refreshPanel=new JPanel();
         refreshPanel.setBorder(
                  new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Refresh", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
         refreshPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
         refreshPanel.add(getRefreshLabel());
         refreshPanel.add(getSpinner());
         refreshPanel.add(getLblSekunden());
      }
      return refreshPanel;
   }
   private JLabel getRefreshLabel() {
      if (refreshLabel==null) {
         refreshLabel=new JLabel("Refresh Sensordata every");
      }
      return refreshLabel;
   }
   private JSpinner getSpinner() {
      if (spinner==null) spinner=new JSpinner(new SpinnerNumberModel(10, 1, 120, 1));
      return spinner;
   }
   private class Abfrage extends TimerTask {
      @Override
      public void run() {
         synchronized (requests) {
            sensorTimer.schedule(new Abfrage(), ((Number) getSpinner().getValue()).longValue()*1000);
            for (Tasmota tasmota:Data.data.tasmotasMitSensoren) try {
               requests.put(tasmota.request(Sensor.STATUS_8), tasmota);
            } catch (URISyntaxException e) {
               e.printStackTrace();
            }
         }
      }
   }

   private class Auswertung extends TimerTask {
      @Override
      public void run() {
         synchronized (requests) {
            sensorTimer.schedule(new Auswertung(), ((Number) getSpinner().getValue()).longValue()*220);
            @SuppressWarnings("unchecked")
            LinkedHashMap<CompletableFuture<HttpResponse<String>>, Tasmota> tmp= //
                     (LinkedHashMap<CompletableFuture<HttpResponse<String>>, Tasmota>) requests.clone();
            for (Entry<CompletableFuture<HttpResponse<String>>, Tasmota> entry:tmp.entrySet()) try {
               CompletableFuture<HttpResponse<String>> request=entry.getKey();
               if (!request.isDone()) continue;
               requests.remove(request);
               if (request.isCancelled()||request.isCompletedExceptionally()) continue;
               Sensor.antwortAuswerten(entry);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }
   private JLabel getLblSekunden() {
      if (lblSekunden==null) {
         lblSekunden=new JLabel("s");
         lblSekunden.setName("lblSekunden");
      }
      return lblSekunden;
   }
}

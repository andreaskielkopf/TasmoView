package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.beans.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.*;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * Suche nach Tasmota-Sensoren im Netzwerk
 *
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class SensorScanner extends SwingWorker<String, String> {
   private static class ScanOf extends SwingWorker<Tasmota, Integer> {
      private final Tasmota tasm;
      public ScanOf(final Tasmota tasm1) {
         tasm=tasm1;
         System.out.print("+");
      }
      @Override
      protected Tasmota doInBackground() throws Exception {
         try {
            Thread.currentThread().setName(this.getClass().getSimpleName() + " " + tasm.ipPart);
            if ((tasm == null) || tasm.lokaleSensoren.isEmpty())
               return null;
            final Instant      i =Instant.now();
            final List<HttpResponse<String>> sl=tasm.requests(new String[] {Sensor.STATUS_8});
            if (sl.size() > 1) {
               final String     erg=sl.get(1).body();
               final JsonObject j0 =JsonObject.convertToJson(erg);
               for (final Sensor sensor:tasm.lokaleSensoren) {
                  final JsonObject j1=j0.getJsonObject(sensor.kennung);
                  if (j1 == null)
                     continue;
                  // Instant i=Messwert.getInstant(j0.getJsonObject("Time"));
                  sensor.addWert(i, j1);
                  System.out.print(".");
               }
            }
         } catch (final Exception e) {
            e.printStackTrace();
         }
         return tasm;
      }
      @Override
      protected void done() {
         System.out.print("-");
      }
   }
   private static final DateTimeFormatter tf=DateTimeFormatter.ofPattern("EE HH:mm:ss");
   private final JSpinner                 sensorRefreshSpinner;
   private final JLabel                   lastRead;
   /**
    * Erzeuge und starte eine Task die nach einer Anzahl von 256 Geräten sucht
    *
    * @param jLabel
    *
    * @param rescan
    *           erneuere die Infos ohne neue Geräte zu suchen
    * @param progressBar
    *           Fortschrittsbalken für die Anzeige
    * @param scanButton
    *           Scanbutton
    * @param refreshButton
    *           Refreshbutton
    */
   public SensorScanner(JSpinner sensorRefreshSpinner1, JLabel lastRead1) {
      sensorRefreshSpinner=sensorRefreshSpinner1;
      lastRead=lastRead1;
   }
   @SuppressWarnings("resource")
   @Override
   protected String doInBackground() throws Exception {
      try {
         final HashSet<ScanOf> scans=new HashSet<>();
         Thread.currentThread().setName(this.getClass().getSimpleName());
         int wartezeit=10;
         while (true) {
            for (final Tasmota tasmota:Data.getData().tasmotasMitSensoren) {
               final ScanOf x=new ScanOf(tasmota);
               scans.add(x);
               TasmoScanner.getPool().submit(x);// automatic execute in threadpool
            }
            try {
               if (sensorRefreshSpinner != null)
                  if (sensorRefreshSpinner.getValue() instanceof final Integer i)
                     wartezeit=i.intValue();
            } catch (final Exception ignore1) {
               ignore1.printStackTrace();
            }
            if (!TasmoView.keepRunning)
               return null;
            StringBuilder text=new StringBuilder(" ");
            try {
               text.append(tf.format(LocalDateTime.now()));
            } catch (final Exception e1) {
               e1.printStackTrace();
            }
            try {
               Thread.sleep(1000L * wartezeit);
            } catch (RuntimeException | InterruptedException ignore) {
               ignore.printStackTrace();
            }
            final HashSet<ScanOf> cleanup=new HashSet<>(scans);
            for (final ScanOf s:cleanup) {
               try {
                  if (s.isDone())
                     scans.remove(s);
               } catch (final Exception e) {
                  e.printStackTrace();
               }
            }
            text.append("(").append(cleanup.size()).append(":").append(cleanup.size() - scans.size()).append(")");
            publish(text.toString());
            System.out.println();
            System.out.print("S");
         }
      } catch (final Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return "DDD";
   }
   @Override
   protected void process(List<String> chunks) {
      if (lastRead != null) {
         for (final String text:chunks) {
            lastRead.setText(text);
            System.out.println(text);
         }
         lastRead.repaint(100);
      }
   }
}

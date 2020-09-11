package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.TasmoView;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * Suche nach Tasmota-Sensoren im Netzwerk
 * 
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class SensorScanner extends SwingWorker<String, String> {
   @Override
   protected void process(List<String> chunks) {
      if (lastRead!=null) {
         for (String text:chunks) {
            lastRead.setText(text);
            System.out.println(text);
         }
         lastRead.repaint(100);
      }
   }
   private final JSpinner                 sensorRefreshSpinner;
   private final JLabel                   lastRead;
   private static final DateTimeFormatter tf=DateTimeFormatter.ofPattern("EE HH:mm:ss");
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
   public SensorScanner(JSpinner sensorRefreshSpinner, JLabel lastRead) {
      this.sensorRefreshSpinner=sensorRefreshSpinner;
      this.lastRead=lastRead;
   }
   @Override
   protected String doInBackground() throws Exception {
      HashSet<ScanOf> scans=new HashSet<>();
      Thread.currentThread().setName(this.getClass().getSimpleName());
      int wartezeit=10;
      while (true) {
         for (Tasmota tasmota:Data.data.tasmotasMitSensoren) {
            ScanOf x=new ScanOf(tasmota);
            scans.add(x);
            TasmoScanner.exec.submit(x);// automatic execute in threadpool
         }
         try {
            wartezeit=(int) sensorRefreshSpinner.getValue();
         } catch (Exception ignore1) {
            ignore1.printStackTrace();
         }
         if (!TasmoView.keepRunning) return null;
         String text=" ";
         try {
            text+=tf.format(LocalDateTime.now());
         } catch (Exception e1) {
            e1.printStackTrace();
         }
         try {
            Thread.sleep(1000l*wartezeit);
         } catch (RuntimeException|InterruptedException ignore) {
            ignore.printStackTrace();
         }
         HashSet<ScanOf> cleanup=new HashSet<SensorScanner.ScanOf>(scans);
         for (ScanOf s:cleanup) {
            try {
               if (s.isDone()) scans.remove(s);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
         text+="("+cleanup.size()+":"+(cleanup.size()-scans.size())+")";
         publish(text);
         System.out.println();
         System.out.print("S");
      }
   }
   private class ScanOf extends SwingWorker<Tasmota, Integer> {
      private final Tasmota tasm;
      @Override
      protected void done() {
         System.out.print("-");
      }
      public ScanOf(final Tasmota tasm) {
         this.tasm=tasm;
         System.out.print("+");
      }
      @Override
      protected Tasmota doInBackground() throws Exception {
         Thread.currentThread().setName(this.getClass().getSimpleName()+" "+tasm.ipPart);
         if (tasm==null) return null;
         if (tasm.sensoren.isEmpty()) return null;
         try {
            Instant      i  =Instant.now();
            List<String> sl =tasm.requests(new String[] {Sensor.STATUS_8});
            String       erg=sl.get(1);
            JsonObject   j0 =JsonObject.convertToJson(erg);
            for (Sensor sensor:tasm.sensoren) {
               JsonObject j1=j0.getJsonObject(sensor.kennung);
               if (j1==null) continue;
               // Instant i=Messwert.getInstant(j0.getJsonObject("Time"));
               sensor.addWert(i, j1);
               System.out.print(".");
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
         return tasm;
      }
   }
}

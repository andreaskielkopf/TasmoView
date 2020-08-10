package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JSpinner;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * Suche nach Tasmota-Sensoren im Netzwerk
 * 
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class SensorScanner extends SwingWorker<String, String> {
   // public static final ExecutorService exec=Executors.newWorkStealingPool(25);
   private final JSpinner sensorRefreshSpinner;
   /**
    * Erzeuge und starte eine Task die nach einer Anzahl von 256 Geräten sucht
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
   public SensorScanner(JSpinner sensorRefreshSpinner) {
      this.sensorRefreshSpinner=sensorRefreshSpinner;
      // Thread.currentThread().setName(this.getClass().getSimpleName());
   }
   @Override
   protected String doInBackground() throws Exception {
      HashSet<ScanOf> scans=new HashSet<>();
      Thread.currentThread().setName(this.getClass().getSimpleName());
      while (true) {
         // int test=1;
         for (Tasmota tasmota:Data.data.tasmotasMitSensoren) {
            ScanOf x=new ScanOf(tasmota);
            scans.add(x);
            TasmoScanner.exec.submit(x);// automatic execute in threadpool
         }
         HashSet<ScanOf> cleanup=new HashSet<SensorScanner.ScanOf>(scans);
         for (ScanOf s:cleanup) {
            try {
               if (s.isDone()) scans.remove(s);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
         int wartezeit=(int) sensorRefreshSpinner.getValue();
         try {
            Thread.sleep(1000l*wartezeit);
         } catch (RuntimeException|InterruptedException ignore) {
            ignore.printStackTrace();
         }
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
         // int test=1;
         if (tasm==null) return null;
         if (tasm.sensoren.isEmpty()) return null;
         try {
            Instant           i  =Instant.now();
            ArrayList<String> sl =tasm.request(Sensor.STATUS_8);
            String            erg=sl.get(1);
            JsonObject        j0 =JsonObject.interpret(erg);
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

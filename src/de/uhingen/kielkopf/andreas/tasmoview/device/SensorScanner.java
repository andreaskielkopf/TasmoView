package de.uhingen.kielkopf.andreas.tasmoview.device;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JSpinner;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Messwert;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * Suche nach Tasmota-Sensoren im Netzwerk
 * 
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class SensorScanner extends SwingWorker<String, String> {
   public static final ExecutorService exec=Executors.newWorkStealingPool(25);
   private final JSpinner              sensorRefreshSpinner;
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
      super();
      this.sensorRefreshSpinner=sensorRefreshSpinner;
      exec.submit(this);// automatic execute in threadpool
   }
   @Override
   protected String doInBackground() throws Exception {
      HashSet<ScanOf> scans=new HashSet<>();
      while (true) {
         for (Tasmota tasmota:Data.data.tasmotasMitSensoren)
            scans.add(new ScanOf(tasmota));
         for (ScanOf s:scans) {
            if (s.isDone()) scans.remove(s);
         }
         int wartezeit=(int) sensorRefreshSpinner.getValue();
         try {
            Thread.sleep(10000l*wartezeit);
         } catch (InterruptedException ignore) {}
      }
   }
   private class ScanOf extends SwingWorker<String, Integer> {
      @Override
      protected void done() {}
      private final Tasmota tasmota;
      public ScanOf(Tasmota tasmota) {
         this.tasmota=tasmota;
         exec.submit(this);// automatic execute in threadpool
      }
      @Override
      protected String doInBackground() throws Exception {
         if (tasmota==null) return null;
         if (tasmota.sensoren.isEmpty()) return null;
         try {
            ArrayList<String> sl =tasmota.request(Sensor.STATUS_8);
            String            erg=sl.get(1);
            JsonObject        j0 =JsonObject.interpret(erg);
            for (Sensor sensor:tasmota.sensoren) {
               JsonObject j1=j0.getJsonObject(sensor.kennung);
               if (j1==null) continue;
               Instant i=Messwert.getInstant(j0.getJsonObject("Time"));
               sensor.addWert(i, j1);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
         return null;
      }
   }
}

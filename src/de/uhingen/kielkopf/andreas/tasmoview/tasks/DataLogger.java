package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.TasmoView;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * @author andreas kielkopf
 * @date 2020.08.13
 * 
 *       Thread um alle Sensordaten von Zeit zu Zeit zu speichern Die Zeit ist übver die GUI einstellbar, und der letzte Zeitpunkt wird in der GUI angezeigt
 */
public class DataLogger extends SwingWorker<Boolean, LocalDateTime> {
   private static final Path              savePath=FileSystems.getDefault().getPath("TasmoView").toAbsolutePath();
   private static final DateTimeFormatter df      =DateTimeFormatter.ofPattern("yyyyMMdd");
   private static final DateTimeFormatter tf      =DateTimeFormatter.ofPattern("EE HH:mm:ss");
   private final JSpinner                 saveSpinner;
   private final JLabel                   lastSaved;
   private static DataLogger              singleton;
   public DataLogger(JSpinner saveSpinner, JLabel lastSaved) {
      this.saveSpinner=saveSpinner;
      this.lastSaved=lastSaved;
      System.out.println("Savepath="+savePath);
   }
   @Override
   protected void process(List<LocalDateTime> chunks) {
      if (lastSaved!=null) for (LocalDateTime td:chunks)
         lastSaved.setText(" "+tf.format(td));
   }
   @Override
   protected Boolean doInBackground() throws Exception {
      singleton=this;
      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
            try {
               System.out.println("Shutdown-Hook running 1");
               TasmoView.keepRunning=false;
               System.out.println("Shutdown-Hook running 2");
               DataLogger dl=DataLogger.singleton;
               if (dl!=null) System.out.println(DataLogger.saveNow(0));
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      Thread.currentThread().setName(this.getClass().getSimpleName());
      int sekunden=5*60;
      while (TasmoView.keepRunning) {
         try {
            if (saveSpinner!=null) sekunden=60*(int) saveSpinner.getValue();
         } catch (Exception ignore1) {
            ignore1.printStackTrace();
         }
         try {
            Thread.sleep(1000l*sekunden);
         } catch (InterruptedException ignore) {}
         LocalDateTime ldt=saveNow(3);
         if (ldt!=null) publish(ldt);
      }
      return null;
   }
   static public LocalDateTime saveNow(int rest) {
      System.out.println("L");
      synchronized (singleton) {
         System.out.print("L");
         String date=df.format(LocalDateTime.now());
         for (Sensor sensor:Data.data.gesamtSensoren) {
            try {
               if (sensor.saveWerte.isEmpty()) continue;
               if (sensor.pfad==null) {
                  if (sensor.tasmota==null) continue;
                  String tasmoName=Integer.toString(sensor.tasmota.ipPart);
                  Path   tPath    =savePath.resolve(tasmoName);
                  if (Files.notExists(tPath)) Files.createDirectories(tPath);
                  // JsonObject friendlyObj=sensor.tasmota.deviceName);
                  if (sensor.tasmota.deviceName!=null) {
                     String friendlyName=sensor.tasmota.deviceName;
                     Path   fPath       =savePath.resolve(friendlyName);
                     if (Files.notExists(fPath)) Files.createSymbolicLink(fPath, tPath);
                  }
                  if (sensor.kennung==null) continue;
                  String sensorName=sensor.kennung;
                  if ((sensor.typ!=null)&&(!sensor.typ.isEmpty())) sensorName=sensorName+"."+sensor.typ;
                  sensorName=sensorName+"."+date;
                  Path sPath=tPath.resolve(sensorName);
                  if (sPath==null) continue;
                  sensor.pfad=sPath;
               }
               try (BufferedWriter bw=Files.newBufferedWriter(sensor.pfad, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND)) {
                  System.out.print("l");
                  while (sensor.saveWerte.size()>rest) {
                     bw.append(sensor.saveWerte.pollFirst().save()); // hole den ältesten Wert
                     bw.newLine();
                  }
                  return (LocalDateTime.now());
               } // automatic close
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
         return null;
      }
   }
}

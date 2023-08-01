package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.TasmoView;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

/**
 * @author andreas kielkopf
 * @date 2020.08.13
 *
 *       Thread um alle Sensordaten von Zeit zu Zeit zu speichern Die Zeit ist übver die GUI einstellbar, und der letzte
 *       Zeitpunkt wird in der GUI angezeigt
 */
public class DataLogger extends SwingWorker<Boolean, LocalDateTime> {
   private static final Path              savePath=FileSystems.getDefault().getPath("TasmoView").toAbsolutePath();
   private static final DateTimeFormatter df      =DateTimeFormatter.ofPattern("yyyyMMdd");
   private static final DateTimeFormatter tf      =DateTimeFormatter.ofPattern("EE HH:mm:ss");
   private static DataLogger              singleton;
   private final JSpinner                 saveSpinner;
   private final JLabel                   lastSaved;
   public DataLogger(JSpinner saveSpinner1, JLabel lastSaved1) {
      saveSpinner=saveSpinner1;
      lastSaved=lastSaved1;
      System.out.println("Savepath=" + savePath);
   }
   static public LocalDateTime saveNow(int rest) {
      System.out.println("L");
      synchronized (singleton) {
         System.out.print("L");
         final String date=df.format(LocalDateTime.now());
         for (final Sensor sensor:Data.getData().gesamtSensoren) {
            try {
               if (sensor.saveWerte.isEmpty())
                  continue;
               if (sensor.pfad == null) {
                  if (sensor.tasmota == null)
                     continue;
                  final String tasmoName=Integer.toString(sensor.tasmota.ipPart);
                  final Path   tPath    =savePath.resolve(tasmoName);
                  if (Files.notExists(tPath))
                     Files.createDirectories(tPath);
                  // JsonObject friendlyObj=sensor.tasmota.deviceName);
                  if (sensor.tasmota.deviceName != null) {
                     final String friendlyName=sensor.tasmota.deviceName;
                     final Path   fPath       =savePath.resolve(friendlyName);
                     if (Files.notExists(fPath))
                        Files.createSymbolicLink(fPath, tPath);
                  }
                  if (sensor.kennung == null)
                     continue;
                  StringBuilder sensorName=new StringBuilder().append(sensor.kennung);
                  if ((sensor.typ != null) && (!sensor.typ.isEmpty()))
                     sensorName.append(".").append(sensor.typ);
                  sensorName.append(".").append(date);
                  final Path sPath=tPath.resolve(sensorName.toString());
                  if (sPath == null)
                     continue;
                  sensor.pfad=sPath;
               }
               try (BufferedWriter bw=Files.newBufferedWriter(sensor.pfad, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                  System.out.print("l");
                  while (sensor.saveWerte.size() > rest) {
                     bw.append(sensor.saveWerte.pollFirst().save()); // hole den ältesten Wert
                     bw.newLine();
                  }
                  return (LocalDateTime.now());
               } // automatic close
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
         return null;
      }
   }
   @Override
   protected Boolean doInBackground() throws Exception {
      try {
         singleton=this;
         Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
               try {
                  System.out.println("Shutdown-Hook running 1");
                  TasmoView.keepRunning=false;
                  System.out.println("Shutdown-Hook running 2");
                  final DataLogger dl=DataLogger.singleton;
                  if (dl != null)
                     System.out.println(DataLogger.saveNow(0));
               } catch (final Exception e) {
                  e.printStackTrace();
               }
            }
         });
         Thread.currentThread().setName(this.getClass().getSimpleName());
         int sekunden=5 * 60;
         while (TasmoView.keepRunning) {
            try {
               if (saveSpinner != null)
                  if (saveSpinner.getValue() instanceof final Integer i)
                     sekunden=60 * i.intValue();
            } catch (final Exception ignore1) {
               ignore1.printStackTrace();
            }
            try {
               Thread.sleep(1000L * sekunden);
            } catch (final InterruptedException ignore) {}
            final LocalDateTime ldt=saveNow(3);
            if (ldt != null)
               publish(ldt);
         }
         return null;
      } catch (final Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return null;
   }
   @Override
   protected void process(List<LocalDateTime> chunks) {
      if (lastSaved != null)
         for (final LocalDateTime td:chunks)
            lastSaved.setText(" " + tf.format(td));
   }
}

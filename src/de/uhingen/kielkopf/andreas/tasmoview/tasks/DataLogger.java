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

import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonString;
import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

public class DataLogger extends SwingWorker<Boolean, Long> {
   static private final int        intervallSekunden=2*60;
   private Path                    savePath;
   private final DateTimeFormatter dtf;
   public DataLogger() {
      this.savePath=FileSystems.getDefault().getPath("TasmoView").toAbsolutePath();
      dtf=DateTimeFormatter.ofPattern("yyyyMMdd");
      System.out.println("Savepath="+savePath);
   }
   @Override
   protected Boolean doInBackground() throws Exception {
      Thread.currentThread().setName(this.getClass().getSimpleName());
      while (true) {
         try {
            Thread.sleep(1000l*intervallSekunden);
         } catch (InterruptedException ignore) {}
         System.out.println("L");
         System.out.print("L");
         String date=dtf.format(LocalDateTime.now());
         for (Sensor sensor:Data.data.sensoren) {
            try {
               if (sensor.saveWerte.isEmpty()) continue;
               if (sensor.pfad==null) {
                  if (sensor.tasmota==null) continue;
                  String tasmoName=Integer.toString(sensor.tasmota.ipPart);
                  Path   tPath    =savePath.resolve(tasmoName);
                  if (Files.notExists(tPath)) Files.createDirectories(tPath);
                  JsonObject friendlyObj=sensor.tasmota.name.list.get(0);
                  if (friendlyObj instanceof JsonString) {
                     String friendlyName=((JsonString) friendlyObj).value;
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
                  while (sensor.saveWerte.size()>2) {
                     bw.append(sensor.saveWerte.pollFirst().save()); // hole den ältesten Wert
                     bw.newLine();
                     // if (sensor.saveWerte.size()<=2) break;
                  }
                  // bw.close();
               } // automatic close
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}

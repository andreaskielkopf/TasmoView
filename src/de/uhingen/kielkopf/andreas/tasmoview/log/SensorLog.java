/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.log;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Andreas Kielkopf Interne Datenstruktur die den Log für ein Device intern(RAM) und extern(DISK) speichert
 */
public class SensorLog {
   public static Path                basis       =Path.of(System.getProperty("user.home"))   //
            .resolve(".log").resolve("TasmoView");
   private String                    ip;
   private String                    status;
   private String                    start       ="{";
   private String                    date;
   private ArrayList<String>         list        =new ArrayList<>();
   private Path                      path;
   public static int                 maxListCount=15;
   static private HashSet<SensorLog> allLogs     =new HashSet<>();
   /**
    * Erzeugt einen Sensorlog für die IP und den Status
    * 
    * @param ip0
    */
   public SensorLog(String ip0) {
      ip=URLEncoder.encode(ip0, StandardCharsets.UTF_8);
   }
   /**
    * Schreibt das momentane Array in die Datei
    * 
    * @throws IOException
    */
   public void flush() {
      if (!list.isEmpty()) {
         try {
            Files.write(path, list, StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         } catch (IOException e) {
            System.err.println(e);
         }
         list.clear();
      }
   }
   @Override
   public final String toString() {
      return new StringBuilder(ip)//
               .append(" ").append(status)//
               .append(" ").append(date)//
               .append(" ").append(path)//
               .append("(").append(list.size()).append(")").toString();
   }
   /**
    * Fügt dem Log eine Zeile hinzu
    * 
    * @param txt
    * @throws IOException
    */
   public void add(String txt) throws IOException {
      if (status == null) // erstmalig status setzen
         setStatus(txt);
      if (!txt.startsWith(start)) // nur jeweils den selben status übernehmen
         return;
      if (list.contains(txt))
         return; // keine doppelten Zeilen speichern
      if (!txt.contains(date)) {// Wenn sich der Tag geändert hat
         flush(); // vorhandene Daten vollends schreiben
         setStatus(txt); // status neu setzen
      }
      list.add(txt);
      if (list.size() >= maxListCount)
         flush();
   }
   private void setStatus(String txt) throws IOException {
      if (status == null)// Dieser Log enthält ab jetzt daten, und muss beim shutdown flush() ausführen
         allLogs.add(this);
      String[] a=txt.split('"' + "", 7);// {"StatusSNS":{"Time":"2025-05-01T08:10:44" ...
      if ((a.length < 7) || !a[1].startsWith("Status") || !a[3].equals("Time")) {
         flush();
         throw new IOException("Ohne Status/Time: " + (txt.length() <= 32 ? txt : txt.substring(0, 32)));
      }
      status=a[1];
      start="{\"" + a[1] + "\":";
      date=a[5].length() <= 10 ? a[5] : a[5].substring(0, 10);// Datum ausschneiden
      if (basis.startsWith("/root"))// mit root-rechten nach /var/log umziehen
         basis=Path.of("/var/log/TasmoView");
      path=basis.resolve(ip).resolve(date);
      System.out.println(this);
      try {
         Files.createDirectories(path.getParent());
      } catch (IOException e) {
         System.err.println(e);
      }
   }
   public void remove() {
      allLogs.remove(this);
      System.out.println("removed: " + this);
   }
   /**
    * Beim Shutdown alle logs ins Dateisystem schreiben
    */
   static public void flushAllLogs() {
      System.out.println("Flush all Logs");
      for (SensorLog log:allLogs) {
         System.out.println(log);
         if (!log.list.isEmpty())
            log.flush();
      }
   }
}

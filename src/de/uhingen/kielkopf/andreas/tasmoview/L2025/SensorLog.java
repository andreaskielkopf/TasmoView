/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2025;

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
   // public static Path basis0=Path.of("/var/log/tasmoView/");
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
      // System.out.println(this);
   }
   /**
    * Schreibt das momentane Array in die Datei
    * 
    * @throws IOException
    */
   public void flush() {
      if (list.isEmpty())
         return;
      // System.out.println(this);
      try {
         Files.write(path, list, StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      } catch (IOException e) {
         e.printStackTrace();
      }
      list.clear();
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
      if (!txt.startsWith(start)) // nur jeweils der selbe status übernehmen
         return;
      if (list.contains(txt))
         return; // keine doppelten Zeilen speichern
      if (!txt.contains(date)) {
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
      String first=txt;// {"StatusSNS":{"Time":"2025-05-01T08:10:44" ...
      String[] tl=first.split('"' + "", 7);
      status=tl[1];
      start="{\"" + status + "\":";
      if (!status.startsWith("Status") || !tl[3].equals("Time")) {
         flush();
         throw new IOException("Ungültiger Status " + txt);
      }
      date=tl[5].substring(0, 10);
      if (basis.startsWith("/root")) {
         System.out.println("basis starts with /root");
         basis=Path.of("/var/log/TasmoView");
      }
      // if (basis.startsWith("/home"))
      // System.out.println("basis starts with /home");
      path=basis.resolve(ip);
      try {
         Files.createDirectories(path);
      } catch (IOException e) {
         e.printStackTrace();
      }
      path=path.resolve(date);
      System.out.println(this);
   }
   public void remove() {
      allLogs.remove(this);
      System.out.println("removed " + this);
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

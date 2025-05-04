/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.log;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.Builder;

import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class OneScanner implements Runnable {
   public static String      username="andreas";
   public static String      password="akf4sonoff";
   public static int         sekunden=300;         // alle 5 Minuten
   public final String       ip;
   private static HttpClient client;
   private Builder           builder;
   private SensorLog         sensorlog;
   /**
    * Erstellt den Scanner und bereitet alles vor
    * 
    * @param ip1
    * @param anfrage
    * @throws URISyntaxException
    * @throws InterruptedException
    * @throws ExecutionException
    */
   public OneScanner(String ip1, String anfrage) throws URISyntaxException, InterruptedException, ExecutionException {
      if (client == null)
         client=HttpClient.newHttpClient();
      ip=ip1;
      builder=HttpRequest.newBuilder(new URI("http://" + URLEncoder.encode(ip, StandardCharsets.UTF_8) + "/cm?user="
               + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&password="
               + URLEncoder.encode(password, StandardCharsets.UTF_8) + "&cmnd="
               + URLEncoder.encode(anfrage, StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(10));
   }
   public CompletableFuture<HttpResponse<String>> ask() {
      return client.sendAsync(builder.GET().build(), BodyHandlers.ofString());
   }
   /**
    * Main-Methode als schneller Scanner für den Server
    * 
    * @param args[0]
    *           Basis-IP-Range in der Form "192.168.178."
    * @param args[1]
    *           Sekunden zwischen den Abfragen der Sensoren "300" = 5 Minuten
    * @param args[2]
    *           Anzahl der gesammelten Einträge bis zum save "15" = 15 * 5 Minuten
    * 
    */
   public static void main(String[] args) {
      if ((args.length < 1) || args.length > 3) {
         for (String s:List.of("Usage:"//
                  , "java -jar TasmoViewLog.jar 192.168.178. 300 12"//
                  , "192.168.178."//
                  , "    This will scan for Tasmota Sensos from 192.168.178.2 .. 192.168.178.254"//
                  , "300"//
                  , "    This will get sensor-data every 300 Seconds"//
                  , "12"//
                  , "    This will save everytime the cache holds 12 or more lines"//
                  , ""//
                  , "Data will be stored in /var/log/TasmoView/ or in ~/.log/TasmoView/"//
                  , "This program is under GPL (2025-05-04)"))
            System.out.println(s);
      }
      String basis="192.168.178.";
      if ((args.length >= 1) && (args[0].matches("([0-9]{1,3}[.]){3}")))
         basis=args[0];
      if ((args.length >= 2) && (args[1].matches("[0-9]{2,3}")))// 10-999
         sekunden=Math.max(10, Integer.parseInt(args[1]));
      if ((args.length >= 3) && (args[2].matches("[0-9]{1,3}")))// 1-999
         SensorLog.maxListCount=Math.max(1 + 100 / sekunden, Integer.parseInt(args[1]));
      /// @todo Flags einbinden
      // sh.setName("ShutdownHook");
      Runtime.getRuntime().addShutdownHook(new Thread(SensorLog::flushAllLogs));
      try {
         username="andreas";
         password="akf4sonoff";
         HashSet<Thread> scanners=new HashSet<>();
         for (int i=2; i <= 254; i++)
            scanners.add(new OneScanner(basis + Integer.toString(i), "status 10").start());
         for (Thread oneScanner:scanners)
            oneScanner.join(); // warte in main bis der Thread angehalten wird
      } catch (URISyntaxException | InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
   }
   /**
    * Starte den scanner als virtuellen Thread und gub den Thread zurück
    * 
    * @return thread
    */
   public Thread start() {
      sensorlog=new SensorLog(ip);
      try {
         Thread.sleep(100);
      } catch (InterruptedException e) {
         System.err.println(e);
      }
      return Thread.ofVirtual().name(ip).start(this);
   }
   @Override
   public void run() {
      try {
         int countdown=5;// nach 5 erfolglosen versuchen abbrechen
         while (true) {
            try {
               CompletableFuture<HttpResponse<String>> erg=ask();
               String txt=erg.get(9, TimeUnit.SECONDS).body();// {"StatusSNS":{"Time":"2025-05-01T08:10:44" ...
               if (!txt.startsWith("{\"Status")// keine Statusdaten
                        || txt.length() < 70) { // das ist höchstens die Zeit, sonst nix
                  System.out.println(txt.length() <= 44 ? txt : txt.subSequence(0, 44));
                  Thread.sleep(1000);
                  if (countdown-- < 0)
                     break;
                  continue; // zu kurz um daten zu enthalten, oder kein Status
               }
               if (countdown < 10)
                  countdown++;
               sensorlog.add(txt);
            } catch (TimeoutException e) {
               if (countdown-- < 0)
                  break;
               // System.err.println(e);
            }
            Thread.sleep(sekunden * 1000 - 150);// geschätzt 90ms Antwortzeit
         }
      } catch (ConnectException | ExecutionException ignore) {/* ignore */
      } catch (InterruptedException | IOException e) {
         System.err.println(e + " " + ip);
      }
      sensorlog.flush();
      sensorlog.remove();
   }
}

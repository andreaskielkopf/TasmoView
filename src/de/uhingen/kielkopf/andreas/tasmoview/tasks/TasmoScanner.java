package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import static javax.swing.SwingUtilities.invokeLater;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.prefs.BackingStoreException;

import javax.swing.JProgressBar;
import javax.swing.JToggleButton;

import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.tasmoview.*;
import de.uhingen.kielkopf.andreas.tasmoview.grafik.JPowerPane;
import de.uhingen.kielkopf.andreas.tasmoview.table.TasmoList;

/**
 * Suche nach Tasmota-Geräten im Netzwerk
 *
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class TasmoScanner implements Runnable {
   static private TasmoScanner    me;
   static private ExecutorService pool;
   static public final ExecutorService getPool() {
      if (pool == null) {
         pool=Version.getVx();
//         System.out.println("Verwende " + pool.getClass().getSimpleName());
      }
      return pool;
   }
   @SuppressWarnings("resource")
   static public TasmoScanner getTasmoScanner(boolean rescan1, JProgressBar progressBar1, JToggleButton scanButton1,
            JToggleButton refreshButton1) {
      if (me == null) {
         me=new TasmoScanner(rescan1, progressBar1, scanButton1, refreshButton1);
         getPool().execute(me);
      }
      return me;
   }
   private final ConcurrentSkipListMap<Integer, ScanFor> laufendeAnfragen=new ConcurrentSkipListMap<>();
   @SuppressWarnings("resource")
   private ScanFor getScanFor(Integer j) {
      if (!laufendeAnfragen.containsKey(j)) {
         ScanFor scanFor1=new ScanFor(this, j);
         laufendeAnfragen.put(j, scanFor1);
         // System.out.println("Anfrage an:(" + j + ")");
         getPool().execute(scanFor1); // wird auch sofort gestartet
      }
      return laufendeAnfragen.get(j);
   }
   /**
    * Sucht nach genau einer IP im Netzwerk
    */
   public class ScanFor implements Runnable {
      private final Integer j;
      // private final TasmoScanner tasmoScanner;
      private ScanFor(TasmoScanner ts, Integer j1) {
         j=j1;
         // tasmoScanner=ts;
         System.out.print("^");
      }
      @Override
      public void run() {
         try {
            Thread.currentThread().setName(this.getClass().getSimpleName() + " " + j);
            scanFor(j);
         } catch (final Exception e) {
            e.printStackTrace();
         } finally {
            laufendeAnfragen.remove(j);
            offen.clear(j);
            done();
         }
      }
      private void done() {
         System.out.print("v");
         progressBar.setString(j.toString());
         progressBar.setValue(offen.size() - offen.cardinality());
      }
      static private final String[] SCAN_FRAGEN=new String[] {Tasmota.SUCHANFRAGE};
      /**
       * Suchen nach dem Gerät mit der IP i
       *
       * @param i
       *           Teil der IP
       * @throws UnknownHostException
       * @throws URISyntaxException
       */
      private final void scanFor(Integer i) throws UnknownHostException, URISyntaxException {
         Tasmota tasmota=Tasmota.getTasmota(i);
         // System.out.println("Starte Anfrage an (" + i + ")");
         List<HttpResponse<String>> erg=tasmota.requests(SCAN_FRAGEN);
         if (tasmota.process(erg)) {
            isTasmota.set(i);
            noTasmota.clear(i);
            List<HttpResponse<String>> antwort=tasmota.requests(Tasmota.ZUSATZ_FRAGEN);
            if (!antwort.isEmpty())
               tasmota.process(antwort);
            publish(tasmota);
         } else {
            isTasmota.clear(i);
            noTasmota.set(i);
         }
         laufendeAnfragen.remove(i);
      }
      /**
       * Melde diesen Tasmota als gefunden
       * 
       * @param tasmota
       */
      private void publish(Tasmota tasmota) {
         TasmoScanner.publish(j, tasmota);
      }
   }
   /** privates Segment mit 256 IPs */
   private static final int    MAXIMUM_IPS  =256;
   /** Zeitabstand beim Scan */
   private static final int    ABSTAND_IN_MS=10;
   /** Dies scheint ein Tasmota zu sein */
   private static final BitSet isTasmota    =new BitSet(MAXIMUM_IPS);
   /** Dies ist sicher kein Tasmota */
   private static final BitSet noTasmota    =new BitSet(MAXIMUM_IPS);
   private static final String GESAMTLIST   ="Tasmotas";
   private final JProgressBar  progressBar;
   private final JToggleButton scanButton;
   private final JToggleButton refreshButton;
   private final boolean       rescan;
   private final BitSet        offen        =new BitSet(MAXIMUM_IPS);
   /**
    * Erzeuge und starte eine Task die nach einer Anzahl von MAXIMUM_IPS Geräten sucht
    *
    * @param rescan1
    *           erneuere die Infos ohne neue Geräte zu suchen
    * @param progressBar1
    *           Fortschrittsbalken für die Anzeige
    * @param scanButton1
    *           Scanbutton
    * @param refreshButton1
    *           Refreshbutton
    */
   private TasmoScanner(boolean rescan1, JProgressBar progressBar1, JToggleButton scanButton1,
            JToggleButton refreshButton1) {
      rescan=rescan1;
      progressBar=progressBar1;
      scanButton=scanButton1;
      refreshButton=refreshButton1;
      invokeLater(() -> progressBar1.setMaximum(MAXIMUM_IPS));
   }
   private static void saveTasmotaHint() {
      try {
         System.out.println(Data.getData().prefs.get(GESAMTLIST, ""));
         final ArrayList<String> list=new ArrayList<>();
         for (int i=isTasmota.nextSetBit(0); i >= 0; i=isTasmota.nextSetBit(i + 1))
            list.add(Integer.toString(i));
         if (!list.isEmpty()) {
            final String gesamtTasmotas=String.join(",", list);
            Data.getData().prefs.put(GESAMTLIST, gesamtTasmotas);
            Data.getData().prefs.flush();
            System.out.println("Tasmotas stored:" + gesamtTasmotas);
         }
      } catch (final BackingStoreException e1) {
         e1.printStackTrace();
      }
   }
   @Override
   public void run() {
      try {
         Thread.currentThread().setName(this.getClass().getSimpleName());
         if (!rescan) {
            offen.set(0, MAXIMUM_IPS - 1);// alle suchen
            offen.andNot(isTasmota);// ausser die schon gefunden worden sind
            offen.andNot(noTasmota);// ausser die falsch geantwortet haben
         } else {
            offen.clear();
            offen.or(isTasmota);// nur die, die schon letztes mal gefunden wurden aktualisieren
         }
         ArrayList<Integer> searchlist=new ArrayList<>();
         for (int i=offen.nextSetBit(0); i >= 0; i=offen.nextSetBit(i + 1))
            searchlist.add(Integer.valueOf(i));
         Collections.shuffle(searchlist);// zufällige Reihenfolge
         String gesamtlist=Data.getData().prefs.get(GESAMTLIST, "");
         if (!gesamtlist.isEmpty()) { // TODO
            String[] list=gesamtlist.split(",");
            for (final String zahl:list) {
               try {
                  Integer z=Integer.valueOf(zahl);
                  isTasmota.set(z.intValue());
                  final int pos=searchlist.indexOf(z);
                  if (pos >= 0) {
                     searchlist.remove(pos);
                     searchlist.add(z); // ans Ende anfügen
                  }
               } catch (final NumberFormatException e) {
                  e.printStackTrace();
               }
            }
         }
         Collections.reverse(searchlist);
         for (final Integer j:searchlist) {
            if (!TasmoView.keepRunning)
               return;
            publish(j.toString()); // im Progressbar anzeigen
            // int offen1=0;
            // for (final ScanFor scanFor:lanfragen)
            // if (!scanFor.isDone())
            // offen1++;
            try {
               Thread.sleep(ABSTAND_IN_MS /* laufendeAnfragen.size() */);
            } catch (final Exception ignore) {/* kleine Pause */}
            getScanFor(j);
            // anfragen.add(x);// mit autostart
         }
         int runde=60;// Maximal eine Minute warten
         while (!laufendeAnfragen.isEmpty()) {
            if (--runde < 1)
               break;
            System.out.println("");
            System.out.print("noch offen: ");
            for (final ScanFor scanFor:new ArrayList<>(laufendeAnfragen.values()))
               System.out.print(scanFor.j + "-");
            Thread.sleep(1000);
         }
         // for (final ScanFor scanFor:anfragen)
         // scanFor.cancel(true);
         // anfragen.clear();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      } finally {
         laufendeAnfragen.clear();// referenzen entfernen
         invokeLater(() -> done());
      }
      // return "fertig";
   }
   /**
    * @param string
    */
   private void publish(String string) {
      if (progressBar instanceof JProgressBar p)
         invokeLater(() -> progressBar.setString(string));
   }
   /**
    * Melde diesen Tasmota als gefunden
    * 
    * Erledigt was an der GUI angezeigt wer4den soll
    * 
    * @param j
    * 
    * @param tasmota
    */
   private static void publish(Integer j, Tasmota tasmota) {
      Data.getData().tasmotasD.put(j, tasmota);
      invokeLater(() -> Data.getData().dataModel.fireTableDataChanged());
      if (Data.getData().powerpane instanceof JPowerPane jpp)
         invokeLater(() -> jpp.recalculateListe());
      // if (progressBar.getMaximum() != 256)
      // progressBar.setMaximum(256);
   }
   private void done() {
      progressBar.setValue(progressBar.getMaximum());
      progressBar.setString("Found " + isTasmota.cardinality() + " Tasmotas");
      if (!rescan) {
         scanButton.setSelected(false);
      } else {
         refreshButton.setSelected(false);
      }
      scanButton.setEnabled(true);
      refreshButton.setEnabled(true);
      saveTasmotaHint();
      TasmoList.recalculateColumnames();
      if (Data.getData().powerpane instanceof JPowerPane jpp)
         jpp.recalculateListe();
      me=null; // Für die nächste Runde brauchts einen neuen
   }
}

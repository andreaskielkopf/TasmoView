package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import static javax.swing.SwingUtilities.invokeLater;

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
   /** Zeitabstand beim Scan */
   static final int                                      ABSTAND_IN_MS   =10;
   static final String                                   GESAMTLIST      ="Tasmotas";
   /** privates Segment mit 256 IPs */
   static final int                                      IPS_MAXIMUM     =256;
   /** Dies ist sicher kein Tasmota */
   static final BitSet                                   isNoTasmota     =new BitSet(IPS_MAXIMUM);
   /** Noch keine Anfrage oder Antwort */
   static final BitSet                                   isOffen         =new BitSet(IPS_MAXIMUM);
   /** Dies scheint ein Tasmota zu sein */
   static final BitSet                                   isTasmota       =new BitSet(IPS_MAXIMUM);
   static private TasmoScanner                           me;
   static private ExecutorService                        pool;
   static private final String[]                         SCAN_FRAGEN     =new String[] {Tasmota.SUCHANFRAGE};
   private final ConcurrentSkipListMap<Integer, ScanFor> laufendeAnfragen=new ConcurrentSkipListMap<>();
   private final JProgressBar                            progressBar;
   private final JToggleButton                           refreshButton;
   private final boolean                                 rescan;
   private final JToggleButton                           scanButton;
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
      invokeLater(() -> progressBar1.setMaximum(IPS_MAXIMUM));
   }
   /**
    * Sucht nach genau einer IP im Netzwerk
    */
   public record ScanFor(TasmoScanner ts, Integer nr) implements Runnable {
      @Override
      public void run() {
         try {
            Thread.currentThread().setName(this.getClass().getSimpleName() + " " + nr);
            Tasmota tasmota=Tasmota.getTasmota(nr);
            List<HttpResponse<String>> erg=tasmota.requests(SCAN_FRAGEN);
            if (tasmota.process(erg)) {
               isTasmota.set(nr);
               isNoTasmota.clear(nr);
               List<HttpResponse<String>> antwort=tasmota.requests(Tasmota.ZUSATZ_FRAGEN);
               if (!antwort.isEmpty())
                  tasmota.process(antwort);
               Data.getData().tasmotasD.put(nr, tasmota);
               invokeLater(() -> Data.getData().dataModel.fireTableDataChanged());
               if (Data.getData().powerpane instanceof JPowerPane jpp)
                  invokeLater(() -> jpp.recalculateListe());
            } else {
               isTasmota.clear(nr);
               isNoTasmota.set(nr);
            }
            ts.laufendeAnfragen.remove(nr);
         } catch (final Exception e) {
            e.printStackTrace();
         } finally {
            ts.laufendeAnfragen.remove(nr);
            isOffen.clear(nr);
            // System.out.print("v");
            ts.progressBar.setString(nr.toString());
            ts.progressBar.setValue(isOffen.size() - isOffen.cardinality());
         }
      }
   }
   @Override
   public void run() {
      try {
         Thread.currentThread().setName(this.getClass().getSimpleName());
         if (!rescan) {
            isOffen.set(0, IPS_MAXIMUM - 1);// alle suchen
            isOffen.andNot(isTasmota);// ausser die schon gefunden worden sind
            isOffen.andNot(isNoTasmota);// ausser die falsch geantwortet haben
         } else {
            isOffen.clear();
            isOffen.or(isTasmota);// nur die, die schon letztes mal gefunden wurden aktualisieren
         }
         List<Integer> searchlist=new LinkedList<>();
         for (int i=isOffen.nextSetBit(0); i >= 0; i=isOffen.nextSetBit(i + 1))
            searchlist.add(Integer.valueOf(i));
         Collections.shuffle(searchlist);// zufällige Reihenfolge
         for (final String zahl:Data.getData().prefs.get(GESAMTLIST, "").split(",")) {
            try { // Die bekannten Tasmotas an den beginn der Liste
               Integer z=Integer.valueOf(zahl);
               searchlist.remove(z);
               searchlist.addFirst(z);
               isTasmota.set(z.intValue());
            } catch (final NumberFormatException ignore) { /* */ }
         }
         for (final Integer j:searchlist) {
            try {
               Thread.sleep(ABSTAND_IN_MS /* laufendeAnfragen.size() */);
            } catch (final Exception ignore) {/* kleine Pause */}
            if (progressBar instanceof JProgressBar p)
               invokeLater(() -> progressBar.setString(j.toString())); // im Progressbar anzeigen
            if (!TasmoView.keepRunning)
               return;
            getScanFor(j);
         }
         int runde=60;// Maximal eine Minute warten
         while (!laufendeAnfragen.isEmpty()) {
            if (--runde < 1)
               break;
            System.out.println("");
            System.out.print("noch offen: ");
            for (final ScanFor scanFor:new ArrayList<>(laufendeAnfragen.values()))
               System.out.print(scanFor.nr + "-");
            Thread.sleep(1000);
         }
      } catch (final InterruptedException e) {
         e.printStackTrace();
      } finally {
         laufendeAnfragen.clear();// alle referenzen entfernen
         invokeLater(() -> {
            saveTasmotaHint();
            progressBar.setValue(progressBar.getMaximum());
            progressBar.setString("Found " + isTasmota.cardinality() + " Tasmotas");
            if (!rescan)
               scanButton.setSelected(false);
            else
               refreshButton.setSelected(false);
            scanButton.setEnabled(true);
            refreshButton.setEnabled(true);
            TasmoList.recalculateColumnames();
            if (Data.getData().powerpane instanceof JPowerPane jpp)
               jpp.recalculateListe();
         });
         me=null; // Für die nächste Runde brauchts einen neuen
      }
   }
   @SuppressWarnings("resource")
   private ScanFor getScanFor(Integer j) {
      if (!laufendeAnfragen.containsKey(j)) {
         ScanFor scanFor1=new ScanFor(this, j);
         laufendeAnfragen.put(j, scanFor1);
         getPool().execute(scanFor1); // wird auch sofort gestartet
      }
      return laufendeAnfragen.get(j);
   }
   static public final ExecutorService getPool() {
      if (pool == null) {
         pool=Version.getVx();
      }
      return pool;
   }
   @SuppressWarnings("resource")
   static public final TasmoScanner getTasmoScanner(boolean rescan1, JProgressBar progressBar1,
            JToggleButton scanButton1, JToggleButton refreshButton1) {
      if (me == null) {
         me=new TasmoScanner(rescan1, progressBar1, scanButton1, refreshButton1);
         getPool().execute(me);
      }
      return me;
   }
   static private void saveTasmotaHint() {
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
}

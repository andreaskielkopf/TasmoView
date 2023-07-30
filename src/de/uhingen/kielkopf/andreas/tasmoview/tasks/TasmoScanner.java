package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;

import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.TasmoView;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.table.TasmoList;

/**
 * Suche nach Tasmota-Geräten im Netzwerk
 *
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class TasmoScanner extends SwingWorker<String, String> {
   private class ScanFor extends SwingWorker<String, Tasmota> {
      private final Integer j;
      public ScanFor(Integer j1) {
         j=j1;
         System.out.print("^");
      }
      @Override
      protected String doInBackground() throws Exception {
         try {
            Thread.currentThread().setName(this.getClass().getSimpleName() + " " + j);
            scanFor(j.intValue());
            offen.clear(j.intValue());
            return null;
         } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         return null;
      }
      @Override
      protected void done() {
         System.out.print("v");
         progressBar.setString(j.toString());
         progressBar.setValue(offen.size() - offen.cardinality());
      }
      @Override
      protected void process(List<Tasmota> chunks) {
         Data.data.dataModel.fireTableDataChanged();
         // Data.data.tasmolist.repaint(1000);
         // TasmoList.recalculateColumnames();
         if ((Data.data != null) && (Data.data.powerpane != null))
            Data.data.powerpane.recalculateListe();
      }
      /**
       * Suchen nach dem Gerät mit der IP i
       *
       * @param i
       *           Teil der IP
       */
      private final void scanFor(int i) {
         try {
            Tasmota tasmota=new Tasmota(i);
            if (Data.data.tasmotas.contains(tasmota))
               tasmota=Data.data.tasmotas.ceiling(tasmota);
            final List<String> erg=tasmota.requests(new String[] {Tasmota.SUCHANFRAGE});
            if (erg.size() > 1) {
               // teste das mal mit mina
            }
            if (erg.size() > 1) {// Es ist eine Antwort gekommen
               Data.data.tasmotas.add(tasmota);
               if (tasmota.process(erg)) {
                  isTasmota.set(i);
                  final List<String> antwort=tasmota.requests(Tasmota.ZUSATZ_FRAGEN);
                  if (antwort.size() > Tasmota.ZUSATZ_FRAGEN.length) {
                     tasmota.process(antwort);
                  }
                  publish(tasmota);
               } else
                  noTasmota.set(i);
            }
         } catch (final Exception e) {
            e.printStackTrace();
         }
      }
   }
   public static final ExecutorService pool         =Executors.newWorkStealingPool();
   /** privates Segment mit 256 IPs */
   private static final int            MAXIMUM_IPS  =256;
   /** Zeitabstand beim Scan */
   private static final int            ABSTAND_IN_MS=100;
   /** Dies scheint ein Tasmota zu sein */
   private static final BitSet         isTasmota    =new BitSet(MAXIMUM_IPS);
   /** Dies ist sicher kein Tasmota */
   private static final BitSet         noTasmota    =new BitSet(MAXIMUM_IPS);
   private static final String         GESAMTLIST   ="Tasmotas";
   private final JProgressBar          progressBar;
   private final JToggleButton         scanButton;
   private final JToggleButton         refreshButton;
   private final boolean               rescan;
   private final BitSet                offen        =new BitSet(MAXIMUM_IPS);
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
   public TasmoScanner(boolean rescan1, JProgressBar progressBar1, JToggleButton scanButton1,
            JToggleButton refreshButton1) {
      rescan=rescan1;
      progressBar=progressBar1;
      scanButton=scanButton1;
      refreshButton=refreshButton1;
      progressBar1.setMaximum(MAXIMUM_IPS);
   }
   private static void saveTasmotaHint() {
      try {
         System.out.println(Data.data.prefs.get(GESAMTLIST, ""));
         final ArrayList<String> list=new ArrayList<>();
         for (int i=isTasmota.nextSetBit(0); i >= 0; i=isTasmota.nextSetBit(i + 1))
            list.add(Integer.toString(i));
         if (!list.isEmpty()) {
            final String gesamtTasmotas=String.join(",", list);
            Data.data.prefs.put(GESAMTLIST, gesamtTasmotas);
            Data.data.prefs.flush();
            System.out.println("Tasmotas stored:" + gesamtTasmotas);
         }
      } catch (final BackingStoreException e1) {
         e1.printStackTrace();
      }
   }
   @Override
   protected String doInBackground() throws Exception {
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
         final ArrayList<Integer> searchlist=new ArrayList<>();
         for (int i=offen.nextSetBit(0); i >= 0; i=offen.nextSetBit(i + 1))
            searchlist.add(Integer.valueOf(i));
         Collections.shuffle(searchlist);// zufällige Reihenfolge
         final String gesamtlist=Data.data.prefs.get(GESAMTLIST, "");
         if (!gesamtlist.isEmpty()) {
            final String[] list=gesamtlist.split(",");
            for (final String zahl:list) {
               try {
                  final Integer z=Integer.valueOf(zahl);
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
         final ArrayList<ScanFor> anfragen=new ArrayList<>();
         for (final Integer j:searchlist) {
            if (!TasmoView.keepRunning)
               return null;
            publish(j.toString());
            int offen1=0;
            for (final ScanFor scanFor:anfragen)
               if (!scanFor.isDone())
                  offen1++;
            try {
               Thread.sleep(ABSTAND_IN_MS * offen1);
            } catch (final Exception ignore) {}
            final ScanFor x=(new ScanFor(j));
            TasmoScanner.pool.submit(x);
            anfragen.add(x);// mit autostart
         }
         int runde=60;// Maximal eine Minute warten
         while (!anfragen.isEmpty()) {
            if (--runde < 1)
               break;
            System.out.println("");
            System.out.print("noch offen: ");
            final ArrayList<ScanFor> cleanup=new ArrayList<>(anfragen);
            for (final ScanFor scanFor:cleanup)
               if (scanFor.isDone())
                  anfragen.remove(scanFor);
               else
                  System.out.print(Integer.toHexString(scanFor.j.intValue()) + "-");
            Thread.sleep(1000);
         }
         for (final ScanFor scanFor:anfragen)
            scanFor.cancel(true);
         anfragen.clear();
      } catch (final InterruptedException e) {
         e.printStackTrace();
         throw e;
      }
      return "fertig";
   }
   @Override
   protected void done() {
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
      if ((Data.data != null) && (Data.data.powerpane != null))
         Data.data.powerpane.recalculateListe();
   }
   @Override
   protected void process(List<String> chunks) {
      if (progressBar.getMaximum() != 256)
         progressBar.setMaximum(256);
   }
}

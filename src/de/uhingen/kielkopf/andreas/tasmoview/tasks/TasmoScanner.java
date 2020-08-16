package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.TasmoView;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

/**
 * Suche nach Tasmota-Geräten im Netzwerk
 * 
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class TasmoScanner extends SwingWorker<String, String> {
   public static final ExecutorService exec         =Executors.newWorkStealingPool(100);
   /** privates Segment mit 256 IPs */
   private static final int            MAXIMUM_IPS  =256;
   /** Zeitabstand beim Scan */
   private static final int            ABSTAND_IN_MS=100;
   /** Dies scheint ein Tasmota zu sein */
   private static final BitSet         isTasmota    =new BitSet(MAXIMUM_IPS);
   /** Dies ist sicher kein Tasmota */
   private static final BitSet         noTasmota    =new BitSet(MAXIMUM_IPS);
   private final JProgressBar          progressBar;
   private final JToggleButton         scanButton;
   private final JToggleButton         refreshButton;
   private final boolean               rescan;
   private final BitSet                offen        =new BitSet(MAXIMUM_IPS);
   /**
    * Erzeuge und starte eine Task die nach einer Anzahl von MAXIMUM_IPS Geräten sucht
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
   public TasmoScanner(boolean rescan, JProgressBar progressBar, JToggleButton scanButton, JToggleButton refreshButton) {
      super();
      this.rescan=rescan;
      this.progressBar=progressBar;
      this.scanButton=scanButton;
      this.refreshButton=refreshButton;
      progressBar.setMaximum(MAXIMUM_IPS);
   }
   @Override
   protected String doInBackground() throws Exception {
      Thread.currentThread().setName(this.getClass().getSimpleName());
      try {
         if (!rescan) {
            offen.set(0, MAXIMUM_IPS-1);// alle suchen
            offen.andNot(isTasmota);// ausser die schon gefunden worden sind
            offen.andNot(noTasmota);// ausser die falsch geantwortet haben
         } else {
            offen.clear();
            offen.or(isTasmota);// nur die, die schon letztes mal gefunden wurden aktualisieren
         }
         ArrayList<Integer> searchlist=new ArrayList<>();
         for (int i=offen.nextSetBit(0); i>=0; i=offen.nextSetBit(i+1))
            searchlist.add(i);
         Collections.shuffle(searchlist);// zufällige Reihenfolge
         ArrayList<ScanFor> anfragen=new ArrayList<>();
         for (Integer j:searchlist) {
            if (!TasmoView.keepRunning) return null;
            publish(Integer.toString(j));
            int offen=0;
            for (ScanFor scanFor:anfragen)
               if (!scanFor.isDone()) offen++;
            try {
               Thread.sleep(ABSTAND_IN_MS*offen);
            } catch (Exception ignore) {}
            ScanFor x=(new ScanFor(j));
            TasmoScanner.exec.submit(x);
            anfragen.add(x);// mit autostart
         }
         int runde=60;// Maximal eine Minute warten
         while (!anfragen.isEmpty()) {
            if (--runde<1) break;
            System.out.println("");
            System.out.print("noch offen: ");
            ArrayList<ScanFor> cleanup=new ArrayList<>(anfragen);
            for (ScanFor scanFor:cleanup)
               if (scanFor.isDone())
                  anfragen.remove(scanFor);
               else
                  System.out.print(Integer.toHexString(scanFor.j)+"-");
            Thread.sleep(1000);
         }
         for (ScanFor scanFor:anfragen)
            scanFor.cancel(true);
         anfragen.clear();
      } catch (InterruptedException e) {
         e.printStackTrace();
         throw e;
      }
      return "fertig";
   }
   @Override
   protected void process(List<String> chunks) {
      if (progressBar.getMaximum()!=256) progressBar.setMaximum(256);
   }
   @Override
   protected void done() {
      progressBar.setValue(progressBar.getMaximum());
      progressBar.setString("Found "+isTasmota.cardinality()+" Tasmotas");
      if (!rescan) {
         scanButton.setSelected(false);
      } else {
         refreshButton.setSelected(false);
      }
      scanButton.setEnabled(true);
      refreshButton.setEnabled(true);
   }
   private class ScanFor extends SwingWorker<String, Tasmota> {
      @Override
      protected void process(List<Tasmota> chunks) {
         Data.data.dataModel.fireTableDataChanged();
         Data.data.tasmolist.repaint(1000);
      }
      private final Integer j;
      public ScanFor(Integer j) {
         this.j=j;
         System.out.print("^");
      }
      @Override
      protected String doInBackground() throws Exception {
         Thread.currentThread().setName(this.getClass().getSimpleName()+" "+j);
         scanFor(j);
         offen.clear(j);
         return null;
      }
      @Override
      protected void done() {
         System.out.print("v");
         progressBar.setString(Integer.toString(j));
         progressBar.setValue(offen.size()-offen.cardinality());
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
            if (Data.data.tasmotas.contains(tasmota)) tasmota=Data.data.tasmotas.ceiling(tasmota);
            ArrayList<String> erg=tasmota.request(Tasmota.SUCHANFRAGE);
            if (erg.size()>1) {// Es ist eine Antwort gekommen
               Data.data.tasmotas.add(tasmota);
               if (tasmota.process(erg)) {
                  isTasmota.set(i);
                  publish(tasmota);
               } else
                  noTasmota.set(i);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
}

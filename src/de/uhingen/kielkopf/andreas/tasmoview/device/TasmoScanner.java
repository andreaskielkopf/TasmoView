package de.uhingen.kielkopf.andreas.tasmoview.device;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

/**
 * Suche nach Tasmota-Geräten im Netzwerk
 * 
 * @author andreas T=Endergebniss, V=Zwischenergebnisse
 */
public class TasmoScanner extends SwingWorker<String, String> {
   public static final ExecutorService exec         =Executors.newWorkStealingPool();
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
      exec.submit(this);// automatic execute in threadpool
   }
   @Override
   protected String doInBackground() throws Exception {
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
            publish(Integer.toString(j));
            try {
               Thread.sleep(ABSTAND_IN_MS);
            } catch (Exception ignore) {}
            anfragen.add(new ScanFor(j));// mit autostart
         }
         for (ScanFor scanFor:anfragen)
            scanFor.get();// warte bis all die Anfragen durch sind
      } catch (InterruptedException|ExecutionException e) {
         e.printStackTrace();
         throw e;
      }
      return "fertig";
   }
   @Override
   protected void process(List<String> chunks) {
      for (String s:chunks)
         progressBar.setString(s);
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
         exec.submit(this);// automatic execute in threadpool
      }
      @Override
      protected String doInBackground() throws Exception {
         scanFor(j);
         offen.clear(j);
         return null;
      }
      @Override
      protected void done() {
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
            // Data.data.unconfirmed.add(tasmota); // System.out.println(tasmota);
            System.out.print(" "+i);
            ArrayList<String> erg=tasmota.request(Tasmota.SUCHANFRAGE);
            if (erg.size()>1) {// Es ist eine Antwort gekommen
               Data.data.tasmotas.add(tasmota);
               // Data.data.unconfirmed.remove(tasmota);
               System.out.println(">>"+i);
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

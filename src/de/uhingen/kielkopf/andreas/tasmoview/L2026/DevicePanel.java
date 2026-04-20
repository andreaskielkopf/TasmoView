/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026;

import java.awt.FlowLayout;
import java.util.concurrent.*;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.devices.Device;

/**
 * @author Andreas Kielkopf
 *
 */
public class DevicePanel extends JPanel {
   private static final long serialVersionUID=1L;
   private JCheckBox         OkBox;
   private NetworkScanner    ns;
   /**
    * Create the panel.
    */
   public DevicePanel(NetworkScanner ns_) {
      initialize();
      if (ns_ instanceof NetworkScanner n) {
         this.ns=n;
         createButtons(this);
         ns.merge();
         mark(this);
      }
   }
   /**
    * @param devicePanel
    * 
    */
   private void mark(DevicePanel devicePanel) {
      devicePanel.getOkBox().setSelected(false);
      Thread.startVirtualThread(() -> {// NetworkScanner.X().execute(
         try {
            var a=ns.mark(); // Thread.currentThread().setName(TOOL_TIP_TEXT_KEY)
            while (!a.await(1000, TimeUnit.MILLISECONDS))
               SwingUtilities.invokeLater(() -> devicePanel.getOkBox().setText("Coundown = " + a.getCount()));
            a.await();
            SwingUtilities.invokeLater(() -> {
               var ok=devicePanel.getOkBox();
               ok.setText("fertig");
               ok.setSelected(true);
               ok.repaint(1000);
            });
         } catch (InterruptedException _) { /* ignore */ }
      });
   }
   void createButtons(DevicePanel devicePanel) {
      Thread.startVirtualThread(() -> {// NetworkScanner.X().execute(() -> {
         final var changeQueue=new LinkedTransferQueue<Device>();
         final var buttons=new ConcurrentHashMap<DeviceButton, Device>();
         ns.register(changeQueue, Device.class);
         while (changeQueue instanceof LinkedTransferQueue<Device> q)
            try {
               while (q.poll(1000, TimeUnit.MILLISECONDS) instanceof Device d)
                  SwingUtilities.invokeLater(() -> {
                     if (d.getID() instanceof ID id) {
                        for (var entry:buttons.entrySet())
                           if (entry.getValue() instanceof Device dv //
                                    && dv.getID() instanceof ID id2//
                                    && (id.compareTo(id2) == 0) //
                                    && entry.getKey() instanceof DeviceButton b) {
                              // Update vorhandenen Button
                              entry.setValue(d);
                              b.setDevice(d);
                              return;
                           }
                        var b=new DeviceButton(d);
                        buttons.put(b, d);
                        devicePanel.add(b);
                        devicePanel.revalidate();
                        devicePanel.repaint(1000);
                     }
                  });
            } catch (InterruptedException e) {/* ignore */ }
      });
   }
   private void initialize() {
      setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
      add(getOkBox());
   }
   public JCheckBox getOkBox() {
      if (OkBox == null) {
         OkBox=new JCheckBox("");
      }
      return OkBox;
   }
}

/**
 *
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026;

import java.awt.FlowLayout;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.devices.Device;
import de.uhingen.kielkopf.andreas.tasmoview.L2026.devices.ID;

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
      if (ns_ instanceof final NetworkScanner n) {
         ns=n;
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
            final var a=ns.mark(); // Thread.currentThread().setName(TOOL_TIP_TEXT_KEY)
            while (!a.await(1000, TimeUnit.MILLISECONDS))
               SwingUtilities.invokeLater(() -> devicePanel.getOkBox().setText("Coundown = " + a.getCount()));
            a.await();
            SwingUtilities.invokeLater(() -> {
               final var ok=devicePanel.getOkBox();
               ok.setText("fertig");
               ok.setSelected(true);
               ok.repaint(1000);
            });
         } catch (final InterruptedException _) { /* ignore */ }
      });
   }
   void createButtons(DevicePanel devicePanel) {
      Thread.startVirtualThread(() -> {// NetworkScanner.X().execute(() -> {
         final var changeQueue=new LinkedTransferQueue<Device>();
         final var buttons=new ConcurrentHashMap<DeviceButton, Device>();
         ns.register(changeQueue, Device.class);
         while (changeQueue instanceof final LinkedTransferQueue<Device> q)
            try {
               while (q.poll(1000, TimeUnit.MILLISECONDS) instanceof final Device d)
                  SwingUtilities.invokeLater(() -> {
                     if (d.getID() instanceof final ID id) {
                        for (final var entry:buttons.entrySet())
                           if (entry.getValue() instanceof final Device dv //
                                    && dv.getID() instanceof final ID id2//
                                    && id.compareTo(id2) == 0 //
                                    && entry.getKey() instanceof final DeviceButton b) {
                              // Update vorhandenen Button
                              entry.setValue(d);
                              b.setDevice(d);
                              return;
                           }
                        final var b=new DeviceButton(d);
                        buttons.put(b, d);
                        devicePanel.add(b);
                        devicePanel.revalidate();
                        devicePanel.repaint(1000);
                     }
                  });
            } catch (final InterruptedException e) {/* ignore */ }
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

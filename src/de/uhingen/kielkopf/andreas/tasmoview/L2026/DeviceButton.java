/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026;

import java.awt.Desktop;
import java.io.IOException;
import java.net.*;

import javax.swing.JButton;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.devices.Device;

/**
 * Ein Button der einen Link zur HTTP-Seite des Device bereitstellt
 * 
 * @author Andreas Kielkopf
 *
 */
public class DeviceButton extends JButton {
   private static final long serialVersionUID=-7847555817922554768L;
   private Device            device;
   /**
    * @return
    */
   public Device getDevice() {
      return device;
   }
   /**
    * @param d
    */
   public void setDevice(Device d) {
      this.device=d;
      setToolTipText(d.toString());
   }
   /**
    * @param device_
    *           Device zu dem der Link produzieret wird
    * 
    */
   public DeviceButton(@NonNull Device device_) {
      super();
      if (device_ instanceof Device d) {
         setDevice(d);
         setText(d.getID().getName());
         addActionListener(_ -> Thread.ofVirtual().start(() -> {// System.out.println(device);
            if (getDevice().getID().getIn4() instanceof Inet4Address in4a)
               openURL(in4a.getHostAddress());
         }));
      }
   } // Frame
   /**
    * Öffnet diese URL im Browser
    * 
    * @param host
    */
   public static void openURL(String host) {
      try {
         if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http", "andreas:akf4sonoff", host, -1, null, null, null));
            /// http://andreas:akf4sonoff@192.168.178.28
         } else {
            System.err.println("Desktop is not suportet. trying Runtime.exec");
            final var ziel="http://" + host;
            try (var process=Runtime.getRuntime().exec(switch (System.getProperty("os.name")) {
               case String s when s.contains("win") -> //
                        new String[] {"rundll32", "url.dll,FileProtocolHandler", ziel};
               case String s when s.contains("mac") || s.contains("darwin") -> //
                        new String[] {"open", ziel};
               case String s when s.contains("nix") || s.contains("nux") || s.contains("aix") -> //
                        new String[] {"xdg-open", ziel};
               default -> throw new UnsupportedOperationException(
                        "Browser-start not supported for:" + System.getProperty("os.name"));
            })) {
               process.waitFor();
            }
         }
      } catch (InterruptedException | IOException | URISyntaxException e) {
         e.printStackTrace();
      }
   }
}

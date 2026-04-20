/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026.devices;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.ID;
import de.uhingen.kielkopf.andreas.tasmoview.L2026.NetworkScanner;

/**
 * @author Andreas Kielkopf
 *
 */
public final class Ipdevice implements Device {
   private static final long      serialVersionUID=-6575398051194179404L;
   final private @NonNull ID      id;
   final private @NonNull Instant since;
   static private Builder         builder         =                      //
            HttpRequest.newBuilder().GET()                               //
                     .timeout(Duration.ofSeconds(10))                    //
                     .header("Accept", "text/html");
   /**
    * @param d
    *           Device aus dem das generiert wurde
    */
   public Ipdevice(@NonNull Device d) {
      if (d.getID().getIn4() == null)
         throw new NullPointerException("Die IP darf nicht null sein");
      this.id=d.getID();
      this.since=d.getSince();
   }
   @Override
   public @NonNull ID getID() {
      return id;
   }
   @Override
   public @NonNull Instant getSince() {
      return since;
   }
   @Override
   public String toString() {
      var sb=new StringBuilder(getClass().getSimpleName());
      sb.append(" ").append(since.toString().substring(0, 19).replace('T', ' '));
      sb.append(" ").append(getID());
      return sb.toString();
   }
   @Override
   public Device upgrade() {
      if (id.getIn4() instanceof Inet4Address in4a)
         try {
            var request=builder.copy().uri(URI.create("http://" + in4a.getHostAddress())).build();
            var response=NetworkScanner.getClient1().send(request, HttpResponse.BodyHandlers.ofString());
            return new Httpdevice(this).withResponse(response);
         } catch (IOException | InterruptedException _) { /* ignore */ }
      return null;
   }
   /**
    * @param inet4Address
    * @return
    */
   public Device withHostname() {
      id.getHostName();
      return this;
   }
}

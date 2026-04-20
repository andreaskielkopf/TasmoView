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
public final class Httpdevice implements Device {
   private static final long                  serialVersionUID=8232906367082339227L;
   private @NonNull ID                        id;
   private @NonNull Instant                   since;
   // private String pass ="andreas:akf4sonoff";
   @SuppressWarnings("unused") private String body;
   private int                                statusCode;
   static private Builder                     builder         =                     //
            HttpRequest.newBuilder().GET()                                          //
                     .timeout(Duration.ofSeconds(10))                               //
                     .header("Accept", "text/html");
   /**
    * @param d
    *           Device aus dem das generiert wurde
    * 
    */
   public Httpdevice(@NonNull Device d) {
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
      sb.append(" ");
      sb.append(since.toString().substring(0, 19).replace('T', ' ')).append(" ");
      sb.append(getID());
      if (statusCode != 0)
         sb.append(" ").append(statusCode);
      return sb.toString();
   }
   /**
    * @param response
    * @return
    */
   Httpdevice withResponse(HttpResponse<String> response_) {
      setBody(response_.body());
      setStatus(response_.statusCode());
      return this;
   }
   /**
    * @param body
    */
   private void setBody(String body_) {
      body=body_;
   }
   /**
    * @param statusCode
    */
   private void setStatus(int statusCode_) {
      statusCode=statusCode_;
   }
   @Override
   public Device upgrade() {
      if (id.getIn4() instanceof Inet4Address in4a && (statusCode != 200)) // nur wenn notwendig upgraden
         try {
            var request=builder.copy().uri(URI.create("http://" + in4a.getHostAddress())).build();
            var response=NetworkScanner.getClientA().send(request, HttpResponse.BodyHandlers.ofString());
            return new Httpdevice(this).withResponse(response);
         } catch (IOException | InterruptedException _) { /* ignore */ }
      return null;
   }
}

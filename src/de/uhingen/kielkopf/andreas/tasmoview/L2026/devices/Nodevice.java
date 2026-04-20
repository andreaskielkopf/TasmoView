/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026.devices;

import java.net.Inet4Address;
import java.time.Instant;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.ID;

/**
 * @author Andreas Kielkopf
 *
 */
public final class Nodevice implements Device {
   private static final long serialVersionUID=5872690568389687908L;
   @NonNull final private ID id;
   /**
    * @param in
    *           Ip-Adresse nach der gesucht wurde
    */
   public Nodevice(Inet4Address in) {
      this.id=new ID(in);
   }
   @Override
   public ID getID() {
      return id;
   }
   @SuppressWarnings("null")
   @Override
   public @NonNull Instant getSince() {
      return Instant.now();
   }
   @Override
   public Device upgrade() {
      if (id.isReachable())
         return new Ipdevice(this).withHostname();
      return null; // oder besser this ?
   }
   @Override
   public String toString() {
      return getClass().getSimpleName() + " " + getID().toString();
   }
}

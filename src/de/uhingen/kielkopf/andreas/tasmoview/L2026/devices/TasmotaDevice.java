/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026.devices;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.ID;

/**
 * @author Andreas Kielkopf
 *
 */
public final class TasmotaDevice implements Device {
   private static final long      serialVersionUID=3534884870265736424L;
   @NonNull final private ID      id;
   @NonNull final private Instant since;
  
   /**
    * @param d
    *           Device aus dem dies generiert wurde
    */
   public TasmotaDevice(@NonNull Device d) {
      this.id=d.getID();
      this.since=d.getSince();
   }
 
   @Override
   public @NonNull ID getID() {
      return id;
   }
   @Override
   public String toString() {
      return getClass().getSimpleName() + " " + getID().toString();
   }
   @Override
   public @NonNull Instant getSince() {
      return since;
   }
}

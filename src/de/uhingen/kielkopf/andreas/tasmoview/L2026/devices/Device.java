/**
 *
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026.devices;

import java.io.Serializable;
import java.time.Instant;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Andreas Kielkopf
 *
 */
public sealed interface Device extends Device_ID, Comparable<Device>, Serializable
         permits TasmotaDevice, Nodevice, Ipdevice, Httpdevice {
   /**
    * @return id des Device
    */
   @NonNull
   ID getID();
   /**
    * @return Seit wann existiert dieses Device
    */
   @NonNull
   Instant getSince();
   /**
    * @return ein upgraded Device wenn möglich
    */
   default Device upgrade() {
      return null; // oder besser this ?
   }
   @Override
   default int compareTo(Device o) {
      return getID().compareTo(o.getID());
   }
}

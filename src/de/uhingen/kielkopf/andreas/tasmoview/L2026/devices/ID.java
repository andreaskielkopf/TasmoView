/**
 *
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026.devices;

import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Eindeutige Kennung für Devices
 *
 * @author Andreas Kielkopf
 *
 */
public final class ID implements Device_ID, Comparable<ID>, Serializable {
   private static final long serialVersionUID=6708041368133280587L;
   private Inet4Address      in4;
   private long              mac;
   private String            hostname;
   private String            name;
   /**
    * @param in4_
    *           IPadresse
    */
   public ID(Inet4Address in4_) {
      this(in4_, 0);
   }
   /**
    * @param in4_
    *           IPadresse
    * @param mac_
    *           MAC
    */
   public ID(Inet4Address in4_, long mac_) {
      setIn4(in4_);
      setMac(mac_);
   }
   private static int toInt(byte[] b) {
      return b instanceof byte[] && b.length >= 4
               ? (b[0] & 0xFF) << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | b[3] & 0xFF
               : 0;
   }
   @Override
   public int compareTo(ID o) {
      final var c=Long.compareUnsigned(getMac(), o.getMac());
      if (c != 0)
         return c;
      if (getIn4() instanceof final Inet4Address i && o.getIn4() instanceof final Inet4Address oi)
         return Integer.compareUnsigned(toInt(i.getAddress()), toInt(oi.getAddress()));
      if (getIn4() instanceof Inet4Address)
         return -1;
      if (o.getIn4() instanceof Inet4Address)
         return 1;
      return 0;
   }
   /**
    * @return mac
    */
   public long getMac() {
      return mac;
   }
   /**
    * @param mac_
    *           mac
    */
   public void setMac(long mac_) {
      mac=mac_;
   }
   /**
    * @return ip
    */
   public @Nullable Inet4Address getIn4() {
      return in4;
   }
   /**
    * @param in4_
    *           IP-Adresse
    */
   public void setIn4(Inet4Address in4_) {
      in4=in4_;
   }
   /**
    * @param m
    *           mac
    * @return MAC-formattet Hexstring
    */
   static public String getMAC(long m) {
      final var sb=new StringBuilder(Long.toHexString(m));
      sb.insert(1, "0".repeat(16 - sb.length()));
      for (var i=2; i < 22; i+=3)
         sb.insert(i, ":");
      return sb.toString();
   }
   /**
    * @return ist die IP erreichbar ? (10 Sek Timeout !)
    */
   public boolean isReachable() {
      if (in4 instanceof final Inet4Address in4a)
         try {
            return in4a.isReachable(10000);
         } catch (final IOException _) { /* ignore */ }
      return false;
   }
   @Override
   public String toString() {
      final var sb=new StringBuilder(getName()); // else // sb.append(in4);
      if (mac != 0)
         sb.append("(").append(getMAC(mac)).append(")");
      return sb.toString();
   }
   /**
    * @return
    */
   @SuppressWarnings("null")
   public @NonNull String getHostName() {
      if (hostname == null)
         if (in4 instanceof final Inet4Address in4a && in4a.getHostName() instanceof final String hn)
            hostname=hn;
         else
            hostname="";
      return hostname;
   }
   @SuppressWarnings("null")
   public @NonNull String getName() {
      if (name == null) {
         final var hn=getHostName();
         if (hn.matches("^[0-9.]+$"))
            name=hn;
         else
            name=hn.split("[.]")[0];
      }
      return name;
   }
}

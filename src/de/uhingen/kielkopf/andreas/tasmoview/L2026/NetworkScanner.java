/**
 * 
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import de.uhingen.kielkopf.andreas.tasmoview.L2026.devices.Device;
import de.uhingen.kielkopf.andreas.tasmoview.L2026.devices.Nodevice;

/**
 * NetzwerkScanner der nach erreichbaren Devices sucht, und diese an registrierte Queues meldet
 * 
 * @author Andreas Kielkopf
 *
 */
public class NetworkScanner implements Notifier<Device_ID> {
   private static NetworkScanner   me;
   final private ConcurrentSkipListMap<ID, Device>                                                  //
                                   devices    =new ConcurrentSkipListMap<ID, Device>();
   static private ExecutorService  exec       =Executors.newVirtualThreadPerTaskExecutor();
   transient ConcurrentHashMap<WeakReference<LinkedTransferQueue<? extends Device_ID>>, Class<?>>   //
                                   notifiers;
   private LinkedTransferQueue<ID> changeQueue=new LinkedTransferQueue<>();
   final private String            name;
   private static Authenticator    auth;
   private static HttpClient       client1, clientA;
   public static HttpClient getClient1() {
      if (client1 == null)
         client1=HttpClient.newBuilder().build();
      return client1;
   }
   public static HttpClient getClientA() {
      if (auth == null)
         auth=new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication("andreas", "akf4sonoff".toCharArray());
            }
         };
      if (clientA == null)
         if (HttpClient.newBuilder().authenticator(auth) instanceof Builder b)
            clientA=((getClient1().executor() instanceof Optional<Executor> o && o.isPresent()) //
                     ? b.executor(o.get())
                     : b).build();
      return clientA;
   }
   /**
    * @throws InterruptedException
    */
   public NetworkScanner(Class<?> c) {
      if (me != null)
         throw new UnsupportedOperationException("Es darf nur ein(1) Netzwerk geben");
      super();
      name=c.getSimpleName();
      shutdownHook();
      me=this;
   }
   NetworkScanner logOutput() {
      exec.execute(() -> {
         register(changeQueue, ID.class);
         while (changeQueue instanceof LinkedTransferQueue<ID> q)
            try {
               while (q.poll(100, TimeUnit.MILLISECONDS) instanceof ID id) {
                  if (devices.get(id) instanceof Device d)
                     System.out.println(d);
               }
            } catch (InterruptedException e) {/* ignore */ }
      });
      return this;
   }
   private void shutdownHook() {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         System.out.println("Shutdown-Hook");
         if (client1 != null)
            client1.shutdownNow();
         if (clientA != null)
            clientA.shutdownNow();
         exec.shutdownNow();
         save(devices);
         try {
            Thread.sleep(500);
         } catch (InterruptedException _) {/* ignore */}
         // System.out.println("Aufräumen fertig.");
      }));
   }
   private Device replace(ID id1, Device d2) {
      if (id1 instanceof ID) // bisherigen Eintrag entfernen
         devices.remove(id1);
      var id2=d2.getID(); // neuen Eintrag einfügen
      devices.put(id2, d2);
      if (!(d2 instanceof Nodevice)) { // wenn sinnvoll
         notify(id2); // benachrichtige ID
         notify(d2); // benachrichtige Device
      }
      return d2;
   }
   @Override
   public ConcurrentHashMap<WeakReference<LinkedTransferQueue<? extends Device_ID>>, Class<?>> notifiers() {
      if (notifiers == null)
         notifiers=new ConcurrentHashMap<>();
      return notifiers;
   }
   /**
    * @return
    */
   public static List<InterfaceAddress> getInterfaces() {
      var addresses=new ArrayList<InterfaceAddress>();
      try {
         var nets=NetworkInterface.getNetworkInterfaces();
         while (nets.hasMoreElements())
            if (nets.nextElement() instanceof NetworkInterface ni && !ni.isLoopback() && ni.isUp())
               for (var ifadr:ni.getInterfaceAddresses())
                  if (ifadr.getAddress() instanceof Inet4Address)
                     addresses.add(ifadr);
      } catch (SocketException _) { /* ignore */ }
      return addresses;
   }
   /**
    * Mache eine Liste aller lokal möglichen IPs (nur IPv4)
    * 
    * @return liste der IPs
    */
   private Map<ID, Device> getLocalDevices() {
      if (devices.isEmpty())
         for (var ifadr:getInterfaces()) {
            System.out.println(ifadr.getAddress().getHostAddress() + " : prefix=" + ifadr.getNetworkPrefixLength());
            final var bcast=ifadr.getBroadcast();
            final var myint=ByteBuffer.wrap(ifadr.getAddress().getAddress()).getInt();
            final var anzahl=1 << (32 - ifadr.getNetworkPrefixLength());
            final var bb=ByteBuffer.allocate(4);
            /// Suche alle Adressen die im gleichen Netz liegen
            for (var i=1; i < anzahl; i++)
               try {
                  if (InetAddress
                           .getByAddress(bb.clear().putInt(myint ^ i).flip().array()) instanceof final Inet4Address in4a
                           && !bcast.equals(in4a))
                     replace(null, new Nodevice(in4a));
               } catch (UnknownHostException _) { /* ignore */ }
         }
      return devices;
   }
   /**
    * @param devices2
    */
   private void save(Map<ID, Device> devices2) {
      final var gesamt=new ArrayList<Object>();
      final var info=new ArrayList<String>();
      gesamt.add(name);
      final var device2=new ConcurrentSkipListMap<ID, Device>();
      for (var entry:devices.entrySet())
         if (entry.getValue() instanceof Device d && !(d instanceof Nodevice)) {
            device2.put(entry.getKey(), d);
            info.add(d.toString());
         }
      gesamt.add(device2);
      try (var oos=new ObjectOutputStream(new FileOutputStream(name + ".ser"))) {
         oos.writeObject(gesamt);
         Files.write(Paths.get(name + ".txt"), info, StandardCharsets.UTF_8);
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }
   /**
    * @param devices2
    */
   public void merge() {
      getLocalDevices();
      var file=Path.of(name + ".ser").toFile();
      if (file.exists())
         try (var ois=new ObjectInputStream(new FileInputStream(file))) {
            if (ois.readObject() instanceof ArrayList al && al.get(1) instanceof ConcurrentSkipListMap d2)
               for (var obj:d2.values())
                  if (obj instanceof Device d)
                     replace(null, d);
         } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
         }
   }
   CountDownLatch mark() {
      var a=new ArrayList<Device>(getLocalDevices().values());
      final var cdl=new CountDownLatch(a.size());
      for (var d:a)
         exec.execute(() -> {
            try {
               Thread.currentThread().setName(d.getID().toString());
               var device=d;
               while (device.upgrade() instanceof Device ud) {
                  device=ud;
                  System.out.println(device + " upgrade to " + ud);
               }
               if (device instanceof Device && device != d)
                  replace(d.getID(), device); // upgraden bis es nicht mehr geht
               // long nr=cdl.getCount();
               cdl.countDown(); // Fertigmeldung
               // System.out.print(nr);
               // System.out.print((nr % 10 == 0) ? System.lineSeparator() : " ");
            } catch (Exception e) {
               System.err.println(e.toString());
            }
         });
      return cdl;
   }
   /**
    * @param args
    * @throws IOException
    * @throws InterruptedException
    */
   public static void main(String[] args) throws IOException, InterruptedException {
      var ns=new NetworkScanner(NetworkScanner.class).logOutput();
      ns.merge();
      ns.mark().await(); // warte bis der Scan durch ist
      Thread.sleep(1000);
   }
   /**
    * @return
    * @return
    */
   public static ExecutorService X() {
      return exec;
   }
}

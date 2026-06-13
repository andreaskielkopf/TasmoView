/**
 * Notifier-Pattern als Interface mit default Methoden
 */
package de.uhingen.kielkopf.andreas.tasmoview.L2026;

import java.lang.ref.WeakReference;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;

/**
 * @author Andreas Kielkopf
 * @param <T>
 *           sealed Interface der Objekte die beobachtet werden sollen
 *
 */
public interface Notifier<T> {
   /**
    * @return liefer eine Passende HashMap in die die registrierungen eingetragen werden können
    */
   ConcurrentHashMap<WeakReference<LinkedTransferQueue<? extends T>>, Class<?>> notifiers();
   /**
    * Registriert eine Queue die Objekte vom Typ <T> zurückmeldet, wenn sie veränderungen erfahren haben
    *
    * Ist <T> eine Klasse, so können sich mehrere Objekte beim Notifier registrieren, und erhalten dann Objekte der Klasse <T>, wenn diese per
    * notify(<T> o) verschickt werden.
    *
    * Ist <T> ein (sealed) Interface, dann können Objekte aller Klassen per notify() übergeben werden, die dieses Interface implementieren.
    *
    * Wenn beim Registrieren eine Queue verwendet wird, die nur an einer der Klassen interessiert ist, muss sie das durch angabe der gewünschten
    * Klasse als 2. Parameter mitteilen
    *
    * @param queue
    *           für Objekte der Klasse <T>
    * @param c
    *           gewünschte Klasse oder Interface
    */
   default void register(LinkedTransferQueue<? extends T> queue, Class<? extends T> c) {
      notifiers().put(// Sobald die Queue stirbt, wird sie automatisch aus der Liste entfernt
               new WeakReference<>(queue)//
               , c); // Class der Objekte die gewünscht werden
   }
   /**
    * Informiere alle Listener über änderungen an den Objekten.
    *
    * Jeder Listener wird über Objekte informiert, für die er registriert ist indem das Objekt in die Queue eingestellt wird. Die Queue wird nur über
    * eine WeakReference gehalten. Sobald die Queue stirbt, wird sie automatisch aus der Liste entfernt.
    *
    * @param o
    */
   @SuppressWarnings("unchecked") // Der cast in die Queue ist vorher explizit getestet
   default void notify(T o) {
      for (final Entry<WeakReference<LinkedTransferQueue<? extends T>>, Class<?>> e:notifiers().entrySet())
         // try {
         if (e.getKey().get() instanceof final LinkedTransferQueue q) { // wenn die queue noch lebt
            if (e.getValue() instanceof final Class c && c.isInstance(o)) // und das Objekt von der gewünschten Klasse ist
               q.offer(c.cast(o)); // Objekt in die queue einstellen
         } else {
            notifiers().remove(e.getKey());// remove lost weak Reference
         }
      // } catch (ClassCastException _) {/**/}
   }
}

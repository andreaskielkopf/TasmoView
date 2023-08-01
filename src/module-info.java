/**
 * 
 */
/**
 * @author Andreas Kielkopf
 *
 */
module TasmoView {
   exports de.uhingen.kielkopf.andreas.tasmoview.grafik;
   exports de.uhingen.kielkopf.andreas.tasmoview.sensors;
   exports de.uhingen.kielkopf.andreas.tasmoview.tasks;
   exports de.uhingen.kielkopf.andreas.tasmoview.table;
   exports de.uhingen.kielkopf.andreas.tasmoview;
   requires transitive Beans;
   requires transitive java.desktop;
   requires java.net.http;
   requires transitive java.prefs;
   requires org.eclipse.jdt.annotation;
}

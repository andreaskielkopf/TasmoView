package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.grafik.Skala;

/**
 * Panel das die Kurven der Gewünschten Sensoren darstellt
 *
 * @author andreas
 *
 */
public class SensorGraphPanel extends JPanel {
   private static final long                          serialVersionUID=8079918630139793226L;
   private final CopyOnWriteArrayList<Sensor>         sensorList      =new CopyOnWriteArrayList<>();
   // private Skala zeitSkala =new Skala("Zeit");
   private JLayeredPane                               layerdpPane;
   private int                                        oldh;
   private int                                        oldw;
   private AffineTransform                            at;
   // TreeSet<String> sensorTypen=new TreeSet<String>();
   private final ConcurrentSkipListMap<String, Skala> skalen          =new ConcurrentSkipListMap<>();
   public SensorGraphPanel() {
      setOpaque(false);
      // zeitSkala.setVertical(false);
      // setForeground(Color.RED);
      setBorder(new TitledBorder(null, "Gp", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      setLayout(new BorderLayout(0, 0));
      add(getLayeredPane(), BorderLayout.CENTER);
   }
   private void calculate() {
      if ((oldw == 0) || (oldh == 0))
         return;
      at=new AffineTransform();
      at.translate(0, oldh);
      at.scale(1, -1);
      at.translate(10, 10);
      for (final Skala skala:skalen.values())
         skala.setSize(oldw, oldh);
      getLayeredPane().setSize(oldw, oldh);
   }
   private JLayeredPane getLayeredPane() {
      if (layerdpPane == null) {
         layerdpPane=new JLayeredPane();
         layerdpPane.setBounds(new Rectangle(0, 0, 4000, 2000));
      }
      return layerdpPane;
   }
   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      final Graphics2D g2d=(Graphics2D) g.create(); // Kontext abkoppeln
      final int        w  =getWidth();
      final int        h  =getHeight();
      if ((h != oldh) || (w != oldw)) {
         oldh=h;
         oldw=w;
         calculate();
      }
      // Koordinatensystem links unten
      g2d.transform(at);
      for (final Skala skala:skalen.values())
         skala.paintSkala(g2d);
   }
   /** Wenn notwendig die Skala anpassen */
   public void recalculateSkala(Sensor sensor) {
      if (!sensorList.contains(sensor))
         return;
      final Skala skala=skalen.get(sensor.typ);
      if (skala == null)
         return;
      double  min   =0;
      double  max   =0;
      boolean isNull=true;
      for (final Sensor s:sensorList)
         if (s.typ.equals(sensor.typ)) {
            if (isNull) {
               min=s.getMinWert();
               max=s.getMaxWert();
               isNull=false;
            } else {
               min=Math.min(min, s.getMinWert());
               max=Math.max(max, s.getMaxWert());
            }
         }
      if (!isNull)
         skala.setRaster(min, max);
      repaint(1000); // später mal neu zeichnen
   }
   /**
    * aktiviert die Kurven aller übergebenen Sensoren
    *
    * @param sl
    *           sensorliste
    */
   public void setSensors(Collection<Sensor> sl) {
      if ((sl == null) || sl.isEmpty())
         sensorList.addAll(Data.getData().gesamtSensoren);
      else {
         sensorList.retainAll(sl);
         sensorList.addAllAbsent(sl);
      }
      for (final Skala skala:skalen.values()) {
         skala.graphSensoren.clear();
         skala.show(false);
      }
      for (final Sensor sensor:sensorList) {
         if (!skalen.containsKey(sensor.typ))
            skalen.put(sensor.typ, new Skala(sensor.typ));
         skalen.get(sensor.typ).graphSensoren.add(sensor);
      }
      for (final Skala skala:skalen.values())
         skala.recalculateGrenzwerte();
      calculate();
      for (final Sensor sensor:sensorList)
         (skalen.get(sensor.typ)).show(true);
      repaint(1000);
   }
}

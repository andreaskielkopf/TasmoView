package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.grafik.Skala;

public class SensorGraphPanel extends JPanel {
   private static final long                    serialVersionUID=8079918630139793226L;
   private CopyOnWriteArrayList<Sensor>         sensorList      =new CopyOnWriteArrayList<Sensor>();
   // private Skala zeitSkala =new Skala("Zeit");
   private JLayeredPane                         layerdpPane;
   private int                                  oldh;
   private int                                  oldw;
   private AffineTransform                      at;
   // TreeSet<String> sensorTypen=new TreeSet<String>();
   private ConcurrentSkipListMap<String, Skala> skalen          =new ConcurrentSkipListMap<String, Skala>();
   public SensorGraphPanel() {
      setOpaque(false);
      // zeitSkala.setVertical(false);
      // setForeground(Color.RED);
      setBorder(new TitledBorder(null, "Gp", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      setLayout(new BorderLayout(0, 0));
      add(getLayeredPane(), BorderLayout.CENTER);
   }
   public void setSensors(Collection<Sensor> sl) {
      if (sl.isEmpty()) this.sensorList.addAll(Data.data.sensoren);
      else {
         this.sensorList.retainAll(sl);
         this.sensorList.addAllAbsent(sl);
      }
      for (Skala skala:skalen.values()) {
         skala.sensoren.clear();
         skala.show(false);
      } // skalen.clear();
      for (Sensor sensor:sensorList) {
         if (!skalen.containsKey(sensor.typ)) skalen.put(sensor.typ, new Skala(sensor.typ));
         Skala sk=skalen.get(sensor.typ);
         sk.show(true);
         sk.sensoren.add(sensor);
      }
      for (Skala skala:skalen.values()) {
         skala.recalculateGrenzwerte();
         // skala.calculateSkala();
      }
      calculate();
      repaint(1000);
   }
   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d=(Graphics2D) g.create(); // Kontext abkoppeln
      int        w  =getWidth();
      int        h  =getHeight();
      if ((h!=oldh)||(w!=oldw)) {
         oldh=h;
         oldw=w;
         calculate();
      }
      // Koordinatensystem links unten
      g2d.transform(at);
      for (Skala skala:skalen.values()) skala.paintSkala(g2d);
   }
   private void calculate() {
      if (oldw==0) return;
      if (oldh==0) return;
      at=new AffineTransform();
      at.translate(0, oldh);
      at.scale(1, -1);
      at.translate(10, 10);
      for (Skala skala:skalen.values()) skala.setSize(oldw, oldh);
      getLayeredPane().setSize(oldw, oldh);
   }
   private JLayeredPane getLayeredPane() {
      if (layerdpPane==null) {
         layerdpPane=new JLayeredPane();
         layerdpPane.setBounds(new Rectangle(0, 0, 4000, 2000));
      }
      return layerdpPane;
   }
   /* Wenn notwendig die Skala anpassen */
   public void recalculateSkala(Sensor sensor) {
      if (!sensorList.contains(sensor)) return;
      Skala skala=skalen.get(sensor.typ);
      if (skala==null) return;
      Double min=null;
      Double max=null;
      for (Sensor s:sensorList) if (s.typ.equals(sensor.typ)) {
         if (min==null) {
            min=s.getMinWert();
            max=s.getMaxWert();
         } else {
            min=Math.min(min, s.getMinWert());
            max=Math.max(max, s.getMaxWert());
         }
      }
      if (min!=null) skala.setRaster(min, max);
      repaint(1000); // später mal neu zeichnen
   }
}

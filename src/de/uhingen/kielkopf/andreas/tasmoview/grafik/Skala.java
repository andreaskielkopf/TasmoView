package de.uhingen.kielkopf.andreas.tasmoview.grafik;

import java.awt.*;
import java.awt.geom.*;
import java.util.Map.Entry;
import java.util.TreeSet;

import de.uhingen.kielkopf.andreas.tasmoview.sensors.Sensor;

public class Skala {
   static private final Stroke  STROKE_DASHED=new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
            new float[] {3, 7}, 0);
   static private final Stroke  STROKE_TICKS =new BasicStroke(1.6f);
   static private final Stroke  STROKE_LABELS=new BasicStroke(2f);
   static private final Stroke  STROKE_GRAPHS=new BasicStroke(2f);
   static private int           MINORTICK    =7;
   static private int           MIDLETICK    =15;
   static private int           MAJORTICK    =20;
   // static private final long serialVersionUID=-1232799680846666655L;
   public final TreeSet<Sensor> graphSensoren=new TreeSet<>();
   private float                rulerpos     =0.5f;
   private Raster               raster       =null;
   // private Boolean vertical =true;
   private int                  oldw         =1;
   private int                  oldh         =1;
   private Color                color        =Color.MAGENTA;
   /* Die Ticks als Pfad */
   Path2D.Double                pTicks       =new Path2D.Double();
   /* Das Raster als Pfad */
   Path2D.Double                pRaster      =new Path2D.Double();
   // private AffineTransform at1 =new AffineTransform();
   private AffineTransform      at2;
   Rectangle2D.Double           r            =null;
   final String                 typ;
   private AffineTransform      at3;
   private boolean              visible      =true;
   public Skala(String t) {
      typ=t;
      color=switch (t) {
      case "Temperature" -> {
         rulerpos=0.2f;
         yield Color.RED;
      }
      case "Humidity" -> {
         rulerpos=0.7f;
         yield Color.BLUE;
      }
      default -> {
         rulerpos=0.4f;
         yield Color.MAGENTA;
      }
      };
   }
   /** berechne die Pfade für die Skala und das Raster neu */
   public void calculateSkala() {
      if (raster == null)
         return;
      double abstand=raster.rmax - raster.rmin;
      if ((oldw == 0) || (oldh == 0))
         return;
      synchronized (pTicks) {
         // System.out.println("recalculate "+typ+" "+raster.rmin+":"+raster.rmax);
         pTicks.reset();
         pRaster.reset();
         final float start=oldw * rulerpos;
         // System.out.println("start:"+start);
         for (final double mi:raster.getMinors()) {
            pTicks.moveTo(start, mi);
            pTicks.lineTo(start + MINORTICK, mi);
         }
         for (final double mi:raster.getMidles()) {
            pTicks.moveTo(start, mi);
            pTicks.lineTo(start + MIDLETICK, mi);
            // ra.moveTo(0, mi);
            // ra.lineTo(oldw, mi);
         }
         for (final double mi:raster.getMajors()) {
            pTicks.moveTo(start, mi);
            pTicks.lineTo(start + MAJORTICK, mi);
            pRaster.moveTo(0, mi);
            pRaster.lineTo(oldw, mi);
            // System.out.println(mi);
         }
         final double sy=(oldh - 20d) / abstand;
         at2=AffineTransform.getScaleInstance(1, sy);
         at2.translate(0, -raster.rmin);
         pRaster.transform(at2);
         pTicks.transform(at2);
      }
   }
   // public void setVertical(boolean vertical) { this.vertical=vertical; }
   public float getRulerpos() {
      return rulerpos;
   }
   public void paintSkala(Graphics2D g2d) {
      if (!visible)
         return;
      final AffineTransform tmp=g2d.getTransform();
      // g2d.transform(at1);
      g2d.setColor(color.darker());
      g2d.setFont(new Font("Dialog", Font.PLAIN, 19));
      synchronized (pTicks) {
         // System.out.println(typ);
         // g2d.setStroke(new BasicStroke(1f));
         g2d.setStroke(STROKE_DASHED);
         if (pRaster != null)
            g2d.draw(pRaster);
         g2d.setStroke(STROKE_TICKS);
         if (pTicks != null)
            g2d.draw(pTicks);
      }
      // AffineTransform at3=at2.createInverse();
      // try { g2d.transform(at2.createInverse()); } catch (NoninvertibleTransformException e) { e.printStackTrace(); }
      if (raster == null)
         return;
      g2d.scale(1, -1);
      final float start=oldw * (rulerpos + 0.025f);
      g2d.setStroke(STROKE_LABELS);
      for (final Entry<Double, String> entry:raster.getLabels().entrySet()) {
         final float f=(float) (((20 - oldh) * (entry.getKey().doubleValue() - raster.rmin))
                  / (raster.rmax - raster.rmin));
         g2d.drawString(entry.getValue(), start, f);
      }
      r=null;
      for (final Sensor sensor:graphSensoren)
         if (sensor.path.getCurrentPoint() != null) {
            if (r == null)
               r=(Rectangle2D.Double) sensor.path.getBounds2D().clone();
            else
               r.add(sensor.path.getBounds2D());
         }
      if (r != null) {
         g2d.scale(1, -1);
         r.setRect(r.getX(), r.getY() - 0.5d, r.getWidth(), r.getHeight() + 1d);
         final double sx=((oldw - 20) / (r.getWidth() + 0.1d));  // Sekunden
         final double sy=((oldh - 20) / (r.getHeight() + 0.1d)); // °C, %...
         at3=AffineTransform.getScaleInstance(sx, sy);
         at3.translate(-r.getX(), -r.getY());
         g2d.setStroke(STROKE_GRAPHS);
         g2d.setColor(color);
         for (final Sensor sensor:graphSensoren) {
            final Path2D.Double p=sensor.getPath();
            p.transform(at3);
            g2d.setColor(sensor.color);
            g2d.draw(p);
         }
      }
      g2d.setTransform(tmp);
   }
   /** berechne die Grenzwerte für min und max neu */
   public void recalculateGrenzwerte() {
      if (graphSensoren.isEmpty())
         return;
      double min=graphSensoren.first().getMinWert();
      double max=graphSensoren.first().getMaxWert();
      for (final Sensor sensor:graphSensoren) {
         min=Math.min(min, sensor.getMinWert());
         max=Math.max(max, sensor.getMaxWert());
      }
      if (raster == null)
         return;
      if (raster.isRasterChanged(min, max))
         calculateSkala();
   }
   /** Wenn notwendig das Raster anpassen */
   public void setRaster(double min, double max) {
      if (raster == null) {
         raster=new Raster(min, max);
         calculateSkala();
      }
      // neu berechnen weil sich die werte stark geändert haben
      if (raster.isRasterChanged(min, max))
         calculateSkala();
   }
   public void setRulerpos(float rulerpos1) {
      rulerpos=rulerpos1;
      calculateSkala();
   }
   public void setSize(int w, int h) {
      if ((h == oldh) && (w == oldw))
         return;
      oldh=h;
      oldw=w;
      calculateSkala();
   }
   public void show(boolean b) {
      visible=b;
   }
   @Override
   public String toString() {
      return "Skala [typ=" + typ + "]";
   }
}

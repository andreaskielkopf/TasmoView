package de.uhingen.kielkopf.andreas.tasmoview.grafik;

import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Raster verwaltet das Raster und die für eine Skala erforderlichen Werte */
public class Raster {
   static private DecimalFormat                        df3    =new DecimalFormat("###");
   static private DecimalFormat                        df2    =new DecimalFormat("##");
   static private DecimalFormat                        df1    =new DecimalFormat("#.#");
   static private DecimalFormat                        df0    =new DecimalFormat("#.##");
   double                                              rmin   =18d;
   double                                              rmax   =25d;
   double                                              versatz=0.2d * (rmax - rmin);
   private double                                      midle  =0.5d;
   private double                                      minor  =0.1d;
   private final CopyOnWriteArrayList<Double>          majors =new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<Double>          midles =new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<Double>          minors =new CopyOnWriteArrayList<>();
   private final ConcurrentSkipListMap<Double, String> labels =new ConcurrentSkipListMap<>();
   private DecimalFormat                               df;
   public Raster(double min, double max) {
      isRasterChanged(min, max);
   }
   ConcurrentSkipListMap<Double, String> getLabels() {
      return labels;
   }
   CopyOnWriteArrayList<Double> getMajors() {
      return majors;
   }
   CopyOnWriteArrayList<Double> getMidles() {
      return midles;
   }
   CopyOnWriteArrayList<Double> getMinors() {
      return minors;
   }
   /** setzt wenn erforderlich die Werte fürs Raster neu */
   boolean isRasterChanged(double min, double max) {
      final boolean inrange=(min > rmin) && (min < (rmin + versatz)) && (max > (rmax + versatz)) && (max < rmax);
      if (inrange)
         return false;
      final double abstand=max - min;
      rmin=min - (0.1 * abstand);
      rmax=max + (0.1 * abstand);
      versatz=0.2d * (rmax - rmin);
      double skala;
      if (abstand > 200d) {
         skala=100d;
         df=df3;
      } else
         if (abstand > 20d) {
            skala=10d;
            df=df2;
         } else
            if (abstand > 2d) {
               skala=1d;
               df=df1;
            } else {
               skala=0.1d;
               df=df0;
            }
      double major=skala / 1d;
      midle=skala / 2d;
      minor=skala / 10d;
      final double test=minor / 2;
      // double a=min/minor;
      // int b=
      final double rest=rmin % minor;
      // double rest2=min%midle;
      // double rest3=min%major;
      majors.clear();
      midles.clear();
      minors.clear();
      for (double i=rmin; i <= (rmax + rest); i+=minor) {
         final double j =Math.abs(i);
         final Double ii=Double.valueOf(i);
         // double s=Math.signum(i);
         if (((j % major) < test) || ((j % major) > (major - test)))
            majors.add(ii);
         else
            if ((j % midle) < test)
               midles.add(ii);
            else
               if ((j % midle) > (midle - test))
                  midles.add(ii);
               else
                  minors.add(ii);
      }
      labels.clear();
      for (final Double m:majors) {
         final String s=df.format(m);
         labels.put(m, s);
      }
      return true;
   }
   @Override
   public String toString() {
      return "Raster [rmin=" + rmin + ", rmax=" + rmax + "]";
   }
}

package de.uhingen.kielkopf.andreas.tasmoview.grafik;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

/** Raster verwaltet das Raster und die für eine Skala erforderlichen Werte */
public class Raster {
   double                                        rmin   =18d;
   double                                        rmax   =25d;
   double                                        versatz=0.2d*(rmax-rmin);
   private double                                major  =1d;
   private double                                midle  =0.5d;
   private double                                minor  =0.1d;
   private ArrayList<Double>                     majors =new ArrayList<Double>();
   private ArrayList<Double>                     midles =new ArrayList<Double>();
   private ArrayList<Double>                     minors =new ArrayList<Double>();
   private ConcurrentSkipListMap<Double, String> labels =new ConcurrentSkipListMap<Double, String>();
   private static DecimalFormat                  df3    =new DecimalFormat("###");
   private static DecimalFormat                  df2    =new DecimalFormat("##");
   private static DecimalFormat                  df1    =new DecimalFormat("#.#");
   private static DecimalFormat                  df0    =new DecimalFormat("#.##");
   private DecimalFormat                         df;
   public Raster(double min, double max) {
      isRasterChanged(min, max);
   }
   /** setzt wenn erforderlich die Werte fürs Raster neu */
   boolean isRasterChanged(Double min, Double max) {
      boolean inrange=(min>rmin)&&(min<rmin+versatz)&&(max>rmax+versatz)&&(max<rmax);
      if (inrange) return false;
      double abstand=max-min;
      rmin=min-0.1*abstand;
      rmax=max+0.1*abstand;
      versatz=0.2d*(rmax-rmin);
      double skala;
      if (abstand>200d) {
         skala=100d;
         df=df3;
      } else if (abstand>20d) {
         skala=10d;
         df=df2;
      } else if (abstand>2d) {
         skala=1d;
         df=df1;
      } else {
         skala=0.1d;
         df=df0;
      }
      major=skala/1d;
      midle=skala/2d;
      minor=skala/10d;
      double test=minor/2;
      // double a=min/minor;
      // int b=
      double rest=rmin%minor;
      // double rest2=min%midle;
      // double rest3=min%major;
      majors.clear();
      midles.clear();
      minors.clear();
      for (double i=rmin; i<=rmax+rest; i+=minor) {
         if ((i%major)<test) majors.add(i);
         else if ((i%major)>major-test) majors.add(i);
         else if ((i%midle)<test) midles.add(i);
         else if ((i%midle)>midle-test) midles.add(i);
         else minors.add(i);
      }
      labels.clear();
      for (Double m:majors) {
         String s=df.format(m);
         labels.put(m, s);
      }
      return true;
   }
   ArrayList<Double> getMajors() {
      return majors;
   }
   ArrayList<Double> getMinors() {
      return minors;
   }
   ArrayList<Double> getMidles() {
      return midles;
   }
   ConcurrentSkipListMap<Double, String> getLabels() {
      return labels;
   }
   @Override
   public String toString() {
      return "Raster [rmin="+rmin+", rmax="+rmax+"]";
   }
}

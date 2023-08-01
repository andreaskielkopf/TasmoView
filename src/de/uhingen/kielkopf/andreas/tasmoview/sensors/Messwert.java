package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import de.uhingen.kielkopf.andreas.beans.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.beans.minijson.JsonString;

public class Messwert implements Comparable<Messwert> {
   public final Instant    instant;
   public final JsonObject json;
   public double           value;
   // static DateTimeFormatter dtf=DateTimeFormatter.ofPattern("HH:mm:ss");
   public Messwert(Instant i, JsonObject j, double v) {
      instant=i;
      json=j;
      value=v;
   }
   public static Instant getInstant(JsonObject jsonObject) {
      if ((jsonObject instanceof JsonString js)) {
         if (js.name.equals("Time"))
            try {
               return Instant.parse(js.value + "Z");
            } catch (final Exception e) {
               e.printStackTrace();
            }
      }
      System.err.println("Unable to parse " + jsonObject + " : " + Instant.now());
      return Instant.now();
   }
   static public Messwert save2Messwert(String line) {
      return null;
   }
   @Override
   public int compareTo(Messwert o) {
      return instant.compareTo(o.instant);
   }
   public String save() {
      final StringBuilder sb=new StringBuilder();
      // LocalDateTime ldt=LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      // sb.append(dtf.format(ldt));
      sb.append(instant.truncatedTo(ChronoUnit.SECONDS));
      sb.append(",");
      sb.append(value);
      System.out.println(sb.toString());
      return sb.toString();
   }
}

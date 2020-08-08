package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.time.Instant;

import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonString;

public class Messwert implements Comparable<Messwert> {
   public final Instant    instant;
   public final JsonObject json;
   public double           value;
   public Messwert(Instant i, JsonObject j, double v) {
      instant=i;
      json=j;
      value=v;
   }
   static public Messwert save2Messwert(String line) {
      return null;
   }
   public String save() {
      StringBuilder sb=new StringBuilder();
      sb.append(instant);
      sb.append(",");
      sb.append(value);
      System.out.println(sb.toString());
      return sb.toString();
   }
   @Override
   public int compareTo(Messwert o) {
      return this.instant.compareTo(o.instant);
   }
   public static Instant getInstant(JsonObject jsonObject) {
      if ((jsonObject instanceof JsonString)) {
         JsonString js=(JsonString) jsonObject;
         if (js.name.equals("Time")) try {
            return Instant.parse(js.value+"Z");
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      System.err.println("Unable to parse "+jsonObject+" : "+Instant.now());
      return Instant.now();
   }
}

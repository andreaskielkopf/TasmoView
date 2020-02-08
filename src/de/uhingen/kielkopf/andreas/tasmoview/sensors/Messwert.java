package de.uhingen.kielkopf.andreas.tasmoview.sensors;

import java.time.Instant;

import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;

public class Messwert implements Comparable<Messwert> {
   public final Instant    instant;
   public final JsonObject json;
   public final double     value;
   public Messwert(Instant i, JsonObject j, Double v) {
      instant=i;
      json=j;
      value=v;
   }
   @Override
   public int compareTo(Messwert o) {
      return this.instant.compareTo(o.instant);
   }
}

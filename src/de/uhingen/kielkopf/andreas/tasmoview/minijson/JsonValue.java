package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.regex.Matcher;

public class JsonValue extends JsonObject {
   public final String value;
   private Double      d=null;
   public JsonValue(String s) {
      Matcher m=namedValueP.matcher(s);
      if (m.matches()) {
         name=m.group(1);
         value=m.group(2);
      } else {
         name=null;
         m=valueP.matcher(s);
         value=m.matches() ? m.group(0) : null;
      }
      validate(value, s, this);
   }
   public final Double getDoubleValue() {
      if (d==null) d=((value==null)||value.equals("null")) ? 0d : Double.valueOf(value);
      return d;
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      if (name!=null&&!name.isEmpty()) {
         sb.append('"');
         sb.append(name);
         sb.append("\":");
      }
      if (value!=null) sb.append(value);
      return sb.toString();
   }
}

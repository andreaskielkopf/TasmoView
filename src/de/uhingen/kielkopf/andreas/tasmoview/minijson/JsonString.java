package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.regex.Matcher;

public class JsonString extends JsonObject {
   public final String value;
   public JsonString(String s) {
      Matcher m=namedStringP.matcher(s);
      if (m.matches()) {
         name=m.group(1);
         value=m.group(2);
      } else {
         name=null;
         m=stringP.matcher(s);
         value=m.matches() ? m.group(1) : null;
      }
      validate(value, s, this);
   }
   public JsonString(String name1, String value1) {
      name=name1;
      value=value1;
   }
   @Override
   public String toString() {
      final StringBuilder sb=new StringBuilder();
      if ((name != null) && (!name.isEmpty())) {
         sb.append('"');
         sb.append(name);
         sb.append("\":");
      }
      sb.append('"');
      sb.append(value);
      sb.append('"');
      return sb.toString();
   }
}

package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.regex.Matcher;

public class JsonString extends JsonObject {
   public final String value;
   public JsonString(String name, String value) {
      super();
      this.name=name;
      this.value=value;
   }
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
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      if ((name!=null)&&(!name.isEmpty())) {
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

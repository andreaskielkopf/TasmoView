package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.ArrayList;
import java.util.regex.Matcher;

public class JsonArray extends JsonContainer {
   public JsonArray(String s) {
      super();
      String  rest;
      Matcher m=namedArrayP.matcher(s);
      if (m.matches()) {
         name=m.group(1);
         rest=m.group(2);
      } else {
         m=arrayP.matcher(s);
         name=null;
         rest=m.matches() ? m.group(1) : null;
      }
      if (rest!=null) {
         ArrayList<String> split=new ArrayList<String>();
         String            s3   ="";
         for (String s2:rest.split(KOMMA)) {
            if (s3.isEmpty()) {
               if (JsonObject.isPaarweise(s2))
                  split.add(s2);
               else
                  s3=s2;
            } else {
               s3=s3+KOMMA+s2;
               if (JsonObject.isPaarweise(s3)) {
                  split.add(s3);
                  s3="";
               }
            }
         }
         if (!s3.isEmpty()) split.add(s3);
         for (String part:split)
            list.add(JsonObject.convertToJson(part));
      }
      validate(rest, s, this);
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      if ((name!=null)&&(!name.isEmpty())) {
         sb.append('"');
         sb.append(name);
         sb.append("\":");
      }
      sb.append('[');
      boolean first=true;
      for (JsonObject j:list) {
         if (first)
            first=false;
         else
            sb.append(KOMMA);
         sb.append(j);
      }
      sb.append(']');
      return sb.toString();
   }
}

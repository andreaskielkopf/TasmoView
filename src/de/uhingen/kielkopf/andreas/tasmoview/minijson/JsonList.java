package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class JsonList extends JsonObject {
   public final List<JsonObject> list=new ArrayList<>();
   public JsonList(String s) {
      super();
      String  rest;
      Matcher m=namedListP.matcher(s);
      if (m.matches()) {
         name=m.group(1);
         rest=m.group(2);
      } else {
         m=listP.matcher(s);
         name=null;
         rest=m.matches() ? m.group(1) : null;
      }
      if (rest!=null) {
         ArrayList<String> split=new ArrayList<String>();
         String            s3   ="";
         for (String s2:rest.split(KOMMA)) {
            if (s3.isEmpty()) {
               if (JsonObject.isPaarweise(s2)) split.add(s2);
               else s3=s2;
            } else {
               s3=s3+KOMMA+s2;
               if (JsonObject.isPaarweise(s3)) {
                  split.add(s3);
                  s3="";
               }
            }
         }
         if (!s3.isEmpty()) split.add(s3);
         for (String part:split) list.add(JsonObject.interpret(part));
      }
      validate(rest, s, this);
   }
   public JsonObject getJsonObject(int i) {
      return list.get(i);
   }
   public ArrayList<JsonObject> getAll() {
      ArrayList<JsonObject> c=new ArrayList<JsonObject>();
      c.add(this);
      for (JsonObject jsonObject:list) c.addAll(jsonObject.getAll());
      return c;
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder();
      if ((name!=null)&&(!name.isEmpty())) {
         sb.append('"');
         sb.append(name);
         sb.append("\":");
      }
      sb.append('{');
      boolean first=true;
      for (JsonObject j:list) {
         if (first) first=false;
         else sb.append(KOMMA);
         sb.append(j);
      }
      sb.append('}');
      return sb.toString();
   }
}

package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ein Objekt das Json-Daten enthält optional mit einem Namen */
public abstract class JsonObject {
   public static final String  KOMMA       =",";
   public static final Pattern arrayP      =Pattern.compile("\\[(.+)\\]");                                    // 28
   public static final Pattern namedArrayP =Pattern.compile("\\\"([^\\\"]+)\\\":\\[([^\\]]+)\\]");            // 28
   public static final Pattern listP       =Pattern.compile("\\{(.+)\\}");                                    // 277
   public static final Pattern namedListP  =Pattern.compile("\\\"([^\\\"]+)\\\":\\{(.+)\\}");                 // 130
   public static final Pattern stringP     =Pattern.compile("\\\"([^\\\",:\\[\\]\\{\\}]*)\\\"");              // 2451
   public static final Pattern namedStringP=Pattern.compile("\\\"([^\\\"]+)\\\":\\\"([^\\\"]*)\\\"");         // 984
   public static final Pattern valueP      =Pattern.compile("[^ :,\\\"\\[\\]\\{\\}]+");                       // 5879
   public static final Pattern namedValueP =Pattern.compile("\\\"([^\\\"]+)\\\":([^\\\"\\[{}\\\\[\\\\],]+)"); // 389
   public String               name;
   /** Zerteile den String in hierarchische JsonObjekte */
   public static final JsonObject convertToJson(String part) {
      if (isValue(part))
         return new JsonValue(part);
      if (isString(part))
         return new JsonString(part);
      if (isList(part))
         return new JsonList(part);
      if (isArray(part))
         return new JsonArray(part);
      return null;
   }
   public static final boolean isArray(String s) {
      Matcher m=arrayP.matcher(s);
      if (m.matches())
         return true;
      m=namedArrayP.matcher(s);
      return m.matches();
   }
   public static final boolean isList(String s) {
      Matcher m=listP.matcher(s);
      if (m.matches())
         return true;
      m=namedListP.matcher(s);
      return m.matches();
   }
   /** teste ob der String irgendwie asymetrisch ist */
   public static final boolean isPaarweise(String s) {
      int dquote=0, quote=0, eckig=0, rund=0, geschwungen=0;
      for (final char c:s.toCharArray()) {
         switch (c) {
         case '"':
            dquote++;
            break;
         case '(':
            rund++;
            break;
         case ')':
            rund--;
            break;
         case '[':
            eckig++;
            break;
         case ']':
            eckig--;
            break;
         case '{':
            geschwungen++;
            break;
         case '}':
            geschwungen--;
            break;
         case '\'':
            quote++;
            break;
         default:
            break;
         }
      }
      return ((geschwungen == 0) && (eckig == 0) && ((dquote % 2) == 0) && (rund == 0) && ((quote % 2) == 0));
   }
   public static final boolean isString(String s) {
      Matcher m=namedStringP.matcher(s);
      if (m.matches())
         return true;
      m=stringP.matcher(s);
      return m.matches();
   }
   public static final boolean isValue(String s) {
      Matcher m=valueP.matcher(s);
      if (m.matches())
         return true;
      m=namedValueP.matcher(s);
      return m.matches();
   }
   /** gemeinsame routine zum test der Umwandlung */
   protected static void validate(String v, String s, JsonObject jo) {
      if (v == null)
         System.err.print(s + ":passt nicht auf:" + jo.getClass().getSimpleName());
      if (!s.equals(jo.toString()))
         System.err.println("(" + s + ")!=(" + jo.toString() + ")");
   }
   public ArrayList<JsonObject> getAll() {
      final ArrayList<JsonObject> c=new ArrayList<>();
      c.add(this);
      return c;
   }
   /** Gib alle passenden Objekte zurück oder eine leere Liste */
   public ArrayList<JsonObject> getAll(String name1) {
      final ArrayList<JsonObject> c=new ArrayList<>();
      for (final JsonObject o:getAll()) {
         if (name1 == null) {
            if (o.name == null)
               c.add(o);
         } else
            if (name1.equals(o.name))
               c.add(o);
      }
      return c;
   }
   /** Gib das erste passende Objekt zurück oder null */
   public final Double getDoubleValue(String name1) {
      for (final JsonObject jo:getAll(name1)) {
         if (jo instanceof JsonValue jd)
            return jd.getDoubleValue();
      }
      return null;
   }
   /** Gib das erste passende Objekt zurück oder null */
   public JsonObject getJsonObject(String name1) {
      for (final JsonObject o:getAll()) {
         if (name1 == null) {
            if (o.name == null)
               return o;
         } else
            if (name1.equals(o.name))
               return o;
      }
      return null;
   }
}

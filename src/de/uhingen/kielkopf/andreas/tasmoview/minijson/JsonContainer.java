package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.ArrayList;
import java.util.List;

public class JsonContainer extends JsonObject {
   public final List<JsonObject> list=new ArrayList<>();
   public JsonContainer() {}
   public JsonObject getJsonObject(int i) {
      return list.get(i);
   }
   public ArrayList<JsonObject> getAll() {
      ArrayList<JsonObject> c=new ArrayList<JsonObject>();
      c.add(this);
      for (JsonObject jsonObject:list)
         c.addAll(jsonObject.getAll());
      return c;
   }
}

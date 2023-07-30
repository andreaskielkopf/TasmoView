package de.uhingen.kielkopf.andreas.tasmoview.minijson;

import java.util.ArrayList;
import java.util.List;

public class JsonContainer extends JsonObject {
   public final List<JsonObject> list=new ArrayList<>();
   public JsonContainer() {}
   @Override
   public ArrayList<JsonObject> getAll() {
      final ArrayList<JsonObject> c=new ArrayList<>();
      c.add(this);
      for (final JsonObject jsonObject:list)
         c.addAll(jsonObject.getAll());
      return c;
   }
   public JsonObject getJsonObject(int i) {
      return list.get(i);
   }
}

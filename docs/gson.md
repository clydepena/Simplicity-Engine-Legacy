# Gson: reference

Status: **reference.** The engine uses Gson 2.11 (`simplicity/build.gradle`) for saving and loading. The existing adapters are `components/ComponentDeserializer` and `simplicity/GameObjectDeserializer`. Related: `asset-pipeline.md`, `open-world-plan.md` (section 7, project folder).

How to use Gson, every way to customize it, and the problems that matter for this engine.

---

## 1. Basics

```java
Gson gson = new Gson();                          // or new GsonBuilder()...create()
String json = gson.toJson(obj);                  // object -> JSON text
Foo foo = gson.fromJson(json, Foo.class);        // JSON text -> object
gson.toJson(obj, writer);  gson.fromJson(reader, Foo.class);   // straight to/from files
```

Default behaviour:

- **Fields are read and written directly, through reflection.** Getters and setters are never called. Private fields are included.
- **`static` and `transient` fields are skipped.** Marking a field `transient` is the simplest way to leave it out.
- **`null` fields are left out** of the output.
- **The constructor doesn't always run on load.**
  - If the class has a no-arg constructor, Gson uses it.
  - If not, Gson creates the object *without running any constructor*. Field initializers such as `List<X> list = new ArrayList<>()` then never run, and any field missing from the JSON is `null` or `0`.
  - So give save-data classes a no-arg constructor.
- **Supported automatically:** nested objects, arrays, `List`, `Map`, enums and records (Gson 2.10+).
- **Output is compact, on one line,** unless pretty printing is turned on.

---

## 2. Generics need a `TypeToken`

Java erases generic types at runtime, so `List<Foo>.class` doesn't exist:

```java
Type t = new TypeToken<List<Foo>>(){}.getType();
List<Foo> list = gson.fromJson(json, t);

// 2.10+: TypeToken.getParameterized(List.class, Foo.class).getType()
```

Passing `List.class` gives a list of `LinkedTreeMap`s, not `Foo`s. Arrays avoid the problem, which is why the save code loads a `GameObject[]`.

---

## 3. `GsonBuilder` options

```java
Gson gson = new GsonBuilder()
    .setPrettyPrinting()                         // indented, readable output (good for save files and diffs)
    .serializeNulls()                            // write "field": null instead of leaving it out
    .disableHtmlEscaping()                       // write < > = ' as they are, not as <
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // myField -> my_field
    .excludeFieldsWithModifiers(Modifier.STATIC) // replaces the default (static + transient): transient fields are now saved
    .excludeFieldsWithoutExposeAnnotation()      // only fields marked @Expose are saved
    .setExclusionStrategies(new MyStrategy())    // skip fields/classes by any rule you write
    .setVersion(2.0)                             // works with @Since / @Until
    .serializeSpecialFloatingPointValues()       // allow NaN/Infinity (they throw otherwise)
    .enableComplexMapKeySerialization()          // Map<Vector2i, Cell> keys become objects, not toString()
    .setStrictness(Strictness.LENIENT)           // 2.11: accept comments, unquoted names, etc.
    .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE) // see section 8, item 4
    .registerTypeAdapter(Foo.class, adapter)     // custom code for exactly Foo
    .registerTypeHierarchyAdapter(Component.class, adapter) // custom code for Component AND all subclasses
    .registerTypeAdapterFactory(factory)         // custom code chosen by a rule you write (section 5C)
    .create();
```

**Build one `Gson` and reuse it.** It's thread-safe, and each instance caches the reflection data it collects.

---

## 4. Field annotations

```java
class Player {
    @SerializedName("hp")                    int health;   // JSON name is "hp"
    @SerializedName(value = "hp", alternate = {"health", "HP"}) int health2; // also READS old names: good for renames
    @Expose                                  String name;  // needs excludeFieldsWithoutExposeAnnotation()
    @Expose(serialize = true, deserialize = false) int score; // written but never read
    @Since(1.1)                              int mana;     // skipped when setVersion(...) < 1.1
    transient                                Texture cache; // never saved
    @JsonAdapter(ColorAdapter.class)         Vector4f tint; // custom adapter for just this field
}
```

`alternate` is the tool for save-format changes: rename a field, and old save files still load.

An `ExclusionStrategy` applies your own rule instead of annotations. This one skips fields marked with a custom `@EditorOnly` annotation, plus every `Texture`:

```java
new ExclusionStrategy() {
    public boolean shouldSkipField(FieldAttributes f) { return f.getAnnotation(EditorOnly.class) != null; }
    public boolean shouldSkipClass(Class<?> c) { return c == Texture.class; }
};
```

---

## 5. Custom conversion

### A. `JsonSerializer` / `JsonDeserializer` (tree-based)

Gson hands you a `JsonElement` tree, and you read or build it by hand. The engine's adapters use this style. It's easy to write but slower, because the whole tree is built first.

```java
class Vec2Adapter implements JsonSerializer<Vector2f>, JsonDeserializer<Vector2f> {
    public JsonElement serialize(Vector2f v, Type t, JsonSerializationContext ctx) {
        JsonArray a = new JsonArray(); a.add(v.x); a.add(v.y); return a;   // [1.0, 2.0] instead of {"x":1.0,"y":2.0}
    }
    public Vector2f deserialize(JsonElement e, Type t, JsonDeserializationContext ctx) {
        JsonArray a = e.getAsJsonArray(); return new Vector2f(a.get(0).getAsFloat(), a.get(1).getAsFloat());
    }
}
```

- `ctx.serialize(obj)` / `ctx.deserialize(elem, Type)` hand a sub-object back to Gson, using any other adapters you registered.
- **Trap: `ctx.serialize(src)` on the same type the adapter handles calls the adapter again, forever.**
  - `ComponentDeserializer` avoids this by passing `src.getClass()`, the concrete subclass.
  - That subclass isn't registered, so Gson uses the default reflection for it.

### B. `TypeAdapter` (streaming: fast, full control)

```java
class Vec2TypeAdapter extends TypeAdapter<Vector2f> {
    @Override public void write(JsonWriter out, Vector2f v) throws IOException {
        if (v == null) { out.nullValue(); return; }
        out.beginArray().value(v.x).value(v.y).endArray();
    }
    @Override public Vector2f read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
        in.beginArray(); float x = (float) in.nextDouble(), y = (float) in.nextDouble(); in.endArray();
        return new Vector2f(x, y);
    }
}
// register: .registerTypeAdapter(Vector2f.class, new Vec2TypeAdapter().nullSafe())  // nullSafe() does the null handling for you
```

Use this for small, frequent types (vectors, colors, tile data) and for large files such as open-world tilemaps.

### C. `TypeAdapterFactory` (adapters chosen by a rule)

A factory decides at runtime which types it handles. The classic use is wrapping the default behaviour, known as the delegate pattern:

```java
class PostLoadFactory implements TypeAdapterFactory {
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Component.class.isAssignableFrom(type.getRawType())) return null;   // null = "not mine"
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);           // the adapter Gson would have used
        return new TypeAdapter<T>() {
            public void write(JsonWriter out, T v) throws IOException { delegate.write(out, v); }
            public T read(JsonReader in) throws IOException {
                T obj = delegate.read(in);
                ((Component) obj).onDeserialized();   // hypothetical hook: rebuild caches, runtime state
                return obj;
            }
        };
    }
}
```

This is the way to say "save normally, then run this extra step".

**Polymorphism:** Gson also has `RuntimeTypeAdapterFactory`, but it's only in the Gson repo's `extras` folder, not the published jar, so it would be copied into the project. `ComponentDeserializer`'s type/properties wrapper already does the same job.

### D. `InstanceCreator`

Controls how Gson creates the empty object before it fills in the fields. It fixes the "constructor never runs" problem for classes you can't change:

```java
.registerTypeAdapter(GameObject.class, (InstanceCreator<GameObject>) t -> new GameObject("unnamed"))
```

---

## 6. Tree model (no classes)

```java
JsonObject root = JsonParser.parseString(text).getAsJsonObject();   // or parseReader(reader)
int version = root.has("version") ? root.get("version").getAsInt() : 1;
root.getAsJsonArray("objects").forEach(e -> ...);
root.addProperty("version", 2); root.add("meta", new JsonObject());
String out = gson.toJson(root);                  // a tree can be written just like an object
Foo foo = gson.fromJson(root.get("foo"), Foo.class);   // part of a tree becomes an object
```

This is the standard way to **migrate old save formats**: read the tree, check `"version"`, fix it up, then convert it to objects.

---

## 7. Streaming API (no classes, no tree)

`JsonReader` / `JsonWriter` also work on their own, for reading or writing huge files piece by piece with low memory use:

```java
try (JsonWriter w = new JsonWriter(writer)) {
    w.beginObject().name("version").value(1).name("cells").beginArray();
    for (Cell c : cells) gson.toJson(c, Cell.class, w);   // mix objects in anywhere
    w.endArray().endObject();
}
```

---

## 8. Pitfalls

1. **Nested classes break `ComponentDeserializer`** (`components/ComponentDeserializer.java`, which both saves and loads components). (Open bug.)
   - It saves `getCanonicalName()` and loads with `Class.forName(type)`.
   - For a nested class, the canonical name is `Outer.Inner`, but `Class.forName` needs `Outer$Inner`.
   - Saving works, but loading throws "Unknown element type".
   - Fix: save `src.getClass().getName()`.
2. **Circular references cause a `StackOverflowError`.** An example is Component → gameObject → components → Component. Gson doesn't detect cycles, so back-references must be `transient`. `Component.gameObject` and `GameObject.transform` already are; new back-references need the same.
3. **The constructor doesn't run on load** (section 1). This is why `GameObjectDeserializer` builds the object by hand with `new GameObject(name)`.
4. **Numbers inside `Object` or `Map<String, Object>` come back as `Double`**: `5` becomes `5.0`. Use `setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)` for loosely typed data.
5. **Enums are saved by `name()`,** so renaming a constant breaks old saves. Put `@SerializedName` on the enum constant to fix that.
6. **Map keys use `toString()`** by default. Keys other than String or number keys, such as cell coordinates, need `enableComplexMapKeySerialization()`.
7. **`NaN`/`Infinity` throw** unless `serializeSpecialFloatingPointValues()` is set.
8. **Creating a new `Gson` per call** wastes the reflection cache. Currently the save/load code and `GameObject.copy` each build their own.

---

## 9. Recommended engine setup

```java
public final class Json {
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeHierarchyAdapter(Component.class, new ComponentDeserializer())
        .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
        .registerTypeAdapter(Vector2f.class, new Vec2TypeAdapter().nullSafe())
        .create();
}
```

- **Build it once, and use `Json.GSON` everywhere:** `SimplicityEditorIO`, `GameObject.copy` and so on.
- **`registerTypeHierarchyAdapter` covers subclasses** as well.
  - Inside the adapter, `ctx.serialize(src, src.getClass())` still gets default reflection, because the adapter doesn't claim the concrete class itself.
- **Always write a `"version"` field** at the top of save files, so the migration in section 6 is possible when the format changes.

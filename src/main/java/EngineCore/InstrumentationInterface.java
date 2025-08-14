// src/main/java/EngineCore/DefaultComponents/Extra/InstrumentationInterface.java
package EngineCore;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstrumentationInterface {
  static volatile Instrumentation inst;

  public static void premain(String args, Instrumentation i)  { init(i); }
  public static void agentmain(String args, Instrumentation i){ init(i); }

  private static void init(Instrumentation i) {
    inst = i;
    i.addTransformer(new java.lang.instrument.ClassFileTransformer() {
      @Override public byte[] transform(ClassLoader loader, String name, Class<?> cls,
                                        java.security.ProtectionDomain pd, byte[] bytes) {
        // fires whenever a class is defined
        return null; // no bytecode change
      }
    }, false);
  }

  // --- Utilities ---

  private static Instrumentation requireInst() {
    Instrumentation i = inst;
    if (i == null)
      throw new IllegalStateException("Instrumentation agent not installed yet (premain/agentmain not called).");
    return i;
  }

  /** Snapshot of all currently loaded class objects. */
  public static Class<?>[] getAllLoadedClasses() {
    return requireInst().getAllLoadedClasses();
  }

  /** Sorted list of all currently loaded fully-qualified class names. */
  public static List<String> getAllLoadedClassNames() {
    Class<?>[] loaded = getAllLoadedClasses();
    List<String> names = new ArrayList<>(loaded.length);
    for (Class<?> c : loaded) names.add(c.getName());
    Collections.sort(names);
    return names;
  }

  /** Returns the first loaded class with this FQCN, or null if not loaded. */
  public static Class<?> getLoadedClass(String fqcn) {
    for (Class<?> c : getAllLoadedClasses())
      if (c.getName().equals(fqcn)) return c;
    return null;
  }

  /** Returns all loaded classes with this FQCN (possible when multiple classloaders exist). */
  public static List<Class<?>> getLoadedClassesAllLoaders(String fqcn) {
    List<Class<?>> out = new ArrayList<>();
    for (Class<?> c : getAllLoadedClasses())
      if (c.getName().equals(fqcn)) out.add(c);
    return out;
  }

  /** Human-friendly loader name for a loaded class FQCN (first match), or null if not loaded. */
  public static String getLoaderName(String fqcn) {
    Class<?> c = getLoadedClass(fqcn);
    if (c == null) return null;
    ClassLoader l = c.getClassLoader();
    return (l == null) ? "<bootstrap>" : l.getClass().getName();
  }

  // ---- “Auto-cast” helpers (runtime-checked) ----

  /** Return the loaded class, verified to be assignable to target type (throws if not). */
  public static <T> Class<? extends T> getLoadedClassAs(String fqcn, Class<T> targetType) {
    Class<?> c = getLoadedClass(fqcn);
    if (c == null) return null;
    return c.asSubclass(targetType); // ClassCastException if incompatible
  }

  /** Same as above, but target type is given by name at runtime. Returns Class<?> (validated). */
  public static Class<?> getLoadedClassAs(String fqcn, String targetFqcn) throws ClassNotFoundException {
    Class<?> c = getLoadedClass(fqcn);
    if (c == null) return null;
    ClassLoader prefer = (c.getClassLoader() != null) ? c.getClassLoader()
                                                      : Thread.currentThread().getContextClassLoader();
    Class<?> target = Class.forName(targetFqcn, false, prefer);
    if (!target.isAssignableFrom(c)) {
      throw new ClassCastException("Loaded class " + fqcn + " is not assignable to " + targetFqcn);
    }
    return c; // dynamically verified; caller can safely reflect/instantiate against target
  }

  /** Quick check: is the loaded class assignable to a target given by name? */
  public static boolean isAssignableTo(String fqcn, String targetFqcn) {
    Class<?> c = getLoadedClass(fqcn);
    if (c == null) return false;
    try {
      ClassLoader prefer = (c.getClassLoader() != null) ? c.getClassLoader()
                                                        : Thread.currentThread().getContextClassLoader();
      Class<?> target = Class.forName(targetFqcn, false, prefer);
      return target.isAssignableFrom(c);
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  // ---- Optional: instantiate ----

  /** Instantiate a loaded class via no-arg ctor (returns Object; no compile-time type needed). */
  public static Object newInstance(String fqcn) throws ReflectiveOperationException {
    Class<?> c = getLoadedClass(fqcn);
    if (c == null) throw new ClassNotFoundException("Not currently loaded: " + fqcn);
    var ctor = c.getDeclaredConstructor();
    ctor.setAccessible(true); // may require --add-opens in strict module setups
    return ctor.newInstance();
  }

  /** Instantiate and cast to a target interface/class known at runtime by name (validated). */
  public static Object newInstanceAs(String fqcn, String targetFqcn) throws ReflectiveOperationException {
    Class<?> c = getLoadedClassAs(fqcn, targetFqcn); // validates assignability
    var ctor = c.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }
}

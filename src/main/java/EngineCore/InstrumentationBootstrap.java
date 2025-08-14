// src/main/java/EngineCore/InstrumentationBootstrap.java
package EngineCore;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public final class InstrumentationBootstrap {
  private static volatile boolean installed;

  /** Call once (e.g., in a static block of your boilerplate module). */
  public static synchronized void install(Class<?> targetAgentClass) {
    if (isInstalled()) return;

    // targetAgentClass is the class that owns the real init(Instrumentation) you want to run,
    // e.g. EngineCore.InstrumentationInterface
    requireAgentHostClass(targetAgentClass);

    final Path agentJar;
    try {
      agentJar = buildTempAgentJarWithShim(); // includes only AgentShim
    } catch (IOException ioe) {
      throw new IllegalStateException("Failed to create temp agent JAR", ioe);
    }

    final String argFqcn = targetAgentClass.getName();
    boolean attached = trySelfAttach(agentJar, argFqcn);
    if (!attached) {
      try {
        tryForkedAttach(agentJar, argFqcn);
        attached = true;
      } catch (IOException | InterruptedException forkFail) {
        throw new IllegalStateException("Agent install failed (self-attach + forked attach)", forkFail);
      }
    }

    // Verify the *app's* InstrumentationInterface got initialized
    if (!attached || InstrumentationInterface.inst == null) {
      throw new IllegalStateException(
          "Agent JAR loaded but Instrumentation is still null. " +
          "Check stderr for agent exceptions. Ensure a full JDK is used.");
    }

    installed = true;

    // Optional: informative note for JDK 24+
    if (Runtime.version().feature() >= 24) {
      System.err.println("[EngineCore] Agent installed. " +
          "Note: JDK 24 prints a warning on dynamic agent loading. " +
          "To hide it, start the TARGET with -XX:+EnableDynamicAgentLoading.");
    }
  }

  /** Install using FQCN of the app’s agent host class (e.g., EngineCore.InstrumentationInterface). */
  public static synchronized void install(String targetAgentClassName) {
    if (isInstalled()) return;
    try {
      install(Class.forName(targetAgentClassName));
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Agent host class not found: " + targetAgentClassName, e);
    }
  }

  /** True only if attach succeeded AND the app’s InstrumentationInterface set its Instrumentation. */
  public static boolean isInstalled() {
    return installed && InstrumentationInterface.inst != null;
  }

  // ---------------- internals ----------------

  private static boolean trySelfAttach(Path agentJar, String argFqcn) {
    try {
      System.setProperty("jdk.attach.allowAttachSelf", "true");
      String pid = currentPid();

      Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
      Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
      try {
        // Pass the target class FQCN to the shim
        vmClass.getMethod("loadAgent", String.class, String.class)
               .invoke(vm, agentJar.toString(), argFqcn);
      } finally {
        vmClass.getMethod("detach").invoke(vm);
      }
      return true;
    } catch (ClassNotFoundException e) {
      return false; // jdk.attach not available in this process
    } catch (InvocationTargetException ite) {
      // Common under mvn exec:java or blocked self-attach
      return false;
    } catch (Throwable t) {
      return false;
    }
  }

  private static void tryForkedAttach(Path agentJar, String argFqcn) throws IOException, InterruptedException {
    String javaBin = findJavaBinary();
    String pid = currentPid();
    String cp  = codeSourcePathOf(Fork.class); // where this class lives (jar or classes dir)

    List<String> cmd = new ArrayList<>();
    cmd.add(javaBin);
    // Ensure Attach API is present in the helper
    cmd.add("--add-modules"); cmd.add("jdk.attach");
    // (Optional) suppress future restriction warnings in helper; target still warns on JDK 24 unless VM flag is set at startup
    cmd.add("-XX:+EnableDynamicAgentLoading");

    cmd.add("-cp"); cmd.add(cp);
    cmd.add(Fork.class.getName());
    cmd.add(pid);
    cmd.add(agentJar.toString());
    cmd.add(argFqcn);

    Process p = new ProcessBuilder(cmd)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .start();
    int exit = p.waitFor();
    if (exit != 0) {
      throw new IOException("Forked attacher exited with code " + exit + ". Command: " + String.join(" ", cmd));
    }
  }

  /** Helper main used by the forked JVM to attach to the target PID and load our shim JAR. */
  public static final class Fork {
    public static void main(String[] args) throws Exception {
      if (args.length != 3) {
        System.err.println("Usage: Fork <pid> <agentJar> <targetFQCN>");
        System.exit(2);
      }
      String pid = args[0], jar = args[1], fqcn = args[2];
      Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
      Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
      try {
        vmClass.getMethod("loadAgent", String.class, String.class).invoke(vm, jar, fqcn);
      } finally {
        vmClass.getMethod("detach").invoke(vm);
      }
      System.err.println("[InstrumentationBootstrap] Attached to " + pid + " and loaded agent " + jar);
    }
  }

  /**
   * The only class we package inside the temp agent JAR.
   * It is loaded by the agent loader and immediately bridges into the app’s
   * EngineCore.InstrumentationInterface (already on the app classpath) to invoke its private init(Instrumentation).
   */
  public static final class AgentShim {
    public static void premain(String args, Instrumentation inst) { bridge(args, inst); }
    public static void agentmain(String args, Instrumentation inst) { bridge(args, inst); }

 // Inside InstrumentationBootstrap.AgentShim
    private static void bridge(String targetFqcn, java.lang.instrument.Instrumentation inst) {
      try {
        if (targetFqcn == null || targetFqcn.isEmpty()) {
          targetFqcn = "EngineCore.InstrumentationInterface";
        }

        // 1) Prefer the ACTUAL loaded class discovered via Instrumentation (correct loader!)
        Class<?> host = null;
        for (Class<?> c : inst.getAllLoadedClasses()) {
          if (c.getName().equals(targetFqcn)) { host = c; break; }
        }

        // 2) If not yet loaded, try likely loaders in order
        if (host == null) {
          ClassLoader[] loaders = new ClassLoader[] {
            Thread.currentThread().getContextClassLoader(), // often the app loader
            ClassLoader.getSystemClassLoader(),             // sometimes enough
            AgentShim.class.getClassLoader()                // our (agent) loader last
          };
          for (ClassLoader cl : loaders) {
            if (cl == null) continue;
            try {
              host = Class.forName(targetFqcn, false, cl);
              break;
            } catch (ClassNotFoundException ignore) {}
          }
        }

        // 3) Still not found? Give a precise error with hints
        if (host == null) {
          throw new ClassNotFoundException(
            targetFqcn + " not visible to system/TCCL/agent loaders. " +
            "Check the FQCN/package and ensure the class is on the target JVM classpath."
          );
        }

        // 4) Call the app’s private static init(Instrumentation)
        var m = host.getDeclaredMethod("init", java.lang.instrument.Instrumentation.class);
        m.setAccessible(true);
        m.invoke(null, inst);

        System.err.println("[AgentShim] bridged Instrumentation into " + targetFqcn
            + " via loader: " + String.valueOf(host.getClassLoader()));
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException("AgentShim bridge failed for target " + targetFqcn, t);
      }
    }

  }

  /** Build a temp agent JAR containing only AgentShim with the proper manifest. */
  private static Path buildTempAgentJarWithShim() throws IOException {
    Manifest mf = new Manifest();
    Attributes a = mf.getMainAttributes();
    a.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    String shim = AgentShim.class.getName();
    a.putValue("Agent-Class",   shim);
    a.putValue("Premain-Class", shim);
    a.putValue("Can-Redefine-Classes", "true");
    a.putValue("Can-Retransform-Classes", "false");

    Path jar = Files.createTempFile("enginecore-agent-", ".jar");
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar), mf)) {
      writeClassBytes(jos, AgentShim.class);
    }
    jar.toFile().deleteOnExit();
    return jar;
  }

  private static void writeClassBytes(JarOutputStream jos, Class<?> c) throws IOException {
    String entry = c.getName().replace('.', '/') + ".class"; // e.g., EngineCore/InstrumentationBootstrap$AgentShim.class
    jos.putNextEntry(new JarEntry(entry));
    InputStream in = (InstrumentationBootstrap.class.getClassLoader() != null)
        ? InstrumentationBootstrap.class.getClassLoader().getResourceAsStream(entry)
        : ClassLoader.getSystemResourceAsStream(entry);
    if (in == null) throw new FileNotFoundException("Class bytes not found for " + c.getName() + " at " + entry);
    try (in) { in.transferTo(jos); }
    jos.closeEntry();
  }

  private static String findJavaBinary() {
    String home = System.getProperty("java.home");
    Path java = Paths.get(home, "bin", isWindows() ? "java.exe" : "java");
    return java.toAbsolutePath().toString();
  }

  private static boolean isWindows() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("win");
  }

  private static String codeSourcePathOf(Class<?> c) {
    try {
      return Paths.get(c.getProtectionDomain().getCodeSource().getLocation().toURI())
                  .toAbsolutePath().toString();
    } catch (URISyntaxException e) {
      return System.getProperty("java.class.path");
    }
  }

  private static String currentPid() {
    try {
      Class<?> ph = Class.forName("java.lang.ProcessHandle");
      Object current = ph.getMethod("current").invoke(null);
      long pid = (long) ph.getMethod("pid").invoke(current);
      return Long.toString(pid);
    } catch (Throwable ignore) {
      String jvm = ManagementFactory.getRuntimeMXBean().getName();
      int at = jvm.indexOf('@');
      return (at > 0) ? jvm.substring(0, at) : jvm;
    }
  }

  private static void requireAgentHostClass(Class<?> c) {
    // We’ll reflectively call a private static init(Instrumentation) on this class.
    try {
      c.getDeclaredMethod("init", java.lang.instrument.Instrumentation.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          "Expected a static method init(Instrumentation) on " + c.getName() +
          " (used by AgentShim to bridge into the app).", e);
    }
  }

  private InstrumentationBootstrap() {}
}

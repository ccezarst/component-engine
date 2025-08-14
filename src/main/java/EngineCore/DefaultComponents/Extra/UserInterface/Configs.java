package EngineCore.DefaultComponents.Extra.UserInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Configs {
    private static final Path FILE = Paths.get("config.properties");
    private static final Properties props = new Properties();
    private static long lastModified = 0;

    private static void reloadIfNeeded() {
        try {
            long ts = Files.exists(FILE)
                ? Files.getLastModifiedTime(FILE).toMillis()
                : 0;
            synchronized (props) {
                if (ts != lastModified) {
                    props.clear();
                    if (ts > 0) {
                        try (InputStream in = Files.newInputStream(FILE)) {
                            props.load(in);
                        }
                    }
                    lastModified = ts;
                }
            }
        } catch (IOException ignored) {}
    }

    public static String readConfig(String key) {
        synchronized (props) {
            reloadIfNeeded();
            String value = props.getProperty(key);

            // Dacă lipsește cheia, o adăugăm cu valoare goală și semnalăm eroarea
            if (value == null) {
                try {
                    if (!Files.exists(FILE)) {
                        Files.createFile(FILE);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Nu am putut crea fișierul de configurare [" 
                        + FILE.toAbsolutePath() + "]", e);
                }
                props.setProperty(key, "");
                try (OutputStream out = Files.newOutputStream(FILE)) {
                    props.store(out, "Proprietate lipsă adăugată automat");
                    FileTime ft = Files.getLastModifiedTime(FILE);
                    lastModified = ft.toMillis();
                } catch (IOException e) {
                    throw new RuntimeException("Nu am putut salva configurația în [" 
                        + FILE.toAbsolutePath() + "]", e);
                }
                throw new MissingConfigException(
                    "Lipsește configurația pentru cheia '" + key + 
                    "' în fișierul: " + FILE.toAbsolutePath()
                );
            }

            // Dacă există, dar e goală sau doar spații, semnalăm și asta
            if (value.trim().isEmpty()) {
                throw new MissingConfigException(
                    "Valoarea pentru cheia '" + key + 
                    "' este goală în fișierul: " + FILE.toAbsolutePath()
                );
            }

            return value;
        }
    }

    public static void writeConfig(String key, String value) {
        synchronized (props) {
            reloadIfNeeded();
            props.setProperty(key, value);
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, null);
                FileTime ft = Files.getLastModifiedTime(FILE);
                lastModified = ft.toMillis();
            } catch (IOException e) {
                throw new RuntimeException("Eroare la scrierea în fișierul [" 
                    + FILE.toAbsolutePath() + "]", e);
            }
        }
    }
    
    public static Map<String,String> getAllConfigs() {
        synchronized (props) {
            reloadIfNeeded();
            Map<String,String> map = new HashMap<>();
            for (String name : props.stringPropertyNames()) {
                map.put(name, props.getProperty(name));
            }
            return map;
        }
    }

    public static class MissingConfigException extends RuntimeException {
        public MissingConfigException(String message) {
            super(message);
        }
    }
}

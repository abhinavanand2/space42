package com.space42.utils;
import java.util.Properties;
public final class ConfigManager {
    private final Properties values = new Properties();
    public ConfigManager() {
        try (var in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) throw new IllegalStateException("Missing config.properties");
            values.load(in);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot load configuration"); }
    }
    public String get(String key) {
        return System.getProperty(key, System.getenv().getOrDefault(key.toUpperCase().replace('.', '_'), values.getProperty(key)));
    }
    public int number(String key) { return Integer.parseInt(get(key)); }
}

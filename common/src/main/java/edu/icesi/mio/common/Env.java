package edu.icesi.mio.common;

public final class Env {
    private Env() {
    }

    public static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public static int intValue(String name, int fallback) {
        try {
            return Integer.parseInt(value(name, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static boolean booleanValue(String name, boolean fallback) {
        String value = value(name, String.valueOf(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
}

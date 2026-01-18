package org.example.util;

public final class Log {
    private Log() {}

    public static void v(Config cfg, String msg) {
        if (cfg != null && cfg.isVerbose()) {
            System.out.println(msg);
        }
    }

    public static void i(String msg) {
        System.out.println(msg);
    }

    public static void e(String msg) {
        System.err.println(msg);
    }
}

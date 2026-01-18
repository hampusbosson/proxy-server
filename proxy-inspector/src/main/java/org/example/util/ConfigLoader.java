package org.example.util;

import java.util.*;

/**
 * Loads configuration from CLI args only (no config file support).
 *
 * Supported flags:
 *   --mode=proxy|api|both
 *   --proxy-port=8888
 *   --api-port=9090
 *   --max-transactions=1000
 *   --block-host=example.com        (repeatable)
 *   --block-path=host:/path         (repeatable, e.g. example.com:/admin)
 *   --verbose
 *   --help
 */
public final class ConfigLoader {

    private ConfigLoader() {}

    public static Config load(String[] args) {
        Args parsed = parseArgs(args);

        if (parsed.help) {
            throw new UsageException(usage());
        }

        // Defaults
        Mode mode = Mode.BOTH;
        int proxyPort = 8888;
        int apiPort = 9090;
        int maxTransactions = 1000;

        // Apply CLI overrides
        if (parsed.mode != null) mode = parsed.mode;
        if (parsed.proxyPort != null) proxyPort = parsed.proxyPort;
        if (parsed.apiPort != null) apiPort = parsed.apiPort;
        if (parsed.maxTransactions != null) maxTransactions = parsed.maxTransactions;

        return new Config(
                mode,
                proxyPort,
                apiPort,
                maxTransactions,
                parsed.blockedHosts,
                parsed.blockedPathsForHosts,
                parsed.verbose
        );
    }

    //Parsing
    private static Args parseArgs(String[] args) {
        Args out = new Args();
        if (args == null) return out;

        for (String a : args) {
            if (a == null) continue;
            String s = a.trim();
            if (s.isEmpty()) continue;

            if (s.equals("--help") || s.equals("-h")) {
                out.help = true;
                continue;
            }

            if (s.startsWith("--mode=")) {
                out.mode = parseMode(s.substring("--mode=".length()));
                continue;
            }

            if (s.startsWith("--proxy-port=")) {
                out.proxyPort = parseIntStrict(s.substring("--proxy-port=".length()), "proxy port");
                continue;
            }

            if (s.startsWith("--api-port=")) {
                out.apiPort = parseIntStrict(s.substring("--api-port=".length()), "api port");
                continue;
            }

            if (s.startsWith("--max-transactions=")) {
                out.maxTransactions = parseIntStrict(s.substring("--max-transactions=".length()), "max transactions");
                continue;
            }

            if (s.startsWith("--block-host=")) {
                String host = s.substring("--block-host=".length()).trim();
                if (!host.isEmpty()) out.blockedHosts.add(host);
                continue;
            }

            if (s.startsWith("--block-path=")) {
                String spec = s.substring("--block-path=".length()).trim();
                HostPath hp = parseHostPath(spec);
                out.blockedPathsForHosts.computeIfAbsent(hp.host, k -> new ArrayList<>()).add(hp.path);
                continue;
            }

            if (s.equals("--verbose")) {
                out.verbose = true;
                continue;
            }

            // hard fail on unknown args
            throw new UsageException("Unknown argument: " + s + "\n\n" + usage());
        }

        return out;
    }

    private static Mode parseMode(String raw) {
        if (raw == null) throw new UsageException("Missing mode value\n\n" + usage());
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "proxy" -> Mode.PROXY_ONLY;
            case "api" -> Mode.API_ONLY;
            case "both" -> Mode.BOTH;
            default -> throw new UsageException("Invalid mode: " + raw + "\n\n" + usage());
        };
    }

    private static int parseIntStrict(String raw, String what) {
        if (raw == null) throw new UsageException("Missing " + what + "\n\n" + usage());
        String t = raw.trim();
        if (t.isEmpty()) throw new UsageException("Missing " + what + "\n\n" + usage());
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new UsageException("Invalid " + what + ": " + raw + "\n\n" + usage());
        }
    }

    private static HostPath parseHostPath(String spec) {
        // format: host:/path
        if (spec == null) throw new UsageException("Invalid --block-path (empty)\n\n" + usage());
        String t = spec.trim();
        int idx = t.indexOf(':');
        if (idx <= 0 || idx == t.length() - 1) {
            throw new UsageException("Invalid --block-path. Use host:/path (e.g. example.com:/admin)\n\n" + usage());
        }
        String host = t.substring(0, idx).trim();
        String path = t.substring(idx + 1).trim();
        if (host.isEmpty() || path.isEmpty()) {
            throw new UsageException("Invalid --block-path. Use host:/path\n\n" + usage());
        }
        return new HostPath(host, path);
    }

    // ---------------- Usage / Errors ----------------

    public static String usage() {
        return """
                Usage:
                  java -jar proxyinspector.jar [options]

                Options:
                  --mode=proxy|api|both
                  --proxy-port=8888
                  --api-port=9090
                  --max-transactions=1000
                  --block-host=example.com        (repeatable)
                  --block-path=host:/path         (repeatable, e.g. example.com:/admin)
                  --verbose
                  --help

                Examples:
                  java -jar proxyinspector.jar --mode=both
                  java -jar proxyinspector.jar --block-host=httpbin.org
                  java -jar proxyinspector.jar --block-path=example.com:/admin --block-path=example.com:/private
                """;
    }

    public static final class UsageException extends RuntimeException {
        public UsageException(String message) { super(message); }
    }

    // Internal DTOs

    private static final class Args {
        boolean help;
        Mode mode;
        Integer proxyPort;
        Integer apiPort;
        Integer maxTransactions;
        final List<String> blockedHosts = new ArrayList<>();
        final Map<String, List<String>> blockedPathsForHosts = new HashMap<>();
        boolean verbose;
    }

    private record HostPath(String host, String path) {}
}
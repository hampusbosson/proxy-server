package org.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads runtime configuration from CLI args and (optionally) a simple config file.
 *
 * Supported flags:
 *   --mode=proxy|api|both
 *   --proxy-port=8888
 *   --api-port=9090
 *   --max-transactions=1000
 *   --block-host=example.com        (repeatable)
 *   --block-path=host:/path         (repeatable, e.g. example.com:/admin)
 *   --config=path/to/config.txt     (optional, simple line-based format)
 *   --help
 *
 * Config file format (lines, '#' comments allowed):
 *   mode=both
 *   proxyPort=8888
 *   apiPort=9090
 *   maxTransactions=1000
 *   blockHost=httpbin.org
 *   blockPath=example.com:/admin
 *   blockPath=example.com:/private
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
        List<String> blockedHosts = new ArrayList<>();
        Map<String, List<String>> blockedPathsForHosts = new HashMap<>();

        // Optional config file first (so CLI flags can override)
        if (parsed.configPath != null) {
            Mode finalMode = mode;
            int finalProxyPort = proxyPort;
            int finalApiPort = apiPort;
            int finalMaxTransactions = maxTransactions;
            applyConfigFile(parsed.configPath, new ApplyTarget() {
                @Override public void setMode(Mode m) { modeHolder.value = m; }
                @Override public void setProxyPort(int p) { proxyPortHolder.value = p; }
                @Override public void setApiPort(int p) { apiPortHolder.value = p; }
                @Override public void setMaxTransactions(int n) { maxTxHolder.value = n; }
                @Override public void addBlockedHost(String h) { blockedHosts.add(h); }
                @Override public void addBlockedPath(String host, String path) {
                    blockedPathsForHosts.computeIfAbsent(host, k -> new ArrayList<>()).add(path);
                }

                // holders so we can mutate from anonymous class cleanly
                final Holder<Mode> modeHolder = new Holder<>(finalMode);
                final Holder<Integer> proxyPortHolder = new Holder<>(finalProxyPort);
                final Holder<Integer> apiPortHolder = new Holder<>(finalApiPort);
                final Holder<Integer> maxTxHolder = new Holder<>(finalMaxTransactions);

                @Override public Mode getMode() { return modeHolder.value; }
                @Override public int getProxyPort() { return proxyPortHolder.value; }
                @Override public int getApiPort() { return apiPortHolder.value; }
                @Override public int getMaxTransactions() { return maxTxHolder.value; }
            });

            // Re-read current values from file-applier holders (see above)
            // (This is a little verbose to keep everything in one file without extra classes.)
            // We'll re-apply by loading the file into temp state and then overwriting defaults:
            FileState fs = readConfigFileState(parsed.configPath);
            if (fs.mode != null) mode = fs.mode;
            if (fs.proxyPort != null) proxyPort = fs.proxyPort;
            if (fs.apiPort != null) apiPort = fs.apiPort;
            if (fs.maxTransactions != null) maxTransactions = fs.maxTransactions;
            blockedHosts.addAll(fs.blockedHosts);
            mergePaths(blockedPathsForHosts, fs.blockedPathsForHosts);
        }

        // Apply CLI overrides
        if (parsed.mode != null) mode = parsed.mode;
        if (parsed.proxyPort != null) proxyPort = parsed.proxyPort;
        if (parsed.apiPort != null) apiPort = parsed.apiPort;
        if (parsed.maxTransactions != null) maxTransactions = parsed.maxTransactions;

        blockedHosts.addAll(parsed.blockedHosts);
        mergePaths(blockedPathsForHosts, parsed.blockedPathsForHosts);

        return new Config(
                mode,
                proxyPort,
                apiPort,
                maxTransactions,
                blockedHosts,
                blockedPathsForHosts
        );
    }

    // ---------------- Parsing ----------------

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

            if (s.startsWith("--config=")) {
                out.configPath = s.substring("--config=".length()).trim();
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

    // ---------------- Config file support ----------------

    private static void applyConfigFile(String path, ApplyTarget target) {
        // You can keep this method empty if you only want CLI.
        // It's here so you can extend later without changing your public API.
        // We read with readConfigFileState() and apply below.
    }

    private static FileState readConfigFileState(String pathStr) {
        Path p = Path.of(pathStr);
        if (!Files.exists(p)) {
            throw new UsageException("Config file not found: " + pathStr);
        }

        FileState fs = new FileState();

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;

                int eq = s.indexOf('=');
                if (eq <= 0 || eq == s.length() - 1) {
                    throw new UsageException("Invalid config line: " + s);
                }

                String key = s.substring(0, eq).trim();
                String val = s.substring(eq + 1).trim();

                switch (key) {
                    case "mode" -> fs.mode = parseMode(val);
                    case "proxyPort" -> fs.proxyPort = parseIntStrict(val, "proxyPort");
                    case "apiPort" -> fs.apiPort = parseIntStrict(val, "apiPort");
                    case "maxTransactions" -> fs.maxTransactions = parseIntStrict(val, "maxTransactions");
                    case "blockHost" -> {
                        if (!val.isEmpty()) fs.blockedHosts.add(val);
                    }
                    case "blockPath" -> {
                        HostPath hp = parseHostPath(val);
                        fs.blockedPathsForHosts.computeIfAbsent(hp.host, k -> new ArrayList<>()).add(hp.path);
                    }
                    default -> throw new UsageException("Unknown config key: " + key);
                }
            }
        } catch (IOException e) {
            throw new UsageException("Failed to read config file: " + e.getMessage());
        }

        return fs;
    }

    private static void mergePaths(Map<String, List<String>> into, Map<String, List<String>> from) {
        if (from == null || from.isEmpty()) return;
        for (Map.Entry<String, List<String>> e : from.entrySet()) {
            String host = e.getKey();
            List<String> paths = e.getValue();
            if (host == null || paths == null) continue;
            into.computeIfAbsent(host, k -> new ArrayList<>()).addAll(paths);
        }
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
                  --config=path/to/config.txt     (optional)
                  --help

                Examples:
                  java -jar proxyinspector.jar --mode=both
                  java -jar proxyinspector.jar --block-host=httpbin.org
                  java -jar proxyinspector.jar --block-path=example.com:/admin --block-path=example.com:/private
                  java -jar proxyinspector.jar --config=proxyinspector.conf
                """;
    }

    public static final class UsageException extends RuntimeException {
        public UsageException(String message) { super(message); }
    }

    // ---------------- Internal DTOs ----------------

    private static final class Args {
        boolean help;
        String configPath;
        Mode mode;
        Integer proxyPort;
        Integer apiPort;
        Integer maxTransactions;
        final List<String> blockedHosts = new ArrayList<>();
        final Map<String, List<String>> blockedPathsForHosts = new HashMap<>();
    }

    private record HostPath(String host, String path) {
    }

    private static final class FileState {
        Mode mode;
        Integer proxyPort;
        Integer apiPort;
        Integer maxTransactions;
        final List<String> blockedHosts = new ArrayList<>();
        final Map<String, List<String>> blockedPathsForHosts = new HashMap<>();
    }

    private interface ApplyTarget {
        void setMode(Mode m);
        void setProxyPort(int p);
        void setApiPort(int p);
        void setMaxTransactions(int n);
        void addBlockedHost(String h);
        void addBlockedPath(String host, String path);

        Mode getMode();
        int getProxyPort();
        int getApiPort();
        int getMaxTransactions();
    }

    private static final class Holder<T> {
        T value;
        Holder(T value) { this.value = value; }
    }
}
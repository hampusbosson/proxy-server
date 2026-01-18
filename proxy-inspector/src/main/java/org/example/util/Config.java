package org.example.util;

import java.util.*;

//TODO: REFACTOR NORMALIZATION OF BLOCKED PATHS AND HOSTS

public final class Config {
    private final Mode mode;
    private final int proxyPort;
    private final int apiPort;
    private final int maxTransactions;
    private final Set<String> blockedHosts;
    private final Map<String, Set<String>> blockedPathsForHosts;
    private final boolean verbose;

    public Config(
            Mode mode,
            int proxyPort,
            int apiPort,
            int maxTransactions,
            List<String> blockedHosts,
            Map<String, List<String>> blockedPathsForHosts,
            boolean verbose
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (proxyPort <= 0 || proxyPort > 65535) {
            throw new IllegalArgumentException("Invalid proxy port");
        }
        if (apiPort <= 0 || apiPort > 65535) {
            throw new IllegalArgumentException("Invalid API port");
        }
        if (maxTransactions <= 0) {
            throw new IllegalArgumentException("maxTransactions must be > 0");
        }

        this.mode = mode;
        this.proxyPort = proxyPort;
        this.apiPort = apiPort;
        this.maxTransactions = maxTransactions;
        this.verbose = verbose;

        // Normalize blocked hosts
        if (blockedHosts == null) {
            this.blockedHosts = Set.of();
        } else {
            List<String> normalized = new ArrayList<>();
            for (String h : blockedHosts) {
                if (h == null) continue;
                String v = h.trim().toLowerCase();
                if (!v.isEmpty()) normalized.add(v);
            }
            this.blockedHosts = Set.copyOf(normalized);
        }

        // Normalize blocked paths per host
        if (blockedPathsForHosts == null) {
            this.blockedPathsForHosts = Map.of();
        } else {
            Map<String, Set<String>> normalized = new HashMap<>();
            for (Map.Entry<String, List<String>> e : blockedPathsForHosts.entrySet()) {
                String host = e.getKey();
                List<String> paths = e.getValue();
                if (host == null || paths == null) continue;

                String nhost = host.trim().toLowerCase();
                if (nhost.isEmpty()) continue;

                List<String> npaths = new ArrayList<>();
                for (String p : paths) {
                    if (p == null) continue;
                    String np = p.trim().toLowerCase();
                    if (np.isEmpty()) continue;
                    if (!np.startsWith("/")) np = "/" + np;
                    if (np.endsWith("/") && np.length() > 1) {
                        np = np.substring(0, np.length() - 1);
                    }
                    npaths.add(np);
                }

                if (!npaths.isEmpty()) {
                    normalized.put(nhost, Set.copyOf(npaths));
                }
            }
            this.blockedPathsForHosts = Map.copyOf(normalized);
        }
    }

    // getters
    public Mode getMode() { return mode; }
    public int getProxyPort() { return proxyPort; }
    public int getApiPort() { return apiPort; }
    public int getMaxTransactions() { return maxTransactions; }
    public Set<String> getBlockedHosts() { return blockedHosts; }
    public Map<String, Set<String>> getBlockedPathsForHosts() { return blockedPathsForHosts; }
    public boolean isVerbose() { return verbose; }
}
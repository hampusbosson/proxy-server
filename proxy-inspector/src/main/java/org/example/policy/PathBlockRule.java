package org.example.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PathBlockRule {
    private final Map<String, Set<String>> blockedHostPaths; // all the paths blocked for a specific host

    public PathBlockRule(Map<String, Set<String>> blockedPaths) {
        Map<String, Set<String>> normalized = new HashMap<>();

        if (blockedPaths != null && !blockedPaths.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : blockedPaths.entrySet()) {
                String host = entry.getKey();
                Set<String> paths = entry.getValue();

                if (host == null || paths == null) {
                    continue;
                }

                String normalizedHost = host.trim().toLowerCase();
                if (normalizedHost.isEmpty()) {
                    continue;
                }

                Set<String> normalizedPaths = new HashSet<>();
                for (String path : paths) {
                    if (path == null) {
                        continue;
                    }

                    // empty path, skip
                    String normalizedPath = path.trim().toLowerCase();
                    if (normalizedPath.isEmpty()) {
                        continue;
                    }

                    // admin becomes /admin
                    if (!normalizedPath.startsWith("/")) {
                        normalizedPath = "/" + normalizedPath;
                    }

                    // /admin/ becomes just /admin
                    if (normalizedPath.endsWith("/") && normalizedPath.length() > 1) {
                        normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
                    }

                    normalizedPaths.add(normalizedPath);
                }

                if (!normalizedPaths.isEmpty()) {
                    normalized.put(normalizedHost, Set.copyOf(normalizedPaths));
                }
            }
        }

        this.blockedHostPaths = Map.copyOf(normalized); // immutable
    }


    public PolicyDecision evaluatePathForHost(String host, String path) {
        if (host == null || path == null) return null;

        String normalizedHost = host.trim().toLowerCase();
        if (normalizedHost.isEmpty()) return null;

        // normalize
        String normalizedPath = path.trim().toLowerCase();
        if (normalizedPath.isEmpty()) return null;
        if (!normalizedPath.startsWith("/")) normalizedPath = "/" + normalizedPath;
        if (normalizedPath.endsWith("/") && normalizedPath.length() > 1) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        Set<String> blockedPaths = blockedHostPaths.get(normalizedHost);
        if (blockedPaths == null || blockedPaths.isEmpty()) return null;

        for (String blocked : blockedPaths) {
            // exact match
            if (normalizedPath.equals(blocked)) {
                return PolicyDecision.block(403, "Blocked path " + blocked + " on host " + normalizedHost);
            }
            // prefix match, but not /admin matching /administrator
            if (normalizedPath.startsWith(blocked + "/")) {
                return PolicyDecision.block(403, "Blocked path " + blocked + " on host " + normalizedHost);
            }
        }

        return null;
    }

}

package org.example.policy;

import java.util.HashSet;
import java.util.Set;

public class HostBlockRule {
    private final Set<String> blockedHosts;

    public HostBlockRule(Set<String> blockedHosts) {
        Set<String> normalizedHosts = new HashSet<>();

        if (blockedHosts != null && !blockedHosts.isEmpty()) {
            for (String host : blockedHosts) {
                if (host == null) {
                    continue;
                }

                String normalizedHost = host.trim().toLowerCase();
                if (normalizedHost.isEmpty()) {
                    continue;
                }

                normalizedHosts.add(normalizedHost);
            }
        }

        this.blockedHosts = Set.copyOf(normalizedHosts); // immutable
    }

    public PolicyDecision evaluateHost(String host) {
        if (host == null || host.isEmpty()) {
            return null;
        }

        String normalizedHost = host.trim().toLowerCase();
        if (normalizedHost.isEmpty()) {
            return null;
        }

        if (this.blockedHosts.contains(normalizedHost)) {
            return PolicyDecision.block(403, "Host is on blocked list");
        }

        return null;
    }

}

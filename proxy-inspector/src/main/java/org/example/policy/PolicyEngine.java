package org.example.policy;

import org.example.http.HttpRequest;
import org.example.util.Config;

public final class PolicyEngine {
    private final HostBlockRule hostRule;
    private final PathBlockRule pathRule;
    private final RateLimiter rateLimiter;

    public PolicyEngine(Config cfg) {
        this.hostRule = new HostBlockRule(cfg.getBlockedHosts());
        this.pathRule = new PathBlockRule(cfg.getBlockedPathsForHosts());
        this.rateLimiter = new RateLimiter(30, 10_000, 5 * 60_000);
    }

    public PolicyDecision evaluate(HttpRequest request, String clientIp) {
        String targetHost = request.getHost();
        String targetPath = request.getPath();
        // String method = request.getMethod(); //TODO: needed for rate limit per clientIP later

        rateLimiter.evictIdle();
        if (!rateLimiter.allow(clientIp)) {
            return PolicyDecision.block(429, "Too many requests");
        }

        // block whole hosts
        PolicyDecision decision = hostRule.evaluateHost(targetHost);
        if (decision != null && decision.isBlocked()) {
            return decision;
        }

        // block paths for specific hosts
        decision = pathRule.evaluatePathForHost(targetHost, targetPath);
        if (decision != null && decision.isBlocked()) {
            return decision;
        }

        return PolicyDecision.allow();
    }


}

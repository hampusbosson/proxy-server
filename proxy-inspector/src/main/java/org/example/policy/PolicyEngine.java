package org.example.policy;

import org.example.http.HttpRequest;

import java.util.Map;
import java.util.Set;

public final class PolicyEngine {
    private static final RateLimiter RATE_LIMITER = new RateLimiter(30, 10_000, 5 * 60_000); // no more than 30 requests per 10 seconds, evict after 5 min idle

    // immutable host block rules, TODO: load from file later
    private static final HostBlockRule HOST_RULE = new HostBlockRule(Set.of(
            "bad.com",
            "tracker.example",
            "httpbin.org"
    ));
    // TODO: load from file later
    private static final PathBlockRule PATH_RULE = new PathBlockRule(Map.of(
            "example.com", Set.of("/admin", "/private"),
            "api.foo.com", Set.of("/debug")
    ));

    public static PolicyDecision evaluate(HttpRequest request, String clientIp) {
        String targetHost = request.getHost();
        String targetPath = request.getPath();
        // String method = request.getMethod(); //TODO: needed for rate limit per clientIP later

        RATE_LIMITER.evictIdle();
        if (!RATE_LIMITER.allow(clientIp)) {
            return PolicyDecision.block(429, "Too many requests");
        }

        // block whole hosts
        PolicyDecision decision = HOST_RULE.evaluateHost(targetHost);
        if (decision != null && decision.isBlocked()) {
            return decision;
        }

        // block paths for specific hosts
        decision = PATH_RULE.evaluatePathForHost(targetHost, targetPath);
        if (decision != null && decision.isBlocked()) {
            return decision;
        }

        return PolicyDecision.allow();
    }


}

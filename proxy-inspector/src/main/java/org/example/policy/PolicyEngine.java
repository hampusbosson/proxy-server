package org.example.policy;

import org.example.http.HttpRequest;

import java.util.Map;
import java.util.Set;

public final class PolicyEngine {

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
        //String method = request.getMethod(); //TODO: needed for rate limit per clientIP later

        // block whole hosts
        PolicyDecision decision = HOST_RULE.evaluateHost(targetHost);
        if (decision != null) {
            return decision;
        }

        // block paths for specific hosts
        decision = PATH_RULE.evaluatePathForHost(targetHost, targetPath);
        if (decision != null) {
            return decision;
        }

        //TODO: Rate limit per clientIP (429), method specific rules

        return PolicyDecision.allow();
    }


}

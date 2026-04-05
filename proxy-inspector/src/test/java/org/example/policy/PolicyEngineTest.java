package org.example.policy;

import org.example.http.HttpRequest;
import org.example.util.Config;
import org.example.util.Mode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEngineTest {

    @Test
    void allowsRequestsWhenNoRulesMatch() {
        PolicyEngine engine = new PolicyEngine(config(List.of(), Map.of()));

        PolicyDecision decision = engine.evaluate(request("example.com", "http://example.com/docs"), "127.0.0.1");

        assertTrue(decision.isAllowed());
        assertEquals(200, decision.getHttpStatus());
    }

    @Test
    void blocksConfiguredHosts() {
        PolicyEngine engine = new PolicyEngine(config(List.of("example.com"), Map.of()));

        PolicyDecision decision = engine.evaluate(request("example.com", "http://example.com/"), "127.0.0.1");

        assertTrue(decision.isBlocked());
        assertEquals(403, decision.getHttpStatus());
        assertEquals("Host is on blocked list", decision.getReason());
    }

    @Test
    void blocksConfiguredPathPrefixesForHost() {
        PolicyEngine engine = new PolicyEngine(config(
                List.of(),
                Map.of("example.com", List.of("/admin"))
        ));

        PolicyDecision decision = engine.evaluate(
                request("example.com", "http://example.com/admin/users"),
                "127.0.0.1"
        );

        assertTrue(decision.isBlocked());
        assertEquals(403, decision.getHttpStatus());
        assertTrue(decision.getReason().contains("/admin"));
    }

    @Test
    void doesNotBlockSimilarButDifferentPath() {
        PolicyEngine engine = new PolicyEngine(config(
                List.of(),
                Map.of("example.com", List.of("/admin"))
        ));

        PolicyDecision decision = engine.evaluate(
                request("example.com", "http://example.com/administrator"),
                "127.0.0.1"
        );

        assertFalse(decision.isBlocked());
        assertTrue(decision.isAllowed());
    }

    private static Config config(List<String> blockedHosts, Map<String, List<String>> blockedPaths) {
        return new Config(Mode.BOTH, 8888, 9090, 1000, blockedHosts, blockedPaths, false);
    }

    private static HttpRequest request(String host, String target) {
        return new HttpRequest(
                "GET",
                target,
                "HTTP/1.1",
                Map.of("Host", host),
                null
        );
    }
}

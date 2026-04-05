package org.example.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @Test
    void loadsDefaultsWhenNoArgsProvided() {
        Config config = ConfigLoader.load(new String[0]);

        assertEquals(Mode.BOTH, config.getMode());
        assertEquals(8888, config.getProxyPort());
        assertEquals(9090, config.getApiPort());
        assertEquals(1000, config.getMaxTransactions());
        assertTrue(config.getBlockedHosts().isEmpty());
        assertTrue(config.getBlockedPathsForHosts().isEmpty());
    }

    @Test
    void parsesCustomValuesAndNormalizesRules() {
        Config config = ConfigLoader.load(new String[] {
                "--mode=proxy",
                "--proxy-port=8080",
                "--api-port=9191",
                "--max-transactions=250",
                "--block-host=Example.com",
                "--block-host=HTTPBIN.ORG",
                "--block-path=example.com:admin",
                "--block-path=example.com:/private/",
                "--verbose"
        });

        assertEquals(Mode.PROXY_ONLY, config.getMode());
        assertEquals(8080, config.getProxyPort());
        assertEquals(9191, config.getApiPort());
        assertEquals(250, config.getMaxTransactions());
        assertTrue(config.getBlockedHosts().contains("example.com"));
        assertTrue(config.getBlockedHosts().contains("httpbin.org"));
        assertTrue(config.getBlockedPathsForHosts().get("example.com").contains("/admin"));
        assertTrue(config.getBlockedPathsForHosts().get("example.com").contains("/private"));
        assertTrue(config.isVerbose());
    }

    @Test
    void rejectsUnknownArguments() {
        ConfigLoader.UsageException exception = assertThrows(
                ConfigLoader.UsageException.class,
                () -> ConfigLoader.load(new String[] { "--wat=1" })
        );

        assertTrue(exception.getMessage().contains("Unknown argument"));
    }

    @Test
    void rejectsInvalidBlockPathFormat() {
        ConfigLoader.UsageException exception = assertThrows(
                ConfigLoader.UsageException.class,
                () -> ConfigLoader.load(new String[] { "--block-path=example.com" })
        );

        assertTrue(exception.getMessage().contains("Invalid --block-path"));
    }
}

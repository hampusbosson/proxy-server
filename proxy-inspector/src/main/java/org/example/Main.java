package org.example;
import org.example.api.ApiServer;
import org.example.log.TransactionStore;
import org.example.policy.PolicyEngine;
import org.example.server.ProxyServer;
import org.example.util.Config;
import org.example.util.ConfigLoader;
import org.example.util.Mode;

public class Main {
    public static void main(String[] args) {
        Config config;

        try {
            config = ConfigLoader.load(args);
        } catch (ConfigLoader.UsageException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        TransactionStore store = new TransactionStore(config.getMaxTransactions());
        PolicyEngine policyEngine = new PolicyEngine(config);

        // start proxy server
        if (config.getMode() == Mode.PROXY_ONLY || config.getMode() == Mode.BOTH) {
            ProxyServer proxy = new ProxyServer(config, store, policyEngine);
            new Thread(proxy::start, "proxy-server").start();
        }

        // start api server
        if (config.getMode() == Mode.API_ONLY || config.getMode() == Mode.BOTH) {
            ApiServer api = new ApiServer(config, store);
            new Thread(api::start, "api-server").start();
        }

    }
}
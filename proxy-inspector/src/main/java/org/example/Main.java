package org.example;
import org.example.server.ProxyServer;

public class Main {
    public static void main(String[] args) {
        ProxyServer server = new ProxyServer();

        server.start();
    }
}
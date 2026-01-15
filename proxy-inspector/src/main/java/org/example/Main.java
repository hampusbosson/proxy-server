package org.example;
import org.example.api.ApiServer;
import org.example.server.ProxyServer;

public class Main {
    public static void main(String[] args) {
        ProxyServer proxyServer = new ProxyServer();
        ApiServer apiServer = new ApiServer();

        Thread proxyThread = new Thread(proxyServer::start, "proxy-server");
        Thread apiThread = new Thread(apiServer::start, "api-server");

        proxyThread.start();
        apiThread.start();
    }
}
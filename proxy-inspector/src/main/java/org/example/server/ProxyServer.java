package org.example.server;

import org.example.log.TransactionStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyServer {
    private static final int port = 8888;
    private final ServerSocket server;
    private final ExecutorService pool = Executors.newFixedThreadPool(50);
    private volatile boolean running;
    AtomicInteger counter = new AtomicInteger(0);
    private static final TransactionStore TRANSACTION_STORE = new TransactionStore(1000);

    public ProxyServer() {
        try {
            server = new ServerSocket(port);
        } catch(IOException e) {
            System.err.println("Error creating server: " + e);
            throw new RuntimeException();
        }
    }

    public void start() {
        try {
            running = true;
            System.out.println("Server running on port: " + port);
            while (running) {
                Socket connection = server.accept();
                Callable<Void> task = new ClientConnectionHandler(connection, counter);
                pool.submit(task);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            pool.shutdown();
        }
    }

    public void stop() {
        running = false;
        try {
            server.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e);
        } finally {
            pool.shutdown();
        }
    }

    public static TransactionStore getTransactionStore() {
        return TRANSACTION_STORE;
    }
}

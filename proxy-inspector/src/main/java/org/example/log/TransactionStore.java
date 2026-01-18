package org.example.log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class TransactionStore {
    private final int maxSize;
    private final ArrayDeque<Transaction> deque = new ArrayDeque<>();

    public TransactionStore(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException();
        }

        this.maxSize = maxSize;
    }

    public synchronized void add(Transaction t) {
        if (t == null) {
            return;
        }

        if (deque.size() >= maxSize) {
            deque.removeFirst(); // drop oldest
        }
        deque.addLast(t); // add newest
    }

    public synchronized List<Transaction> list() {
        return new ArrayList<>(deque); // snapshot copy
    }

    // Returns the current number of stored transactions in a thread-safe manner
    public synchronized int sizeSafe() {
        return deque.size();
    }

    // returns the latest n transactions in the right order
    public synchronized List<Transaction> getRecent(int n) {
        if (n <= 0) {
            return List.of();
        }

        int size = deque.size();
        int from = Math.max(0, size - n);

        List<Transaction> result = new ArrayList<>(Math.min(n, size));
        int i = 0;
        for (Transaction t : deque) {
            if (i >= from) result.add(t);
            i++;
        }

        return result;
    }

}

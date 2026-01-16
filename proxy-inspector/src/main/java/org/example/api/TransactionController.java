package org.example.api;

import org.example.log.Transaction;
import org.example.log.TransactionStore;
import org.example.log.Verdict;
import org.example.server.ProxyServer;

import java.util.ArrayList;
import java.util.List;

/**
 * TransactionController provides read-only functions that returns data from the shared TransactionStore.
 * <p>
 * This class is used by the API to:
 * - List recent transactions
 * - filter transactions by verdict (ALLOW, BLOCKED, ERROR)
 * - provide statistics for dashboards or monitoring
 * <p>
 * It does not modify transactions, only reads from TransactionStore
 */

public class TransactionController {
    private static final int DEFAULT_LIMIT = 50;  // default number of transactions if no limit is provided
    private static final int MAX_LIMIT = 1000; // max limit for transactions to prevent large memory responses
    private final TransactionStore store; // Shared transaction store (singleton owned by ProxyServer)

    public TransactionController() {
        store = ProxyServer.getTransactionStore();
    }

    public ApiResponse<List<Transaction>> listTransactions(Integer limit, String verdictStr) {
        int n = clampLimit(limit);

        // fetch most recent N transactions
        List<Transaction> recent = store.getRecent(n);

        // Parse verdict filter if provided
        Verdict verdict = parseVerdictOrNull(verdictStr);
        if (verdictStr != null && verdict == null) {
            return ApiResponse.error(400, "Invalid verdict. Use ALLOWED, BLOCKED or ERROR");
        }

        // No verdict filter: return all recent transactions
        if (verdict == null) {
            return ApiResponse.ok(recent);
        }

        // Apply verdict filter
        List<Transaction> filteredRecents = new ArrayList<>();
        for (Transaction t : recent) {
            if (t.getVerdict() == verdict) {
                filteredRecents.add(t);
            }
        }

        return ApiResponse.ok(filteredRecents);
    }

    /**
     * Returns statistics for all stored transactions.
     * to be used for dashboard and statistics.
     */
    public ApiResponse<StatsResponse> stats() {
        List<Transaction> all = store.list();

        long total = 0;
        long allowed = 0;
        long blocked = 0;
        long error = 0;

        long bytes = 0;
        long sumDurationMs = 0;
        long durationCount = 0;

        // Aggregate statistics
        for (Transaction tx : all) {
            if (tx == null) continue;

            total++;

            if (tx.getVerdict() == Verdict.ALLOWED) allowed++;
            else if (tx.getVerdict() == Verdict.BLOCKED) blocked++;
            else if (tx.getVerdict() == Verdict.ERROR) error++;

            bytes += Math.max(0, tx.getBytesFromServer());

            long durMs = tx.getDurationMs();
            if (durMs >= 0) {
                sumDurationMs += durMs;
                durationCount++;
            }
        }

        long avgMs = (durationCount == 0) ? 0 : (sumDurationMs / durationCount);

        StatsResponse stats = new StatsResponse(
                total,
                allowed,
                blocked,
                error,
                bytes,
                avgMs
        );

        return ApiResponse.ok(stats);
    }

    /**
     * Ensures the requested limit stays within safe bounds
     */
    private int clampLimit(Integer limit) {
        int n = (limit == null) ? DEFAULT_LIMIT : limit;
        if (n < 1) n = 1; // returns at least one entry
        if (n > MAX_LIMIT) n = MAX_LIMIT;
        return n;
    }

    /**
     * Parses a verdict string into a Verdict enum
     * Returns null if the input is null, empty or invalid
     */
    private Verdict parseVerdictOrNull(String verdictString) {
        if (verdictString == null) return null;

        String verdict = verdictString.trim().toUpperCase();
        if (verdict.isEmpty()) return null;

        try {
            return Verdict.valueOf(verdict);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Simple record class used for /stats responses.
     */
    public record StatsResponse(long total, long allowed, long blocked, long error, long bytesFromServerTotal,
                                long avgDurationMs) {
    }
}

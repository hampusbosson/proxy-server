package org.example.api;

import org.example.log.Transaction;
import org.example.log.TransactionStore;
import org.example.log.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionControllerTest {

    @Test
    void listTransactionsRespectsLimitAndVerdictFilter() {
        TransactionStore store = new TransactionStore(10);
        store.add(transaction("GET", "example.com", "/", Verdict.ALLOWED, 100, 10, null));
        store.add(transaction("GET", "blocked.com", "/admin", Verdict.BLOCKED, 0, 0, "Host is on blocked list"));
        store.add(transaction("GET", "error.test", "/", Verdict.ERROR, 0, 15, "Bad Gateway"));

        TransactionController controller = new TransactionController(store);

        ApiResponse<List<Transaction>> response = controller.listTransactions(2, "BLOCKED");

        assertTrue(response.success);
        assertNull(response.error);
        assertEquals(1, response.data.size());
        assertEquals(Verdict.BLOCKED, response.data.get(0).getVerdict());
    }

    @Test
    void listTransactionsRejectsInvalidVerdict() {
        TransactionController controller = new TransactionController(new TransactionStore(10));

        ApiResponse<List<Transaction>> response = controller.listTransactions(10, "BAD");

        assertFalse(response.success);
        assertEquals(400, response.statusCode);
        assertEquals("Invalid verdict. Use ALLOWED, BLOCKED or ERROR", response.error);
    }

    @Test
    void statsAggregatesStoredTransactions() {
        TransactionStore store = new TransactionStore(10);
        store.add(transaction("GET", "example.com", "/", Verdict.ALLOWED, 1200, 25, null));
        store.add(transaction("GET", "blocked.com", "/admin", Verdict.BLOCKED, 0, 0, "blocked"));
        store.add(transaction("GET", "error.test", "/", Verdict.ERROR, 0, 35, "Bad Gateway"));

        TransactionController controller = new TransactionController(store);

        ApiResponse<TransactionController.StatsResponse> response = controller.stats();

        assertTrue(response.success);
        assertEquals(3, response.data.total());
        assertEquals(1, response.data.allowed());
        assertEquals(1, response.data.blocked());
        assertEquals(1, response.data.error());
        assertEquals(1200, response.data.bytesFromServerTotal());
        assertEquals(20, response.data.avgDurationMs());
    }

    private static Transaction transaction(
            String method,
            String host,
            String path,
            Verdict verdict,
            long bytes,
            long durationMs,
            String errorMessage
    ) {
        Transaction transaction = new Transaction(method, host, 80, path, 0);
        transaction.setVerdict(verdict);
        transaction.setBytesFromServer(bytes);
        transaction.setEndNs(durationMs * 1_000_000);
        transaction.setErrorMessage(errorMessage);
        return transaction;
    }
}

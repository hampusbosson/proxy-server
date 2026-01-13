package org.example.log;

public class Transaction {
    private final String method;
    private final String host;
    private final int port;
    private final String path;

    private long startNs;
    private long endNs;

    private long bytesFromServer;
    private Verdict verdict;
    private String errorMessage;

    public Transaction(String method, String host, int port, String path, long startNs) {
        this.method = method;
        this.host = host;
        this.port = port;
        this.path = path;
        this.startNs = startNs;
        this.verdict = Verdict.ALLOWED; // default, can change later
    }

    /*
    SETTERS
     */
    public void setEndNs(long endNs) {
        this.endNs = endNs;
    }

    public void setBytesFromServer(long bytesFromServer) {
        this.bytesFromServer = bytesFromServer;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /*
    GETTERS
     */
    public String getMethod() {
        return method;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public long getStartNs() {
        return startNs;
    }

    public long getEndNs() {
        return endNs;
    }

    public long getBytesFromServer() {
        return bytesFromServer;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getDurationNs() {
        if (endNs == 0) return 0;
        return endNs - startNs;
    }

    public long getDurationMs() {
        return getDurationNs() / 1_000_000;
    }

    /*
    TO STRING
     */
    @Override
    public String toString() {
        return verdict +
                " " + method +
                " " + host + ":" + port +
                " " + path +
                " bytes=" + bytesFromServer +
                " durationMs=" + getDurationMs() +
                (errorMessage != null ? " error=\"" + errorMessage + "\"" : "");
    }

}

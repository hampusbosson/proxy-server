package org.example.policy;

public class PolicyDecision {
    private final Decision decision;
    private final String reason;
    private final int httpStatus;


    public PolicyDecision(Decision decision, int httpStatus, String reason) {
        this.decision = decision;
        this.reason = reason;
        this.httpStatus = httpStatus;
    }

    public boolean isAllowed() {
        return this.decision == Decision.ALLOW;
    }

    public boolean isBlocked() {
        return this.decision == Decision.BLOCK;
    }

    public Decision getDecision() {
        return this.decision;
    }

    public String getReason() {
        return this.reason;
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }


    // static factory metods for convenience
    public static PolicyDecision allow() {
        return new PolicyDecision(Decision.ALLOW, 200, "Allowed");
    }

    public static PolicyDecision block(int httpStatus, String reason) {
        return new PolicyDecision(Decision.BLOCK, httpStatus, reason);
    }

}

package net.techcrunch.outflowPayment.reconciliation;

public record AssignReconciliationItemRequest(String assignedTo, String note, String actor) {
}

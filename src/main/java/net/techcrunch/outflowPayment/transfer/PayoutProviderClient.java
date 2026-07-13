package net.techcrunch.outflowPayment.transfer;

public interface PayoutProviderClient {

    String providerName();

    PayoutTransferResult transfer(PayoutTransferRequest request);
}

package net.techcrunch.outflowPayment.payment;


import java.io.Serializable;


public class AdminApproval implements Serializable {
    public String paymentApprovalStatus;

    public String getPaymentApprovalStatus() {
        return paymentApprovalStatus;
    }

    public void setPaymentApprovalStatus(String paymentApprovalStatus) {
        this.paymentApprovalStatus = paymentApprovalStatus;
    }
}

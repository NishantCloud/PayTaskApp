package com.qolorco.paytask.models;

import com.google.firebase.Timestamp;

public class WithdrawalModel {
    private String qolor_email;
    private double amount;
    private String status;
    private Timestamp requested_at;

    public WithdrawalModel() {} // Firestore needs this

    public String getQolor_email() {
        return qolor_email;
    }

    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public Timestamp getRequested_at() { return requested_at; }
}


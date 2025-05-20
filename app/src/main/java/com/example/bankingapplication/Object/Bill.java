package com.example.bankingapplication.Object;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class Bill implements Serializable {
    private String UID;
    private String transactionId;
    private Integer amount;
    private String status;
    private transient Timestamp dueDate;
    private String billNumber;
    private String provider;
    private String type;
    private String userId;

    public Bill() {
    }

    public Bill(String UID, String transactionId, Integer amount, String status, Timestamp dueDate, String billNumber, String provider, String type, String userId) {
        this.UID = UID;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.dueDate = dueDate;
        this.billNumber = billNumber;
        this.provider = provider;
        this.type = type;
        this.userId = userId;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }



    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

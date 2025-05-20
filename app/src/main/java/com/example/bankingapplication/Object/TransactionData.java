package com.example.bankingapplication.Object;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class TransactionData implements Serializable {
    private String UID;
    private Integer amount;
    private String type;
    private String status;
    private String description;
    private transient Timestamp createdAt;
    private String frontAccountId;
    private String toAccountId;

    public TransactionData() {
    }

    public TransactionData(String UID, Integer amount, String type, String status, String description, Timestamp createdAt, String frontAccountId, String toAccountId) {
        this.UID = UID;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.frontAccountId = frontAccountId;
        this.toAccountId = toAccountId;
    }

    public TransactionData(String UID, Integer amount, String type, String status, String description, Timestamp createdAt, String frontAccountId) {
        this.UID = UID;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.frontAccountId = frontAccountId;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFrontAccountId() {
        return frontAccountId;
    }

    public void setFrontAccountId(String frontAccountId) {
        this.frontAccountId = frontAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }
}

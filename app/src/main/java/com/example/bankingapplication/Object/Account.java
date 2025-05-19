package com.example.bankingapplication.Object;

import com.google.firebase.Timestamp;

public class Account {
    private String UID;
    private String accountNumber;
    private String userId;
    private String status;
    private CheckingAccount checking;
    private SavingAccount saving;
    private MortgageAccount mortgage;
    private transient Timestamp  createdAt;

    public Account() {
    }

    public Account(String UID, String accountNumber, String userId, String status, CheckingAccount checking, SavingAccount saving, MortgageAccount mortgage, Timestamp createdAt) {
        this.UID = UID;
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.status = status;
        this.checking = checking;
        this.saving = saving;
        this.mortgage = mortgage;
        this.createdAt = createdAt;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CheckingAccount getChecking() {
        return checking;
    }

    public void setChecking(CheckingAccount checking) {
        this.checking = checking;
    }

    public SavingAccount getSaving() {
        return saving;
    }

    public void setSaving(SavingAccount saving) {
        this.saving = saving;
    }

    public MortgageAccount getMortgage() {
        return mortgage;
    }

    public void setMortgage(MortgageAccount mortgage) {
        this.mortgage = mortgage;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
package com.example.bankingapplication.Object;

import com.google.firebase.firestore.Exclude;

public class CheckingAccount {
    private Integer balance;

    @Exclude  // Firebase sẽ không lưu thuộc tính này
    private Account parentAccount;

    public CheckingAccount() {
        // Default constructor required for calls to DataSnapshot.getValue(CheckingAccount.class)
    }

    // Getter + Setter
    public Account getParentAccount() {
        return parentAccount;
    }
    public void setParentAccount(Account parent) {
        this.parentAccount = parent;
    }

    public CheckingAccount(Integer balance) {
        this.balance = balance;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }
}
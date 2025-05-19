package com.example.bankingapplication.Object;

import com.google.firebase.firestore.Exclude;

public class SavingAccount {
    private Integer balance;
    private double interestRate;

    @Exclude  // Firebase sẽ không lưu thuộc tính này
    private Account parentAccount;

    public Account getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(Account parent) {
        this.parentAccount = parent;
    }

    public SavingAccount() {
    }

    public SavingAccount(Integer balance, double interestRate, String status) {
        this.balance = balance;
        this.interestRate = interestRate;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

}

package com.example.bankingapplication.Object;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class MortgageAccount {
    private Integer loanAmount;
    private Integer paymentAmount;
    private Integer remainingAmount;
    private int interestRate;
    private String paymentFrequency;
    private transient Timestamp startDate;
    private transient Timestamp endDate;
    @Exclude  // Firebase sẽ không lưu thuộc tính này
    private Account parentAccount;

    public Account getParentAccount() {
        return parentAccount;
    }
    public void setParentAccount(Account parent) {
        this.parentAccount = parent;
    }

    public MortgageAccount() {
        // Default constructor required for calls to DataSnapshot.getValue(MortgageAccount.class)
    }

    public MortgageAccount(Integer loanAmount, Integer paymentAmount, Integer remainingAmount, int interestRate, String paymentFrequency, Timestamp startDate, Timestamp endDate) {
        this.loanAmount = loanAmount;
        this.paymentAmount = paymentAmount;
        this.remainingAmount = remainingAmount;
        this.interestRate = interestRate;
        this.paymentFrequency = paymentFrequency;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Integer getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(Integer loanAmount) {
        this.loanAmount = loanAmount;
    }

    public Integer getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Integer paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public Integer getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(Integer remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public int getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(int interestRate) {
        this.interestRate = interestRate;
    }

    public String getPaymentFrequency() {
        return paymentFrequency;
    }

    public void setPaymentFrequency(String paymentFrequency) {
        this.paymentFrequency = paymentFrequency;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }
}
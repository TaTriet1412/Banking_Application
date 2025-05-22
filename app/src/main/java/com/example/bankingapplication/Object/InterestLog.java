package com.example.bankingapplication.Object;

import com.google.firebase.Timestamp;

public class InterestLog {
    private String id;
    private Timestamp period;
    private Integer interestAmount;
    private Double interestRate;

    public InterestLog() {
        // Required empty constructor for Firestore
    }

    public InterestLog(String id, Timestamp period, Integer interestAmount, Double interestRate) {
        this.id = id;
        this.period = period;
        this.interestAmount = interestAmount;
        this.interestRate = interestRate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getPeriod() {
        return period;
    }

    public void setPeriod(Timestamp period) {
        this.period = period;
    }

    public Integer getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(Integer interestAmount) {
        this.interestAmount = interestAmount;
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }
}

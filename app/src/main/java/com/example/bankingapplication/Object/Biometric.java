package com.example.bankingapplication.Object;

public class Biometric {
    private String faceUrl;
    private String cardFrontUrl;
    private String cardBackUrl;

    public Biometric(String faceUrl, String cardFrontUrl, String cardBackUrl) {
        this.faceUrl = faceUrl;
        this.cardFrontUrl = cardFrontUrl;
        this.cardBackUrl = cardBackUrl;
    }
}

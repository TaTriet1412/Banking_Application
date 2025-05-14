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

    public Biometric() {
        // Default constructor required for Firebase
    }

    // ✅ Getter cho Firebase
    public String getFaceUrl() {
        return faceUrl;
    }

    public String getCardFrontUrl() {
        return cardFrontUrl;
    }

    public String getCardBackUrl() {
        return cardBackUrl;
    }

    // ✅ Setter nếu cần
    public void setFaceUrl(String faceUrl) {
        this.faceUrl = faceUrl;
    }

    public void setCardFrontUrl(String cardFrontUrl) {
        this.cardFrontUrl = cardFrontUrl;
    }

    public void setCardBackUrl(String cardBackUrl) {
        this.cardBackUrl = cardBackUrl;
    }
}

package com.example.bankingapplication.Utils;

import java.util.Random;

public class AccountUtils {
    
    // Payment frequency types
    public static final String PAYMENT_FREQUENCY_MONTHLY = "monthly";
    public static final String PAYMENT_FREQUENCY_QUARTERLY = "quarterly";
    public static final String PAYMENT_FREQUENCY_ANNUALLY = "annually";
    
    // Account status types
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_CLOSED = "closed";
    
    // Function to generate a random account number (10 digits)
    public static String generateRandomAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    // Function to translate payment frequency for display
    public static String translateTypeOfPaymentFrequency(String frequency) {
        if (frequency == null) return "N/A";
        
        switch (frequency.toLowerCase()) {
            case PAYMENT_FREQUENCY_MONTHLY:
                return "Hàng tháng";
            case PAYMENT_FREQUENCY_QUARTERLY:
                return "Hàng quý";
            case PAYMENT_FREQUENCY_ANNUALLY:
                return "Hàng năm";
            default:
                return frequency;
        }
    }
    
    // Add other account utility methods as needed
}

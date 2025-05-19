package com.example.bankingapplication.Utils;

public class NumberFormat {
    public static String convertToCurrencyFormatHasUnit(Integer amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
        return nf.format(amount) + " VNĐ";
    }

    public static String convertToCurrencyFormatOnlyNumber(Integer amount) {
        if (amount == null) {
            return "0";
        }
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
        return nf.format(amount);
    }


}

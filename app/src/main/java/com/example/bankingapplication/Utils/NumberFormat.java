package com.example.bankingapplication.Utils;

public class NumberFormat {
public static String convertToCurrencyFormat(Integer amount) {
    if (amount == null) {
        return "0 VNĐ";
    }
    java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
    return nf.format(amount) + " VNĐ";
}
}

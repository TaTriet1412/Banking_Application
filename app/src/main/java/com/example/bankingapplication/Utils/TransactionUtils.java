package com.example.bankingapplication.Utils;

public class TransactionUtils {

    public static String translateTransactionType(String type) {
        if (type == null) return "Không xác định";

        switch (type.toLowerCase()) {
            case "deposit":
                return "Nạp tiền";
            case "withdrawal":
                return "Rút tiền";
            case "transfer":
                return "Chuyển khoản";
            case "payment":
                return "Thanh toán";
            default:
                return "Không xác định";
        }
    }
}

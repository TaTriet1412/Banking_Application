package com.example.bankingapplication.Utils;

public class BillUtils {

    public static String translateBillType(String type) {
        if (type == null) return "Không xác định";

        switch (type.toLowerCase()) {
            case "transfer":
                return "Chuyển khoản";
            case "electricity":
                return "Hóa đơn điện";
            case "water":
                return "Hóa đơn nước";
            case "phone":
                return "Hóa đơn điện thoại";
            case "flight":
                return "Vé máy bay";
            case "movie":
                return "Vé xem phim";
            case "hotel":
                return "Đặt phòng khách sạn";
            case "ecommerce":
                return "Mua sắm online";
            default:
                return "Không xác định";
        }
    }
}

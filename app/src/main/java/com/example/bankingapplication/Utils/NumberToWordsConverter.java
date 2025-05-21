package com.example.bankingapplication.Utils;

public class NumberToWordsConverter {

    private static final String[] chuSo = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    private static final String[] donViNhom = {"", "nghìn", "triệu", "tỷ"};

    /**
     * Chuyển số thành chữ Tiếng Việt
     * @param number Số cần chuyển
     * @return Chuỗi chữ Tiếng Việt biểu diễn số đó
     */
    public static String convertNumberToVietnameseWords(int number) {
        if (number == 0) return "Không";

        long num = (long) Math.abs(number);
        String result = "";
        int i = 0;
        boolean firstGroup = true;

        while (num > 0) {
            int group = (int) (num % 1000);
            if (group != 0) {
                String groupText = convertThreeDigits(group, !firstGroup && (num / 1000 > 0));
                result = groupText + (donViNhom[i].isEmpty() ? "" : (" " + donViNhom[i])) + (result.isEmpty() ? "" : (" " + result));
            }
            num /= 1000;
            i++;
            firstGroup = false;
        }
        result = result.trim();
        if (result.isEmpty()) return "Không";
        result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        return result;
    }

    private static String convertThreeDigits(int number, boolean needLeadingZeroHundredAndLe) {
        int hundred = number / 100;
        int ten = (number % 100) / 10;
        int unit = number % 10;
        String result = "";

        // Xử lý hàng trăm
        if (hundred != 0) {
            result += chuSo[hundred] + " trăm ";
        } else if (needLeadingZeroHundredAndLe && (ten != 0 || unit != 0)) {
            result += "không trăm ";
        }

        // Xử lý hàng chục và đơn vị
        if (ten > 1) { // Hai mươi, Ba mươi,... Chín mươi
            result += chuSo[ten] + " mươi ";
            if (unit == 1) {
                result += "mốt"; // Hai mươi mốt
            } else if (unit == 4 && ten > 1) { // Bốn (hai mươi tư, ba mươi tư)
                result += "tư";
            } else if (unit == 5) {
                result += "lăm"; // Hai mươi lăm
            } else if (unit != 0) {
                result += chuSo[unit];
            }
        } else if (ten == 1) { // Mười, Mười một, Mười lăm
            result += "mười ";
            if (unit == 1) {
                result += "một";
            } else if (unit == 5) {
                result += "lăm"; // Khác với "năm" ở hàng đơn vị đứng một mình
            } else if (unit != 0) {
                result += chuSo[unit];
            }
        } else { // Hàng chục là 0
            if (unit != 0) {
                if (hundred != 0 || (needLeadingZeroHundredAndLe && result.contains("không trăm"))) {
                    result += "lẻ ";
                }
                result += chuSo[unit];
            }
        }
        return result.trim();
    }
}

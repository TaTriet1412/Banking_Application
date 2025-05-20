package com.example.bankingapplication.Object;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp; // <<<<<< IMPORT THÊM
import java.text.SimpleDateFormat;  // <<<<<< IMPORT NẾU MUỐN FORMAT THÀNH STRING
import java.util.Date;
import java.util.Locale;

public class BankOffice {
    private String UID;
    private String name;
    private String address;
    private LatLng latLng;
    private String phone;
    private Timestamp openHours;   // <<<<<< THAY ĐỔI KIỂU DỮ LIỆU
    private Timestamp closeHours;  // <<<<<< THAY ĐỔI KIỂU DỮ LIỆU

    public BankOffice(String UID, String name, String address, LatLng latLng, String phone, Timestamp openHours, Timestamp closeHours) {
        this.UID = UID;
        this.name = name;
        this.address = address;
        this.latLng = latLng;
        this.phone = phone;
        this.openHours = openHours;
        this.closeHours = closeHours;
    }

    // Getters
    public String getUID() { return UID; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LatLng getLatLng() { return latLng; }
    public String getPhone() { return phone; }
    public Timestamp getOpenHoursTimestamp() { return openHours; } // Getter cho Timestamp
    public Timestamp getCloseHoursTimestamp() { return closeHours; } // Getter cho Timestamp

    // Getter để lấy String đã định dạng (tùy chọn)
    public String getOpenHoursFormatted() {
        if (openHours == null) return "N/A";
        Date date = openHours.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public String getCloseHoursFormatted() {
        if (closeHours == null) return "N/A";
        Date date = closeHours.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public String toString() {
        return "BankOffice{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", latLng=" + latLng +
                ", open=" + getOpenHoursFormatted() + // Sử dụng hàm format
                ", close=" + getCloseHoursFormatted() + // Sử dụng hàm format
                '}';
    }
}
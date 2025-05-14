package com.example.bankingapplication.Object;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class User implements Serializable {
    private String UID;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String nationalId;
    private String role;
    private Boolean gender;
    private transient Timestamp dateOfBirth;
    private transient Timestamp kycDate;

    private Biometric biometricData;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }


    public User(String UID, String name, String email, String phone, String address, String nationalId,
                String role, Boolean gender, Timestamp dateOfBirth, Timestamp kycDate,
                Biometric biometricData) {
        this.UID = UID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.nationalId = nationalId;
        this.role = role;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.kycDate = kycDate;
        this.biometricData = biometricData;
    }

    public User(String UID, String name, String email, String phone, String address, String nationalId,
                String role, Boolean gender, Timestamp dateOfBirth, Timestamp kycDate) {
        this.UID = UID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.nationalId = nationalId;
        this.role = role;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.kycDate = kycDate;
    }

    public User(String email, String phone, String nationalId) {
        this.email = email;
        this.phone = phone;
        this.nationalId = nationalId;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Timestamp getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Timestamp dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }


    public Timestamp getKycDate() {
        return kycDate;
    }

    public void setKycDate(Timestamp kycDate) {
        this.kycDate = kycDate;
    }

    public Biometric getBiometricData() {
        return biometricData;
    }

    public String genderToString() {
        if (this.gender == null) {
            return "Khác"; // Or some other appropriate string for null
        } else if (this.gender) {
            return "Nam";
        } else {
            return "Nữ";
        }
    }


    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getGender() {
        return gender;
    }

    public void setGender(Boolean gender) {
        this.gender = gender;
    }

    public void setBiometricData(Biometric biometricData) {
        this.biometricData = biometricData;
    }
}


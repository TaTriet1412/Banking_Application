package com.example.bankingapplication.Utils;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;

public class GlobalVariables {
    private static GlobalVariables instance;
    private User currentUser;
    private Account currentAccount;

    // Private constructor để tránh khởi tạo từ bên ngoài
    private GlobalVariables() {}

    // Lấy instance của class (Singleton)
    public static synchronized GlobalVariables getInstance() {
        if (instance == null) {
            instance = new GlobalVariables();
        }
        return instance;
    }

    // Getter và setter cho currentAccount
    public User getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account currentAccount) {
        this.currentAccount = currentAccount;
    }
}


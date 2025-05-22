package com.example.bankingapplication.Adapter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
// import androidx.fragment.app.FragmentActivity; // Có thể giữ lại nếu bạn có case dùng với Activity
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.CustomerCheckingAccountFragment;
import com.example.bankingapplication.CustomerMortgageAccountFragment;
import com.example.bankingapplication.CustomerSavingAccountFragment;

public class CustomerAccountPagerAdapter extends FragmentStateAdapter {

    private Account accountData;

    // Constructor để dùng bên trong một Fragment cha (như CustomerDetailsFragment)
    public CustomerAccountPagerAdapter(@NonNull Fragment fragment, Account account) {
        super(fragment); // Gọi constructor của FragmentStateAdapter với Fragment
        this.accountData = account;
    }

    // (Tùy chọn) Giữ lại constructor này nếu bạn cũng muốn dùng adapter này trực tiếp từ một Activity
    // public CustomerAccountPagerAdapter(@NonNull FragmentActivity fragmentActivity, Account account) {
    //     super(fragmentActivity);
    //     this.accountData = account;
    // }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d("PagerAdapter", "Creating fragment for position: " + position);
        if (accountData == null) {
            Log.e("PagerAdapter", "accountData is NULL when creating fragment for position " + position);
            return new Fragment(); // Fallback
        }

        switch (position) {
            case 0: // Thẻ/Thanh toán
                Log.d("PagerAdapter", "Checking data for pos 0: " + (accountData.getChecking() != null ? "Exists" : "NULL"));
                return CustomerCheckingAccountFragment.newInstance(
                        accountData.getChecking(),
                        accountData.getAccountNumber(),
                        accountData.getUID()
                );
            case 1: // Tiết kiệm
                Log.d("PagerAdapter", "Saving data for pos 1: " + (accountData.getSaving() != null ? "Exists" : "NULL"));
                if (accountData.getSaving() != null) {
                    Log.d("PagerAdapter", "Saving Interest Rate for pos 1: " + accountData.getSaving().getInterestRate());
                }
                return CustomerSavingAccountFragment.newInstance(
                        accountData.getSaving(),
                        accountData.getAccountNumber(),
                        accountData.getUID() // parentAccountId
                );
            case 2: // Vay/Thế chấp
                Log.d("PagerAdapter", "Mortgage data for pos 2: " + (accountData.getMortgage() != null ? "Exists" : "NULL"));
                if (accountData.getMortgage() != null) {
                    Log.d("PagerAdapter", "Mortgage Loan Amount for pos 2: " + accountData.getMortgage().getLoanAmount());
                }
                return CustomerMortgageAccountFragment.newInstance(
                        accountData.getMortgage(),
                        accountData.getAccountNumber(),
                        accountData.getUID() // Thêm parentAccountId
                );
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public void setAccountData(Account account) {
        this.accountData = account;
        notifyDataSetChanged();
    }
}
package com.example.bankingapplication.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.bankingapplication.CheckingAccountFragment;
import com.example.bankingapplication.MortgageAccountFragment;
import com.example.bankingapplication.SavingAccountFragment;

// AccountFragmentAdapter.java
public class AccountFragmentAdapter extends FragmentStateAdapter {

    public AccountFragmentAdapter(FragmentActivity fragmentActivity, FragmentManager fm, Lifecycle lifecycle) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CheckingAccountFragment();
            case 1:
                return new SavingAccountFragment();
            case 2:
                return new MortgageAccountFragment();
            default:
                return new CheckingAccountFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Số lượng tab
    }
}
// BankOfficerMainActivity.java
package com.example.bankingapplication;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.bankingapplication.Adapter.UserAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class BankOfficerMainActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_officer_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation_officer);

        // Lấy màu từ resources
        int colorTextActive = ContextCompat.getColor(this, R.color.bottom_nav_text_active); // Ví dụ: #4A148C (Tím đậm)
        int colorTextInactive = ContextCompat.getColor(this, R.color.bottom_nav_text_inactive); // Ví dụ: @color/grey_text_dark

        // Tạo ColorStateList CHỈ CHO MÀU CHỮ
        int[][] textStates = new int[][] {
                new int[] { android.R.attr.state_checked}, // checked
                new int[] {-android.R.attr.state_checked}  // unchecked
        };
        int[] textColors = new int[] {
                colorTextActive,
                colorTextInactive
        };
        ColorStateList textColorStateList = new ColorStateList(textStates, textColors);
        bottomNavigationView.setItemTextColor(textColorStateList);

        // Đảm bảo icon không bị tint bởi code Java, vì đã có app:itemIconTint="@null" trong XML
        // bottomNavigationView.setItemIconTintList(null); // Có thể thêm dòng này để chắc chắn hơn nữa


        if (savedInstanceState == null) {
            loadFragment(new UsersListFragment(), false);
            bottomNavigationView.setSelectedItemId(R.id.page_manage_customers);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.page_manage_customers) {
                selectedFragment = new UsersListFragment();
            } else if (itemId == R.id.page_myProfile) {
                selectedFragment = new OfficerProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, false);
            }
            return true;
        });
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_container_officer, fragment);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        fragmentTransaction.commit();
    }

    public void navigateToCustomerDetails(String customerId) {
        CustomerDetailsFragment customerDetailsFragment = CustomerDetailsFragment.newInstance(customerId);
        loadFragment(customerDetailsFragment, true);
    }

    @Override
    public void onUserItemClick(String userId) {
        navigateToCustomerDetails(userId);
    }
}
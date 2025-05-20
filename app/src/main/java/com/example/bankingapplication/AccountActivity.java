package com.example.bankingapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bankingapplication.Adapter.AccountFragmentAdapter;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AccountActivity extends AppCompatActivity {
    androidx.appcompat.widget.Toolbar toolbar; // Changed from android.widget.Toolbar
    TabItem tab_checking, tab_saving, tab_mortgage;
    ViewPager2 viewPager;
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);

        initContentView();
    }

    private void initContentView() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Set up the ViewPager with the adapter
        AccountFragmentAdapter adapter = new AccountFragmentAdapter(this, getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        
        // Connect the TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Thẻ");
                            break;
                        case 1:
                            tab.setText("Tiết kiệm");
                            break;
                        case 2:
                            tab.setText("Vay");
                            break;
                    }
                }).attach();
        
        // Set default tab to checking account (position 0)
        viewPager.setCurrentItem(0, false);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
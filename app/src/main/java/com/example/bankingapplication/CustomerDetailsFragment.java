package com.example.bankingapplication;

import android.app.Activity; // Import Activity nếu bạn dùng ActivityResultLauncher sau này
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bankingapplication.Adapter.CustomerAccountPagerAdapter;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CustomerDetailsFragment extends Fragment {

    private static final String TAG = "CustomerDetailsFragment";
    private static final String ARG_CUSTOMER_ID = "customer_id";
    private String customerId;

    private ShapeableImageView imgCustomerAvatarDetailOfficer;
    private TextView tvCustomerDetailName, tvCustomerDetailEmail, tvCustomerDetailIdCard, tvCustomerDetailPhone;
    private LinearLayout llEditCustomerInfoOfficer;

    private TabLayout tabLayoutCustomerAccounts;
    private ViewPager2 viewPagerCustomerAccounts;
    private CustomerAccountPagerAdapter pagerAdapter;

    private boolean isInitialLoadDone = false;

    private User currentLoadedUser;
    // private Account currentDisplayingAccount; // Không cần thiết nếu PagerAdapter giữ Account

    // Sử dụng ActivityResultLauncher để làm mới dữ liệu sau khi EditCustomerInfoActivity hoàn thành
    private ActivityResultLauncher<Intent> editCustomerInfoLauncher;

    public CustomerDetailsFragment() {
        // Required empty public constructor
    }

    public static CustomerDetailsFragment newInstance(String customerId) {
        CustomerDetailsFragment fragment = new CustomerDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getString(ARG_CUSTOMER_ID);
            Log.d(TAG, "onCreate: Received customerId: " + customerId);
        } else {
            Log.e(TAG, "onCreate: No arguments received (customerId is null).");
            if (getActivity() != null) {
                Toast.makeText(getContext(), "Lỗi: Không có ID khách hàng.", Toast.LENGTH_LONG).show();
                getActivity().getSupportFragmentManager().popBackStack(); // Đóng fragment nếu không có ID
            }
            return; // Không tiếp tục nếu không có customerId
        }

        // Khởi tạo ActivityResultLauncher
        editCustomerInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Thông tin đã được cập nhật, load lại dữ liệu của khách hàng
                        Log.d(TAG, "EditCustomerInfoActivity returned OK, refreshing data...");
                        if (getContext() != null) Toast.makeText(getContext(), "Đang làm mới thông tin...", Toast.LENGTH_SHORT).show();
                        if (customerId != null && !customerId.isEmpty()) {
                            loadCustomerDetailsAndAccounts(customerId);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_details_for_officer, container, false);

        imgCustomerAvatarDetailOfficer = view.findViewById(R.id.img_customer_avatar_detail_officer);
        tvCustomerDetailName = view.findViewById(R.id.tv_customer_detail_name);
        tvCustomerDetailEmail = view.findViewById(R.id.tv_customer_detail_email);
        tvCustomerDetailIdCard = view.findViewById(R.id.tv_customer_detail_id_card);
        tvCustomerDetailPhone = view.findViewById(R.id.tv_customer_detail_phone);
        llEditCustomerInfoOfficer = view.findViewById(R.id.ll_edit_customer_info_officer);

        tabLayoutCustomerAccounts = view.findViewById(R.id.tab_layout_customer_accounts);
        viewPagerCustomerAccounts = view.findViewById(R.id.view_pager_customer_accounts);

        // Khởi tạo PagerAdapter một lần ở đây với dữ liệu ban đầu là null
        // Các Fragment con trong adapter sẽ xử lý trường hợp dữ liệu null
        pagerAdapter = new CustomerAccountPagerAdapter(this, null);
        viewPagerCustomerAccounts.setAdapter(pagerAdapter);

        // Kết nối TabLayout với ViewPager2
        new TabLayoutMediator(tabLayoutCustomerAccounts, viewPagerCustomerAccounts,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText("Thẻ"); break;
                        case 1: tab.setText("Tiết kiệm"); break;
                        case 2: tab.setText("Vay"); break;
                    }
                }).attach();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayoutCustomerAccounts.setVisibility(View.GONE);
        viewPagerCustomerAccounts.setVisibility(View.GONE);

        if (customerId != null && !customerId.isEmpty()) {
            if (!isInitialLoadDone) { // Chỉ load lần đầu
                Log.d(TAG, "onViewCreated: Triggering initial data load for customerId: " + customerId);
                loadCustomerDetailsAndAccounts(customerId);
                isInitialLoadDone = true;
            }
        }
        // else: Trường hợp customerId null đã được xử lý trong onCreate

        if (llEditCustomerInfoOfficer != null) {
            llEditCustomerInfoOfficer.setOnClickListener(v -> openEditCustomerInfoActivity());
        }
    }

    private void openEditCustomerInfoActivity() {
        if (currentLoadedUser != null && customerId != null && getActivity() != null) {
            Intent intent = new Intent(getActivity(), EditCustomerInfoActivity.class);
            intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_ID, customerId);
            intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_NAME, currentLoadedUser.getName());
            intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_EMAIL, currentLoadedUser.getEmail());
            intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_PHONE, currentLoadedUser.getPhone());
            intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_ADDRESS, currentLoadedUser.getAddress());
            intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_NATIONAL_ID, currentLoadedUser.getNationalId());
            if (currentLoadedUser.getDateOfBirth() != null) {
                intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_DOB_SECONDS, currentLoadedUser.getDateOfBirth().getSeconds());
            }
            if (currentLoadedUser.getGender() != null) { // Truyền kiểu Boolean
                intent.putExtra(EditCustomerInfoActivity.EXTRA_CUSTOMER_GENDER, currentLoadedUser.getGender());
            }
            editCustomerInfoLauncher.launch(intent); // Sử dụng launcher
        } else {
            if (getContext() != null) Toast.makeText(getContext(), "Không thể sửa, dữ liệu khách hàng chưa được tải.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCustomerDetailsAndAccounts(String uId) {
        Log.d(TAG, "loadCustomerDetailsAndAccounts - Fetching user data for UID: " + uId);
        Firestore.getUser(uId, user -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "loadCustomerDetailsAndAccounts - Fragment not attached or context null after Firestore.getUser.");
                return;
            }
            if (user != null) {
                this.currentLoadedUser = user;
                Log.d(TAG, "loadCustomerDetailsAndAccounts - User loaded: " + user.getName());
                displayUserInfo(user);
                loadAccountsForViewPager(uId); // Load tài khoản sau khi có thông tin user
            } else {
                Toast.makeText(getContext(), "Không thể tải thông tin khách hàng.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "loadCustomerDetailsAndAccounts - User data is null for UID: " + uId);
            }
        });
    }

    private void displayUserInfo(User user) {
        if (user == null) return;
        tvCustomerDetailName.setText(user.getName() != null ? user.getName() : "N/A");
        tvCustomerDetailEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        tvCustomerDetailIdCard.setText(user.getNationalId() != null ? user.getNationalId() : "N/A");
        tvCustomerDetailPhone.setText(user.getPhone() != null ? user.getPhone() : "N/A");
        if (imgCustomerAvatarDetailOfficer != null) {
            // TODO: Load avatar thực tế nếu có URL
            imgCustomerAvatarDetailOfficer.setImageResource(R.drawable.ic_avatar);
        }
    }

    private void loadAccountsForViewPager(String userIdForAccounts) {
        Log.d(TAG, "loadAccountsForViewPager - Fetching accounts for user ID: " + userIdForAccounts);
        Firestore.getAccountsByUserId(userIdForAccounts, (accountList, e) -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "loadAccountsForViewPager - Fragment not attached or context null after Firestore.getAccountsByUserId.");
                return;
            }

            if (e == null) {
                if (accountList != null && !accountList.isEmpty()) {
                    Account fetchedAccount = accountList.get(0);
                    if (fetchedAccount != null) {
                        Log.d(TAG, "loadAccountsForViewPager - Account found: " + fetchedAccount.getAccountNumber());
                        Log.d(TAG, "  Saving is null? " + (fetchedAccount.getSaving() == null));
                        if (fetchedAccount.getSaving() != null) Log.d(TAG, "  Saving Rate: " + fetchedAccount.getSaving().getInterestRate());
                        Log.d(TAG, "  Mortgage is null? " + (fetchedAccount.getMortgage() == null));
                        if (fetchedAccount.getMortgage() != null) Log.d(TAG, "  Mortgage Loan: " + fetchedAccount.getMortgage().getLoanAmount());

                        pagerAdapter.setAccountData(fetchedAccount); // Cập nhật dữ liệu cho PagerAdapter

                        tabLayoutCustomerAccounts.setVisibility(View.VISIBLE);
                        viewPagerCustomerAccounts.setVisibility(View.VISIBLE);
                        Log.d(TAG, "loadAccountsForViewPager - ViewPager data updated and views set to visible.");
                    } else {
                        showNoAccountDataUI();
                        Log.e(TAG, "loadAccountsForViewPager - First account in list is null for user: " + userIdForAccounts);
                    }
                } else {
                    showNoAccountDataUI();
                    Log.d(TAG, "loadAccountsForViewPager - No accounts found for user: " + userIdForAccounts);
                    if (getContext() !=null) Toast.makeText(getContext(), "Khách hàng này chưa có tài khoản nào.", Toast.LENGTH_SHORT).show();
                }
            } else {
                showNoAccountDataUI();
                String errorMessage = "Lỗi tải thông tin tài khoản của khách hàng.";
                if (e.getMessage() != null) errorMessage += ": " + e.getMessage();
                Log.e(TAG, "loadAccountsForViewPager - Error: " + errorMessage, e);
                if (getContext() !=null) Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showNoAccountDataUI() {
        if (tabLayoutCustomerAccounts != null) tabLayoutCustomerAccounts.setVisibility(View.GONE);
        if (viewPagerCustomerAccounts != null) viewPagerCustomerAccounts.setVisibility(View.GONE);
        Log.d(TAG, "showNoAccountDataUI called.");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load/refresh dữ liệu khi Fragment resume, nhưng chỉ khi customerId hợp lệ
        // Điều này sẽ xử lý cả lần load đầu tiên (nếu onViewCreated chưa kịp) và các lần refresh sau đó.
        if (customerId != null && !customerId.isEmpty()) {
            Log.d(TAG, "onResume: Triggering data load/refresh for customerId: " + customerId);
            loadCustomerDetailsAndAccounts(customerId);
        }
    }
}
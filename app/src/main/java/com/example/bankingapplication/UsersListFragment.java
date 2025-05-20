package com.example.bankingapplication; // Hoặc package của fragment officer

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView; // Import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Adapter.UserAdapter;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class UsersListFragment extends Fragment {

    private static final String TAG = "UsersListFragment";

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> customerList;
    private FloatingActionButton fabAddCustomer;
    private ProgressBar progressBar;
    private TextView tvNoCustomers; // TextView để hiển thị khi không có khách hàng

    private UserAdapter.OnUserClickListener userClickListener;

    public UsersListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof UserAdapter.OnUserClickListener) {
            userClickListener = (UserAdapter.OnUserClickListener) context;
            Log.d("UsersListFragment", "Listener attached from Activity.");
        } else {
            userClickListener = null; // Hoặc throw new RuntimeException(...)
            Log.e("UsersListFragment", context.toString() + " must implement UserAdapter.OnUserClickListener");
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users_list, container, false);

        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        fabAddCustomer = view.findViewById(R.id.fab_add_customer);
        progressBar = view.findViewById(R.id.progressBar_users_list); // Đảm bảo ID này có trong layout
        tvNoCustomers = view.findViewById(R.id.tv_no_customers);     // Đảm bảo ID này có trong layout

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        // Dữ liệu sẽ được load trong onResume để đảm bảo cập nhật sau khi thêm mới
        // loadCustomerData(); // Có thể bỏ nếu onResume đã xử lý

        fabAddCustomer.setOnClickListener(v -> {
            if (getActivity() == null) return;
            Intent intent = new Intent(getActivity(), SignUpActivity.class);
            // intent.putExtra("CREATED_BY_OFFICER", true);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        customerList = new ArrayList<>();
        // Truyền userClickListener (đã được gán từ Activity trong onAttach)
        userAdapter = new UserAdapter(getContext(), customerList, userClickListener);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    private void loadCustomerData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvNoCustomers != null) tvNoCustomers.setVisibility(View.GONE);
        recyclerViewUsers.setVisibility(View.GONE); // Ẩn RecyclerView trong khi tải

        Firestore.getAllCustomers(new Firestore.FirestoreGetCustomersCallback() {
            @Override
            public void onCallback(List<User> fetchedList, Exception e) {
                if (!isAdded() || getContext() == null) { // Kiểm tra Fragment còn attached
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    return;
                }
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (e == null) {
                    if (fetchedList != null && !fetchedList.isEmpty()) {
                        userAdapter.updateUserList(fetchedList); // Dùng hàm của adapter để cập nhật
                        recyclerViewUsers.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Customers loaded: " + fetchedList.size());
                    } else {
                        userAdapter.updateUserList(new ArrayList<>()); // Xóa list cũ nếu không có KH mới
                        if (tvNoCustomers != null) tvNoCustomers.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No customers found.");
                    }
                } else {
                    userAdapter.updateUserList(new ArrayList<>());
                    if (tvNoCustomers != null) {
                        tvNoCustomers.setText("Lỗi tải dữ liệu. Vui lòng thử lại.");
                        tvNoCustomers.setVisibility(View.VISIBLE);
                    }
                    Log.e(TAG, "Error loading customers: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Lỗi tải danh sách khách hàng.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại dữ liệu khi fragment được hiển thị lại (ví dụ sau khi thêm user từ SignUpActivity)
        // Điều này đảm bảo danh sách được cập nhật.
        Log.d(TAG, "onResume: Loading customer data");
        loadCustomerData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        userClickListener = null; // Giải phóng listener để tránh memory leak
    }
}
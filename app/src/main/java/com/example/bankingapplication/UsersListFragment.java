package com.example.bankingapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView; // Import SearchView
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bankingapplication.Adapter.UserAdapter;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // Dùng cho Java 8+ stream API

public class UsersListFragment extends Fragment {

    private static final String TAG = "UsersListFragment";

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> allCustomersList = new ArrayList<>(); // Danh sách gốc chứa tất cả khách hàng
    private List<User> filteredCustomersList = new ArrayList<>(); // Danh sách đã lọc để hiển thị
    private FloatingActionButton fabAddCustomer;
    private ProgressBar progressBar;
    private TextView tvNoCustomers;
    private SearchView searchViewCustomers; // Thêm biến cho SearchView

    private UserAdapter.OnUserClickListener userClickListener;
    private static final int REQUEST_CREATE_CUSTOMER = 1001;

    public UsersListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof UserAdapter.OnUserClickListener) {
            userClickListener = (UserAdapter.OnUserClickListener) context;
        } else {
            Log.e(TAG, context.toString() + " must implement UserAdapter.OnUserClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users_list, container, false);

        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        fabAddCustomer = view.findViewById(R.id.fab_add_customer);
        progressBar = view.findViewById(R.id.progressBar_users_list);
        tvNoCustomers = view.findViewById(R.id.tv_no_customers);
        searchViewCustomers = view.findViewById(R.id.search_view_customers); // Ánh xạ SearchView

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearchView(); // Gọi hàm cài đặt SearchView
        // loadCustomerData(); // Dữ liệu sẽ được load trong onResume

        fabAddCustomer.setOnClickListener(v -> {
            if (getActivity() == null) return;
            Intent intent = new Intent(getActivity(), SignUpActivity.class);
            intent.putExtra("CREATED_BY_OFFICER", true);
            startActivityForResult(intent, REQUEST_CREATE_CUSTOMER);
        });
    }

    private void setupRecyclerView() {
        // filteredCustomersList sẽ được dùng cho adapter
        userAdapter = new UserAdapter(getContext(), filteredCustomersList, userClickListener);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    private void setupSearchView() {
        searchViewCustomers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Người dùng nhấn nút tìm kiếm trên bàn phím (thường không cần xử lý riêng)
                filterCustomers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Lọc danh sách mỗi khi text thay đổi
                filterCustomers(newText);
                return true;
            }
        });
    }

    private void filterCustomers(String query) {
        filteredCustomersList.clear();
        if (query.isEmpty()) {
            filteredCustomersList.addAll(allCustomersList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (User user : allCustomersList) {
                // Tìm kiếm theo tên hoặc email (không phân biệt hoa thường)
                if ((user.getName() != null && user.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseQuery))) {
                    filteredCustomersList.add(user);
                }
            }
        }

        if (filteredCustomersList.isEmpty() && !allCustomersList.isEmpty()) { // Nếu có KH gốc nhưng không có kết quả tìm kiếm
            tvNoCustomers.setText("Không tìm thấy khách hàng phù hợp.");
            tvNoCustomers.setVisibility(View.VISIBLE);
            recyclerViewUsers.setVisibility(View.GONE);
        } else if (filteredCustomersList.isEmpty() && allCustomersList.isEmpty()) { // Nếu không có KH nào cả
            tvNoCustomers.setText("Không có khách hàng nào.");
            tvNoCustomers.setVisibility(View.VISIBLE);
            recyclerViewUsers.setVisibility(View.GONE);
        }
        else {
            tvNoCustomers.setVisibility(View.GONE);
            recyclerViewUsers.setVisibility(View.VISIBLE);
        }
        userAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView với danh sách đã lọc
    }


    private void loadCustomerData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvNoCustomers != null) tvNoCustomers.setVisibility(View.GONE);
        recyclerViewUsers.setVisibility(View.GONE);

        Firestore.getAllCustomers((fetchedList, e) -> {
            if (!isAdded() || getContext() == null) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                return;
            }
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            allCustomersList.clear(); // Xóa danh sách gốc trước khi thêm mới
            if (e == null) {
                if (fetchedList != null && !fetchedList.isEmpty()) {
                    allCustomersList.addAll(fetchedList);
                    Log.d(TAG, "Customers loaded: " + allCustomersList.size());
                } else {
                    Log.d(TAG, "No customers found.");
                }
            } else {
                Log.e(TAG, "Error loading customers: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Lỗi tải danh sách khách hàng.", Toast.LENGTH_LONG).show();
            }
            // Sau khi load xong, áp dụng filter hiện tại (có thể là query rỗng)
            filterCustomers(searchViewCustomers.getQuery().toString());
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Loading customer data");
        loadCustomerData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_CUSTOMER && resultCode == Activity.RESULT_OK) {
            Toast.makeText(getContext(), "Tạo khách hàng mới thành công", Toast.LENGTH_SHORT).show();
            loadCustomerData(); // Load lại để cập nhật danh sách
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        userClickListener = null;
    }
}
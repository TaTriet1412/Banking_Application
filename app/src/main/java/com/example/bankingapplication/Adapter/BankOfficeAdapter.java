package com.example.bankingapplication.Adapter;

import android.content.Context;
import android.location.Location;
import android.util.Log; // Thêm Log để debug
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bankingapplication.Object.BankOffice;
import com.example.bankingapplication.R;
import java.util.ArrayList; // Thêm import này
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BankOfficeAdapter extends RecyclerView.Adapter<BankOfficeAdapter.BankOfficeViewHolder> {

    private List<BankOffice> bankOfficeList;
    private Context context;
    private Location currentUserLocation;
    private OnOfficeClickListener listener;
    private static final String ADAPTER_TAG = "BankOfficeAdapter"; // Thêm tag cho Log

    public interface OnOfficeClickListener {
        void onOfficeClick(BankOffice office);
    }

    public BankOfficeAdapter(Context context, List<BankOffice> initialBankOfficeList, Location currentUserLocation, OnOfficeClickListener listener) {
        this.context = context;
        // Khởi tạo bankOfficeList bằng một ArrayList mới để tránh tham chiếu trực tiếp
        this.bankOfficeList = new ArrayList<>(initialBankOfficeList);
        this.currentUserLocation = currentUserLocation;
        this.listener = listener;
        Log.d(ADAPTER_TAG, "Adapter created. Initial location: " + (currentUserLocation == null ? "null" : currentUserLocation.toString()));
        sortOfficesByDistance(); // Sắp xếp ngay khi khởi tạo nếu có vị trí
    }

    public void updateUserLocationAndSort(Location newLocation) {
        Log.d(ADAPTER_TAG, "Updating user location to: " + (newLocation == null ? "null" : newLocation.toString()));
        this.currentUserLocation = newLocation;
        sortOfficesByDistance();
        notifyDataSetChanged();
    }

    public void updateBankOfficeList(List<BankOffice> newOfficeList) {
        Log.d(ADAPTER_TAG, "Updating bank office list. New list size: " + newOfficeList.size());
        this.bankOfficeList.clear();
        this.bankOfficeList.addAll(newOfficeList);
        sortOfficesByDistance(); // Sắp xếp lại khi danh sách ngân hàng thay đổi
        notifyDataSetChanged();
    }

    private void sortOfficesByDistance() {
        if (currentUserLocation == null) {
            Log.w(ADAPTER_TAG, "Cannot sort by distance, currentUserLocation is null.");
            // Không sắp xếp nếu không có vị trí người dùng, hoặc có thể sắp xếp theo tên mặc định
            // Collections.sort(bankOfficeList, Comparator.comparing(BankOffice::getName)); // Ví dụ sắp xếp theo tên
            return;
        }
        if (bankOfficeList == null || bankOfficeList.isEmpty()) {
            Log.w(ADAPTER_TAG, "Bank office list is null or empty, cannot sort.");
            return;
        }

        Collections.sort(bankOfficeList, new Comparator<BankOffice>() {
            @Override
            public int compare(BankOffice o1, BankOffice o2) {
                if (o1.getLatLng() == null && o2.getLatLng() == null) return 0;
                if (o1.getLatLng() == null) return 1; // o1 không có tọa độ, đẩy xuống cuối
                if (o2.getLatLng() == null) return -1; // o2 không có tọa độ, đẩy xuống cuối

                float[] results1 = new float[1];
                Location.distanceBetween(currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                        o1.getLatLng().latitude, o1.getLatLng().longitude, results1);

                float[] results2 = new float[1];
                Location.distanceBetween(currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                        o2.getLatLng().latitude, o2.getLatLng().longitude, results2);

                return Float.compare(results1[0], results2[0]);
            }
        });
        Log.d(ADAPTER_TAG, "List sorted by distance.");
    }

    @NonNull
    @Override
    public BankOfficeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bank_office, parent, false);
        return new BankOfficeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BankOfficeViewHolder holder, int position) {
        BankOffice office = bankOfficeList.get(position);
        holder.tvOfficeName.setText(office.getName() != null ? office.getName() : "N/A");
        holder.tvOfficeAddress.setText(office.getAddress() != null ? office.getAddress() : "N/A");
        holder.tvOfficePhone.setText("SĐT: " + (office.getPhone() != null ? office.getPhone() : "N/A"));

        // Hiển thị giờ đóng cửa
        holder.tvOfficeHours.setText("Giờ: " + office.getOpenHoursFormatted() + " - " + office.getCloseHoursFormatted());

        if (currentUserLocation != null && office.getLatLng() != null) {
            float[] results = new float[1];
            Location.distanceBetween(currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                    office.getLatLng().latitude, office.getLatLng().longitude, results);
            float distanceInKm = results[0] / 1000;
            holder.tvOfficeDistance.setText(String.format(Locale.US, "%.1f km", distanceInKm));
        } else {
            holder.tvOfficeDistance.setText("N/A");
            // Log.w(ADAPTER_TAG, "Cannot calculate distance for " + office.getName() + ". UserLocation: " + (currentUserLocation == null) + ", OfficeLatLng: " + (office.getLatLng() == null));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOfficeClick(office);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bankOfficeList == null ? 0 : bankOfficeList.size();
    }

    static class BankOfficeViewHolder extends RecyclerView.ViewHolder {
        TextView tvOfficeName, tvOfficeAddress, tvOfficePhone, tvOfficeHours, tvOfficeDistance;
        public BankOfficeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOfficeName = itemView.findViewById(R.id.tv_office_name);
            tvOfficeAddress = itemView.findViewById(R.id.tv_office_address);
            tvOfficePhone = itemView.findViewById(R.id.tv_office_phone);
            tvOfficeHours = itemView.findViewById(R.id.tv_office_hours); // Đã có trong layout item_bank_office.xml
            tvOfficeDistance = itemView.findViewById(R.id.tv_office_distance);
        }
    }
}
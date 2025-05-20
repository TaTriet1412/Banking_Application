// com.example.bankingapplication.Adapter.UserAdapter.java
package com.example.bankingapplication.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.R;
// import com.bumptech.glide.Glide; // Nếu dùng Glide

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private OnUserClickListener userClickListener;

    // Interface để Activity/Fragment implement
    public interface OnUserClickListener {
        void onUserItemClick(String userId);
        // void onEditUserClick(String userId); // (Tùy chọn)
        // void onDeleteUserClick(String userId, String userName); // (Tùy chọn)
    }

    public UserAdapter(Context context, List<User> userList, OnUserClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.userClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.textUserName.setText(user.getName() != null ? user.getName() : "N/A");
        holder.textUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");

        // Load avatar
        // if (user.getBiometricData() != null && user.getBiometricData().getFaceUrl() != null
        //        && !user.getBiometricData().getFaceUrl().isEmpty()) {
        //     Glide.with(context)
        //          .load(user.getBiometricData().getFaceUrl())
        //          .placeholder(R.drawable.ic_user_placeholder_grey) // Placeholder của bạn
        //          .error(R.drawable.ic_user_placeholder_grey)       // Ảnh lỗi
        //          .circleCrop()
        //          .into(holder.imageUserAvatar);
        // } else {
        //     holder.imageUserAvatar.setImageResource(R.drawable.ic_user_placeholder_grey);
        // }
        holder.imageUserAvatar.setImageResource(R.drawable.ic_user_placeholder_white); // Tạm thời

        holder.itemView.setOnClickListener(v -> {
            if (userClickListener != null && user.getUID() != null) {
                userClickListener.onUserItemClick(user.getUID());
            } else {
                Log.e("UserAdapter", "ClickListener or User UID is null.");
                // Có thể hiển thị Toast thông báo lỗi cho dev
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateUserList(List<User> newUserList) {
        this.userList.clear();
        if (newUserList != null) {
            this.userList.addAll(newUserList);
        }
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageUserAvatar;
        TextView textUserName, textUserEmail;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageUserAvatar = itemView.findViewById(R.id.imageUserAvatar);
            textUserName = itemView.findViewById(R.id.textUserName);
            textUserEmail = itemView.findViewById(R.id.textUserEmail);
        }
    }
}
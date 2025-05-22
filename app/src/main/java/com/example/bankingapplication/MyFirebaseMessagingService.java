package com.example.bankingapplication; // Hoặc package của bạn

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.bankingapplication.Utils.AppNotificationChannels; // Import lớp channel của bạn
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String transactionId = null; // Ví dụ: nếu backend gửi ID giao dịch

        // Kiểm tra xem tin nhắn có chứa payload dữ liệu không.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            transactionId = remoteMessage.getData().get("transactionId"); // Lấy ID giao dịch
        }
        // Hoặc nếu backend gửi notification payload
        else if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            // Notification payload không có custom data trực tiếp, bạn phải gửi data payload
        }

        if (title == null) title = getString(R.string.app_name); // Tiêu đề mặc định
        if (body == null) body = "Bạn có thông báo mới."; // Nội dung mặc định

        // Chỉ hiển thị notification nếu người dùng hiện tại là khách hàng
        // và notification này có vẻ dành cho khách hàng (ví dụ, có transactionId)
        // Điều này giúp tránh việc nhân viên cũng nhận được notification của khách hàng
        // nếu họ đăng nhập vào cùng một thiết bị (trường hợp hiếm nhưng có thể)
        // Tuy nhiên, cách tốt nhất là backend chỉ gửi đến token của khách hàng.

        sendNotification(title, body, transactionId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // TODO: Gửi token này lên server của bạn hoặc cập nhật vào Firestore
        // Ví dụ:
        // User currentUser = GlobalVariables.getInstance().getCurrentUser();
        // if (currentUser != null && VariablesUtils.CUSTOMER_ROLE.equals(currentUser.getRole())) {
        //     saveTokenToFirestore(currentUser.getUID(), token); // Gọi hàm đã tạo ở SignInActivity
        // }
    }

    private void sendNotification(String messageTitle, String messageBody, @Nullable String transactionId) {
        Intent intent;
        if (transactionId != null && !transactionId.isEmpty()) {
            // Nếu có transactionId, tạo intent để mở TransactionDetailActivity
            intent = new Intent(this, TransactionDetailActivity.class);
            intent.putExtra("TRANSACTION_ID", transactionId);
        } else {
            // Mặc định, mở CustomerMainActivity
            intent = new Intent(this, CustomerMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, AppNotificationChannels.CHANNEL_CUSTOMER_TRANSACTION_ID) // Sử dụng channel ID đã tạo
                        .setSmallIcon(R.drawable.app_main_icon) // Tạo icon này (ví dụ: logo app thu nhỏ)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Notification channel đã được tạo trong AppNotificationChannels.java
        // Không cần tạo lại ở đây nếu đã làm đúng ở Bước 1.

        notificationManager.notify((int) System.currentTimeMillis() /* ID duy nhất cho mỗi notification */, notificationBuilder.build());
    }

    // Bạn có thể copy hàm saveTokenToFirestore từ SignInActivity vào đây nếu muốn xử lý onNewToken
    // private void saveTokenToFirestore(String userId, String token) { ... }
}
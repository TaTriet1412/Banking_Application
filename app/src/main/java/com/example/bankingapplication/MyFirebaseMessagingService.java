// MyFirebaseMessagingService.java
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
import androidx.annotation.Nullable; // Import cho Nullable
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
        String transactionId = null; // Để mở chi tiết giao dịch

        // Kiểm tra xem tin nhắn có chứa payload dữ liệu không.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            transactionId = remoteMessage.getData().get("transactionId"); // Lấy ID giao dịch nếu có
            // Bạn có thể lấy thêm các custom data khác nếu Cloud Function gửi
        }
        // Hoặc nếu backend gửi notification payload (ít linh hoạt hơn cho custom data)
        else if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (title == null) title = getString(R.string.app_name); // Tiêu đề mặc định
        if (body == null) body = "Bạn có thông báo mới."; // Nội dung mặc định

        sendNotification(title, body, transactionId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // TODO: Gửi token này lên server của bạn hoặc cập nhật vào Firestore
        // Ví dụ: nếu người dùng đang đăng nhập và là customer, cập nhật token
        // User currentUser = GlobalVariables.getInstance().getCurrentUser();
        // if (currentUser != null && VariablesUtils.CUSTOMER_ROLE.equals(currentUser.getRole())) {
        //     saveTokenToFirestore(currentUser.getUID(), token); // Bạn cần hàm này, có thể copy từ SignInActivity
        // }
    }

    private void sendNotification(String messageTitle, String messageBody, @Nullable String transactionId) {
        Intent intent;
        if (transactionId != null && !transactionId.isEmpty()) {
            // Nếu có transactionId, tạo intent để mở TransactionDetailActivity
            intent = new Intent(this, TransactionDetailActivity.class);
            intent.putExtra("TRANSACTION_ID", transactionId); // Đảm bảo key này khớp với TransactionDetailActivity
        } else {
            // Mặc định, mở CustomerMainActivity
            intent = new Intent(this, CustomerMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Để khi mở activity, nó sẽ là activity trên cùng

        // Sử dụng FLAG_IMMUTABLE hoặc FLAG_MUTABLE tùy theo yêu cầu của Android SDK
        int pendingIntentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingIntentFlags = PendingIntent.FLAG_ONE_SHOT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, pendingIntentFlags);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, AppNotificationChannels.CHANNEL_CUSTOMER_TRANSACTION_ID) // Sử dụng channel ID đã tạo
                        .setSmallIcon(R.drawable.app_main_icon) // Tạo icon này (ví dụ: logo app thu nhỏ)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true) // Tự động hủy notification khi user click
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // Để hiện heads-up notification
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Hiển thị trên màn hình khóa

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // ID của notification, đảm bảo là duy nhất để các notification không ghi đè nhau nếu cần
        notificationManager.notify((int) System.currentTimeMillis() /* ID duy nhất */, notificationBuilder.build());
    }

    // (Tùy chọn) Hàm saveTokenToFirestore nếu bạn muốn xử lý onNewToken ở đây
    // private void saveTokenToFirestore(String userId, String token) { ... }
}
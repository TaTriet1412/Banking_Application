package com.example.bankingapplication.Utils; // Hoặc package của bạn

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color; // Nếu bạn muốn tùy chỉnh màu
import android.os.Build;

public class AppNotificationChannels extends Application {

    // Channel cho giao dịch của khách hàng (do nhân viên thực hiện)
    public static final String CHANNEL_CUSTOMER_TRANSACTION_ID = "customer_transaction_updates_channel";
    public static final String CHANNEL_CUSTOMER_TRANSACTION_NAME = "Cập nhật Giao dịch Khách hàng";
    public static final String CHANNEL_CUSTOMER_TRANSACTION_DESCRIPTION = "Thông báo về các giao dịch nạp/rút tiền trên tài khoản của bạn";

    // Bạn có thể thêm các channel khác ở đây nếu cần
    // public static final String CHANNEL_GENERAL_ID = "general_notifications_channel";
    // public static final String CHANNEL_GENERAL_NAME = "Thông báo chung";

    @Override
    public void onCreate() {
        super.onCreate();
        createCustomerTransactionNotificationChannel();
        // createGeneralNotificationChannel(); // Nếu có
    }

    private void createCustomerTransactionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel customerChannel = new NotificationChannel(
                    CHANNEL_CUSTOMER_TRANSACTION_ID,
                    CHANNEL_CUSTOMER_TRANSACTION_NAME,
                    NotificationManager.IMPORTANCE_HIGH // Quan trọng, để hiện heads-up
            );
            customerChannel.setDescription(CHANNEL_CUSTOMER_TRANSACTION_DESCRIPTION);
            customerChannel.enableLights(true);
            customerChannel.setLightColor(Color.BLUE); // Ví dụ màu đèn
            customerChannel.enableVibration(true);
            // customerChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(customerChannel);
            }
        }
    }

    // private void createGeneralNotificationChannel() { ... } // Nếu cần
}
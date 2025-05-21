package com.example.bankingapplication;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.Bill;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Utils.BillUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.TransactionUtils;
import com.example.bankingapplication.Utils.VnPayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PaymentReturnActivity extends AppCompatActivity {

    private static final String TAG = "PaymentReturnActivity";
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 101; // For runtime permissions

    private ImageView ivHomeIcon, ivSuccessCheck;
    private TextView tvSuccessMessage, tvAmountDisplay, tvDateTimeDisplay;
    private LinearLayout llTransactionDetailsContainer;
    private LinearLayout llShare, llSaveImage;
    private AppCompatButton btnBackHome;
    private ScrollView scrollView;
    private Uri savedImageUri = null;
    private TransactionData currentTransaction;
    private Bill currentBill;
    private String vnpayTransactionNo;
    private String displayAmount;
    private String displayDateTime;
    private FrameLayout progressOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_return);

        Log.d(TAG, "PaymentReturnActivity onCreate called");

        // Initialize views
        ivHomeIcon = findViewById(R.id.ivHomeIcon);
        ivSuccessCheck = findViewById(R.id.ivSuccessCheck);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        tvAmountDisplay = findViewById(R.id.tvAmount);
        tvDateTimeDisplay = findViewById(R.id.tvDateTime);
        llTransactionDetailsContainer = findViewById(R.id.llTransactionDetailsContainer);
        llShare = findViewById(R.id.llShare);
        llSaveImage = findViewById(R.id.llSaveImage);
        btnBackHome = findViewById(R.id.btnBackHome);
        scrollView = findViewById(R.id.scrollView);
        progressOverlay = findViewById(R.id.progress_overlay);

        ivHomeIcon.setOnClickListener(v -> navigateToHome());
        btnBackHome.setOnClickListener(v -> navigateToHome());

        llShare.setOnClickListener(v -> shareTransactionDetails());
        llSaveImage.setOnClickListener(v -> saveTransactionAsImageWithPermission());

        // Show progress overlay initially while processing
        progressOverlay.setVisibility(View.VISIBLE);

        Intent receivedIntent = getIntent();
        Uri data = receivedIntent.getData();

        Log.d(TAG, "Intent: " + receivedIntent);
        Log.d(TAG, "Intent action: " + (receivedIntent.getAction() != null ? receivedIntent.getAction() : "null"));
        Log.d(TAG, "Intent data: " + (data != null ? data.toString() : "null"));

        if (data != null) {
            processVnPayResponse(data);
        } else {
            Log.e(TAG, "No data received in intent.");
            progressOverlay.setVisibility(View.GONE);
            displayErrorState("Không nhận được dữ liệu trả về.");
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(PaymentReturnActivity.this, CustomerMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void processVnPayResponse(Uri data) {
        // Already showing progress overlay from onCreate
        Log.d(TAG, "URI received: " + data.toString());

        Map<String, String> params = new HashMap<>();
        Set<String> queryParameterNames = data.getQueryParameterNames();
        for (String key : queryParameterNames) {
            String value = data.getQueryParameter(key);
            params.put(key, value);
            Log.d(TAG, "Param: " + key + " = " + value);
        }

        String responseCode = params.get("vnp_ResponseCode");
        vnpayTransactionNo = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");
        String txnRef = params.get("vnp_TxnRef");

        if (amountStr != null && !amountStr.isEmpty()) {
            try {
                double amountValue = Double.parseDouble(amountStr) / 100;
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                displayAmount = currencyFormatter.format(amountValue);
                tvAmountDisplay.setText(displayAmount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing amount: " + amountStr, e);
                tvAmountDisplay.setText("N/A");
            }
        } else {
            tvAmountDisplay.setText("N/A");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, E dd/MM/yyyy", new Locale("vi", "VN"));
        displayDateTime = sdf.format(new Date());
        tvDateTimeDisplay.setText(displayDateTime);

        boolean isValidHash = verifySecureHash(params, VnPayUtils.vnp_HashSecret);
        if (!isValidHash) {
            Log.w(TAG, "Hash verification failed - Consider additional security measures");
        }

        if (txnRef == null || txnRef.isEmpty()) {
            displayErrorState("Lỗi: Không tìm thấy mã đơn hàng.");
            return;
        }

        String[] ids = parseTransactionReference(txnRef);
        if (ids == null) {
            displayErrorState("Lỗi: Mã đơn hàng không hợp lệ.");
            return;
        }

        String transactionId = ids[0];
        String billId = ids[1];

        Log.d(TAG, "Extracted Transaction ID: " + transactionId);
        Log.d(TAG, "Extracted Bill ID: " + billId);

        if ("00".equals(responseCode)) {
            tvSuccessMessage.setText("Giao dịch thành công!");
            ivSuccessCheck.setImageResource(R.drawable.ic_success_check_big_green);
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
            updatePaymentStatus(transactionId, billId, "completed", vnpayTransactionNo, params);
        } else {
            String errorMessage = "Thanh toán thất bại!";
            if (responseCode != null) {
                errorMessage += "\nMã lỗi VNPAY: " + responseCode;
            }
            displayErrorState(errorMessage);
            Toast.makeText(this, "Thanh toán thất bại. Mã lỗi: " + responseCode, Toast.LENGTH_LONG).show();
            // Chỉ cập nhật transaction status, không cập nhật bill status khi thất bại
            updateTransactionStatusOnly(transactionId, "failed", vnpayTransactionNo, params);
        }
    }
    
    private void updateTransactionStatusOnly(String transactionId, String status, String vnpTransactionNo, 
                                             Map<String, String> vnpayParams) {
        Firestore.getTransactionById(transactionId, transaction -> {
            if (transaction != null) {
                Log.d(TAG, "Retrieved transaction: " + transaction.getUID());
                this.currentTransaction = transaction;
                transaction.setStatus(status);
                
                Firestore.addEditTransaction(transaction, isTransactionUpdated -> {
                    if (isTransactionUpdated) {
                        Log.d(TAG, "Transaction updated successfully with status: " + status);
                        progressOverlay.setVisibility(View.GONE);
                        populateTransactionDetails(this.currentTransaction, null, vnpayParams, status);
                    } else {
                        Log.e(TAG, "Failed to update transaction");
                        progressOverlay.setVisibility(View.GONE);
                        populateTransactionDetails(this.currentTransaction, null, vnpayParams, status);
                    }
                });
            } else {
                Log.e(TAG, "Transaction not found with ID: " + transactionId);
                progressOverlay.setVisibility(View.GONE);
                populateTransactionDetails(null, null, vnpayParams, status);
            }
        });
    }

    private void updatePaymentStatus(String transactionId, String billId, String status, String vnpTransactionNo, Map<String, String> vnpayParams) {
        Firestore.getTransactionById(transactionId, transaction -> {
            if (transaction != null) {
                Log.d(TAG, "Retrieved transaction: " + transaction.getUID());
                this.currentTransaction = transaction;
                transaction.setStatus(status);
                updateTransactionInFirestore(transaction, status, billId, vnpTransactionNo, vnpayParams);
            } else {
                Log.e(TAG, "Transaction not found with ID: " + transactionId);
                if ("completed".equals(status)) {
                    displayErrorState(tvSuccessMessage.getText() + "\n\nLỗi: Không tìm thấy thông tin giao dịch gốc.");
                }
                progressOverlay.setVisibility(View.GONE); // Hide progress overlay on error
                populateTransactionDetails(null, null, vnpayParams, status);
            }
        });
    }

    private void updateTransactionInFirestore(TransactionData transaction, String status, String billId, String vnpTransactionNo, Map<String, String> vnpayParams) {
        Firestore.addEditTransaction(transaction, isTransactionUpdated -> {
            if (isTransactionUpdated) {
                Log.d(TAG, "Transaction updated successfully with status: " + status);
                
                if ("completed".equals(status)) {
                    // Nếu giao dịch thành công, cập nhật số dư của tài khoản
                    updateAccountBalance(transaction);
                }
                
                updateBillInFirestore(billId, status, vnpTransactionNo, vnpayParams);
            } else {
                Log.e(TAG, "Failed to update transaction");
                progressOverlay.setVisibility(View.GONE); // Hide progress overlay on error
                if ("completed".equals(status)) {
                    displayErrorState(tvSuccessMessage.getText() + "\n\nLỗi: Không thể cập nhật giao dịch.");
                }
                populateTransactionDetails(this.currentTransaction, null, vnpayParams, status);
            }
        });
    }
    
    private void updateBillInFirestore(String billId, String status, String vnpTransactionNo, Map<String, String> vnpayParams) {
        if (billId == null || billId.isEmpty()) {
            Log.e(TAG, "updateBillInFirestore: billId is null or empty");
            progressOverlay.setVisibility(View.GONE);
            populateTransactionDetails(this.currentTransaction, null, vnpayParams, status);
            return;
        }
        
        Firestore.getBillById(billId, bill -> {
            if (bill != null) {
                Log.d(TAG, "Retrieved bill: " + bill.getUID());
                this.currentBill = bill;
                
                // Cập nhật thông tin bill
                if ("completed".equals(status)) {
                    bill.setStatus(status); // Chỉ cập nhật trạng thái bill nếu giao dịch thành công
                }
                bill.setTransactionId(this.currentTransaction != null ? this.currentTransaction.getUID() : null);
                
                Firestore.addEditBill(bill, isBillUpdated -> {
                    progressOverlay.setVisibility(View.GONE);
                    if (isBillUpdated) {
                        Log.d(TAG, "Bill updated successfully with status: " + status);
                        populateTransactionDetails(this.currentTransaction, this.currentBill, vnpayParams, status);
                    } else {
                        Log.e(TAG, "Failed to update bill");
                        populateTransactionDetails(this.currentTransaction, null, vnpayParams, status);
                    }
                });
            } else {
                Log.e(TAG, "Bill not found with ID: " + billId);
                progressOverlay.setVisibility(View.GONE);
                populateTransactionDetails(this.currentTransaction, null, vnpayParams, status);
            }
        });
    }
    
    private void updateAccountBalance(TransactionData transaction) {
        // Lấy account hiện tại từ GlobalVariables
        Account currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        
        if (currentAccount == null) {
            Log.e(TAG, "Cannot update account balance: Current account is null");
            // This is likely because the user has timed out or logged out
            // Don't show an error message here as this is a background operation
            // Just log the error and continue
            return;
        }
        
        if (currentAccount.getChecking() != null && 
                currentAccount.getChecking().getBalance() != null && transaction != null) {
            
            int currentBalance = currentAccount.getChecking().getBalance();
            int transactionAmount = transaction.getAmount();
            
            // Trừ số tiền của giao dịch từ số dư hiện tại
            int newBalance = currentBalance - transactionAmount;
            
            // Cập nhật số dư mới vào object CheckingAccount
            currentAccount.getChecking().setBalance(newBalance);
            
            // Cập nhật tài khoản trên Firestore
            Firestore.updateAccountFields(currentAccount.getUID(), 
                    Collections.singletonMap("checking.balance", newBalance), 
                    (isSuccess, e) -> {
                        if (isSuccess) {
                            Log.d(TAG, "Account balance updated successfully. New balance: " + newBalance);
                            // Cập nhật lại GlobalVariables để các màn hình khác có thông tin mới nhất
                            GlobalVariables.getInstance().setCurrentAccount(currentAccount);
                        } else {
                            Log.e(TAG, "Failed to update account balance", e);
                        }
                    });
        } else {
            Log.e(TAG, "Cannot update account balance: Account data is missing or invalid");
        }
    }

    private void displayErrorState(String message) {
        progressOverlay.setVisibility(View.GONE); // Hide progress overlay on error
        tvSuccessMessage.setText(message);
        ivSuccessCheck.setImageResource(R.drawable.ic_error);
        tvAmountDisplay.setText("");
        tvDateTimeDisplay.setText("");
        llTransactionDetailsContainer.setVisibility(View.GONE);
        // Disable sharing/saving for failed transactions
        llShare.setEnabled(false);
        llSaveImage.setEnabled(false);
        llShare.setAlpha(0.5f);
        llSaveImage.setAlpha(0.5f);
    }

    private String[] parseTransactionReference(String txnRef) {
        if (txnRef != null && txnRef.contains(":")) {
            return txnRef.split(":");
        }
        return null;
    }

    private void populateTransactionDetails(TransactionData transaction, Bill bill, Map<String, String> vnpayParams, String overallStatus) {
        // This method is always called after Firestore operations complete, so the progress overlay is already hidden
        llTransactionDetailsContainer.removeAllViews();
        llTransactionDetailsContainer.setVisibility(View.VISIBLE);

        Map<String, String> detailsMap = new LinkedHashMap<>();

        if ("completed".equals(overallStatus)) {
            detailsMap.put("Trạng thái", "Thành công");
        } else {
            detailsMap.put("Trạng thái", "Thất bại (Mã VNPAY: " + vnpayParams.get("vnp_ResponseCode") + ")");
        }

        if (transaction != null) {
            detailsMap.put("Loại giao dịch", transaction.getType() != null ? TransactionUtils.translateTransactionType(transaction.getType()) : "N/A");
            detailsMap.put("Nội dung", transaction.getDescription() != null ? transaction.getDescription() : "Không có");
        } else {
            detailsMap.put("Mã đơn hàng (App)", vnpayParams.get("vnp_TxnRef"));
        }

        if (bill != null) {
            detailsMap.put("Loại hóa đơn", bill.getType() != null ? BillUtils.translateBillType(bill.getType()) : "N/A");
            detailsMap.put("Nhà cung cấp", bill.getProvider() != null ? bill.getProvider() : "N/A");
            String customerId = bill.getUserId();
            if (customerId != null) {
                int maxLength = 15;
                if (customerId.length() > maxLength) {
                    customerId = customerId.substring(0, maxLength - 3) + "...";
                }
            } else {
                customerId = "N/A";
            }
            detailsMap.put("Mã khách hàng (HĐ)", customerId);
        }

        detailsMap.put("Mã GD VNPAY", vnpayParams.get("vnp_TransactionNo") != null ? vnpayParams.get("vnp_TransactionNo") : "N/A");
        detailsMap.put("Ngân hàng thanh toán", vnpayParams.get("vnp_BankCode") != null ? vnpayParams.get("vnp_BankCode") : "N/A"); // Consider mapping bank codes to names


        boolean firstItem = true;
        for (Map.Entry<String, String> entry : detailsMap.entrySet()) {
            if (!firstItem) {
                addDivider(llTransactionDetailsContainer);
            }
            addDetailRow(llTransactionDetailsContainer, entry.getKey(), entry.getValue());
            firstItem = false;
        }
    }

    private void addDetailRow(LinearLayout container, String label, String value) {
        LayoutInflater inflater = LayoutInflater.from(this);
        RelativeLayout rowLayout = (RelativeLayout) inflater.inflate(R.layout.item_transaction_detail_row, container, false);
        TextView tvLabel = rowLayout.findViewById(R.id.tvDetailLabel);
        TextView tvValue = rowLayout.findViewById(R.id.tvDetailValue);
        tvLabel.setText(label);
        tvValue.setText(value);
        container.addView(rowLayout);
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (1 * getResources().getDisplayMetrics().density)
        );
        int marginInDp = (int) (4 * getResources().getDisplayMetrics().density);
        params.setMargins(0, marginInDp, 0, marginInDp);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
        container.addView(divider);
    }

    // --- IMAGE HANDLING ---

    private Bitmap captureViewBitmap(View view) {
        // Ensure the view has a background if you want it in the bitmap
        // For ScrollView, it's better to capture its direct child if the child has dimensions and background
        View content = view; // Could be scrollView or scrollView.getChildAt(0)
        if (view instanceof ScrollView && ((ScrollView) view).getChildCount() > 0) {
            content = ((ScrollView) view).getChildAt(0);
        }

        // Set a temporary white background if none exists, to avoid transparency issues
        Drawable originalBackground = content.getBackground();
        if (originalBackground == null) {
            content.setBackgroundColor(Color.WHITE); // Or your app's default screen background
        }

        Bitmap bitmap = Bitmap.createBitmap(content.getWidth(), content.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        content.draw(canvas);

        // Restore original background if we changed it
        if (originalBackground == null) {
            content.setBackground(null);
        }
        return bitmap;
    }


    private void shareTransactionDetails() {
        if (tvSuccessMessage.getText().toString().toLowerCase().contains("thất bại")) {
            Toast.makeText(this, "Không thể chia sẻ giao dịch thất bại.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show a progress dialog while preparing the image
        ProgressBar progressBar = new ProgressBar(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(progressBar)
            .setMessage("Đang chuẩn bị ảnh...")
            .setCancelable(false)
            .create();
        dialog.show();
        
        // Run on a background thread to avoid UI freezing
        new Thread(() -> {
            Uri imageUriToShare = null;
            
            try {
                // Always create a new bitmap for sharing to avoid URI permission issues
                Bitmap bitmap = captureViewBitmap(scrollView);
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(PaymentReturnActivity.this, "Không thể tạo ảnh để chia sẻ.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                File imageFileForShare = saveBitmapToCacheForSharing(bitmap);
                if (imageFileForShare == null || !imageFileForShare.exists()) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(PaymentReturnActivity.this, "Không thể lưu ảnh tạm để chia sẻ.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                imageUriToShare = FileProvider.getUriForFile(
                    PaymentReturnActivity.this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    imageFileForShare
                );
                
                Log.d(TAG, "Sharing image URI: " + imageUriToShare);
                
                // Create the share intent on the UI thread
                final Uri finalImageUri = imageUriToShare;
                runOnUiThread(() -> {
                    dialog.dismiss();
                    
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, finalImageUri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chi tiết giao dịch 3T Banking");
                        
                        String shareTextBody = "Xem chi tiết giao dịch từ 3T Banking.";
                        if (currentTransaction != null && currentTransaction.getDescription() != null) {
                            shareTextBody += "\nNội dung: " + currentTransaction.getDescription();
                        }
                        shareTextBody += "\nSố tiền: " + displayAmount;
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareTextBody);
                        
                        // Add permission flag
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        
                        // Create and start the chooser
                        Intent chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ biên lai qua");
                        startActivity(chooserIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting share activity", e);
                        Toast.makeText(PaymentReturnActivity.this, "Không thể chia sẻ ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in shareTransactionDetails", e);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PaymentReturnActivity.this, "Lỗi khi chia sẻ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private File saveBitmapToCacheForSharing(Bitmap bitmap) {
        try {
            // Use external files directory instead of cache
            File sharePath = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_receipts");
            if (!sharePath.exists() && !sharePath.mkdirs()) {
                Log.e(TAG, "Failed to create share directory");
                return null;
            }
            
            // Clean old files to avoid clutter
            cleanupOldFiles(sharePath);
            
            String fileName = "GiaoDich_" + System.currentTimeMillis() + ".png";
            File imageFile = new File(sharePath, fileName);
            
            try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                return imageFile;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap for sharing: ", e);
            return null;
        }
    }
    
    private void cleanupOldFiles(File directory) {
        try {
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null && files.length > 10) { // Keep just 10 most recent files
                    // Sort by last modified date (newest first)
                    Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    
                    // Delete older files
                    for (int i = 10; i < files.length; i++) {
                        if (!files[i].delete()) {
                            Log.w(TAG, "Could not delete old file: " + files[i].getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up old files", e);
        }
    }

    // Wrapper for permission request
    private void saveTransactionAsImageWithPermission() {
        // For Android 6.0 (API 23) and above, you need to request runtime permissions
        // For simplicity, this example assumes permissions are granted or handled elsewhere.
        // In a real app, you MUST request Manifest.permission.WRITE_EXTERNAL_STORAGE
        // if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        //     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
        // } else {
        //     performSaveTransactionAsImage();
        // }
        performSaveTransactionAsImage(); // Call directly for now, add permission check in production
    }


    // @Override
    // public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //     super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    //     if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
    //         if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //             performSaveTransactionAsImage();
    //         } else {
    //             Toast.makeText(this, "Quyền ghi vào bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show();
    //         }
    //     }
    // }

    private void performSaveTransactionAsImage() {
        if (tvSuccessMessage.getText().toString().toLowerCase().contains("thất bại")) {
            Toast.makeText(this, "Không thể lưu ảnh giao dịch thất bại.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = captureViewBitmap(scrollView);
        if (bitmap == null) {
            Toast.makeText(this, "Không thể tạo ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "3TBanking_Receipt_" + System.currentTimeMillis() + ".png";
        OutputStream fos = null;
        Uri imageUri = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "3TBankingReceipts");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    fos = getContentResolver().openOutputStream(imageUri);
                }
            } else {
                // For older versions, use direct file path (requires WRITE_EXTERNAL_STORAGE)
                File publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File appImageDir = new File(publicPicturesDir, "3TBankingReceipts");
                if (!appImageDir.exists() && !appImageDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: " + appImageDir.getAbsolutePath());
                    Toast.makeText(this, "Không thể tạo thư mục lưu ảnh.", Toast.LENGTH_SHORT).show();
                    return;
                }
                File imageFile = new File(appImageDir, fileName);
                fos = new FileOutputStream(imageFile);
                // Expose this file via FileProvider for consistency or if needed by other apps
                // But MediaStore URI is generally preferred for gallery interaction
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
                savedImageUri = imageUri;
                // Trigger media scanner for older versions
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFile)); // Use file URI for scanner
                sendBroadcast(mediaScanIntent);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Toast.makeText(this, "Ảnh đã được lưu vào thư mục Ảnh/3TBankingReceipts.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Image saved, URI: " + imageUri);
                savedImageUri = imageUri; // Store the public URI

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && imageUri != null) {
                    ContentValues updateValues = new ContentValues();
                    updateValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(imageUri, updateValues, null, null);
                }
            } else {
                throw new IOException("Failed to get output stream.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi lưu ảnh: ", e);
            Toast.makeText(this, "Lỗi khi lưu ảnh.", Toast.LENGTH_SHORT).show();
            if (imageUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Clean up pending entry if saving failed
                getContentResolver().delete(imageUri, null, null);
            }
            savedImageUri = null;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing FileOutputStream: ", e);
            }
        }
    }



    private boolean verifySecureHash(Map<String, String> params, String secretKey) {
        // Implement proper VNPAY hash verification here (ideally on server)
        Log.w(TAG, "verifySecureHash is a STUB. Real validation should be on SERVER SIDE or implemented fully here.");
        return true;
    }
}
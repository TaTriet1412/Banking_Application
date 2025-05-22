package com.example.bankingapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.chaos.view.PinView;
import com.example.bankingapplication.Firebase.FirebaseStorageManager;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.Bill;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.EmailUtils;
import com.example.bankingapplication.Utils.FaceCompareUtils;
import com.example.bankingapplication.Utils.FaceDetectionUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.NumberToWordsConverter;
import com.example.bankingapplication.Utils.VnPayUtils;
import com.google.firebase.Timestamp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ConfirmElectricityPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_NUMBER = "extra_bill_number";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_BILL_TYPE = "extra_bill_type";
    private static final String TAG = "ConfirmElectricity";
    private static final int FACE_AUTH_THRESHOLD = 10000000; // 10 million VND threshold
    private static final String DEFAULT_PIN = "123456"; // This should come from user's account in production
    private static final int FACE_CAPTURE_REQUEST_CODE = 100;

    private Toolbar toolbar;
    private TextView tvBillNumber, tvCustomerName, tvProvider, tvAmount, tvAmountInWords;
    private TextView tvBillStatusMessage, tvConfirmDueDate, tvAuthMethod;
    private AppCompatButton btnConfirmPayment;
    private FrameLayout progressOverlay;
    private Bill currentBill;
    private int billAmount = 0;

    // User and account info
    private User currentUser;
    private Account currentAccount;

    // Bill info
    private String billNumberExtra, providerNameExtra, billTypeExtra;

    // Face authentication
    private Bitmap userFaceBitmap;
    private boolean isFaceLoaded = false;
    private Uri photoUri = null;

    // Variables to save state during camera activity
    private String savedUserUID;
    private String savedAccountUID;

    // PIN Dialog
    private AlertDialog pinDialog;
    private PinView pinViewPin;
    private TextView tvPinMessage;
    private Button btnVerifyPin;
    private ImageButton btnClosePinDialog;

    // OTP Dialog
    private AlertDialog otpDialog;
    private PinView pinViewOtp;
    private TextView tvOtpMessage, tvResendOtp;
    private Button btnVerifyOtp;
    private ImageButton btnCloseOtpDialog;

    private CountDownTimer resendOtpTimer;
    private CountDownTimer otpExpiryTimer;

    private String currentOtp;
    private long otpExpiryTimestamp;
    private final long OTP_EXPIRY_DURATION = 2 * 60 * 1000; // 2 minutes
    private int[] resendDelaysSeconds = {15, 20, 30, 50, 90};
    private int currentResendAttempt = 0;
    private boolean isOtpExpired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_electricity_payment);

        // Initialize views
        toolbar = findViewById(R.id.toolbar_confirm_electricity);
        tvBillNumber = findViewById(R.id.tv_confirm_bill_number);
        tvCustomerName = findViewById(R.id.tv_confirm_customer_name);
        tvProvider = findViewById(R.id.tv_confirm_provider);
        tvAmount = findViewById(R.id.tv_confirm_amount);
        tvAmountInWords = findViewById(R.id.tv_confirm_amount_in_words);
        tvBillStatusMessage = findViewById(R.id.tv_confirm_bill_status_message);
        tvConfirmDueDate = findViewById(R.id.tv_confirm_due_date);
        tvAuthMethod = findViewById(R.id.tv_auth_method); // Changed to TextView
        btnConfirmPayment = findViewById(R.id.btn_confirm_payment_final);
        progressOverlay = findViewById(R.id.progress_overlay);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Get current user and account
        currentUser = GlobalVariables.getInstance().getCurrentUser();
        currentAccount = GlobalVariables.getInstance().getCurrentAccount();

        if (currentUser == null || currentAccount == null) {
            Toast.makeText(this, "Lỗi: Không thể tải thông tin người dùng/tài khoản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set customer name
        tvCustomerName.setText(currentUser.getName());

        // Get intent data
        Intent intent = getIntent();
        if (intent != null) {
            billNumberExtra = intent.getStringExtra(EXTRA_BILL_NUMBER);
            providerNameExtra = intent.getStringExtra(EXTRA_PROVIDER_NAME);
            billTypeExtra = intent.getStringExtra(EXTRA_BILL_TYPE);
            Log.d(TAG, "Received Bill Number: " + billNumberExtra + ", Provider: " + providerNameExtra + ", Type: " + billTypeExtra);
        }

        if (billNumberExtra == null || providerNameExtra == null || billTypeExtra == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin hóa đơn đầu vào.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bill number or provider name from intent is null.");
            finish();
            return;
        }

        // Set bill data UI
        tvBillNumber.setText(billNumberExtra);
        tvProvider.setText(providerNameExtra);

        // Load bill details
        loadBillDetailsUsingFirestoreClass();

        // Set confirm button click
        btnConfirmPayment.setOnClickListener(v -> {
            if (validateBill()) {
                // Check if face authentication is needed based on amount
                if (billAmount >= FACE_AUTH_THRESHOLD) {
                    if (isFaceLoaded && userFaceBitmap != null) {
                        startFaceAuthentication();
                    } else {
                        preloadUserFaceImage();
                        Toast.makeText(this, "Đang tải dữ liệu xác thực. Vui lòng thử lại sau giây lát.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showPinDialog(); // Start normal PIN authentication
                }
            }
        });
    }

    // Method to save biometric state before camera launch
    private void saveBiometricState() {
        Log.d(TAG, "Saving biometric state before camera launch");
        if (currentUser != null) {
            savedUserUID = currentUser.getUID();
        }
        if (currentAccount != null) {
            savedAccountUID = currentAccount.getUID();
        }
    }

    // Method to restore biometric state after camera activity
    private boolean restoreBiometricState() {
        Log.d(TAG, "Restoring biometric state after camera");

        // Check if we need to restore state (if GlobalVariables lost references)
        if (GlobalVariables.getInstance().getCurrentUser() == null ||
                GlobalVariables.getInstance().getCurrentAccount() == null) {

            Log.d(TAG, "GlobalVariables lost state, attempting to restore");

            // If we have saved UIDs, try to reload the data
            if (savedUserUID != null && savedAccountUID != null) {
                Log.d(TAG, "Attempting to restore with saved UIDs - User: " + savedUserUID + ", Account: " + savedAccountUID);

                // Use synchronous Firestore get to ensure data is loaded before continuing
                try {
                    // Simple synchronous implementation for direct retrieval
                    User user = synchronousGetUser(savedUserUID);
                    Account account = synchronousGetAccount(savedAccountUID);

                    if (user != null && account != null) {
                        // Restore the global variables
                        GlobalVariables.getInstance().setCurrentUser(user);
                        GlobalVariables.getInstance().setCurrentAccount(account);

                        // Update our local references
                        currentUser = user;
                        currentAccount = account;

                        Log.d(TAG, "Successfully restored user and account synchronously");
                        return true;
                    } else {
                        Log.e(TAG, "Failed to retrieve user or account synchronously");
                        return false;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during synchronous state restoration", e);
                    return false;
                }
            }
            return false;
        }

        // No need to restore if references are still valid
        Log.d(TAG, "GlobalVariables state intact, no need to restore");
        currentUser = GlobalVariables.getInstance().getCurrentUser();
        currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        return (currentUser != null && currentAccount != null);
    }

    // Synchronous implementation to get User from Firestore
    private User synchronousGetUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        try {
            // Create a CountDownLatch to wait for the async operation
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final User[] result = new User[1];

            Firestore.getUser(userId, user -> {
                result[0] = user;
                latch.countDown();
            });

            // Wait for the callback to complete (with timeout)
            if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for user data");
                return null;
            }

            return result[0];
        } catch (Exception e) {
            Log.e(TAG, "Error in synchronousGetUser", e);
            return null;
        }
    }

    // Synchronous implementation to get Account from Firestore
    private Account synchronousGetAccount(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return null;
        }

        try {
            // Create a CountDownLatch to wait for the async operation
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final Account[] result = new Account[1];

            Firestore.getAccount(accountId, account -> {
                result[0] = account;
                latch.countDown();
            });

            // Wait for the callback to complete (with timeout)
            if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for account data");
                return null;
            }

            return result[0];
        } catch (Exception e) {
            Log.e(TAG, "Error in synchronousGetAccount", e);
            return null;
        }
    }

    private boolean validateBill() {
        if (currentBill == null) {
            Toast.makeText(this, "Không tìm thấy thông tin hóa đơn", Toast.LENGTH_SHORT).show();
            return false;
        }

        if ("completed".equals(currentBill.getStatus())) {
            Toast.makeText(this, "Hóa đơn này đã được thanh toán", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if account has sufficient balance
        if (currentAccount.getChecking() == null ||
                currentAccount.getChecking().getBalance() == null ||
                currentAccount.getChecking().getBalance() < billAmount) {

            Toast.makeText(this, "Số dư không đủ để thực hiện giao dịch này", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showConfirmLoading(boolean isLoading) {
        if (isLoading) {
            progressOverlay.setVisibility(View.VISIBLE);
            btnConfirmPayment.setEnabled(false);
        } else {
            progressOverlay.setVisibility(View.GONE);
            btnConfirmPayment.setEnabled(true);
        }
    }

    private void loadBillDetailsUsingFirestoreClass() {
        showConfirmLoading(true);
        Firestore.getElectricityBillByDetails(providerNameExtra, billNumberExtra, billTypeExtra, (bill, e) -> {
            showConfirmLoading(false);
            if (e != null) {
                Log.e(TAG, "Error loading bill details from Firestore class: ", e);
                Toast.makeText(this, "Lỗi tải thông tin hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                displayEmptyBillInfo();
                return;
            }

            if (bill != null) {
                currentBill = bill;
                Log.d(TAG, "Bill details loaded: UID=" + bill.getUID() + ", Amount=" + bill.getAmount() +
                        ", UserID=" + currentUser.getUID() + ", DueDate=" + bill.getDueDate() + ", Type=" + bill.getType());

                // Set bill amount
                Integer amountInteger = bill.getAmount();
                if (amountInteger != null) {
                    billAmount = amountInteger;
                    tvAmount.setText(NumberFormat.convertToCurrencyFormatHasUnit(amountInteger));
                    tvAmountInWords.setText(NumberToWordsConverter.convertNumberToVietnameseWords(amountInteger) + " đồng");
                    Log.d(TAG, "Amount processed: " + amountInteger);

                    // Check if we need face authentication and preload user face image
                    if (amountInteger >= FACE_AUTH_THRESHOLD) {
                        tvAuthMethod.setText("3TB OTP"); // Always display 3TB OTP
                        preloadUserFaceImage();
                    } else {
                        tvAuthMethod.setText("3TB OTP"); // Always display 3TB OTP
                    }
                } else {
                    tvAmount.setText("N/A (amount is null)");
                    tvAmountInWords.setText("");
                    Log.w(TAG, "Amount (Integer) is null in Bill object.");
                }

                // Display due date
                Timestamp dueDateTimestamp = bill.getDueDate();
                if (dueDateTimestamp != null) {
                    Date dueDate = dueDateTimestamp.toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    tvConfirmDueDate.setText(sdf.format(dueDate));
                } else {
                    tvConfirmDueDate.setText("N/A");
                    Log.w(TAG, "DueDate is null in Bill object.");
                }

                // Check bill status
                checkAndDisplayBillStatus(bill);
            } else {
                Log.w(TAG, "Bill not found by Firestore.getElectricityBillByDetails");
                Toast.makeText(this, "Không tìm thấy thông tin hóa đơn chi tiết.", Toast.LENGTH_SHORT).show();
                displayEmptyBillInfo();
            }
        });
    }

    private void preloadUserFaceImage() {
        if (currentUser.getBiometricData() == null ||
                currentUser.getBiometricData().getFaceUrl() == null ||
                currentUser.getBiometricData().getFaceUrl().isEmpty()) {

            Log.e(TAG, "User face image URL is not available");
            Toast.makeText(this, "Không tìm thấy thông tin xác thực khuôn mặt", Toast.LENGTH_SHORT).show();
            return;
        }

        showConfirmLoading(true);
        String faceImageFileName = currentUser.getBiometricData().getFaceUrl();
        Log.d(TAG, "Loading face image from Firebase Storage: " + faceImageFileName);

        // Download the face image from Firebase Storage
        FirebaseStorageManager.downloadFile(faceImageFileName, "faceImg/", 30, bytes -> {
            if (bytes != null) {
                try {
                    // Just decode the image without any rotation or transformation
                    userFaceBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    Matrix matrix = new Matrix();
                    matrix.postRotate(270);
                    userFaceBitmap = Bitmap.createBitmap(
                            userFaceBitmap,
                            0,
                            0,
                            userFaceBitmap.getWidth(),
                            userFaceBitmap.getHeight(),
                            matrix,
                            true
                    );

                    // Log the dimensions of the bitmap for debugging
                    Log.d(TAG, "Face image loaded successfully from Firebase Storage. " +
                            "Dimensions: " + userFaceBitmap.getWidth() + "x" + userFaceBitmap.getHeight());

                    isFaceLoaded = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding face image", e);
                    Toast.makeText(ConfirmElectricityPaymentActivity.this,
                            "Lỗi khi xử lý ảnh xác thực khuôn mặt", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Failed to download face image from Firebase Storage");
                Toast.makeText(ConfirmElectricityPaymentActivity.this,
                        "Không thể tải ảnh xác thực khuôn mặt", Toast.LENGTH_SHORT).show();
            }

            runOnUiThread(() -> showConfirmLoading(false));
        });
    }

    private void checkAndDisplayBillStatus(Bill bill) {
        if (bill == null) return;

        Timestamp dueDateTimestamp = bill.getDueDate();
        String status = bill.getStatus();

        // Hide message by default
        tvBillStatusMessage.setVisibility(View.GONE);

        if (dueDateTimestamp != null && status != null) {
            Date dueDate = dueDateTimestamp.toDate();
            Date currentDate = Calendar.getInstance().getTime();

            // Compare dates
            Calendar calDueDate = Calendar.getInstance();
            calDueDate.setTime(dueDate);
            Calendar calCurrentDate = Calendar.getInstance();
            calCurrentDate.setTime(currentDate);

            // Reset time components
            clearTime(calDueDate);
            clearTime(calCurrentDate);

            // Check if completed
            String lowerCaseStatus = status.toLowerCase();
            boolean isCompleted = lowerCaseStatus.equals("completed") ||
                    lowerCaseStatus.equals("paid") ||
                    lowerCaseStatus.equals("đã thanh toán");

            if (calDueDate.before(calCurrentDate) && !isCompleted) {
                // Bill is overdue
                String overdueMessage = "Hóa đơn này đã quá hạn thanh toán!";
                Log.w(TAG, overdueMessage + " Due: " + dueDate.toString() + ", Status: " + status);
                tvBillStatusMessage.setText(overdueMessage);
                tvBillStatusMessage.setTextColor(Color.RED);
                tvBillStatusMessage.setVisibility(View.VISIBLE);
                btnConfirmPayment.setEnabled(false);
            } else if (isCompleted) {
                // Bill is already paid
                String paidMessage = "Hóa đơn này đã được thanh toán.";
                Log.i(TAG, paidMessage + " Status: " + status);
                tvBillStatusMessage.setText(paidMessage);
                tvBillStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                tvBillStatusMessage.setVisibility(View.VISIBLE);
                btnConfirmPayment.setEnabled(false);
            } else {
                // Bill is pending payment
                String paidMessage = "Hóa đơn này chưa thanh toán.";
                tvBillStatusMessage.setText(paidMessage);
                tvBillStatusMessage.setVisibility(View.VISIBLE);
                Log.i(TAG, "Bill is not overdue or status is normal. Due: " + dueDate.toString() + ", Status: " + status);
            }
        }
    }

    // Utility function to clear time components from Calendar
    private void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void displayEmptyBillInfo() {
        tvAmount.setText("N/A");
        tvAmountInWords.setText("");
        tvConfirmDueDate.setText("N/A");
        btnConfirmPayment.setEnabled(false);
    }

    // Face Authentication methods
    private void startFaceAuthentication() {
        // Save current user and account information to ensure they're preserved
        // through camera activity lifecycle
        saveBiometricState();

        // Show dialog with instructions before starting the camera
        new AlertDialog.Builder(this)
                .setTitle("Xác thực khuôn mặt")
                .setMessage("Vui lòng giữ điện thoại ở khoảng cách phù hợp và nhìn thẳng vào camera để xác thực khuôn mặt.")
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    takeFacePhoto();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private File createImageFile(String prefix) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = prefix + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void takeFacePhoto() {
        // Create Intent to take a picture
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Add instructions and camera hints
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1); // Try to use front camera
        cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
        cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);

        // Ensure that there's a camera activity to handle the intent
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile("JPEG_");
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Lỗi tạo tệp hình ảnh", Toast.LENGTH_SHORT).show();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider",
                            photoFile);

                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                    // Show detailed camera instructions first
                    new AlertDialog.Builder(this)
                            .setTitle("Hướng dẫn chụp ảnh")
                            .setMessage("• Đảm bảo ánh sáng đủ sáng\n" +
                                    "• Nhìn thẳng vào camera\n" +
                                    "• Không đeo kính râm, khẩu trang\n" +
                                    "• Giữ điện thoại ngang tầm mặt\n" +
                                    "• Chụp trong không gian yên tĩnh")
                            .setPositiveButton("Hiểu rồi", (dialog, which) -> {
                                startActivityForResult(cameraIntent, FACE_CAPTURE_REQUEST_CODE);
                            })
                            .setCancelable(false)
                            .show();
                } catch (Exception e) {
                    Log.e(TAG, "Error getting FileProvider URI", e);
                    Toast.makeText(this, "Lỗi khởi động camera", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FACE_CAPTURE_REQUEST_CODE) {
            // First restore state if necessary
            boolean stateRestored = restoreBiometricState();

            if (!stateRestored) {
                Log.e(TAG, "Failed to restore state after camera activity");
                Toast.makeText(this, "Lỗi: Không thể khôi phục thông tin người dùng sau khi chụp ảnh. Vui lòng thử lại.",
                        Toast.LENGTH_LONG).show();
                finish(); // Go back to previous screen since we can't proceed
                return;
            }

            if (resultCode == RESULT_OK) {
                progressOverlay.setVisibility(View.VISIBLE);

                if (photoUri != null) {
                    try {
                        // Load the full size image
                        Bitmap capturedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);

                        // Log image details for debugging
                        Log.d(TAG, "Captured image: width=" + capturedImage.getWidth() +
                                ", height=" + capturedImage.getHeight() +
                                ", size=" + (capturedImage.getByteCount() / 1024) + "KB");

                        // Fix image rotation based on EXIF data
                        capturedImage = rotateImageIfRequired(this, capturedImage, photoUri);

                        if (capturedImage == null) {
                            progressOverlay.setVisibility(View.GONE);
                            Toast.makeText(this, "Lỗi xử lý ảnh đã chụp", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Verify user face is still loaded
                        if (!isFaceLoaded || userFaceBitmap == null) {
                            Log.d(TAG, "Face data lost, reloading...");
                            Toast.makeText(this, "Đang tải lại dữ liệu khuôn mặt...",
                                    Toast.LENGTH_SHORT).show();
                            // Reload user face and retry later
                            preloadUserFaceImage();
                            progressOverlay.setVisibility(View.GONE);
                            return;
                        }

                        // Detect face in captured image
                        Bitmap finalCapturedImage = capturedImage;
                        FaceDetectionUtils.isFacePresent(capturedImage, isFacePresent -> {
                            if (isFacePresent) {
                                Log.d(TAG, "Face detected in image successfully");
                                compareFaces(finalCapturedImage, userFaceBitmap);
                            } else {
                                Log.d(TAG, "Face detection failed, trying with enhanced image");

                                // Try with enhanced image
                                Bitmap enhancedImage = enhanceImageForFaceDetection(finalCapturedImage);

                                FaceDetectionUtils.isFacePresent(enhancedImage, isEnhancedFacePresent -> {
                                    if (isEnhancedFacePresent) {
                                        Log.d(TAG, "Face detected in enhanced image");
                                        compareFaces(enhancedImage, userFaceBitmap);
                                    } else {
                                        // Ask user if they want to continue anyway
                                        runOnUiThread(() -> {
                                            new AlertDialog.Builder(ConfirmElectricityPaymentActivity.this)
                                                    .setTitle("Không phát hiện khuôn mặt rõ ràng")
                                                    .setMessage("Hệ thống không phát hiện rõ khuôn mặt trong ảnh. Bạn vẫn muốn thử xác thực?")
                                                    .setPositiveButton("Vẫn xác thực", (dialog, which) -> {
                                                        compareFaces(finalCapturedImage, userFaceBitmap);
                                                    })
                                                    .setNegativeButton("Chụp lại", (dialog, which) -> {
                                                        progressOverlay.setVisibility(View.GONE);
                                                        takeFacePhoto();
                                                    })
                                                    .setCancelable(false)
                                                    .show();
                                        });
                                    }
                                });
                            }
                        });

                    } catch (IOException e) {
                        Log.e(TAG, "Error loading captured image", e);
                        progressOverlay.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi xử lý ảnh đã chụp", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tìm thấy ảnh đã chụp", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Đã hủy quá trình chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Improved rotation handling from ConfirmTransactionActivity
    private static String orientationToString(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return "NORMAL (1)";
            case ExifInterface.ORIENTATION_ROTATE_90:
                return "ROTATE_90 (6)";
            case ExifInterface.ORIENTATION_ROTATE_180:
                return "ROTATE_180 (3)";
            case ExifInterface.ORIENTATION_ROTATE_270:
                return "ROTATE_270 (8)";
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return "FLIP_HORIZONTAL (2)";
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return "FLIP_VERTICAL (4)";
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return "TRANSPOSE (5)";
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return "TRANSVERSE (7)";
            case ExifInterface.ORIENTATION_UNDEFINED:
                return "UNDEFINED (0)";
            default:
                return "Unknown (" + orientation + ")";
        }
    }

    public static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {
        if (img == null) {
            Log.e(TAG, "rotateImageIfRequired: Input bitmap is null for URI: " + selectedImage);
            return null;
        }
        Log.d(TAG, "rotateImageIfRequired: Attempting to rotate image from URI: " + selectedImage);
        Log.d(TAG, "rotateImageIfRequired: Bitmap dimensions BEFORE rotation: " + img.getWidth() + "x" + img.getHeight());

        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(selectedImage);
            if (input == null) {
                Log.e(TAG, "rotateImageIfRequired: Could not open InputStream for URI: " + selectedImage);
                return img; // Return original if stream cannot be opened
            }

            ExifInterface ei;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) { // API 24+
                Log.d(TAG, "rotateImageIfRequired: Using ExifInterface (InputStream) for API " + android.os.Build.VERSION.SDK_INT);
                ei = new ExifInterface(input);
            } else {
                Log.d(TAG, "rotateImageIfRequired: Using ExifInterface (path) for API " + android.os.Build.VERSION.SDK_INT);
                String path = selectedImage.getPath();
                Log.d(TAG, "rotateImageIfRequired: Path for API < 24 = " + path);
                if (path == null) {
                    Log.e(TAG, "rotateImageIfRequired: Path is null for API < 24. URI: " + selectedImage + ". Cannot read EXIF. Returning original image.");
                    return img;
                }
                ei = new ExifInterface(path);
            }

            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d(TAG, "rotateImageIfRequired: Raw EXIF Orientation for URI " + selectedImage + ": " + orientationToString(orientation) + " [" + orientation + "]");

            Bitmap rotatedBitmap = img;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    Log.d(TAG, "rotateImageIfRequired: Rotating 90 degrees");
                    rotatedBitmap = rotateImage(img, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    Log.d(TAG, "rotateImageIfRequired: Rotating 180 degrees");
                    rotatedBitmap = rotateImage(img, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d(TAG, "rotateImageIfRequired: Rotating 270 degrees");
                    rotatedBitmap = rotateImage(img, 270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    Log.d(TAG, "rotateImageIfRequired: Flipping horizontal");
                    rotatedBitmap = flipImage(img, true, false);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    Log.d(TAG, "rotateImageIfRequired: Flipping vertical");
                    rotatedBitmap = flipImage(img, false, true);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    Log.d(TAG, "rotateImageIfRequired: No rotation needed or orientation is NORMAL/UNDEFINED.");
                    rotatedBitmap = img; // No rotation needed
            }
            if (rotatedBitmap != img) { // If a new bitmap was created
                Log.d(TAG, "rotateImageIfRequired: Bitmap dimensions AFTER rotation: " + rotatedBitmap.getWidth() + "x" + rotatedBitmap.getHeight());
            }
            return rotatedBitmap;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(TAG, "rotateImageIfRequired: Error closing InputStream", e);
                }
            }
        }
    }

    public static Bitmap flipImage(Bitmap source, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        float cx = source.getWidth() / 2f;
        float cy = source.getHeight() / 2f;
        if (horizontal) {
            matrix.postScale(-1, 1, cx, cy);
        }
        if (vertical) {
            matrix.postScale(1, -1, cx, cy);
        }
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    // Enhanced image for face detection
    private Bitmap enhanceImageForFaceDetection(Bitmap original) {
        if (original == null) return null;

        // Create a copy to avoid affecting the original
        Bitmap enhanced = original.copy(original.getConfig(), true);

        // Resize image to optimum size for face detection
        int targetSize = 1024; // Target size for longest dimension
        float scaleFactor = 1.0f;

        int width = enhanced.getWidth();
        int height = enhanced.getHeight();

        int longerDimension = Math.max(width, height);
        if (longerDimension > targetSize) {
            scaleFactor = (float) targetSize / longerDimension;
            int newWidth = Math.round(width * scaleFactor);
            int newHeight = Math.round(height * scaleFactor);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(enhanced, newWidth, newHeight, true);
            enhanced.recycle(); // Free memory
            enhanced = scaledBitmap;
        }

        Log.d(TAG, "Enhanced image dimensions: " + enhanced.getWidth() + "x" + enhanced.getHeight());
        return enhanced;
    }

    private void compareFaces(Bitmap capturedFace, Bitmap storedFace) {
        if (capturedFace == null || storedFace == null) {
            runOnUiThread(() -> {
                progressOverlay.setVisibility(View.GONE);
                Toast.makeText(ConfirmElectricityPaymentActivity.this,
                        "Lỗi xác thực: Không có đủ dữ liệu khuôn mặt", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Prepare images for comparison
        FaceCompareUtils faceCompareUtils = new FaceCompareUtils();
        faceCompareUtils.compareFaces(capturedFace, storedFace, this, new FaceCompareUtils.FaceCompareCallback() {
            @Override
            public void onMatch() {
                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(ConfirmElectricityPaymentActivity.this,
                            "Xác thực khuôn mặt thành công", Toast.LENGTH_SHORT).show();
                    // Proceed to OTP verification
                    showOtpDialog();
                });
            }

            @Override
            public void onMismatch() {
                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(ConfirmElectricityPaymentActivity.this,
                            "Xác thực khuôn mặt thất bại. Khuôn mặt không khớp.",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(ConfirmElectricityPaymentActivity.this,
                            "Lỗi khi xác thực khuôn mặt: " + message,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // PIN Authentication methods
    private void showPinDialog() {
        if (pinDialog != null && pinDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pin_verification, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        pinDialog = builder.create();
        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvPinMessage = dialogView.findViewById(R.id.tvPinMessage);
        pinViewPin = dialogView.findViewById(R.id.pinViewPin);
        btnVerifyPin = dialogView.findViewById(R.id.btnVerifyPin);
        btnClosePinDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvPinMessage.setText("Nhập mã PIN của bạn");
        pinViewPin.requestFocus();

        // Show keyboard
        new Handler().postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(pinViewPin, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);

        pinViewPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnVerifyPin.setEnabled(s.length() == pinViewPin.getItemCount());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        btnVerifyPin.setEnabled(false);

        btnClosePinDialog.setOnClickListener(v -> pinDialog.dismiss());
        btnVerifyPin.setOnClickListener(v -> verifyPin());

        pinDialog.show();
    }

    private void verifyPin() {
        String enteredPin = pinViewPin.getText().toString();

        // In production, verify against user's actual PIN from account
        String actualPin = currentAccount.getPinCode() != null ? currentAccount.getPinCode() : DEFAULT_PIN;

        if (enteredPin.equals(actualPin)) {
            Toast.makeText(this, "Mã PIN hợp lệ", Toast.LENGTH_SHORT).show();
            pinDialog.dismiss();

            // Show OTP dialog after PIN verification
            showOtpDialog();
        } else {
            Toast.makeText(this, "Mã PIN không đúng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            pinViewPin.setText("");
        }
    }

    // OTP Authentication methods
    private void showOtpDialog() {
        if (otpDialog != null && otpDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp_verification, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        otpDialog = builder.create();
        if (otpDialog.getWindow() != null) {
            otpDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvOtpMessage = dialogView.findViewById(R.id.tvOtpMessage);
        pinViewOtp = dialogView.findViewById(R.id.pinViewOtp);
        tvResendOtp = dialogView.findViewById(R.id.tvResendOtp);
        btnVerifyOtp = dialogView.findViewById(R.id.btnVerifyOtp);
        btnCloseOtpDialog = dialogView.findViewById(R.id.btnCloseDialog);

        String maskedEmail = currentUser.getEmail().replaceAll("(?<=.{2}).(?=.*@)", "*");
        tvOtpMessage.setText("Nhập mã OTP đã được gửi đến email của bạn: " + maskedEmail);

        pinViewOtp.requestFocus();

        pinViewOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnVerifyOtp.setEnabled(s.length() == pinViewOtp.getItemCount());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        btnVerifyOtp.setEnabled(false);

        btnCloseOtpDialog.setOnClickListener(v -> {
            cancelTimers();
            otpDialog.dismiss();
        });

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        tvResendOtp.setOnClickListener(v -> handleResendOtp());

        sendNewOtp();
        otpDialog.show();
    }

    private void sendNewOtp() {
        isOtpExpired = false;

        // Generate OTP
        currentOtp = generateRandomOtp();
        otpExpiryTimestamp = System.currentTimeMillis() + OTP_EXPIRY_DURATION;

        // Format expiry time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String expiryTimeFormatted = sdf.format(new Date(otpExpiryTimestamp));

        // Email content
        String subject = "Xác nhận thanh toán hóa đơn điện - 3T Banking";
        String body = "Mã OTP của bạn là: " + currentOtp
                + "\nHiệu lực đến: " + expiryTimeFormatted + " (trong vòng 2 phút)."
                + "\n\nThông tin giao dịch:"
                + "\n- Số tiền: " + NumberFormat.convertToCurrencyFormatHasUnit(billAmount)
                + "\n- Mã hoá đơn: " + billNumberExtra
                + "\n- Nhà cung cấp: " + providerNameExtra;

        EmailUtils.sendEmail(currentUser.getEmail(), subject, body);

        Toast.makeText(this, "Mã OTP đã gửi đến email: " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();

        // Reset UI
        if (pinViewOtp != null) {
            new Handler(Looper.getMainLooper()).post(() -> pinViewOtp.setText(""));
        }
        if (btnVerifyOtp != null) btnVerifyOtp.setEnabled(false);

        startOtpExpiryTimer();
        startResendOtpCooldown();
    }

    private void handleResendOtp() {
        if (tvResendOtp.isEnabled()) {
            cancelTimers();
            currentResendAttempt++;
            sendNewOtp();
        }
    }

    private void startResendOtpCooldown() {
        tvResendOtp.setEnabled(false);
        long delayMillis = getCurrentResendDelay() * 1000L;

        if (resendOtpTimer != null) {
            resendOtpTimer.cancel();
        }
        resendOtpTimer = new CountDownTimer(delayMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendOtp.setText(String.format(Locale.getDefault(), "Gửi lại (%ds)", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvResendOtp.setText("Gửi lại");
                tvResendOtp.setEnabled(true);
            }
        }.start();
    }

    private String generateRandomOtp() {
        int otp = 100000 + new java.util.Random().nextInt(900000);
        return String.valueOf(otp);
    }

    private long getCurrentResendDelay() {
        int index = Math.min(currentResendAttempt, resendDelaysSeconds.length - 1);
        return resendDelaysSeconds[index];
    }

    private void startOtpExpiryTimer() {
        if (otpExpiryTimer != null) {
            otpExpiryTimer.cancel();
        }
        otpExpiryTimer = new CountDownTimer(OTP_EXPIRY_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // You could show a countdown for OTP expiry if needed
            }

            @Override
            public void onFinish() {
                isOtpExpired = true;
                if (otpDialog != null && otpDialog.isShowing()) {
                    Toast.makeText(ConfirmElectricityPaymentActivity.this, "Mã OTP đã hết hạn", Toast.LENGTH_LONG).show();
                    btnVerifyOtp.setEnabled(false);
                    if (pinViewOtp != null) pinViewOtp.setText("");
                }
            }
        }.start();
    }

    private void verifyOtp() {
        String enteredOtp = pinViewOtp.getText().toString();

        // Check expiry in real time
        if (System.currentTimeMillis() > otpExpiryTimestamp) {
            isOtpExpired = true;
            Toast.makeText(this, "Mã OTP đã hết hạn", Toast.LENGTH_SHORT).show();
            if (pinViewOtp != null) pinViewOtp.setText("");
            btnVerifyOtp.setEnabled(false);
            return;
        }

        if (enteredOtp.equals(currentOtp)) {
            Toast.makeText(this, "Xác thực OTP thành công", Toast.LENGTH_SHORT).show();
            cancelTimers();
            otpDialog.dismiss();

            // Process the transaction
            createTransactionAndBill();
        } else {
            Toast.makeText(this, "Mã OTP không đúng", Toast.LENGTH_SHORT).show();
            if (pinViewOtp != null) pinViewOtp.setText("");
        }
    }

    private void cancelTimers() {
        if (resendOtpTimer != null) {
            resendOtpTimer.cancel();
        }
        if (otpExpiryTimer != null) {
            otpExpiryTimer.cancel();
        }
    }
    
    // Transaction processing
    private void createTransactionAndBill() {
        progressOverlay.setVisibility(View.VISIBLE);

        // Create a new transaction for the bill payment
        TransactionData transaction = new TransactionData(
                Firestore.generateId("TR"),
                billAmount,
                "payment", // Type of transaction is payment
                "pending", // Status initially set to pending
                "Thanh toán hóa đơn " + providerNameExtra + " - " + billNumberExtra, // Description
                Timestamp.now(),
                currentAccount.getUID(), // From account
                null // No recipient account for bill payments
        );

        Firestore.addEditTransaction(transaction, isSuccess -> {
            if (isSuccess) {
                Log.d(TAG, "Transaction created successfully: " + transaction.getUID());
                
                // Update the bill with transaction ID and launch payment
                if (currentBill != null) {
                    launchVnPayPayment(transaction, currentBill);
                } else {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi: Không tìm thấy thông tin hóa đơn", Toast.LENGTH_SHORT).show();
                }
            } else {
                progressOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tạo giao dịch", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchVnPayPayment(TransactionData transaction, Bill bill) {
        // Create a composite order reference with both IDs
        String orderRef = transaction.getUID() + ":" + bill.getUID();

        Log.d(TAG, "Creating payment URL with Order Reference: " + orderRef);

        // Create the payment URL
        String paymentUrl = VnPayUtils.createPaymentUrl(
                billAmount,
                transaction.getDescription(),
                orderRef
        );

        progressOverlay.setVisibility(View.GONE);

        try {
            // Launch WebViewPaymentActivity with the payment URL
            Intent intent = new Intent(this, WebViewPaymentActivity.class);
            intent.putExtra("paymentUrl", paymentUrl);
            intent.putExtra("transactionType", "PAYMENT");
            intent.putExtra("amount", String.valueOf(billAmount));
            intent.putExtra("billProvider", providerNameExtra);
            intent.putExtra("billNumber", billNumberExtra);
            startActivity(intent);

            finish(); // Close this activity
        } catch (Exception e) {
            Log.e(TAG, "Error launching WebViewPaymentActivity", e);
            Toast.makeText(this, "Không thể mở trang thanh toán", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimers();
        if (pinDialog != null && pinDialog.isShowing()) {
            pinDialog.dismiss();
        }
        if (otpDialog != null && otpDialog.isShowing()) {
            otpDialog.dismiss();
        }
    }
}

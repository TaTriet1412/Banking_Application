package com.example.bankingapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface; // Import ExifInterface
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // Import NonNull
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.FileProvider;

import com.example.bankingapplication.DTO.Response.CardFrontRes;
import com.example.bankingapplication.Firebase.FirebaseAuth;
import com.example.bankingapplication.Firebase.FirebaseStorageManager;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Biometric;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.ActionUtils;
import com.example.bankingapplication.Utils.AudioEffectUtils;
import com.example.bankingapplication.Utils.FaceCompareUtils;
import com.example.bankingapplication.Utils.FaceDetectionUtils;
import com.example.bankingapplication.Utils.ImageUtils;
import com.example.bankingapplication.Utils.OCRUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream; // Import InputStream
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;


public class SignUpActivity extends AppCompatActivity {
    TextInputEditText edtDisplayName, edtEmail, edtBirthday, edtPhoneNumber, edtPassword, edtConfirmPassword;
    // Make sure you have edtNationalId and edtAddress defined if they are in your XML
    TextInputEditText edtNationalId, edtAddress;

    AutoCompleteTextView autoGender;
    AppCompatButton btnAddAccount;

    ShapeableImageView imgAvatar, imgCardFront, imgCardBack;
    byte[] imgAvatarByte = null;
    byte[] imgCardFrontByte = null;
    byte[] imgCardBackByte = null;
    Uri imgAvatarUri = null;
    Uri imgCardFrontUri = null;
    Uri imgCardBackUri = null;
    private Uri photoUri = null; // This will be saved and restored
    Bitmap bitmapAvatar = null;
    Bitmap bitmapNationalId = null;
    String uid = null;
    private static final String CURRENT_IMAGE_TYPE_KEY = "currentImageTypeKey";
    // boolean imgAvatarChanged = false; // These seem unused, consider removing if not needed
    // boolean imgCardFrontChanged = false;
    // boolean imgCardBackChanged = false;

    CardFrontRes cardFrontData;
    User user;
    FrameLayout progressOverlay;
    AppCompatImageView imgBack;
    private ImageType currentImageType = ImageType.AVATAR;

    private static final String PHOTO_URI_KEY = "photoUri";
    private static final String TAG = "SignUpActivity";
    String displayName, email, birthday, phoneNumber, password, confirmPassword, nationalId, address, gender;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable(PHOTO_URI_KEY);
            if (savedInstanceState.containsKey(CURRENT_IMAGE_TYPE_KEY)) { // Check if key exists
                currentImageType = (ImageType) savedInstanceState.getSerializable(CURRENT_IMAGE_TYPE_KEY);
                Log.d(TAG, "Restored currentImageType: " + currentImageType);
            } else {
                Log.d(TAG, "currentImageTypeKey not found in savedInstanceState.");
            }
        }

        progressOverlay = findViewById(R.id.progress_overlay);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtEmail = findViewById(R.id.edtEmail);
        edtBirthday = findViewById(R.id.edtBirthday);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        autoGender = findViewById(R.id.autoGender);
        imgAvatar = findViewById(R.id.imgAvatar);
        imgBack = findViewById(R.id.imgBack);
        imgCardFront = findViewById(R.id.imgCardFront);
        imgCardBack = findViewById(R.id.imgCardBack);
        progressOverlay = findViewById(R.id.progress_overlay);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        edtNationalId = findViewById(R.id.edtNationalId);
        edtAddress = findViewById(R.id.edtAddress);


        ActionUtils.setUpGenderPicker(autoGender);
        autoGender.setText("Nam", false); // Set default
        ActionUtils.setUpNotFutureDatePicker(edtBirthday);


        initClickEvents();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putParcelable(PHOTO_URI_KEY, photoUri);
        }
        outState.putSerializable(CURRENT_IMAGE_TYPE_KEY, currentImageType); // Save currentImageType
        Log.d(TAG, "Saved currentImageType: " + currentImageType);
    }

    private void initClickEvents() {
        onBackClick();
        onImgAvatarClick();
        onImgCardFrontClick();
        onImgCardBackClick();
        onSubmitClick();
    }

    private void showToastAndStopProgress(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
    }

    private boolean performInitialClientSideValidations() {
        displayName = Objects.requireNonNull(edtDisplayName.getText()).toString().trim();
        email = Objects.requireNonNull(edtEmail.getText()).toString().trim();
        birthday = Objects.requireNonNull(edtBirthday.getText()).toString().trim();
        phoneNumber = Objects.requireNonNull(edtPhoneNumber.getText()).toString().trim();
        password = Objects.requireNonNull(edtPassword.getText()).toString().trim();
        confirmPassword = Objects.requireNonNull(edtConfirmPassword.getText()).toString().trim();
        address = Objects.requireNonNull(edtAddress.getText()).toString().trim();
        gender = autoGender.getText().toString();
        nationalId = Objects.requireNonNull(edtNationalId.getText()).toString().trim();

        if (displayName.isEmpty()) { showToastAndStopProgress("Vui lòng nhập tên hiển thị"); return false; }
        if (email.isEmpty()) { showToastAndStopProgress("Vui lòng nhập email"); return false; }
        if (birthday.isEmpty()) { showToastAndStopProgress("Vui lòng nhập ngày sinh"); return false; }
        if (phoneNumber.isEmpty()) { showToastAndStopProgress("Vui lòng nhập số điện thoại"); return false; }
        if (password.isEmpty()) { showToastAndStopProgress("Vui lòng nhập mật khẩu"); return false; }
        if (confirmPassword.isEmpty()) { showToastAndStopProgress("Vui lòng xác nhận mật khẩu"); return false; }
        if (nationalId.isEmpty()) { showToastAndStopProgress("Vui lòng nhập số CCCD"); return false; }
        if (address.isEmpty()) { showToastAndStopProgress("Vui lòng nhập địa chỉ"); return false; }

        if (password.length() < 6) { showToastAndStopProgress("Mật khẩu phải có ít nhất 6 ký tự"); return false; }
        if (!password.equals(confirmPassword)) { showToastAndStopProgress("Mật khẩu không khớp"); return false; }
        if (imgAvatarByte == null || bitmapAvatar == null) { showToastAndStopProgress("Vui lòng chụp ảnh đại diện"); return false; }
        if (imgCardFrontByte == null || bitmapNationalId == null) { showToastAndStopProgress("Vui lòng chụp mặt trước CCCD"); return false; }
        if (imgCardBackByte == null) { showToastAndStopProgress("Vui lòng chụp mặt sau CCCD"); return false; }
        if (phoneNumber.length() != 10) { showToastAndStopProgress("Số điện thoại phải có 10 chữ số"); return false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showToastAndStopProgress("Email không hợp lệ"); return false; }
        if (nationalId.length() != 12) { showToastAndStopProgress("Số CCCD phải có 12 chữ số"); return false; }

        if (cardFrontData == null || cardFrontData.getResult() == null || cardFrontData.getResult().get("nationalId") == null) {
            showToastAndStopProgress("Chưa nhận diện được thông tin từ mặt trước CCCD. Vui lòng chụp lại."); return false;
        }
        if (!Objects.equals(cardFrontData.getResult().get("nationalId"), nationalId)) {
            showToastAndStopProgress("Số CCCD nhập không khớp với thông tin trên ảnh CCCD."); return false;
        }
        return true;
    }

    private void onSubmitClick() {
        btnAddAccount.setOnClickListener(v -> {
            AudioEffectUtils.clickEffect(this);
            progressOverlay.setVisibility(View.VISIBLE);

            if (!performInitialClientSideValidations()) {
                // Progress overlay is hidden by performInitialClientSideValidations on failure
                return;
            }
            // Start the chain of asynchronous operations
            performFaceComparisonAndContinue();
        });
    }

    private void performFaceComparisonAndContinue() {
        FaceCompareUtils utils = new FaceCompareUtils();
        utils.compareFaces(bitmapAvatar, bitmapNationalId, this, new FaceCompareUtils.FaceCompareCallback() {
            @Override
            public void onMatch() {
                Log.d(TAG, "Face comparison: Match");
                checkExistingUserDataAndContinue();
            }

            @Override
            public void onMismatch() {
                Log.d(TAG, "Face comparison: Mismatch");
                showToastAndStopProgress("Khuôn mặt không trùng khớp với ảnh trên CCCD.");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Face comparison error: " + message);
                showToastAndStopProgress("Lỗi so sánh khuôn mặt: " + message);
            }
        });
    }

    private void checkExistingUserDataAndContinue() {
        Firestore.getAllUserBase(userList -> {
            if (userList == null) {
                showToastAndStopProgress("Không thể kiểm tra thông tin tài khoản. Vui lòng thử lại.");
                return;
            }
            for (User existingUser : userList) {
                if (existingUser.getEmail().equals(email)) {
                    showToastAndStopProgress("Email đã tồn tại.");
                    return;
                }
                if (existingUser.getPhone().equals(phoneNumber)) {
                    showToastAndStopProgress("Số điện thoại đã tồn tại.");
                    return;
                }
                if (existingUser.getNationalId().equals(nationalId)) {
                    showToastAndStopProgress("Số CCCD đã tồn tại.");
                    return;
                }
            }
            Log.d(TAG, "User data check: No duplicates found.");
            firebaseAuthSignUpAndContinue();
        });
    }

    private void firebaseAuthSignUpAndContinue() {
        FirebaseAuth.signUp(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uid = task.getResult(); // Assuming FirebaseAuth.signUp returns UID directly
                if (uid != null && !uid.isEmpty()) {
                    Log.d(TAG, "Firebase Auth SignUp successful. UID: " + uid);
                    createFirestoreUserAndContinue();
                } else {
                    Log.e(TAG, "Firebase Auth SignUp error: UID is null or empty.");
                    showToastAndStopProgress("Lỗi đăng ký: Không nhận được UID.");
                }
            } else {
                Log.e(TAG, "Firebase Auth SignUp failed", task.getException());
                showToastAndStopProgress("Đăng ký tài khoản thất bại: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    private void createFirestoreUserAndContinue() {
        // Note: Image URLs will be updated later by ImageUtils if it handles Firestore updates.
        // If ImageUtils only uploads, then we'd need to collect URLs and update user object here before Firestore.createUser
        // For now, assuming ImageUtils handles the Firestore field update for each image.
        user = new User(uid, displayName, email, phoneNumber, address,
                nationalId, "customer", gender.equals("Nam"), // Assuming "Nam" is the string for male
                convertStringToTimestamp(birthday), Timestamp.now(),new Biometric()
                ); // Biometric URLs will be set by ImageUtils

        Firestore.createUser(user, new Firestore.FirestoreAddCallback() {
            @Override
            public void onCallback(boolean isSuccess) {
                if (isSuccess) {
                    Log.d(TAG, "Firestore user creation successful.");
                    uploadImagesAndFinalize();
                } else {
                    Log.e(TAG, "Firestore user creation failed.");
                    showToastAndStopProgress("Lỗi tạo thông tin người dùng trong cơ sở dữ liệu.");
                    // Consider deleting the Firebase Auth user if Firestore creation fails
                    // FirebaseAuth.deleteCurrentUser();
                }
            }
        });
    }

    private void uploadImagesAndFinalize() {
        Log.d(TAG, "Starting image uploads.");
        // Assuming ImageUtils.uploadImage uploads the image AND updates the corresponding field in Firestore for the user.
        // The fieldPath like "biometricData.faceUrl" suggests this.

        ImageUtils.uploadImage("biometricData.faceUrl", "users", uid, imgAvatarUri, "faceImg/", new ImageUtils.ImageUploadCallback() {
            @Override
            public void onSuccess(String faceImageUrl) {
                Log.d(TAG, "Avatar uploaded successfully: " + faceImageUrl);
                ImageUtils.uploadImage("biometricData.cardFrontUrl", "users", uid, imgCardFrontUri, "cardFrontImg/", new ImageUtils.ImageUploadCallback() {
                    @Override
                    public void onSuccess(String cardFrontImageUrl) {
                        Log.d(TAG, "Card front uploaded successfully: " + cardFrontImageUrl);
                        ImageUtils.uploadImage("biometricData.cardBackUrl", "users", uid, imgCardBackUri, "cardBackImg/", new ImageUtils.ImageUploadCallback() {
                            @Override
                            public void onSuccess(String cardBackImageUrl) {
                                Log.d(TAG, "Card back uploaded successfully: " + cardBackImageUrl);
                                showToastAndStopProgress("Đăng ký thành công!");
                                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finishAffinity();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to upload card back: " + errorMessage);
                                showToastAndStopProgress("Tải lên ảnh mặt sau CCCD thất bại: " + errorMessage);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to upload card front: " + errorMessage);
                        showToastAndStopProgress("Tải lên ảnh mặt trước CCCD thất bại: " + errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to upload avatar: " + errorMessage);
                showToastAndStopProgress("Tải lên ảnh đại diện thất bại: " + errorMessage);
            }
        });
    }


    public static Timestamp convertStringToTimestamp(String dateString) {
        String[] patterns = {"dd/MM/yyyy", "d/M/yyyy", "d/MM/yyyy", "dd/M/yyyy"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
                Date date = dateFormat.parse(dateString);
                return new Timestamp(date);
            } catch (ParseException ignored) {
            }
        }

        // Nếu không parse được
        return null;
    }

    // Removed ToastMsgWhenSubit as it's replaced by showToastAndStopProgress

    private void onBackClick() {
        imgBack.setOnClickListener(v -> finish());
    }

    private void onImgAvatarClick() {
        imgAvatar.setOnClickListener(v -> {
            showImageOptionsDialog(imgAvatarUri, ImageType.AVATAR);
        });
    }

    private void onImgCardFrontClick() {
        imgCardFront.setOnClickListener(v -> {
            showImageOptionsDialog(imgCardFrontUri, ImageType.CARD_FRONT);
        });
    }


    private void onImgCardBackClick() {
        imgCardBack.setOnClickListener(v -> {
            showImageOptionsDialog(imgCardBackUri, ImageType.CARD_BACK);
        });
    }

    private void showImageOptionsDialog(Uri imageUriToShow, ImageType typeForCapture) {
        final CharSequence[] items;
        if (imageUriToShow != null) {
            items = new CharSequence[]{"Xem ảnh", "Chụp ảnh mới"};
        } else {
            items = new CharSequence[]{"Chụp ảnh"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, (dialog, which) -> {
            if (items[which].equals("Xem ảnh")) {
                showImageInDialog(imageUriToShow);
            } else { // "Chụp ảnh" or "Chụp ảnh mới"
                currentImageType = typeForCapture;
                launchCamera();
            }
        });
        builder.show();
    }


    private void showImageInDialog(Uri imgUri) {
        if (imgUri != null) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 40, 40, 40);

            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1000); // Consider making height dynamic or wrap_content
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            try {
                // Load bitmap and apply rotation if needed for display
                Bitmap displayBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imgUri);
                displayBitmap = rotateImageIfRequired(this, displayBitmap, imgUri); // Use the corrected method
                imageView.setImageBitmap(displayBitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image for dialog", e);
                imageView.setImageResource(R.drawable.ic_image_null); // Fallback
                Toast.makeText(this, "Không thể hiển thị ảnh", Toast.LENGTH_SHORT).show();
            }


            layout.addView(imageView);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            dialog.show();
        } else {
            Toast.makeText(this, "Chưa có ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private ActivityResultLauncher<Intent> myResultLuncherCamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Always reset orientation after camera returns
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); // Or your app's default


                if (result.getResultCode() == Activity.RESULT_OK) {

                    if (photoUri == null) {
                        Toast.makeText(SignUpActivity.this, "Lỗi: Không tìm thấy URI ảnh sau khi chụp.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "photoUri is null after camera result. This might happen if onSaveInstanceState is not working correctly or activity was unexpectedly killed.");
                        progressOverlay.setVisibility(View.GONE); // Hide progress if error
                        return;
                    }
                    try {
                        progressOverlay.setVisibility(View.VISIBLE);

                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                        // --- Apply EXIF Rotation Correction ---
                        bitmap = rotateImageIfRequired(this, bitmap, photoUri);
                        // --------------------------------------

                        if (bitmap == null) { // Check if bitmap is null after rotation attempt
                            showToastAndStopProgress("Không thể xử lý ảnh đã chụp.");
                            return;
                        }


                        if (currentImageType == ImageType.CARD_FRONT) { // Removed bitmap != null check as it's done above
                            Bitmap finalBitmap = bitmap;
                            OCRUtils.extractTextFromCardFront(bitmap, new OCRUtils.OcrCallback() {
                                @Override
                                public void onSuccess(Map<String, String> data) {
                                    cardFrontData = new CardFrontRes(data);
                                    Log.d(TAG, "OCR Result: " + cardFrontData.getResult());
                                    String ocrNationalId = cardFrontData.getResult().get("nationalId");
                                    if (ocrNationalId != null) {
                                        Toast.makeText(SignUpActivity.this, "Đã nhận diện CCCD: " + ocrNationalId, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(SignUpActivity.this, "Không nhận diện được số CCCD từ ảnh.", Toast.LENGTH_SHORT).show();
                                    }
                                    imgCardFront.setImageBitmap(finalBitmap);
                                    imgCardFrontUri = photoUri;
                                    imgCardFrontByte = FirebaseStorageManager.bitmapToByteArray(finalBitmap);
                                    bitmapNationalId = finalBitmap;
                                    progressOverlay.setVisibility(View.GONE);
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    Toast.makeText(SignUpActivity.this, "Lỗi OCR: " + errorMessage, Toast.LENGTH_LONG).show();
                                    imgCardFront.setImageResource(R.drawable.ic_image_null);
                                    cardFrontData = null; // Invalidate OCR data
                                    bitmapNationalId = null;
                                    imgCardFrontByte = null;
                                    progressOverlay.setVisibility(View.GONE);
                                }
                            });
                        } else if (currentImageType == ImageType.AVATAR) { // Removed bitmap != null check
                            Bitmap finalBitmap1 = bitmap;
                            FaceDetectionUtils.isFacePresent(bitmap, new FaceDetectionUtils.OnFaceDetectionListener() {
                                @Override
                                public void onFaceDetected(boolean isFacePresent) {
                                    progressOverlay.setVisibility(View.GONE);
                                    if (!isFacePresent) {
                                        Toast.makeText(SignUpActivity.this, "Không phát hiện khuôn mặt trong ảnh đại diện", Toast.LENGTH_SHORT).show();
                                        imgAvatar.setImageResource(R.drawable.ic_image_null);
                                        imgAvatarUri = null;
                                        imgAvatarByte = null;
                                        bitmapAvatar = null;
                                        return;
                                    }
                                    imgAvatar.setImageBitmap(finalBitmap1);
                                    imgAvatarUri = photoUri;
                                    imgAvatarByte = FirebaseStorageManager.bitmapToByteArray(finalBitmap1);
                                    bitmapAvatar = finalBitmap1;
                                }
                            });
                        } else if (currentImageType == ImageType.CARD_BACK) { // Removed bitmap != null check
                            imgCardBack.setImageBitmap(bitmap);
                            imgCardBackUri = photoUri;
                            imgCardBackByte = FirebaseStorageManager.bitmapToByteArray(bitmap);
                            progressOverlay.setVisibility(View.GONE);
                        } else {
                            progressOverlay.setVisibility(View.GONE); // Should not happen
                        }
                        // photoUri = null; // Optional: clear after successful processing
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing camera image", e);
                        Toast.makeText(SignUpActivity.this, "Không thể lấy ảnh từ camera", Toast.LENGTH_SHORT).show();
                        progressOverlay.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(SignUpActivity.this, "Chụp ảnh bị hủy", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
                    // photoUri = null; // Optional: clear if capture was cancelled
                }
            }
    );

    private static String orientationToString(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL: return "NORMAL (1)";
            case ExifInterface.ORIENTATION_ROTATE_90: return "ROTATE_90 (6)";
            case ExifInterface.ORIENTATION_ROTATE_180: return "ROTATE_180 (3)";
            case ExifInterface.ORIENTATION_ROTATE_270: return "ROTATE_270 (8)";
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: return "FLIP_HORIZONTAL (2)";
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: return "FLIP_VERTICAL (4)";
            case ExifInterface.ORIENTATION_TRANSPOSE: return "TRANSPOSE (5)"; // Rotated 90deg CW & flipped horizontally
            case ExifInterface.ORIENTATION_TRANSVERSE: return "TRANSVERSE (7)"; // Rotated 90deg CW & flipped vertically
            case ExifInterface.ORIENTATION_UNDEFINED: return "UNDEFINED (0)";
            default: return "Unknown (" + orientation + ")";
        }
    }
    public static Bitmap rotateImageIfRequired(android.content.Context context, Bitmap img, Uri selectedImage) throws IOException {
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
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
                Log.d(TAG, "rotateImageIfRequired: Using ExifInterface (InputStream) for API " + Build.VERSION.SDK_INT);
                ei = new ExifInterface(input);
            } else {
                Log.d(TAG, "rotateImageIfRequired: Using ExifInterface (path) for API " + Build.VERSION.SDK_INT);
                String path = ActionUtils.getRealPathFromURI(context, selectedImage);
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
                // Handle other cases like flip + rotate if necessary, e.g., TRANSPOSE, TRANSVERSE
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    Log.d(TAG, "rotateImageIfRequired: Flipping horizontal");
                    rotatedBitmap = flipImage(img, true, false);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    Log.d(TAG, "rotateImageIfRequired: Flipping vertical");
                    rotatedBitmap = flipImage(img, false, true);
                    break;
                // Example for TRANSPOSE (might need adjustment depending on desired outcome)
                // case ExifInterface.ORIENTATION_TRANSPOSE: // Rotate 90 and flip horizontal
                //    Log.d(TAG, "rotateImageIfRequired: Transposing (Rotate 90 + Flip Horizontal)");
                //    rotatedBitmap = rotateImage(img, 90);
                //    rotatedBitmap = flipImage(rotatedBitmap, true, false);
                //    break;
                // case ExifInterface.ORIENTATION_TRANSVERSE: // Rotate 270 and flip horizontal (or rotate 90 and flip vertical)
                //    Log.d(TAG, "rotateImageIfRequired: Transversing (Rotate 270 + Flip Horizontal)");
                //    rotatedBitmap = rotateImage(img, 270);
                //    rotatedBitmap = flipImage(rotatedBitmap, true, false);
                //    break;
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

    // Add this flipImage helper method if you need to handle ORIENTATION_FLIP_*
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

    // rotateImage method remains the same
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    // Lấy ImageView tương ứng với loại ảnh đang xử lý
    private ImageView getCurrentImageView() {
        switch (currentImageType) {
            case AVATAR:
                return imgAvatar;
            case CARD_FRONT:
                return imgCardFront;
            case CARD_BACK:
                return imgCardBack;
            default:
                Log.w(TAG, "Unknown currentImageType: " + currentImageType);
                return null; // Or a default
        }
    }


    private void launchCamera() {
        String message = "";
        int targetOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        switch (currentImageType) {
            case AVATAR:
                message = "Giữ điện thoại theo chiều dọc để chụp ảnh đại diện.";
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case CARD_FRONT:
            case CARD_BACK:
                message = "Giữ điện thoại theo chiều ngang để chụp ảnh CCCD.";
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        int finalOrientation = targetOrientation;
        builder.setTitle("Hướng dẫn chụp ảnh")
                .setMessage(message)
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    File imageFile;
                    try {
                        // Create a unique file name
                        String timeStamp = String.valueOf(System.currentTimeMillis());
                        String imageFileName = "JPEG_" + timeStamp + "_";
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        imageFile = File.createTempFile(
                                imageFileName,  /* prefix */
                                ".jpg",         /* suffix */
                                storageDir      /* directory */
                        );
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating image file", e);
                        Toast.makeText(this, "Không thể tạo file ảnh", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // photoUri is now class-level and will be saved/restored
                    photoUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", // Make sure this matches your provider authorities
                            imageFile);

                    Log.d(TAG, "Photo URI set: " + photoUri);


                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                    // Important: Set requested orientation *before* launching camera
                    // The activity might be recreated. photoUri needs to be saved in onSaveInstanceState
                    if (finalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        setRequestedOrientation(finalOrientation);
                    }
                    myResultLuncherCamera.launch(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // If user cancels, reset orientation if it was locked
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); // Or your app's default
                })
                .setOnCancelListener(dialogInterface -> {
                    // Also reset if dialog is cancelled (e.g. back press)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); // Or your app's default
                })
                .show();
    }
}

// enum ImageType remains the same
enum ImageType {
    AVATAR,
    CARD_FRONT,
    CARD_BACK
}


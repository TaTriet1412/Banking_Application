package com.example.bankingapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bankingapplication.Firebase.FirebaseStorageManager;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.ActionUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;


public class SignUpActivity extends AppCompatActivity {
    TextInputEditText edtDisplayName, edtEmail, edtBirthday, edtPhoneNumber, edtPassword, edtConfirmPassword;

    AutoCompleteTextView autoGender;

    ShapeableImageView imgAvatar, imgCardFront, imgCardBack;
    byte[] imgAvatarByte = null;
    byte[] imgCardFrontByte = null;
    byte[] imgCardBackByte = null;
    Uri imgAvatarUri = null;
    Uri imgCardFrontUri = null;
    Uri imgCardBackUri = null;
    boolean imgAvatarChanged = false;
    boolean imgCardFrontChanged = false;
    boolean imgCardBackChanged = false;
    User user;
    FrameLayout progressOverlay;
    AppCompatImageView imgBack;
    private ImageType currentImageType = ImageType.AVATAR;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
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


        ActionUtils.setUpGenderPicker(autoGender);
        ActionUtils.setUpNotFutureDatePicker(edtBirthday);

        initClickEvents();
    }

    private void initClickEvents() {
        onBackClick();
        onImgAvatarClick();
        onImgCardFrontClick();
        onImgCardBackClick();
    }

    private void onBackClick() {
        imgBack.setOnClickListener(v -> finish());
    }

    private void onImgAvatarClick() {
        imgAvatar.setOnClickListener(v -> {
            // Tạo một AlertDialog với 2 lựa chọn: Xem ảnh và Chụp ảnh
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setItems(new String[]{"Xem ảnh", "Chụp ảnh"}, (dialog, which) -> {
                if (which == 0) {
                    // Nếu chọn "Xem ảnh", hiển thị ảnh trong dialog
                    showImageInDialog(imgAvatarUri);
                } else if (which == 1) {
                    // Nếu chọn "Chụp ảnh", thực hiện chức năng chụp ảnh
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    myResultLuncherCamera.launch(intent);
                }
            });

            builder.show();  // Hiển thị AlertDialog
        });
    }

    private void onImgCardFrontClick() {
        imgAvatar.setOnClickListener(v -> {
            // Tạo một AlertDialog với 2 lựa chọn: Xem ảnh và Chụp ảnh
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setItems(new String[]{"Xem ảnh", "Chụp ảnh"}, (dialog, which) -> {
                if (which == 0) {
                    // Nếu chọn "Xem ảnh", hiển thị ảnh trong dialog
                    showImageInDialog(imgCardFrontUri);
                } else if (which == 1) {
                    // Nếu chọn "Chụp ảnh", thực hiện chức năng chụp ảnh
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    myResultLuncherCamera.launch(intent);
                }
            });

            builder.show();  // Hiển thị AlertDialog
        });
    }


    private void onImgCardBackClick() {
        imgAvatar.setOnClickListener(v -> {
            // Tạo một AlertDialog với 2 lựa chọn: Xem ảnh và Chụp ảnh
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setItems(new String[]{"Xem ảnh", "Chụp ảnh"}, (dialog, which) -> {
                if (which == 0) {
                    // Nếu chọn "Xem ảnh", hiển thị ảnh trong dialog
                    showImageInDialog(imgCardBackUri);
                } else if (which == 1) {
                    // Nếu chọn "Chụp ảnh", thực hiện chức năng chụp ảnh
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    myResultLuncherCamera.launch(intent);
                }
            });

            builder.show();  // Hiển thị AlertDialog
        });
    }



    private void showImageInDialog(Uri imgUri) {
        if (imgUri != null) {
            // Tạo một Layout tùy chỉnh chứa ImageView
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 40, 40, 40);

            // Tạo ImageView để hiển thị ảnh
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1000);
            params.weight = 1;  // Đảm bảo hình ảnh có thể co giãn theo chiều cao của layout
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Hiển thị ảnh từ imgAvatarUri trong ImageView
            imageView.setImageURI(imgUri);

            // Thêm ImageView vào layout
            layout.addView(imageView);

            // Tạo một AlertDialog với layout tùy chỉnh này
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);  // Đặt layout vào trong dialog

            // Hiển thị dialog
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);  // Làm nền trong suốt
            dialog.show();
        } else {
            // Nếu không có ảnh, hiển thị thông báo
            Toast.makeText(this, "Chưa có ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // Chụp ảnh từ camera
    private ActivityResultLauncher<Intent> myResultLuncherCamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Xử lý ảnh sau khi chụp
                        handleCapturedImage(result.getData(), getCurrentImageView(), getCurrentImageUri(), getCurrentImageByte());
                    }
                }
            }
    );

    private void handleCapturedImage(Intent data, ImageView imageView, Uri[] imgUri, byte[][] imgByte) {
        // Nhận thumbnail ảnh từ camera
        if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");

            // Cập nhật ImageView với ảnh vừa chụp
            imageView.setImageBitmap(imageBitmap);

            // Chuyển Bitmap sang byte[]
            imgByte[0] = FirebaseStorageManager.bitmapToByteArray(imageBitmap);

            // Chuyển Bitmap sang Uri nếu cần thiết
            imgUri[0] = getImageUriFromBitmap(imageBitmap);

            // Cập nhật các giá trị khác tùy thuộc vào ảnh đang xử lý
            switch (currentImageType) {
                case AVATAR:
                    imgAvatarUri = imgUri[0];
                    imgAvatarByte = imgByte[0];
                    break;
                case CARD_FRONT:
                    imgCardFrontUri = imgUri[0];
                    imgCardFrontByte = imgByte[0];
                    break;
                case CARD_BACK:
                    imgCardBackUri = imgUri[0];
                    imgCardBackByte = imgByte[0];
                    break;
            }
        }
    }

    // Phương thức chuyển Bitmap thành Uri
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        // Chuyển Bitmap thành tệp tạm thời và lấy URI
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "avatar", null);
        return Uri.parse(path);
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
                return imgAvatar;  // Default là avatar
        }
    }

    // Lấy Uri tương ứng với loại ảnh đang xử lý
    private Uri[] getCurrentImageUri() {
        switch (currentImageType) {
            case AVATAR:
                return new Uri[]{imgAvatarUri};
            case CARD_FRONT:
                return new Uri[]{imgCardFrontUri};
            case CARD_BACK:
                return new Uri[]{imgCardBackUri};
            default:
                return new Uri[]{imgAvatarUri};  // Default là avatar
        }
    }

    // Lấy byte[] tương ứng với loại ảnh đang xử lý
    private byte[][] getCurrentImageByte() {
        switch (currentImageType) {
            case AVATAR:
                return new byte[][]{imgAvatarByte};
            case CARD_FRONT:
                return new byte[][]{imgCardFrontByte};
            case CARD_BACK:
                return new byte[][]{imgCardBackByte};
            default:
                return new byte[][]{imgAvatarByte};  // Default là avatar
        }
    }

}

enum ImageType {
    AVATAR,
    CARD_FRONT,
    CARD_BACK
}


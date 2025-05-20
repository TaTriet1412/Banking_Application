package com.example.bankingapplication; // Your package name

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.AppCompatButton;

import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.bankingapplication.Api.VeriphoneApi;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RechargePhoneActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private GridLayout gridLayoutAmounts;
    private AppCompatButton btnContinue;
    private TextInputEditText edtPhoneNumber;
    private ImageView ivCopy;

    private List<AppCompatButton> amountButtons = new ArrayList<>();
    private AppCompatButton selectedAmountButton = null;
    private String selectedAmountValue = "";
    FrameLayout progressOverlay;


    private final String[] amounts = {
            "30.000 VND", "50.000 VND",
            "100.000 VND", "200.000 VND",
            "300.000 VND", "500.000 VND"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge_phone);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        gridLayoutAmounts = findViewById(R.id.gridLayoutAmounts);
        btnContinue = findViewById(R.id.btnContinue);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        ivCopy = findViewById(R.id.ivCopy);
        progressOverlay = findViewById(R.id.progress_overlay);

        populateAmountButtons();
        setDefaultSelectedAmount();

        ivCopy.setOnClickListener(v -> copyPhoneNumberToClipboard());

        continueClick();
    }

    private void continueClick() {
        btnContinue.setOnClickListener(v -> {
            progressOverlay.setVisibility(View.VISIBLE);
            btnContinue.setEnabled(false);

            if (selectedAmountButton != null && !selectedAmountValue.isEmpty()) {
                String phoneNumber = Objects.requireNonNull(edtPhoneNumber.getText()).toString().trim();

                if (!phoneNumber.matches("^[0-9]{10,11}$")) {
                    Toast.makeText(this, "Số điện thoại không đúng định dạng!", Toast.LENGTH_SHORT).show();
                    progressOverlay.setVisibility(View.GONE);
                    btnContinue.setEnabled(true);
                    return;
                }

                VeriphoneApi.checkPhoneNumberWithVeriphone(phoneNumber, carrier -> {
                    runOnUiThread(() -> {
                        if (carrier != null) {
                            // Nếu hợp lệ, tiếp tục chuyển màn hình
                            progressOverlay.setVisibility(View.GONE);
                            btnContinue.setEnabled(true);
                            Intent intent = new Intent(RechargePhoneActivity.this, ConfirmRechargePhoneActivity.class);
                            intent.putExtra("phoneNumber", phoneNumber);
                            intent.putExtra("amount", selectedAmountValue);
                            intent.putExtra("carrier", carrier); // Gửi luôn tên nhà mạng nếu cần
                            startActivity(intent);
                        } else {
                            // Nếu không hợp lệ, hiển thị thông báo
                            progressOverlay.setVisibility(View.GONE);
                            btnContinue.setEnabled(true);
                            Toast.makeText(RechargePhoneActivity.this, "Số điện thoại không hợp lệ hoặc không rõ nhà mạng", Toast.LENGTH_SHORT).show();
                        }
                    });
                });

            } else {
                // Nếu không có số tiền nào được chọn, hiển thị thông báo
                progressOverlay.setVisibility(View.GONE);
                btnContinue.setEnabled(true);
                Toast.makeText(RechargePhoneActivity.this, "Vui lòng chọn số tiền", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Check phone number validity

    private void populateAmountButtons() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        // Padding of the parent ConstraintLayout
        int parentConstraintLayoutPadding = dpToPx(16); // Assuming 16dp on each side
        int totalHorizontalParentPadding = parentConstraintLayoutPadding * 2;

        // Margins for buttons: each button has 4dp left and 4dp right.
        // For two buttons, this means: M B1 M | M B2 M
        // The space taken by margins around the buttons:
        int buttonHorizontalMargin = dpToPx(4);
        int totalMarginSpaceForTwoButtons = buttonHorizontalMargin * 4; // 4dp * 4 edges involved

        // A simpler way for width:
        // The GridLayout itself is constrained to parent with 16dp padding on CL.
        // GridLayout has columnCount=2. The buttons have margins.
        // Total width available for GridLayout content area: screenWidth - totalHorizontalParentPadding
        // Width for each button, considering their own margins:
        // ( (screenWidth - totalHorizontalParentPadding) - (margin_left_b1 + margin_right_b1 + margin_left_b2 + margin_right_b2) ) / 2
        // No, easier:
        int availableWidthForGridContent = screenWidth - totalHorizontalParentPadding;
        // Let buttonWidth be the width *before* its own margins are applied by GridLayout.
        // Each button will have left+right margin of dpToPx(4)+dpToPx(4) = dpToPx(8)
        // So for two buttons, total space for button content + their margins is:
        // 2 * (buttonWidth + dpToPx(8)) = availableWidthForGridContent
        // buttonWidth = (availableWidthForGridContent / 2) - dpToPx(8)
        // This makes buttonWidth the content width. params.width is the full width.
        // Let's use the previous simpler calculation and see, as the GridLayout should handle margins.
        int interButtonHorizontalSpacing = dpToPx(8) + dpToPx(8); // right margin of first + left margin of second
        int buttonWidth = (availableWidthForGridContent - interButtonHorizontalSpacing) / 2;


        int horizontalPaddingInDp = 16;
        int verticalPaddingInDp = 12;

        for (String amountText : amounts) {
            AppCompatButton button = new AppCompatButton(this);
            button.setText(amountText);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            button.setAllCaps(false);

            button.setPadding(
                    dpToPx(horizontalPaddingInDp),
                    dpToPx(verticalPaddingInDp),
                    dpToPx(horizontalPaddingInDp),
                    dpToPx(verticalPaddingInDp)
            );

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = buttonWidth;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            // Margins applied to each button
            params.setMargins(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));

            // *** KEY CHANGE: Remove weights ***
            // Let the GridLayout distribute based on columnCount and button's explicit width & margins.
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED); // No weight
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);   // No weight

            button.setLayoutParams(params);
            setButtonState(button, false, amountText);

            button.setOnClickListener(v -> {
                handleAmountButtonClick((AppCompatButton) v, amountText);
            });

            amountButtons.add(button);
            gridLayoutAmounts.addView(button);
        }
    }

    private void setDefaultSelectedAmount() {
        if (!amountButtons.isEmpty()) {
            AppCompatButton firstButton = amountButtons.get(0);
            handleAmountButtonClick(firstButton, amounts[0]);
        }
    }

    private void handleAmountButtonClick(AppCompatButton clickedButton, String amountValue) {
        if (selectedAmountButton != null && selectedAmountButton != clickedButton) {
            setButtonState(selectedAmountButton, false, selectedAmountButton.getText().toString());
        }
        setButtonState(clickedButton, true, amountValue);
        selectedAmountButton = clickedButton;
        // Loại bỏ dấu phẩy và chữ 'VND'
        selectedAmountValue = amountValue.replace(".", "").replace(" VND", "");
    }


    private void setButtonState(AppCompatButton button, boolean isSelected, String amountText) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.bg_btn_gradient_green_vertical);
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            button.setBackgroundResource(R.drawable.rounded_corner_white_bordered);
            button.setTextColor(ContextCompat.getColor(this, R.color.text_color_primary));
        }
    }

    private void copyPhoneNumberToClipboard() {
        if(edtPhoneNumber.getText() == null || edtPhoneNumber.getText().toString().isEmpty()) {
            Toast.makeText(this, "Chưa tồn tại số điện thoại!", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Phone Number", Objects.requireNonNull(edtPhoneNumber.getText()).toString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép số điện thoại!", Toast.LENGTH_SHORT).show();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
package com.example.bankingapplication.Utils;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.example.bankingapplication.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class ActionUtils {
    public static final String[] genders = {"Nam", "Nữ"};
    public static final String[] roles = {"Nhân viên", "Người dùng"};

    @SuppressLint("UseCompatLoadingForDrawables")
    public static void setUpGenderPicker(AutoCompleteTextView autoGender) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(autoGender.getContext(), android.R.layout.simple_list_item_1, genders);
        autoGender.setAdapter(adapter);
        autoGender.setDropDownBackgroundDrawable(autoGender.getContext().getDrawable(R.drawable.bg_dropdown));
        autoGender.setOnClickListener(v -> autoGender.showDropDown());
    }

    public static void setUpNotFutureDatePicker(TextInputEditText textInputEditText) {
        textInputEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(textInputEditText.getContext(), (view, selectedYear, selectedMonth, selectedDay) -> {
                String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                textInputEditText.setText(date);
            }, year, month, day);

            datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());  // Đặt max date là ngày hiện tại

            datePickerDialog.show();
        });
    }
}

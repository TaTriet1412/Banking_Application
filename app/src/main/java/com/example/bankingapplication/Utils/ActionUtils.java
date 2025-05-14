package com.example.bankingapplication.Utils;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.util.Log; // Import Log

import com.example.bankingapplication.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class ActionUtils {
    public static final String[] genders = {"Nam", "Nữ"};
    public static final String[] roles = {"Nhân viên", "Người dùng"};
    private static final String TAG = "ActionUtils"; // For logging

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

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getRealPathFromURI(final Context context, final Uri uri) {
        Log.d(TAG, "Attempting to get real path for URI: " + uri);

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            Log.d(TAG, "URI is DocumentURI");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                Log.d(TAG, "URI is ExternalStorageDocument");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if ("home".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/Documents/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                Log.d(TAG, "URI is DownloadsDocument");
                try {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id != null && id.startsWith("raw:")) {
                        return id.substring(4);
                    }

                    String[] contentUriPrefixesToTry = new String[]{
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads",
                            "content://downloads/all_downloads"
                    };

                    for (String contentUriPrefix : contentUriPrefixesToTry) {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
                        try {
                            String path = getDataColumn(context, contentUri, null, null);
                            if (path != null) {
                                return path;
                            }
                        } catch (Exception e) {
                            // Ignore and try next prefix
                            Log.w(TAG, "Failed to get path with prefix " + contentUriPrefix + ": " + e.getMessage());
                        }
                    }
                    // If you reach here, it means all attempts failed.
                    Log.e(TAG, "Could not retrieve path for DownloadsDocument URI: " + uri);
                    return null;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "DownloadsDocument URI id is not a valid long: " + DocumentsContract.getDocumentId(uri), e);
                    return null;
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                Log.d(TAG, "URI is MediaDocument");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "URI is ContentScheme");
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "URI is FileScheme");
            return uri.getPath();
        }
        Log.w(TAG, "Unable to get path for URI: " + uri);
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA; // Can be MediaStore.MediaColumns.DATA for general use
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                String path = cursor.getString(columnIndex);
                Log.d(TAG, "getDataColumn successful, path: " + path);
                return path;
            }
        } catch (IllegalArgumentException e) {
            // This can happen if the URI is malformed or the column doesn't exist for this URI
            Log.e(TAG, "getDataColumn failed for URI: " + uri, e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        Log.w(TAG, "getDataColumn failed to find path for URI: " + uri);
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
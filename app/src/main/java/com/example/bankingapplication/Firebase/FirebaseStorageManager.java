package com.example.bankingapplication.Firebase;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FirebaseStorageManager {
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final StorageReference storageRef = storage.getReference();

    public static Task<Boolean> uploadFile(Uri file, String fileName, String folderPath, int timeoutSeconds) {
        // input: Uri file, tên file fileName, folderPath: đường dẫn thư mục chứa file dạng "folder1/folder2/"
        // chú ý: Uri file là đường dẫn đến file trên thiết bị
        // fileName phải được tùy chỉnh trước để tránh trùng lặp
        // output: Task<Boolean> trả về true nếu upload thành công, false nếu thất bại hoặc quá thời gian quy định timeoutSeconds

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        // taọ tham chiếu đến file đã chọn trong thư mục trên Firebase Storage
        StorageReference riversRef = storageRef.child(folderPath + fileName);
        // tạo và bắt đầu task upload
        UploadTask uploadTask = riversRef.putFile(file);

        // Sử dụng ScheduledExecutorService để lên lịch timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] taskCompleted = {false};

        // Tạo một task timeout để đặt kết quả là false nếu upload quá thời gian quy định
        scheduler.schedule(() -> {
            if (!taskCompleted[0]) {
                Log.d("FirebaseStorageManager", "Upload timeout");
                taskCompletionSource.trySetResult(false);  // Đặt kết quả nếu chưa hoàn thành
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // Đăng ký sự kiện lắng nghe kết quả upload
        uploadTask.addOnFailureListener(exception -> {
            Log.d("FirebaseStorageManager", "Upload failed");
            taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
            taskCompletionSource.setResult(false);
        }).addOnSuccessListener(taskSnapshot -> {
            Log.d("FirebaseStorageManager", "Upload success");
            taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
            taskCompletionSource.setResult(true);
        });

        return taskCompletionSource.getTask();
    }

    // chuyển đổi file ảnh thành mảng byte
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
    // upload mảng byte lên Firebase Storage
    public static Task<Boolean> upLoadByteArray(byte[] data, String fileName, String folderPath, int timeoutSeconds) {
        // input: mảng byte data, tên file fileName, folderPath: đường dẫn thư mục chứa file dạng "folder1/folder2/"
        // chú ý: fileName phải được tùy chỉnh trước để tránh trùng lặp
        // output: Task<Boolean> trả về true nếu upload thành công, false nếu thất bại

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        // tạo tham chiếu đến file đã chọn trong thư mục trên Firebase Storage
        StorageReference avatarRef = storageRef.child(folderPath + fileName);
        // tạo và bắt đầu task upload
        UploadTask uploadTask = avatarRef.putBytes(data);

        // Sử dụng ScheduledExecutorService để lên lịch timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] taskCompleted = {false};

        // Tạo một task timeout để đặt kết quả là false nếu upload quá thời gian quy định
        scheduler.schedule(() -> {
            if (!taskCompleted[0]) {
                Log.d("FirebaseStorageManager", "Upload timeout");
                taskCompletionSource.trySetResult(false);  // Đặt kết quả nếu chưa hoàn thành
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // đăng trình nghe sự kiện upload
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("FirebaseStorageManager", "Upload failed");
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() chứa metadata của file đã upload như size, content-type, ...
                Log.d("FirebaseStorageManager", "Upload success");
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(true);
            }
        });
        return taskCompletionSource.getTask();
    }

    // Kiểm tra file có tồn tại trên Firebase Storage hay không
    public static Task<Boolean> checkFileExist(String fileName, String folderPath, int timeoutSeconds) {
        // input: tên file fileName, folderPath: đường dẫn thư mục chứa file dạng "folder1/folder2/"
        // output: Task<Boolean> trả về true nếu file tồn tại, false nếu không tồn tại

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        // tạo tham chiếu đến file đã chọn trong thư mục trên Firebase Storage
        StorageReference avatarRef = storageRef.child(folderPath + fileName);

        // Sử dụng ScheduledExecutorService để lên lịch timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] taskCompleted = {false};

        // Tạo một task timeout để đặt kết quả là false nếu upload quá thời gian quy định
        scheduler.schedule(() -> {
            if (!taskCompleted[0]) {
                Log.d("FirebaseStorageManager", "Check file exist timeout");
                taskCompletionSource.trySetResult(false);  // Đặt kết quả nếu chưa hoàn thành
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // kiểm tra file tồn tại
        avatarRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(false);
            }
        });
        return taskCompletionSource.getTask();
    }

    // Xóa file trên Firebase Storage
    public static Task<Boolean> deleteFile(String fileName, String folderPath, int timeoutSeconds) {
        // input: tên file fileName, folderPath: đường dẫn thư mục chứa file dạng "folder1/folder2/"
        // output: Task<Boolean> trả về true nếu xóa thành công, false nếu thất bại

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        // tạo tham chiếu đến file đã chọn trong thư mục trên Firebase Storage
        StorageReference avatarRef = storageRef.child(folderPath + fileName);

        // Sử dụng ScheduledExecutorService để lên lịch timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] taskCompleted = {false};

        // Tạo một task timeout để đặt kết quả là false nếu upload quá thời gian quy định
        scheduler.schedule(() -> {
            if (!taskCompleted[0]) {
                Log.d("FirebaseStorageManager", "Upload timeout");
                taskCompletionSource.trySetResult(false);  // Đặt kết quả nếu chưa hoàn thành
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // xóa file
        avatarRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(false);
            }
        });
        return taskCompletionSource.getTask();
    }

    // download file từ Firebase Storage về RAM dưới dạng mảng byte
    public interface DownloadFileCallback {
        void onCallback(byte[] bytes);
    }
    public static void downloadFile(String fileName, String folderPath, int timeoutSeconds, DownloadFileCallback callback) {
        // input: tên file fileName, folderPath: đường dẫn thư mục chứa file dạng "folder1/folder2/"
        // output: mảng byte chứa dữ liệu file, null nếu thất bại hoặc quá thời gian quy định timeoutSeconds

        // tạo tham chiếu đến file đã chọn trong thư mục trên Firebase Storage
        StorageReference avatarRef = storageRef.child(folderPath + fileName);

        // Sử dụng ScheduledExecutorService để lên lịch timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] taskCompleted = {false};

        // Tạo một task timeout để đặt kết quả là false nếu upload quá thời gian quy định
        scheduler.schedule(() -> {
            if (!taskCompleted[0]) {
                Log.d("FirebaseStorageManager", "Download timeout");
                callback.onCallback(null);  // Đặt kết quả nếu chưa hoàn thành
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // download file
        final long ONE_MEGABYTE = 1024 * 1024 * 5; // kich thước file tối đa có thể download
        avatarRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Log.d("FirebaseStorageManager", "Download success");
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                callback.onCallback(bytes);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("FirebaseStorageManager", "Download failed: " + e.getMessage());
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                callback.onCallback(null);
            }
        });
    }

    // download file từ Firebase Storage về bộ nhớ vật lý dưới dạng file
    public static Task<Boolean> downloadFileToPhysicalMemory(String fileName, String folderPath, String filePath, int timeoutSeconds) {
        // input: tên file fileName, folderPath: đường dẫn thư mục chứa file dạng "folder1/folder2/",
        // filePath: đường dẫn lưu file trên thiết bị (ví dụ: "file:///storage/emulated/0/Download/test.jpg")

        // output: Task<Boolean> trả về true nếu download thành công, false nếu thất bại

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        // tạo tham chiếu đến file đã chọn trong thư mục trên Firebase Storage
        StorageReference avatarRef = storageRef.child(folderPath + fileName);

        // tạo file trên thiết bị để lưu file download từ filePath
        File localFile = new File(filePath);

        if (localFile.exists()) {
            Log.d("FirebaseStorageManager", "File exists");
            taskCompletionSource.setResult(false); // trả về false nếu file đã tồn tại
        } else {
            System.out.println("File does not exist");
        }

        // Sử dụng ScheduledExecutorService để lên lịch timeout
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] taskCompleted = {false};

        // Tạo một task timeout để đặt kết quả là false nếu upload quá thời gian quy định
        scheduler.schedule(() -> {
            if (!taskCompleted[0]) {
                Log.d("FirebaseStorageManager", "Download timeout");
                taskCompletionSource.trySetResult(false);  // Đặt kết quả nếu chưa hoàn thành
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // download file
        avatarRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("FirebaseStorageManager", "Download failed: " + e.getMessage());
                taskCompleted[0] = true;  // Đánh dấu task đã hoàn thành
                taskCompletionSource.setResult(false);
            }
        });
        return taskCompletionSource.getTask();
    }
}

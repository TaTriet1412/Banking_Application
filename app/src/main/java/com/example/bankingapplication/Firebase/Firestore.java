package com.example.bankingapplication.Firebase;

import android.util.Log;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.Bill;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Firestore {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Interface để truyền kết quả trả về từ Firestore (true: thành công, false: thất bại)
    public interface FirestoreGetUserCallback {
        void onCallback(User user);
    }

    public static void getUser(String UID, FirestoreGetUserCallback callback) {
        db.collection("users")
                .document(UID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        assert user != null;
                        user.setUID(UID);
                        callback.onCallback(user);
                    } else {
                        // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                    callback.onCallback(null);
                });
    }

    public interface FirestoreAddCallback {
        void onCallback(boolean isSuccess);
    }

    public static String generateId(String prefix) {
        return prefix + System.currentTimeMillis() + (int) (Math.random() * 100);
    }


    public static void updateImgUrlForDocument(String fieldUrlName, String collectionName, String document, String imgUrl, FirestoreAddCallback callback) {
        // Get document reference
        DocumentReference docRef = db.collection(collectionName).document(document);

        if (fieldUrlName.contains(".")) {
            // Handle nested fields
            String[] keys = fieldUrlName.split("\\.");
            String parentField = keys[0];
            String childField = keys[1];

            // First get the current document
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    Map<String, Object> updateData = new HashMap<>();

                    // Get existing parent map or create new one
                    Map<String, Object> parentMap = new HashMap<>();
                    if (documentSnapshot.exists() && documentSnapshot.contains(parentField)) {
                        Object existingData = documentSnapshot.get(parentField);
                        if (existingData instanceof Map) {
                            // Cast and preserve all existing fields
                            parentMap = new HashMap<>((Map<String, Object>) existingData);
                        }
                    }

                    // Add/update the specific field within the parent map
                    parentMap.put(childField, imgUrl);

                    // Prepare the update data
                    updateData.put(parentField, parentMap);

                    // Update document
                    docRef.update(updateData)
                            .addOnSuccessListener(aVoid -> callback.onCallback(true))
                            .addOnFailureListener(e -> {
                                System.out.println("Error updating document: " + e.getMessage());
                                callback.onCallback(false);
                            });
                } else {
                    System.out.println("Error getting document: " +
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    callback.onCallback(false);
                }
            });
        } else {
            // Simple field update (no nested fields)
            Map<String, Object> data = new HashMap<>();
            data.put(fieldUrlName, imgUrl);

            docRef.update(data)
                    .addOnSuccessListener(aVoid -> callback.onCallback(true))
                    .addOnFailureListener(e -> callback.onCallback(false));
        }
    }
    private static Map<String, Object> buildNestedMap(String[] keys, Object value) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> current = result;

        for (int i = 0; i < keys.length - 1; i++) {
            Map<String, Object> next = new HashMap<>();
            current.put(keys[i], next);
            current = next;
        }

        current.put(keys[keys.length - 1], value);
        return result;
    }

    public interface FirestoreGetAllUserBaseCallBack {
        void onCallback(List<User> userList);
    }

    public static void getAllUserBase(FirestoreGetAllUserBaseCallBack callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> userList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String nationalId = document.getString("nationalId");
                        userList.add(new User(email, phone, nationalId));
                    }
                    callback.onCallback(userList);
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi nếu cần
                    Log.e("Firestore", "Error getting documents: ", e);
                    callback.onCallback(null);
                });
    }



    public static void createUser(User user, FirestoreAddCallback callback) {
        db.collection("users")
                .document(user.getUID())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Thêm thành công
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    // Thêm thất bại
                    callback.onCallback(false);
                });
    }

    public static void createAccount(Account account, FirestoreAddCallback callback) {
        db.collection("accounts")
                .document(account.getUID())
                .set(account)
                .addOnSuccessListener(aVoid -> {
                    // Thêm thành công
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    // Thêm thất bại
                    callback.onCallback(false);
                });
    }

    public interface FirestoreGetAccountCallback {
        void onCallback(Account account);
    }

    public static void getAccount(String UID, FirestoreGetAccountCallback callback) {
        db.collection("accounts")
                .document(UID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()) {
                        Account account = documentSnapshot.toObject(Account.class);
                        assert account != null;
                        account.setUID(UID);
                        callback.onCallback(account);
                    } else {
                        // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                    callback.onCallback(null);
                });
    }

    public static void getAccountByUserId(String userId, FirestoreGetAccountCallback callback) {
        db.collection("accounts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Account account = document.toObject(Account.class);
                        assert account != null;
                        account.setUID(document.getId());
                        callback.onCallback(account);
                    } else {
                        // Nếu không tìm thấy tài khoản, trả về null
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về null
                    callback.onCallback(null);
                });
    }

    public interface getTransactionCallback {
        void onCallback(TransactionData transactionData);
    }

    public static void getTransaction(String UID, getTransactionCallback callback) {
        db.collection("transactions")
                .document(UID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()) {
                        TransactionData transactionData = documentSnapshot.toObject(TransactionData.class);
                        assert transactionData != null;
                        transactionData.setUID(UID);
                        callback.onCallback(transactionData);
                    } else {
                        // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                    callback.onCallback(null);
                });
    }

    public static void addEditTransaction (TransactionData transactionData, FirestoreAddCallback callback) {
        db.collection("transactions")
                .document(transactionData.getUID())
                .set(transactionData)
                .addOnSuccessListener(aVoid -> {
                    // Thêm thành công
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    // Thêm thất bại
                    callback.onCallback(false);
                });
    }

    public static void getTransactionById(String transactionId, FirestoreGetTransactionCallback callback) {
        if (transactionId == null || transactionId.isEmpty()) {
            Log.e("Firestore", "getTransactionById called with null or empty ID");
            callback.onCallback(null);
            return;
        }
        
        db.collection("transactions").document(transactionId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    TransactionData transaction = documentSnapshot.toObject(TransactionData.class);
                    if (transaction != null) {
                        // Ensure the UID is set since Firestore doesn't store it in the document
                        transaction.setUID(documentSnapshot.getId());
                    }
                    callback.onCallback(transaction);
                } else {
                    Log.d("Firestore", "Transaction document does not exist");
                    callback.onCallback(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Firestore", "Error getting transaction document", e);
                callback.onCallback(null);
            });
    }

    public static void addEditBill (Bill bill, FirestoreAddCallback callback) {
        db.collection("bills")
                .document(bill.getUID())
                .set(bill)
                .addOnSuccessListener(aVoid -> {
                    // Thêm thành công
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    // Thêm thất bại
                    callback.onCallback(false);
                });
    }

    public static void getBillById(String billId, FirestoreGetBillCallback callback) {
        if (billId == null || billId.isEmpty()) {
            Log.e("Firestore", "getBillById called with null or empty ID");
            callback.onCallback(null);
            return;
        }
        
        db.collection("bills").document(billId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Bill bill = documentSnapshot.toObject(Bill.class);
                    if (bill != null) {
                        // Ensure the UID is set since Firestore doesn't store it in the document
                        bill.setUID(documentSnapshot.getId());
                    }
                    callback.onCallback(bill);
                } else {
                    Log.d("Firestore", "Bill document does not exist");
                    callback.onCallback(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Firestore", "Error getting bill document", e);
                callback.onCallback(null);
            });
    }

    // Interface for getting a Transaction
    public interface FirestoreGetTransactionCallback {
        void onCallback(TransactionData transaction);
    }

    // Interface for getting a Bill
    public interface FirestoreGetBillCallback {
        void onCallback(Bill bill);
    }

}
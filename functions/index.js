// functions/index.js
const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Khởi tạo Firebase Admin SDK một lần
if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();
const logger = functions.logger; // Sử dụng logger của Firebase Functions

/**
 * Sends a transaction notification to a specific customer.
 *
 * @param {object} data The data passed to the function.
 * @param {string} data.customerId The UID of the customer to notify.
 * @param {string} data.title The title of the notification.
 * @param {string} data.body The body of the notification.
 * @param {string} [data.transactionId] Optional transaction ID for deep linking.
 * @param {functions.https.CallableContext} context The context of the function call.
 * @return {Promise<object>} A promise that resolves with the result of the send operation.
 */
exports.sendTransactionNotificationToCustomer = functions
    .region("asia-southeast1") // Chỉ định region
    .https.onCall(async (data, context) => {
      logger.info("sendTransactionNotificationToCustomer CALLED with data:", data);

      const {customerId, title, body, transactionId} = data;

      if (!customerId) {
        logger.error("Customer ID is required.");
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Customer ID is required.",
        );
      }
      if (!title || !body) {
        logger.error("Title and body are required for notification.");
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Title and body are required.",
        );
      }

      try {
        logger.info(`Attempting to get user document for customerId: ${customerId}`);
        const userDocRef = firestore.collection("users").doc(customerId);
        const userDoc = await userDocRef.get();

        if (!userDoc.exists) {
          logger.error(`User document not found: ${customerId}`);
          return {success: false, error: "User not found."};
        }

        const userData = userDoc.data();
        const fcmToken = userData.fcmToken;
        logger.info(`User data fetched. FCM Token exists: ${fcmToken ? "Yes" : "No"}`);


        if (!fcmToken) {
          logger.warn(`FCM token not found for user: ${customerId}`);
          return {success: false, error: "FCM token not found for user."};
        }

        const messagePayload = { // Đây là data payload
          data: {
            title: title,
            body: body,
          },
          token: fcmToken,
          android: { // Cấu hình Android
            priority: "high",
            // notification: { // Không cần nếu client tự tạo notification
            //   sound: "default",
            // },
          },
        };

        if (transactionId) {
          messagePayload.data.transactionId = transactionId;
        }

        logger.info(
            `Attempting to send FCM message to token: ${fcmToken} with data:`,
            messagePayload.data,
        );
        const response = await admin.messaging().send(messagePayload); // Sử dụng send() thay vì sendToDevice nếu chỉ có 1 token
        logger.info("Successfully sent message:", response);
        return {success: true, messageId: response};
      } catch (error) {
        logger.error("Error in sendTransactionNotificationToCustomer:", error);
        throw new functions.https.HttpsError(
            "internal",
            "Failed to send notification.",
            error.message, // Gửi message của lỗi
        );
      }
    });

// Ví dụ hàm test đơn giản
exports.simpleCallableTestJS = functions
    .region("asia-southeast1")
    .https.onCall((data, context) => {
      logger.info("simpleCallableTestJS CALLED with data:", data);
      const clientName = data.name || "Guest";
      return {status: `simpleCallableTestJS executed successfully for ${clientName}`};
    });

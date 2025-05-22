// functions/index.js

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

// Callable Function để gửi thông báo giao dịch đến khách hàng
exports.sendTransactionNotificationToCustomer = functions.https.onCall(
    async (data, context) => {
    // TODO: Thêm xác thực mạnh mẽ hơn
    // if (!context.auth || !context.auth.token ||
    //     !context.auth.token.isOfficer) {
    //   console.error(
    //     "Authentication error: Caller is not an authenticated officer."
    //   );
    //   throw new functions.https.HttpsError(
    //     "unauthenticated",
    //     "The function must be called by an authenticated banking officer.",
    //   );
    // }

      const customerId = data.customerId;
      const title = data.title;
      const body = data.body;
      const transactionId = data.transactionId;

      if (!customerId || !title || !body) {
        console.error(
            "Missing required parameters: customerId, title, or body.",
            "Received data:",
            JSON.stringify(data), // Thêm dấu phẩy
        );
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Missing required parameters: customerId, title, body.",
        );
      }

      try {
        const userDoc = await db.collection("users").doc(customerId).get();

        if (!userDoc.exists) {
          console.error(
              "Customer user document not found for ID:",
              customerId,
          );
          return {success: false, error: "Target customer not found."};
        }

        const userData = userDoc.data();
        const customerFcmToken = userData.fcmToken;

        if (
          !customerFcmToken ||
          typeof customerFcmToken !== "string" ||
          customerFcmToken.trim() === ""
        ) {
          console.error(
              "FCM token not found or invalid for customer:",
              customerId,
          );
          return {
            success: false,
            error: "FCM token not available for this customer.",
          };
        }

        const payload = {
          data: {
            title: title,
            body: body,
            transactionId: transactionId || "",
            // screenToOpen: "transaction_detail",
            // notificationType: "CUSTOMER_TRANSACTION",
          },
          token: customerFcmToken,
          android: {
            priority: "high",
            // notification: {
            //   channel_id: "customer_transaction_updates_channel",
            // },
          },
          // apns: {
          //   payload: {
          //     aps: {
          //       sound: "default",
          //       badge: 1,
          //     },
          //   },
          // },
        };

        console.log(
            "Attempting to send FCM message to token:",
            customerFcmToken,
            "with data payload:",
            JSON.stringify(payload.data), // Thêm dấu phẩy
        );

        const response = await admin.messaging().send(payload);
        console.log("Successfully sent message:", response);
        return {success: true, messageId: response};
      } catch (error) {
        console.error(
            "Error sending FCM message for customer:",
            customerId,
            error, // Thêm dấu phẩy
        );
        throw new functions.https.HttpsError(
            "internal",
            "An unexpected error occurred while sending the notification.",
            error.message, // Thêm dấu phẩy
        );
      }
    }, // Thêm dấu phẩy
);

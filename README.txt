Video demo: https://drive.google.com/drive/folders/1BvSTImOPJj1iRojdnIFZ61sx0uYBtLuc?usp=sharing
Link Github: https://github.com/TaTriet1412/Banking_Application

# 3T Banking Application

## Overview
3T Banking is a comprehensive mobile banking application developed for Android that provides a secure and convenient banking experience. The application allows users to manage accounts, transfer funds, pay bills, recharge phones, authenticate with biometric verification, and more.

## Features

### Authentication & Security
- Secure user registration with email and password
- Multi-factor authentication:
  - PIN code verification
  - OTP authentication via email
  - Face recognition biometric authentication
- Automatic session management and restoration

### Account Management
- View checking, savings, and mortgage account details
- Account balance display
- Transaction history

### Banking Operations
- Money transfer between accounts
- Bill payments (electricity bills)
- Mobile phone recharge
- Payment processing with VNPay integration

### Advanced Technology Features
- Facial recognition for authentication
- OCR (Optical Character Recognition) for national ID card processing
- Automatic image orientation correction
- Firebase integration for secure data storage and authentication

## Technical Implementation

### Architecture
- Client-server architecture using Firebase platform
- Model-View-Controller (MVC) pattern

### Backend Technologies
- Firebase Authentication for user management
- Firebase Firestore for database storage
- Firebase Storage for file storage (images, documents)

### Frontend Technologies
- Native Android with Java
- Material Design components for UI
- Custom dialogs for verification processes

### Security Features
- Biometric data encryption and secure storage
- PIN and OTP verification
- Transaction authentication with multiple factors

## Installation and Setup

### Requirements
- Android SDK version 24+
- Firebase project configured with:
  - Authentication
  - Firestore
  - Storage
- VNPay developer account for payment processing

### Configuration
1. Clone the repository
2. Connect the app to your Firebase project
3. Update VNPay credentials in `VnPayUtils.java`
4. Build and run the application

## Usage Flow

### Customer Journey
1. Sign up with personal details and ID verification
2. Log in with credentials
3. Access account management
4. Perform banking operations:
   - Transfer money (with OTP and/or facial verification)
   - Pay bills
   - Recharge phone credits

### Bank Officer Journey
1. Access officer dashboard
2. Manage customer accounts
3. View customer information
4. Process requests and transactions

## Running the Application

### Development Environment Setup
1. Install Android Studio (version 4.2 or later recommended)
2. Install Java JDK 11 or later
3. Install required Android SDK components through Android Studio

### Firebase Configuration
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your project with your package name
3. Download the `google-services.json` file and place it in the app directory
4. Enable Authentication, Firestore Database and Storage services

### VNPay Integration
1. Replace the placeholder VNPay credentials in `VnPayUtils.java`:
   ```java
   public static String vnp_TmnCode = "YOUR_TMN_CODE";
   public static String vnp_HashSecret = "YOUR_HASH_SECRET";
   ```
2. Configure the return URL in your VNPay merchant account to match the app's scheme: `myapp://vnpayresponse`

### Build and Run
1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Connect an Android device (physical device recommended for biometric features)
4. Click Run button (or press Shift+F10) to build and deploy the application

### Testing
1. Start with creating a new user account (provides access to customer features)
2. Sign in with the provided test officer credentials to access banking officer features
   - Email: `tatriet16@gmail.com`
   - Password: `123456`
3. Sign in with the provided test customer credentials to access customer features
   - Email: `triettrinhthinh@gmail.com`
   - Password: `123456`


## Future Improvements
- Add transaction categorization and analytics
- Implement scheduled payments and recurring transfers
- Add support for credit cards and loans
- Enhance security with device binding
- Implement push notifications for transactions

## Contributors
- Team 3T

## License
© 2023-2024 3T Banking. All Rights Reserved.

# Grahak App Setup Guide

## Prerequisites
- Android Studio (latest version recommended)
- JDK 17 or higher
- Physical Android device or emulator with Android 8.0+ (API 26+)

## Step-by-Step Setup

### 1. Firebase Configuration

#### Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or use an existing project
3. Name it "Grahak Marketplace" (or your preferred name)
4. Enable Google Analytics (optional)
5. Create project

#### Add Android App to Firebase
1. In Firebase Console, click the Android icon to add Android app
2. Enter package name: `com.princesanitary.grahak`
3. App nickname: "Grahak Android"
4. Debug signing certificate SHA-1 (optional for now, needed for Google Sign-In):
   ```bash
   cd ~/.android
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Click "Register app"

#### Download google-services.json
1. Download the `google-services.json` file from Firebase Console
2. Move it to: `/Users/madhav/Documents/Projects/grahak/android-app/app/google-services.json`
3. **CRITICAL**: The file MUST be in the `app/` folder, not the project root

#### Enable Firebase Services
In Firebase Console, enable these services:
1. **Authentication**
   - Go to Build → Authentication → Sign-in method
   - Enable "Phone" authentication
   - Enable "Google" (optional, for plumber accounts)
   - Add your phone number for testing (recommended)

2. **Firestore Database**
   - Go to Build → Firestore Database
   - Click "Create database"
   - Start in **Test mode** (for development)
   - Choose region (e.g., `asia-south1` for India)
   - Click "Enable"

3. **Storage**
   - Go to Build → Storage
   - Click "Get started"
   - Start in **Test mode**
   - Click "Done"

### 2. Google Maps API Key

#### Get Maps API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project (it's automatically created in Google Cloud)
3. Go to "APIs & Services" → "Credentials"
4. Click "Create Credentials" → "API Key"
5. Copy the API key
6. (Recommended) Click "Restrict Key":
   - Under "Application restrictions", select "Android apps"
   - Click "Add an item" and add your package name: `com.princesanitary.grahak`
   - Add your SHA-1 certificate fingerprint (from step 1.2.4 above)
   - Under "API restrictions", select "Restrict key"
   - Enable: "Maps SDK for Android", "Places API", "Geocoding API"
   - Click "Save"

#### Enable Required APIs
In Google Cloud Console, go to "APIs & Services" → "Library" and enable:
- Maps SDK for Android
- Places API
- Geocoding API

#### Add API Key to Project
1. Open `/Users/madhav/Documents/Projects/grahak/android-app/local.properties`
2. Replace `YOUR_MAPS_API_KEY_HERE` with your actual API key:
   ```
   MAPS_API_KEY=AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q
   ```

### 3. Android Studio Setup

#### Open Project
1. Launch Android Studio
2. Click "Open" and select: `/Users/madhav/Documents/Projects/grahak/android-app/`
3. Wait for Gradle sync to complete (this may take a few minutes on first run)

#### SDK Setup
If Android Studio prompts about missing SDK:
1. Go to File → Project Structure → SDK Location
2. Make sure SDK path is set (usually `~/Library/Android/sdk`)
3. Click "Apply"

#### Verify Gradle Wrapper
The Gradle wrapper should now be present. Verify by running in Terminal:
```bash
cd /Users/madhav/Documents/Projects/grahak/android-app
./gradlew --version
```

### 4. Build the App

#### First Build
In Android Studio:
1. Click "Build" → "Make Project" (or press ⌘F9)
2. Wait for build to complete
3. Check "Build" output window for any errors

Or via command line:
```bash
cd /Users/madhav/Documents/Projects/grahak/android-app
./gradlew assembleDebug
```

### 5. Run on Device

#### Physical Device
1. Enable Developer Options on your Android phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings

2. Enable USB Debugging:
   - Go to Settings → System → Developer Options
   - Enable "USB debugging"

3. Connect phone via USB cable
4. Accept "Allow USB debugging" prompt on phone
5. In Android Studio, select your device from device dropdown
6. Click "Run" (green play button) or press ⌃R

#### Emulator
1. Click "Device Manager" in Android Studio
2. Click "Create Device"
3. Select a device (e.g., Pixel 5)
4. Select system image (API 33 or higher recommended)
5. Click "Finish"
6. Start the emulator
7. Click "Run" in Android Studio

### 6. Testing User Flows

#### Consumer Flow
1. Launch app
2. Select "Consumer" user type
3. Enter phone number → OTP verification
4. Browse products
5. Add products to cart
6. Checkout
7. Book plumber service

#### Plumber Flow
1. Launch app (or log out and restart)
2. Select "Plumber" user type
3. Complete 4-step registration:
   - Personal info
   - Business details
   - Upload documents (Aadhaar, trade license)
   - Bank details + UPI ID
4. Wait for admin verification (you'll need to verify via Dukaan admin app)
5. Once verified, accept service jobs
6. Create estimates for jobs
7. Track earnings in wallet
8. Request UPI withdrawal

### 7. Firebase Security Rules (Production)

Before launching to production, update Firestore rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Plumbers collection
    match /plumbers/{plumberId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == plumberId;
    }
    
    // Catalog (read-only for consumers, admin-write only)
    match /catalog/{productId} {
      allow read: if request.auth != null;
      allow write: if false; // Admin writes via Dukaan app
    }
    
    // Service jobs
    match /service_jobs/{jobId} {
      allow read, write: if request.auth != null;
    }
    
    // Estimates
    match /estimates/{estimateId} {
      allow read, write: if request.auth != null;
    }
    
    // Orders
    match /orders/{orderId} {
      allow read, write: if request.auth != null;
    }
    
    // Carts
    match /carts/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Transactions (read-only for plumbers)
    match /transactions/{transactionId} {
      allow read: if request.auth != null;
      allow write: if false; // System-generated only
    }
    
    // Withdrawal requests
    match /withdrawal_requests/{requestId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 8. Common Build Errors

#### Error: "google-services.json is missing"
**Solution**: Download from Firebase Console and place in `app/` folder

#### Error: "MAPS_API_KEY not found"
**Solution**: Add your API key to `local.properties` file

#### Error: "Failed to resolve: com.google.firebase..."
**Solution**: Check internet connection and sync Gradle again

#### Error: "Execution failed for task ':app:processDebugGoogleServices'"
**Solution**: Verify `google-services.json` has correct package name: `com.princesanitary.grahak`

#### Error: "Manifest merger failed"
**Solution**: Check AndroidManifest.xml for duplicate permissions or activities

#### Error: "Hilt processor not found"
**Solution**: 
1. File → Invalidate Caches → Invalidate and Restart
2. Clean and rebuild project

### 9. Debugging Runtime Crashes

If app crashes on launch:

1. **Check Logcat** in Android Studio:
   - Filter by package name: `com.princesanitary.grahak`
   - Look for red error lines
   - Copy the stack trace

2. **Common Runtime Issues**:
   - **FirebaseApp not initialized**: Missing `google-services.json`
   - **ClassNotFoundException**: Hilt components not generated (rebuild project)
   - **SecurityException**: Missing permissions in AndroidManifest.xml
   - **NetworkSecurityException**: Check internet permission

3. **Enable Debug Logging**:
   In Android Studio, run:
   ```bash
   adb shell setprop log.tag.Grahak DEBUG
   adb logcat -s Grahak
   ```

### 10. Test Data Setup

For testing, you'll need to populate Firestore with sample data:

#### Products (via Firebase Console)
1. Go to Firestore Database
2. Create collection: `catalog`
3. Add documents with fields:
   - `id`: auto-generated
   - `name`: "PVC Pipe 1 inch"
   - `category`: "Pipes"
   - `pricingType`: "FIXED"
   - `price`: 150.0
   - `unit`: "piece"
   - `inStock`: true
   - `imageUrl`: "" (optional)
   - `description`: "High quality PVC pipe"

#### Admin User (for approving plumbers)
1. Create collection: `admins`
2. Add document with your phone number as ID
3. Fields:
   - `role`: "admin"
   - `permissions`: ["VERIFY_PLUMBERS", "APPROVE_WITHDRAWALS"]

### 11. Next Steps

✅ **App is now set up and ready to run!**

Next actions:
1. Test all user flows (consumer + plumber)
2. Add sample products to Firestore
3. Set up admin verification in Dukaan app
4. Configure production Firebase rules
5. Generate signed APK for testing
6. Plan Google Play Store deployment

### 12. Generate Signed APK (for distribution)

```bash
# In Android Studio:
# Build → Generate Signed Bundle / APK → APK
# Create new keystore (save it securely!)
# Build release APK
```

## Support

If you encounter any issues:
1. Check TROUBLESHOOTING.md
2. Review Firebase Console for service status
3. Check Android Studio Logcat for error messages
4. Verify all API keys and google-services.json are correctly configured

---

**Created**: 2026-09-15  
**Last Updated**: 2026-09-15  
**Version**: 1.0

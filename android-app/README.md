# Grahak Android App

Consumer & Plumber dual-interface Android application built with Jetpack Compose and Kotlin.

## Features

### Consumer Interface
- Browse products with FIXED pricing and RFQ_ONLY items
- Book on-demand plumber services
- Create and track material orders
- Site-drop delivery for work locations
- Order history and tracking

### Plumber Interface  
- Accept service job requests
- Create digital material estimates (Parchi)
- On-site inspection with before/after photos
- Commission-based earnings
- Wallet management with UPI withdrawals
- Real-time job notifications

## Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **DI**: Hilt/Dagger
- **Backend**: Firebase (Auth, Firestore, Storage, FCM)
- **Maps**: Google Maps SDK
- **Image Loading**: Coil
- **Networking**: Retrofit (for UPI integration)

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- Google Maps API key

### 1. Clone and Open Project

```bash
cd android-app
```

Open the `android-app` folder in Android Studio.

### 2. Add Firebase Configuration

1. Download `google-services.json` from Firebase Console (Project B)
2. Place it in `app/google-services.json`

### 3. Configure Google Maps

1. Get a Maps API key from Google Cloud Console
2. Create `local.properties` in the project root:

```properties
MAPS_API_KEY=your_google_maps_api_key_here
```

### 4. Build and Run

```bash
./gradlew assembleDebug
```

Or use Android Studio's Run button.

## Project Structure

```
app/src/main/java/com/princesanitary/grahak/
├── data/
│   ├── model/          # Data classes (Product, User, Plumber, etc.)
│   ├── repository/     # Repository pattern implementations
│   └── service/        # Firebase services, API clients
├── di/                 # Hilt dependency injection modules
├── ui/
│   ├── auth/           # Login, OTP verification
│   ├── consumer/       # Consumer interface screens
│   ├── plumber/        # Plumber interface screens
│   ├── onboarding/     # Role selection
│   └── theme/          # Theme, colors, typography
└── util/               # Utilities, extensions, helpers
```

## Key Features Implementation

### Dual-Tiered Pricing

Products with `pricingType: "FIXED"` show prices directly.
Products with `pricingType: "RFQ_ONLY"` show "Request Quote" button.

### Plumber Workflow

1. Customer books plumber via app
2. Plumber receives notification and accepts job
3. Plumber arrives on-site, creates material estimate
4. Customer approves estimate and pays
5. Shop dispatches materials to work site
6. Plumber completes job, unused items returned
7. Commission unlocks in plumber's wallet
8. Instant UPI withdrawal available

### Real-time Updates

- FCM push notifications for job status changes
- Firestore real-time listeners for order tracking
- Live location tracking for plumbers (Google Maps)

## Testing

```bash
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## Building Release APK

```bash
./gradlew assembleRelease
```

APK will be in `app/build/outputs/apk/release/`

## Troubleshooting

### Build Errors

- Ensure JDK 17 is selected
- Sync Gradle files
- Clear cache: `Build > Clean Project`

### Firebase Authentication Issues

- Verify `google-services.json` is in `app/` folder
- Check Firebase Console for enabled authentication methods
- Verify SHA-1 fingerprint is added in Firebase Console

### Maps Not Loading

- Verify Maps API key in `local.properties`
- Enable Maps SDK in Google Cloud Console
- Check billing is enabled for Maps API

## License

Proprietary - Prince Sanitary Store

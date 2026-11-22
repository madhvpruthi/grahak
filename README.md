# Grahak - Consumer & Plumber Services App

A consumer-facing marketplace app for browsing products, ordering materials, and booking on-demand plumber services.

## 🏗️ What This App Does

**For Consumers:**
- Browse products with smart pricing (FIXED or Request Quote)
- Add products to cart and checkout
- Book on-demand plumber services
- Approve plumber material estimates
- Track orders and delivery

**For Plumbers:**
- Register and complete KYC verification
- Accept service job requests
- Create digital material estimates (Parchi) on-site
- Track earnings and wallet balance
- Request UPI withdrawals

## 🔥 Firebase Backend

Uses **Firebase Project B - "Marketplace"** with:
- Authentication (Phone for consumers/plumbers)
- Firestore Database
- Cloud Storage (images, documents)
- Cloud Messaging (notifications)

## 📱 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt/Dagger
- **Backend**: Firebase SDK
- **Maps**: Google Maps SDK
- **Image Loading**: Coil

## 🔑 Key Features

### Dual-Tiered Pricing
- **FIXED Pricing**: Products with visible prices
- **RFQ_ONLY Pricing**: "Request Quote" items with hidden pricing

### Closed-Loop Plumber Workflow
1. Consumer books plumber for site inspection
2. Plumber creates digital estimate (Parchi) on-site
3. Consumer approves and pays
4. Materials dispatched to work site
5. Job completion triggers commission release

### Single Merchant Model
- All products from one store
- Public plumber registration with verification

## 📁 Project Structure

```
grahak/
├── android-app/           # Consumer & Plumber Android app
├── firebase/              # Firebase config, functions, rules
├── ARCHITECTURE.md        # System architecture
├── PROJECT_STATUS.md      # Development status
├── SETUP_GUIDE.md         # Firebase setup instructions
└── README.md              # This file
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest)
- Firebase CLI
- Java 17+
- Google Maps API key

### Setup Instructions
See `SETUP_GUIDE.md` for detailed Firebase setup and `android-app/README.md` for app configuration.

## 📝 Notes

- Admin features are managed separately (not part of this app)
- Firebase Project B is dedicated for this marketplace
- No GitHub pushes until explicitly requested

---

**Status**: Active development - Consumer and Plumber interfaces

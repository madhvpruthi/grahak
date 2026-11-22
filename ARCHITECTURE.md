# Grahak System Architecture

## 🏗️ System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    PRINCE SANITARY STORE                     │
│                      (Business Owner)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Manages via
                       ↓
┌─────────────────────────────────────────────────────────────┐
│              DUKAAN BUSINESS APP (Existing)                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Regular Features (All Businesses):                   │  │
│  │  • Billing & POS                                      │  │
│  │  • Inventory Management                               │  │
│  │  • Credit Management                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Grahak Admin (Prince Sanitary ONLY):                │  │
│  │  • Publish products to Grahak marketplace            │  │
│  │  • Verify plumbers                                    │  │
│  │  • Manage Grahak orders                              │  │
│  │  • Process plumber payouts                           │  │
│  │  • View marketplace analytics                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  Connects to:                                                │
│  • Firebase Project A (Dukaan data)                          │
│  • Firebase Project B (Grahak data) ← Admin features only   │
└─────────────────────────────────────────────────────────────┘
                       │
                       │ Publishes to
                       ↓
┌─────────────────────────────────────────────────────────────┐
│            FIREBASE PROJECT B - "MARKETPLACE"                │
│                                                               │
│  Collections:                                                │
│  • catalog          (products published from Dukaan)         │
│  • plumbers         (registered service partners)            │
│  • users            (consumer profiles)                      │
│  • service_jobs     (plumber bookings)                       │
│  • orders           (material orders & estimates)            │
│  • admins           (master admin accounts)                  │
│  • wallet_transactions (commission records)                  │
└─────────────────────────────────────────────────────────────┘
                       │
                       │ Consumed by
                       ↓
┌─────────────────────────────────────────────────────────────┐
│              GRAHAK ANDROID APP (This Project)               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Consumer Interface:                                  │  │
│  │  • Browse products (FIXED & RFQ pricing)             │  │
│  │  • Add to cart & checkout                            │  │
│  │  • Book plumber services                             │  │
│  │  • Approve plumber estimates                         │  │
│  │  • Track orders                                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Plumber Interface:                                   │  │
│  │  • Register & KYC verification                        │  │
│  │  • Accept job requests                                │  │
│  │  • Create digital estimates (Parchi)                  │  │
│  │  • Manage wallet & UPI withdrawal                     │  │
│  │  • Toggle operational status                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🔐 Access Control

### Dukaan Business App Users

1. **Prince Sanitary Store (Master Admin)**
   - ✅ Full Dukaan business features
   - ✅ Full Grahak marketplace admin features
   - Identified by: `grahakAccess: true` in user document

2. **Other Businesses (Regular Users)**
   - ✅ Full Dukaan business features (their own data)
   - ❌ NO access to Grahak marketplace features
   - Identified by: `grahakAccess: false` or missing

### Grahak App Users

1. **Consumers**
   - Browse & purchase products
   - Book plumber services
   - Manage orders

2. **Plumbers**
   - Register & get verified
   - Accept service jobs
   - Create material estimates
   - Manage earnings

## 📊 Data Flow

### Product Publishing Flow
```
Dukaan Inventory (Project A)
    ↓
Master Admin clicks "Publish to Grahak"
    ↓
Product synced to Firebase Project B
    ↓
Visible in Grahak Consumer App
```

### Plumber Verification Flow
```
Plumber registers in Grahak App
    ↓
KYC documents uploaded to Project B
    ↓
Master Admin verifies in Dukaan App
    ↓
Plumber status updated to "Verified"
    ↓
Plumber can now accept jobs
```

### Order Flow
```
Consumer places order in Grahak App
    ↓
Order created in Firebase Project B
    ↓
Master Admin sees order in Dukaan App
    ↓
Admin updates fulfillment status
    ↓
Consumer sees updates in real-time
```

## 🔥 Firebase Projects

### Project A: "Dukaan Business" (Existing)
- **Purpose**: Business management for all Dukaan users
- **Collections**: businesses, users, transactions, inventory, customers, etc.
- **Access**: All Dukaan business users

### Project B: "Grahak Marketplace" (New)
- **Purpose**: Consumer marketplace & plumber services
- **Collections**: catalog, plumbers, users, service_jobs, orders
- **Access**: 
  - Read: Grahak app users (consumers & plumbers)
  - Write: Grahak app users (limited) + Master admin from Dukaan app

## 🛡️ Security

### Firestore Rules (Project B)
```javascript
// Catalog - Public read, admin write
match /catalog/{productId} {
  allow read: if resource.data.isPublished == true;
  allow write: if isAdmin();
}

// Plumbers - Own data + admin
match /plumbers/{plumberId} {
  allow read: if isAuthenticated();
  allow write: if isPlumber(plumberId) || isAdmin();
}

// Orders - Customer + attributed plumber + admin
match /orders/{orderId} {
  allow read: if isCustomer(resource.data.customerId) || 
                 isPlumber(resource.data.attributedPlumberId) ||
                 isAdmin();
  allow write: if isAdmin();
}
```

## 📱 Technology Stack

### Grahak Android App
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Backend**: Firebase SDK

### Dukaan Business App (Integration)
- Add dual Firebase connection
- Add Grahak management screens
- Add permission checks (`grahakAccess`)

## 🔄 Integration Points

### Dukaan → Grahak
- Product publishing
- Order fulfillment
- Plumber verification
- Wallet management

### Grahak → Dukaan
- Order notifications
- Low stock alerts
- Plumber withdrawal requests
- Analytics data

## 📝 Summary

- **Grahak App**: Standalone consumer & plumber Android app
- **Admin Features**: Integrated into existing Dukaan Business app
- **Separation**: Only Prince Sanitary can manage Grahak marketplace
- **Isolation**: Other businesses using Dukaan cannot access Grahak features

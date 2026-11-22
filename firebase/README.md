# Firebase Configuration - Project B "Marketplace"

This directory contains Firebase configuration files for the dedicated Grahak marketplace backend.

## Setup Instructions

### 1. Create New Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project"
3. Name it "Grahak Marketplace" (Project B)
4. **Important**: This should be a completely separate project from your existing Dukaan Business app

### 2. Enable Firebase Services

Enable the following services in your Firebase project:

- **Authentication**
  - Enable Phone Authentication (for consumer/plumber login)
  - Enable Email/Password (for admin login)

- **Firestore Database**
  - Create database in production mode
  - Deploy security rules from `firestore.rules`

- **Cloud Storage**
  - Create default storage bucket
  - Deploy storage rules from `storage.rules`

- **Cloud Messaging** (FCM)
  - Enable for push notifications (job alerts, order updates)

### 3. Configure Android App

1. In Firebase Console, add an Android app
2. Package name: `com.princesanitary.grahak`
3. Download `google-services.json`
4. Place it in: `/android-app/app/google-services.json`

### 4. Configure Web Admin Dashboard

1. In Firebase Console, add a Web app
2. Copy the config object
3. Create `/admin-dashboard/.env.local` from `.env.example`
4. Fill in the Firebase config values

### 5. Deploy Firestore Rules & Indexes

```bash
cd firebase
firebase login
firebase use --add  # Select your Project B
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
firebase deploy --only storage
```

### 6. Create Admin User

Run this once to create the master admin account:

```bash
# In Firebase Console > Authentication
# Manually create a user with email/password
# Then add their UID to Firestore:

# Collection: admins
# Document ID: <admin_user_uid>
# Fields:
#   email: "admin@princesanitary.com"
#   name: "Prince Sanitary Admin"
#   role: "MASTER_ADMIN"
#   createdAt: <current_timestamp>
```

## Firestore Collections Structure

```
firestore/
├── catalog/              # Products from Prince Sanitary Store
├── users/                # Consumer accounts
├── plumbers/             # Plumber service partners
├── service_jobs/         # On-demand plumber bookings
├── orders/               # Material orders & estimates
├── admins/               # Master admin accounts (manual setup)
└── wallet_transactions/  # Plumber commission records
```

## Security Notes

- **Separate Firebase Project**: Never mix with existing Dukaan Business app
- **Admin Access**: Only accessible via email/password (not phone auth)
- **Firestore Rules**: Enforce strict read/write permissions
- **Storage Rules**: Validate file types and sizes
- **API Keys**: Keep all credentials in `.env.local` and `.gitignore`

## Local Development with Emulators

```bash
cd firebase
firebase emulators:start
```

This starts local emulators for:
- Auth: http://localhost:9099
- Firestore: http://localhost:8080
- Storage: http://localhost:9199
- Emulator UI: http://localhost:4000

## Bridge to Existing POS (Future)

For inventory sync between Dukaan Business (Project A) and Grahak Marketplace (Project B):

- Use Cloud Functions with Firebase Admin SDK
- Secure webhook endpoints with service account authentication
- One-way sync: POS stock updates → Marketplace catalog
- No direct database access between projects

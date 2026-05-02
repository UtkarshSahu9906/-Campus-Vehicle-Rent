# Campus Vehicle Rent — Setup Guide

## Step 1: Create Firebase Project
1. Go to https://console.firebase.google.com
2. Click **Add project** → name it "CampusVehicleRent"
3. Disable Google Analytics (optional) → **Create project**

## Step 2: Add Android App to Firebase
1. In Firebase Console → **Project Overview** → click **Android icon**
2. Package name: `com.college.vehiclerent`
3. App nickname: Campus Vehicle Rent
4. Click **Register app**
5. **Download `google-services.json`** → place it inside `app/` folder
6. Click Next → Next → Continue to console

## Step 3: Enable Google Sign-In
1. Firebase Console → **Authentication** → **Get started**
2. **Sign-in method** tab → click **Google** → Enable → **Save**
3. Copy the **Web client ID** shown
4. Open `app/src/main/res/values/strings.xml`
5. Replace `YOUR_WEB_CLIENT_ID_HERE` with the copied ID

## Step 4: Enable Firestore
1. Firebase Console → **Firestore Database** → **Create database**
2. Start in **test mode** (for development)
3. Choose a location close to India (e.g. `asia-south1`)

## Step 5: Enable Firebase Storage
1. Firebase Console → **Storage** → **Get started**
2. Start in **test mode** → **Done**

## Step 6: Firestore Security Rules (for production)
Replace test rules with:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
    }
    match /vehicles/{vehicleId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.ownerUid;
    }
  }
}
```

## Step 7: Storage Security Rules (for production)
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /vehicles/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.resource.size < 5 * 1024 * 1024;
    }
  }
}
```

## Step 8: Open in Android Studio
1. Open Android Studio → **Open** → select the `VehicleRentApp` folder
2. Wait for Gradle sync to complete
3. Make sure `google-services.json` is in the `app/` folder
4. Connect your Android device (USB debugging ON) or start an emulator
5. Click **Run ▶**

## App Flow Summary
```
Login (Google Sign-In)
    ↓
  New user? → Role Selection → Owner or Customer
  Returning?  → skip to dashboard

Owner Dashboard
  → Add Vehicle (photo + price + mobile)
  → Long-press card → Edit / Delete

Customer Dashboard
  → Search vehicles
  → Tap card → Vehicle Detail
  → WhatsApp button → opens WhatsApp chat with owner
```

## Firestore Data Structure
```
users/
  {uid}/
    uid, name, email, photo, role

vehicles/
  {vehicleId}/
    ownerUid, ownerName, vehicleType,
    description, imageUrl,
    pricePerHour, mobileNo, available
```

## Troubleshooting
- **Sign-in fails**: Check SHA-1 fingerprint is added in Firebase project settings
- **Images not loading**: Check Firebase Storage rules allow read
- **Firestore writes failing**: Make sure rules allow authenticated users to write
- **WhatsApp not opening**: Check mobile number is 10 digits without country code

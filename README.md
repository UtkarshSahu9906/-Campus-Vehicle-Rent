# 🚗 Campus Vehicle Rent

A modern, peer-to-peer vehicle sharing application designed specifically for college campuses. This app allows students and staff to rent out their vehicles (cycles, scooties, bikes) to others within the community.

## 🌟 Key Features

- **Google Authentication**: Quick and secure sign-in using campus/personal Google accounts.
- **Dual Roles**: 
  - **Owners**: List vehicles, manage availability, set hourly prices, and edit/delete listings.
  - **Customers**: Browse available vehicles, search by type or owner, and view detailed information.
- **Real-time Updates**: Powered by Firebase Firestore for instantaneous data synchronization.
- **Direct Communication**: One-tap WhatsApp integration to connect customers with vehicle owners.
- **Smart Image Handling**: 
  - Automated image compression (resizing and quality optimization) to save data and storage.
  - Efficient image caching using Glide.
- **Responsive UI**: Built with Material Design for a premium, intuitive user experience.

## 🛠️ Tech Stack

- **Platform**: Android (Java)
- **Database**: Firebase Firestore (Real-time NoSQL)
- **Authentication**: Firebase Auth (Google Sign-In)
- **Storage**: Firebase Cloud Storage
- **Image Processing**: Glide, Bitmap Compression
- **UI Components**: Material Design, CircleImageView, RecyclerView

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana or newer.
- A Firebase Project.

### Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/UtkarshSahu9906/-Campus-Vehicle-Rent.git
   ```
2. **Firebase Setup**:
   - Create a project on the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.college.vehiclerent`.
   - Download `google-services.json` and place it in the `app/` directory.
   - Enable **Google Sign-In**, **Firestore**, and **Storage**.
3. **Configure Strings**:
   - Update `YOUR_WEB_CLIENT_ID_HERE` in `app/src/main/res/values/strings.xml` with your Firebase Web Client ID.
4. **Build and Run**:
   - Open the project in Android Studio and click **Run**.

## 📂 Project Structure
```text
com.college.vehiclerent
├── adapter             # RecyclerView adapters
├── model               # Data models (Vehicle, User)
├── LoginActivity       # Entry point & Authentication
├── RoleSelection       # New user onboarding
├── OwnerDashboard      # Management for vehicle owners
├── CustomerDashboard   # Browsing for renters
└── AddVehicleActivity  # Listing creation & Image handling
```

---
Developed with ❤️ for the campus community.
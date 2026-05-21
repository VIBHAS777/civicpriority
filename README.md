# Civic Priority 🏙️

Civic Priority is a unified community platform designed to empower citizens to report, track, and manage local civic issues. 

The platform consists of two perfectly aligned frontends that connect to the same real-time backend:
1. **React Website**: A web-based dashboard for community members and administrators to view reports and manage resources.
   👉 **[Live Website URL: https://vibhas777.github.io/civicpriority/](https://vibhas777.github.io/civicpriority/)**
2. **Android Application**: A native mobile app allowing citizens to quickly report issues (potholes, water leaks, etc.) on the go.
   👉 **[Download the Latest APK (Compiled with Photo Upload) from GitHub Actions or Releases]**

## Features

- **Issue Reporting**: Report civic issues with severity, category, and location tags.
- **Community Voting**: Upvote issues to increase their visibility and priority.
- **Real-Time Sync**: Powered by Firebase Firestore, ensuring the website and mobile app always display the exact same data instantly.
- **Resource Management**: A unique, algorithm-driven dashboard (on the web) to automatically assign available workers and vehicles to high-priority issues.

## Architecture

This is a **monorepo** containing both projects:
- `/website`: The React + Vite web application.
- `/android_app`: The native Android application built with Kotlin and Jetpack Compose.

Both applications share the exact same data schema and connect to **Firebase Firestore**. 

## Setup Instructions

To run this project locally, you must connect it to a Firebase project.

### 1. Firebase Setup
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Enable **Firestore Database** in test mode.
3. Add a **Web App** to the project and copy the configuration keys.
4. Add an **Android App** to the project with the package name `com.civic.priority` and download the `google-services.json` file.

### 2. Running the Website
1. Navigate to the `website` directory.
2. Create a `.env` file and populate it with your Firebase Web API keys:
   ```env
   VITE_FIREBASE_API_KEY=your_api_key
   VITE_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
   VITE_FIREBASE_PROJECT_ID=your_project
   VITE_FIREBASE_STORAGE_BUCKET=your_project.appspot.com
   VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
   VITE_FIREBASE_APP_ID=your_app_id
   ```
3. Run `npm install` followed by `npm run dev`.

### 3. Running the Android App
1. Place the `google-services.json` file into the `android_app/app/` directory.
2. Open the `android_app` directory in Android Studio.
3. Sync Gradle and press **Run**. 
*(Note: If the `google-services.json` file is missing, the app will safely fall back to using local mock data for demonstration purposes!)*

## Automated Deployments

This repository is configured with GitHub Actions to automatically deploy both platforms:
- **Web Deploy**: Pushes to the `main` branch automatically deploy the React website to GitHub Pages.
- **Android Build**: Pushes to the `main` branch automatically compile the Android APK and upload it as a release artifact in the "Actions" tab.

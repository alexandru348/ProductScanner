## Product Scanner (Kotlin)

Product Scanner is an Android application written in Kotlin
that allows you to scan product barcodes and display a health-oriented rating.
The project combines barcode scanning (ML Kit) with a simple UI flow and a crop overlay (preview)
for better control over the image. The repository is public
so sensitive / local configuration files are not included
(for example, the Firebase file: "google-services.json").

## Features

1. Scan barcode from image (chosen from the gallery or using camera of the device) using ML Kit
2. Product rating retrieved from Firebase Firestore (online rating)
3. Clear UI flow for:
- selecting image from gallery / taking photo
- barcode scanning
- displaying results (product name, barcode, category, health score, health bar, 
comparative text)
4. Crop overlay UI (preview):
- overlay rectangle over image
- moving + resizing the rectangle
- toggle for entering crop mode (enabling / disabling)
*  Note: now it's only for preview UI interaction; the real crop (reading and scanning 
from selected zone) is the next step.

## Technologies used

1) Language: Kotlin
2) Platform: Android
3) Barcode scanning:
- Google ML Kit (Barcode Scanning)
4) Backend:
- Firebase Firestore (evaluation data)
5) Build system:
- Gradle (Kotlin DSL)

## Project structure

1. "app/src/main/java/com/example/productscanner"
- MainActivity + repositories + crop overlay view
2. "app/src/main/res/layout/activity_main.xml"
- Main layout
3. "app/google-services.json"
- not included in the repo (see Firebase setup)

## Requirements

1. Android Studio (recommended)
2. An Android device or emulator
3. Internet connection (for evaluation from Firestore)
4. A Firebase project (if you want to run the online evaluation part)

## How to run the project

1. Clone repository:
```bash
git clone https://github.com/alexandru348/ProductScanner.git
```
2. Open the project in Android Studio
3. Do Gradle Sync
4. Configure Firebase (required for Firestore)
5. Select a device / emulator
6. Press Run

## Firebase Setup (required for the online evaluation)

This repository does not include this file: "app/google-services.json" 
because it is ignored for security reasons.

To run the project with Firebase/Firestore:

1. Go to the Firebase Console and create a new Firebase Project
2. Add an Android application to the Firebase project using the package name:
- "com.example.productscanner"
3. Download the generated file:
- "google-services.json"
4. Put the file here (important):
- "<root-project>/app/google-services.json"
5. Do Gradle Sync and run the application again

1) Correct location:
- "<root-project>/app/google-services.json"
2) Wrong locations (don't put it here):
- "<root-project>/google-services.json"
- "<root-project>/app/src/main/"
- "<root-project>/gradle"

## Notes / Limitations

1. The application is designed on a simple and stable flow (image -> scan -> evaluation).
2. Live scan is not implemented intentionally, to keep a very good ratio between simplicity /
quality / efficiency.
3. Crop overlay is currently a UI preview; real crop (reading and scanning from selected zone)
is planned as the next step.


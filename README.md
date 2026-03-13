# ChemoChat - Secure Bluetooth P2P Chat

ChemoChat is an Android application designed for secure, encrypted communication between two devices using Bluetooth Classic.

## Features
- **P2P Connection**: Connect directly via Bluetooth (RFCOMM).
- **AES-256 Encryption**: All data is encrypted using a shared password.
- **QR Auto-Connect**: Host generates a QR code; Joiner scans to connect.
- **Multimedia Support**: Send text, emojis, and (logic implemented for) images/audio.
- **Privacy First**: No internet connection required.

## How to Build
1. Open this project in **Android Studio**.
2. Wait for Gradle sync to complete.
3. Build and run on two Android devices.
4. Ensure Bluetooth and Location permissions are granted.

## Technical Details
- **UI**: Jetpack Compose
- **Encryption**: AES/CBC/PKCS5Padding with PBKDF2 key derivation.
- **Bluetooth**: Classic Bluetooth RFCOMM sockets.
- **QR**: ZXing library for generation and scanning.

## Security Note
This app uses symmetric encryption. Both users must enter the exact same password in the settings to successfully decrypt each other's messages.

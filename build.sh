#!/bin/bash

# Termux-friendly build script for ChemoChat
# Requires: openjdk-17, gradle

echo "Starting ChemoChat Build..."

# Check for dependencies
if ! command -v gradle &> /dev/null
then
    echo "Gradle not found. Please install it using: pkg install gradle"
    exit
fi

# Build the project
gradle assembleDebug

if [ $? -eq 0 ]; then
    echo "Build Successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "Build Failed."
fi

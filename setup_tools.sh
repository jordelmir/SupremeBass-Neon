#!/bin/bash

# Elite Environment Setup for Android Development
# -----------------------------------------------

echo "🔧 Configuring Android SDK Paths..."

# Define SDK Root
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Add Platform Tools (adb) and Emulator to PATH
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# Persist to ZSHRC if not present
if ! grep -q "platform-tools" "$HOME/.zshrc"; then
    echo "" >> "$HOME/.zshrc"
    echo "# Android SDK Tools (Supreme Bass)" >> "$HOME/.zshrc"
    echo "export ANDROID_HOME=\"\$HOME/Library/Android/sdk\"" >> "$HOME/.zshrc"
    echo "export PATH=\"\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator:\$PATH\"" >> "$HOME/.zshrc"
    echo "✅ Added Android tools to ~/.zshrc (Restart terminal to apply globally)"
else
    echo "✅ Android tools already in configuration."
fi

# Check ADB
echo "📡 Checking ADB status..."
which adb
adb version

echo "🚀 Launching Emulator (Medium_Phone_API_36.1)..."
# Check if emulator is already running
if adb devices | grep -q "emulator"; then
    echo "✅ Emulator already running."
else
    echo "⏳ Booting Emulator in background..."
    nohup emulator -avd Medium_Phone_API_36.1 -netdelay none -netspeed full > /dev/null 2>&1 &
    echo "✅ Emulator launched. Please wait ~30 seconds for boot."
fi

echo "---------------------------------------------------"
echo "🎉 Environment Ready."
echo "   Run: ./gradlew installDebug   <-- To install app"
echo "---------------------------------------------------"

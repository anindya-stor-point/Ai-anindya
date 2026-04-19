# AI Vision Guide - Android Project 🚀

This is a professional Android application project designed for "Live Screen Observation and User Guidance" using the power of **Gemini 1.5 Flash**.

## Features 🌟
- **Live Screen Capture**: Uses Android MediaProjection API.
- **AI Guidance**: Analyzes screenshots in real-time to suggest the next user action.
- **Accessibility Integration**: Designed to work as an overlay on top of other apps.
- **Dynamic Overlays**: Renders guidance markers (arrows/boxes) directly on the UI coordinate system.

## Project Structure 📁
- `main.py`: Core logic using Kivy/KivyMD.
- `buildozer.spec`: Configuration for Android APK packaging.
- `requirements.txt`: Python dependencies (Kivy, MD, Gemini SDK, etc.).
- `.github/workflows/android.yml`: CI workflow for automated builds.

## How to Build 🛠️

1. **Local Setup**:
   Clone this repository to your local Linux machine or use Google Colab.
   
2. **Configure API Key**:
   Ensure your `GEMINI_API_KEY` is set in your environment variables.

3. **Run Buildozer**:
   ```bash
   buildozer -v android debug
   ```
   *Note: Ensure you have the necessary Android SDK/NDK dependencies installed.*

4. **Install APK**:
   Find the resulting APK in the `bin/` folder and install it on your Android device.

## License 📄
Apache-2.0

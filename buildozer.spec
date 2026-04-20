# AI Vision Guide - Android Build Configuration
[app]

# (str) Title of your application
title = AI Vision Guide

# (str) Package name
package.name = aivisionguide

# (str) Package domain (usually com.yourdomain)
package.domain = org.ai.tools

# (str) Source code where the main.py live
source.dir = .

# (list) Source files to include (let empty to include all the files)
source.include_exts = py,png,jpg,kv,atlas,json

# (str) Application versioning (method 1)
version = 0.1

# (list) Application requirements
# comma separated e.g. requirements = sqlite3,kivy
requirements = python3,kivy==2.3.0,kivymd==1.2.0,pillow,pyjnius,python-dotenv,openssl,requests,urllib3,certifi,idna,charset-normalizer

# (str) Custom source folders for PyobjC (iOS)
#osx.python_arch = arm64

# (list) Permissions
android.permissions = INTERNET, SYSTEM_ALERT_WINDOW, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, FOREGROUND_SERVICE, CAMERA, RECORD_AUDIO

# (int) Android API to use
android.api = 33

# (int) Minimum API your APK will support
android.minapi = 21

# (str) Android SDK directory
android.sdk_path = /usr/local/lib/android/sdk

# (str) Android NDK directory 
android.ndk_path = /usr/local/lib/android/sdk/ndk/25.2.9519653

# (str) Android NDK version to use
android.ndk = 25b

# (bool) use poseidon (android only)
#android.use_poseidon = True

# (list) Services to create
#android.services = monitor:monitor_service.py

# (list) Android entry point, default is to use start.py
#android.entrypoint = org.kivy.android.PythonActivity

# (list) Screen orientation
orientation = portrait

# (list) List of service to declare
# Specify any service that needs to be declared in AndroidManifest
# Format: <service_name>:<python_file>
#android.services = screenmonitor:service.py

# (list) The Android architectures to build for, choices: armeabi-v7a, arm64-v8a, x86, x86_64
android.archs = arm64-v8a

[buildozer]

# (int) Log level (0 = error only, 1 = info, 2 = debug (with command output))
log_level = 2

# (int) Display warning if buildozer is run as root (0 = off, 1 = on)
warn_on_root = 1

# (bool) Use the system Android SDK/NDK
android.accept_sdk_license = True

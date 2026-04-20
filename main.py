# AI Vision Guide - Background Service & Overlay Implementation
import os
import json
import threading
import time
from io import BytesIO

from kivy.app import App
from kivy.lang import Builder
from kivy.uix.floatlayout import FloatLayout
from kivy.graphics import Color, Line, Rectangle
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.utils import platform

from kivymd.app import MDApp
from kivymd.uix.button import MDFillRoundFlatButton
from kivymd.uix.snackbar import Snackbar
from kivy.uix.popup import Popup
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.boxlayout import BoxLayout
import traceback

def show_error_popup(error_msg):
    # This creates a popup to show errors directly on the phone screen
    content = BoxLayout(orientation='vertical', padding=10)
    lbl = Label(text=f"App Error Output:\n\n{error_msg}", halign="center", valign="middle")
    lbl.bind(size=lbl.setter('text_size'))
    btn = Button(text="Close Application", size_hint=(1, 0.2))
    content.add_widget(lbl)
    content.add_widget(btn)
    
    popup = Popup(title='Critical Crash Detected', content=content, size_hint=(0.9, 0.8), auto_dismiss=False)
    btn.bind(on_release=popup.dismiss)
    popup.open()

# Gemini SDK Integration (Replaced with Raw requests for Android Stability)
import requests
import base64
from PIL import Image

# Android Native Integration
if platform == 'android':
    from jnius import autoclass, cast
    from android.permissions import request_permissions, Permission
    PythonActivity = autoclass('org.kivy.android.PythonActivity')
    currentActivity = PythonActivity.mActivity
    
    def move_app_to_background():
        # Moves the current task to the background (user returns to home screen/previous app)
        currentActivity.moveTaskToBack(True)

KV = '''
MDScreen:
    md_bg_color: 0, 0, 0, 0.3  # Slight tint for the main UI
    
    MDCard:
        size_hint: 0.85, 0.45
        pos_hint: {"center_x": .5, "center_y": .5}
        padding: "20dp"
        spacing: "15dp"
        orientation: "vertical"
        radius: [25, ]
        elevation: 4
        md_bg_color: 0.1, 0.1, 0.1, 1

        MDLabel:
            text: "AI VISION GUIDE"
            halign: "center"
            font_style: "H5"
            bold: True
            theme_text_color: "Custom"
            text_color: 1, 1, 1, 1

        MDLabel:
            id: status_label
            text: "Ready to assist"
            halign: "center"
            theme_text_color: "Secondary"
            font_style: "Caption"

        MDFillRoundFlatButton:
            id: action_btn
            text: "START GUIDANCE"
            font_size: "18sp"
            size_hint: (1, 0.25)
            on_release: app.start_ai_service()
            md_bg_color: 0, 0.5, 0.2, 1

        MDLabel:
            text: "Background Analysis Active"
            halign: "center"
            font_style: "Overline"
            theme_text_color: "Hint"
'''

class AIVisionApp(MDApp):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.is_active = False
        self.guidance_box = None
        # User requested fallback key
        user_key = "AIzaSyCb_jOE_RIDEmY59HDAqpHyb_CUO5viCs0"
        self.api_key = os.environ.get("GEMINI_API_KEY", user_key)
        
        if self.api_key:
            self.model_active = True
            print("API Key loaded, AI Service Ready.")
        else:
            self.model_active = False

    def build(self):
        try:
            self.theme_cls.primary_palette = "Blue"
            self.theme_cls.theme_style = "Dark"
            # Solid standard background to prevent completely blank transparent loading
            Window.clearcolor = (0.05, 0.05, 0.15, 1)
            return Builder.load_string(KV)
        except Exception as e:
            traceback_msg = traceback.format_exc()
            Clock.schedule_once(lambda dt: show_error_popup(f"Build Error: {str(e)}\n\n{traceback_msg}"))
            return Label(text="Fatal Error Initializing UI")

    def on_start(self):
        pass # Moving permission request to button click so it doesn't freeze the startup dashboard

    def check_overlay_permission(self):
        # Checks if we have permission to draw over other apps
        if platform == 'android':
            try:
                Settings = autoclass('android.provider.Settings')
                if not Settings.canDrawOverlays(currentActivity):
                    Snackbar(text="পারমিশন দিন: 'Display over other apps'").open()
                    Intent = autoclass('android.content.Intent')
                    Uri = autoclass('android.net.Uri')
                    intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                   Uri.parse("package:" + currentActivity.getPackageName()))
                    currentActivity.startActivity(intent)
                    return False
            except Exception as e:
                print(f"Overlay Permission Error: {e}")
                return True # Fallback to continue if check fails
        return True

    def on_pause(self):
        # CRITICAL: This keeps the app running when it goes to background
        return True

    def on_resume(self):
        pass

    def start_ai_service(self):
        if not self.api_key:
            Snackbar(text="Error: API Key missing!").open()
            return
            
        if platform == 'android':
            from android.permissions import request_permissions, Permission
            request_permissions([
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.RECORD_AUDIO,
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.FOREGROUND_SERVICE"
            ])

        if not self.check_overlay_permission():
            return

        self.is_active = True
        # Update UI state
        self.root.ids.status_label.text = "Background AI Active"
        self.root.ids.status_label.text_color = (0, 0.8, 0, 1)
        self.root.ids.action_btn.opacity = 0.5
        self.root.ids.action_btn.disabled = True
        self.root.ids.action_btn.text = "SERVICE RUNNING"
        
        # Move app to background so user sees their phone screen
        if platform == 'android':
            try:
                # Start the Background Foreground Service using Kivy's generated Java class
                from jnius import autoclass
                from android import activity
                
                NativeHelper = autoclass('org.ai.tools.aivisionguide.NativeHelper')
                mActivity = autoclass('org.kivy.android.PythonActivity').mActivity
                
                def on_activity_result(request_code, result_code, intent):
                    if request_code == 1000:
                        if result_code == -1: # RESULT_OK
                            Snackbar(text="Screen Access Granted!").open()
                            
                            # Get correct display metrics natively
                            ScreenMetrics = autoclass('android.util.DisplayMetrics')
                            metrics = ScreenMetrics()
                            mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics)
                            w = metrics.widthPixels
                            h = metrics.heightPixels
                            dpi = metrics.densityDpi
                            
                            intent_uri = intent.toUri(0)
                            args = f"{self.api_key}|||{w}|||{h}|||{dpi}|||{intent_uri}"
                            
                            # Start Background Service
                            service = autoclass('org.ai.tools.aivisionguide.ServiceAivision')
                            service.start(mActivity, args)
                            
                            move_app_to_background()
                            Snackbar(text="Realtime Native Vision Active").open()
                            
                            # Resets the button if they come back
                            self.root.ids.action_btn.opacity = 1.0
                            self.root.ids.action_btn.disabled = False
                            self.root.ids.action_btn.text = "START GUIDANCE"
                        else:
                            Snackbar(text="Permission Denied").open()
                            self.root.ids.action_btn.opacity = 1.0
                            self.root.ids.action_btn.disabled = False
                            self.root.ids.action_btn.text = "START GUIDANCE"
                            
                activity.bind(on_activity_result=on_activity_result)
                NativeHelper.requestCapture(mActivity, 1000)
            except Exception as e:
                print(f"Foreground setup failed: {e}")
                Snackbar(text=f"Setup failed: {e}").open()

    def trigger_analysis(self):
        pass # Not used in native loop

    def main_loop(self):
        pass # Deprecated by Native Java Background Service


    def display_guidance(self, x, y, tip):
        if self.is_active and self.guidance_box:
            self.guidance_box.show_box(x, y)
            Snackbar(text=f"AI গাইড: {tip}").open()

if __name__ == '__main__':
    try:
        AIVisionApp().run()
    except Exception as e:
        # Fallback for errors that happen before Kivy can even start
        print(f"CRITICAL STARTUP ERROR:\n{traceback.format_exc()}")

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

# Gemini SDK Integration
import google.generativeai as genai
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

class InteractionBox(FloatLayout):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.size_val = 150
        self.app_ref = App.get_running_app()
        self.active_coord = None
        
    def show_box(self, x, y):
        self.active_coord = (x, y)
        self.canvas.clear()
        with self.canvas:
            # Thick Red Outline
            Color(0.9, 0, 0, 1)
            Line(rectangle=(x - self.size_val/2, y - self.size_val/2, self.size_val, self.size_val), width=8)
            # Semi-transparent red fill
            Color(0.9, 0, 0, 0.1)
            Rectangle(pos=(x - self.size_val/2, y - self.size_val/2), size=(self.size_val, self.size_val))
            
    def on_touch_down(self, touch):
        # Check if user clicked inside the guidance box
        if self.active_coord:
            cx, cy = self.active_coord
            if abs(touch.x - cx) < self.size_val and abs(touch.y - cy) < self.size_val:
                self.hide_box()
                # Trigger immediate new analysis cycle
                self.app_ref.trigger_analysis()
                return True
        return super().on_touch_down(touch)

    def hide_box(self):
        self.active_coord = None
        self.canvas.clear()

class AIVisionApp(MDApp):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.is_active = False
        self.guidance_box = None
        # User requested fallback key
        user_key = "AIzaSyCb_jOE_RIDEmY59HDAqpHyb_CUO5viCs0"
        self.api_key = os.environ.get("GEMINI_API_KEY", user_key)
        
        if self.api_key:
            try:
                # Using 'rest' transport is often more stable on Android than gRPC
                genai.configure(api_key=self.api_key, transport='rest')
                self.model = genai.GenerativeModel('gemini-1.5-flash')
            except Exception as e:
                print(f"Model init error: {e}")
                self.model = None
        else:
            self.model = None

    def build(self):
        try:
            self.theme_cls.primary_palette = "DeepPurple"
            self.theme_cls.theme_style = "Dark"
            # Configure Window to be transparent
            # Black with 0 alpha (transparent)
            Window.clearcolor = (0, 0, 0, 0)
            return Builder.load_string(KV)
        except Exception as e:
            traceback_msg = traceback.format_exc()
            Clock.schedule_once(lambda dt: show_error_popup(f"Build Error: {str(e)}\n\n{traceback_msg}"))
            return Label(text="Fatal Error Initializing UI")

    def on_start(self):
        try:
            if platform == 'android':
                request_permissions([
                    Permission.READ_EXTERNAL_STORAGE,
                    Permission.WRITE_EXTERNAL_STORAGE,
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.FOREGROUND_SERVICE"
                ])
        except Exception as e:
            traceback_msg = traceback.format_exc()
            Clock.schedule_once(lambda dt: show_error_popup(f"Startup/Permission Error:\n{str(e)}\n\n{traceback_msg}"))

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

        if not self.check_overlay_permission():
            return

        self.is_active = True
        # Update UI state
        self.root.ids.status_label.text = "Background AI Active"
        self.root.ids.status_label.text_color = (0, 0.8, 0, 1)
        self.root.ids.action_btn.opacity = 0.5
        self.root.ids.action_btn.disabled = True
        self.root.ids.action_btn.text = "SERVICE RUNNING"
        
        # Add the interaction overlay
        if not self.guidance_box:
            self.guidance_box = InteractionBox()
            Window.add_widget(self.guidance_box)
            
        # Move app to background so user sees their phone screen
        if platform == 'android':
            move_app_to_background()
            
        # Start background loop
        threading.Thread(target=self.main_loop, daemon=True).start()
        Snackbar(text="AI Logic Active in Background").open()

    def trigger_analysis(self):
        # Single analysis pass
        threading.Thread(target=self.analyze_single_frame, daemon=True).start()

    def analyze_single_frame(self):
        # Capture logic
        try:
            time.sleep(1.5) # Time for screen to settle
            
            # Use app's local path to ensure write permissions
            save_path = os.path.join(self.user_data_dir, "frame.png")
            Window.screenshot(save_path)
            
            # Wait for file to exist
            for _ in range(5):
                if os.path.exists(save_path): break
                time.sleep(0.1)
                
            img = Image.open(save_path)
            self.run_ai_logic(img)
        except Exception as e:
            print(f"Frame error: {e}")

    def main_loop(self):
        while self.is_active:
            # Only analyze if no box is showing
            if self.guidance_box and self.guidance_box.active_coord is None:
                self.analyze_single_frame()
            time.sleep(8) # Standard periodic analysis

    def run_ai_logic(self, img):
        if not self.model: return

        prompt = """
        Role: Android UI Expert & Visual Guide.
        Task: Analyze the screenshot and provide one critical next step for the user.
        Rules:
        1. Find X and Y as percentages (0-100).
        2. Give a short, helpful guidance tip in BENGALI (max 5-7 words).
        3. Be specific: "নীল বাটনটিতে ক্লিক করুন", "ব্যাকে যান", "সার্চ বারে লিখুন"।
        Output MUST be strictly valid JSON:
        {
            "x_p": float,
            "y_p": float,
            "tip": "Short Bengali Instruction"
        }
        """
        
        try:
            response = self.model.generate_content([prompt, img])
            # Parse response
            raw_text = response.text.replace('```json', '').replace('```', '').strip()
            data = json.loads(raw_text)
            
            # Map percentages to system pixels
            w, h = Window.size
            x = (data['x_p'] / 100.0) * w
            y = ((100 - data['y_p']) / 100.0) * h
            
            Clock.schedule_once(lambda dt: self.display_guidance(x, y, data['tip']))
        except Exception as e:
            print(f"AI Failure: {e}")

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

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
    md_bg_color: 0, 0, 0, 0  # Background transparency
    
    MDFloatLayout:
        MDFillRoundFlatButton:
            id: action_btn
            text: "START SERVICE"
            font_size: "20sp"
            size_hint: (0.7, 0.1)
            pos_hint: {"center_x": .5, "center_y": .5}
            on_release: app.start_ai_service()
            md_bg_color: 0, 0.4, 0, 1
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
        self.api_key = os.environ.get("GEMINI_API_KEY", "")
        if self.api_key:
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel('gemini-1.5-flash')
        else:
            self.model = None

    def build(self):
        # Configure Window to be transparent
        Window.clearcolor = (0, 0, 0, 0)
        return Builder.load_string(KV)

    def on_start(self):
        if platform == 'android':
            request_permissions([
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.FOREGROUND_SERVICE"
            ])

    def start_ai_service(self):
        if not self.api_key:
            Snackbar(text="Error: API Key missing!").open()
            return

        self.is_active = True
        # Hide the main button
        self.root.ids.action_btn.opacity = 0
        self.root.ids.action_btn.disabled = True
        
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
        # Note: Window.screenshot captures the app's current window.
        # For actual 3rd party app observation, MediaProjection is required.
        try:
            time.sleep(1.5) # Time for screen to settle
            Window.screenshot("frame.png")
            img = Image.open("frame.png")
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
        Analysis: You are a mobile guide assistant. Look at the screen.
        Identify the NEXT logical touch point to help the user.
        Return ONLY valid JSON:
        {
            "x_p": float (0-100),
            "y_p": float (0-100),
            "tip": "short instructions in Bengali"
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
    AIVisionApp().run()

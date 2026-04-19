import os
import json
import base64
import threading
import time
from io import BytesIO

from kivy.app import App
from kivy.lang import Builder
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.graphics import Color, Ellipse, Line
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.utils import platform

from kivymd.app import MDApp
from kivymd.uix.button import MDRaisedButton, MDFillRoundFlatButton
from kivymd.uix.label import MDLabel
from kivymd.uix.snackbar import Snackbar

# Gemini SDK
import google.generativeai as genai
from PIL import Image

# For Android specific APIs
if platform == 'android':
    from jnius import autoclass, cast
    from android.permissions import request_permissions, Permission
    PythonActivity = autoclass('org.kivy.android.PythonActivity')
    currentActivity = PythonActivity.mActivity
    Context = autoclass('android.content.Context')
    Intent = autoclass('android.content.Intent')
    MediaProjectionManager = autoclass('android.media.projection.MediaProjectionManager')

KV = '''
ScreenManager:
    MainScreen:

<MainScreen@Screen>:
    name: 'main'
    MDFloatLayout:
        md_bg_color: 1, 1, 1, 1
        
        MDLabel:
            text: "AI Vision Guide"
            halign: "center"
            pos_hint: {"center_y": .8}
            font_style: "H4"
            theme_text_color: "Primary"

        MDLabel:
            text: "লাইভ স্ক্রিন পর্যবেক্ষণ শুরু করতে নিচের বাটনে ক্লিক করুন"
            halign: "center"
            pos_hint: {"center_y": .7}
            theme_text_color: "Secondary"

        MDFillRoundFlatButton:
            text: "START OBSERVATION"
            pos_hint: {"center_x": .5, "center_y": .4}
            on_release: app.start_recording()

        MDFillRoundFlatButton:
            text: "STOP OBSERVATION"
            pos_hint: {"center_x": .5, "center_y": .3}
            on_release: app.stop_recording()
            md_bg_color: 0.8, 0.2, 0.2, 1

        MDLabel:
            id: status_label
            text: "Status: Idle"
            halign: "center"
            pos_hint: {"center_y": .1}
'''

class GuidanceOverlay(FloatLayout):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.points = []
        
    def draw_marker(self, x, y):
        self.canvas.clear()
        with self.canvas:
            Color(1, 0, 0, 0.8)  # Glowing Red
            # Draw an arrow or a pulse
            Ellipse(pos=(x - 25, y - 25), size=(50, 50))
            Line(circle=(x, y, 40), width=2)
            
    def clear_marker(self):
        self.canvas.clear()

class AIVisionApp(MDApp):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.recording = False
        self.overlay = None
        self.gemini_key = os.environ.get("GEMINI_API_KEY", "")
        if self.gemini_key:
            genai.configure(apiKey=self.gemini_key)
            self.model = genai.GenerativeModel('gemini-1.5-flash')
        else:
            self.model = None

    def build(self):
        self.theme_cls.primary_palette = "DeepPurple"
        return Builder.load_string(KV)

    def on_start(self):
        if platform == 'android':
            request_permissions([
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.CAMERA,
                Permission.RECORD_AUDIO
            ])

    def start_recording(self):
        if not self.gemini_key:
            Snackbar(text="Error: Gemini API Key not found!").open()
            return
            
        if self.recording:
            return
            
        self.recording = True
        self.root.get_screen('main').ids.status_label.text = "Status: Observing..."
        
        # Start a loop in a thread to capture screen and analyze
        threading.Thread(target=self.analysis_loop, daemon=True).start()
        
        # In actual Android, we'd trigger the MediaProjection prompt here
        Snackbar(text="Observing your screen...").open()

    def stop_recording(self):
        self.recording = False
        self.root.get_screen('main').ids.status_label.text = "Status: Stopped"
        if self.overlay:
            self.overlay.clear_marker()

    def capture_screenshot(self):
        # SIMULATION for non-android / Placeholder for Android
        # In real Android: use MediaProjection -> ImageReader -> Bitmap -> PIL.Image
        try:
            # Dummy capture for desktop testing
            Window.screenshot("screenshot.png")
            return Image.open("screenshot.png")
        except Exception as e:
            print(f"Capture error: {e}")
            return None

    def analysis_loop(self):
        while self.recording:
            img = self.capture_screenshot()
            if img:
                self.analyze_and_guide(img)
            time.sleep(5)  # Analyze every 5 seconds to manage API quota

    def analyze_and_guide(self, image):
        if not self.model:
            return

        prompt = """
        Analyze this mobile screen screenshot. 
        Identify the most likely next step for the user if they are trying to 
        navigate commonly. 
        Return ONLY a JSON object with the following format:
        {
            "action": "click",
            "reason": "short explanation",
            "x_percent": float (0-100),
            "y_percent": float (0-100)
        }
        Do not include any other text.
        """
        
        try:
            response = self.model.generate_content([prompt, image])
            data = json.loads(response.text)
            
            # Convert percentage to screen coordinates
            win_w, win_h = Window.size
            x = (data['x_percent'] / 100.0) * win_w
            y = ((100 - data['y_percent']) / 100.0) * win_h # Kivy Y is bottom-up
            
            # Update UI on main thread
            Clock.schedule_once(lambda dt: self.show_guidance(x, y, data['reason']))
            
        except Exception as e:
            print(f"Analysis error: {e}")

    def show_guidance(self, x, y, message):
        if not self.overlay:
            self.overlay = GuidanceOverlay()
            Window.add_widget(self.overlay)
            
        self.overlay.draw_marker(x, y)
        Snackbar(text=f"Guide: {message}").open()

if __name__ == '__main__':
    AIVisionApp().run()

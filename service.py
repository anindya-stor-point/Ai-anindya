import os
import time
import requests
import base64
import json
from io import BytesIO
from PIL import Image

def process_frame(api_key, img_path, result_path):
    if not os.path.exists(img_path):
        return

    try:
        # Load image safely
        img = Image.open(img_path)
        
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
        
        buffered = BytesIO()
        img.save(buffered, format="JPEG")
        img_b64 = base64.b64encode(buffered.getvalue()).decode("utf-8")

        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={api_key}"
        headers = {'Content-Type': 'application/json'}
        payload = {
            "contents": [{
                "parts": [
                    {"text": prompt},
                    {"inline_data": {"mime_type": "image/jpeg", "data": img_b64}}
                ]
            }],
            "generationConfig": {"response_mime_type": "application/json"}
        }

        res = requests.post(url, headers=headers, json=payload, timeout=15)
        if res.status_code == 200:
            response_data = res.json()
            raw_text = response_data['candidates'][0]['content']['parts'][0]['text']
            raw_text = raw_text.replace('```json', '').replace('```', '').strip()
            
            # Write AI output to result.json so the main Kivy UI can consume it
            with open(result_path, 'w', encoding='utf-8') as f:
                f.write(raw_text)
                
            # Remove the frame so we don't process the same one again
            os.remove(img_path)
        else:
            print(f"Service API Error: {res.text}")
            
    except Exception as e:
        print(f"Service Execution Failure: {e}")

if __name__ == '__main__':
    # Argument passed via python service startup
    api_key = os.environ.get('PYTHON_SERVICE_ARGUMENT', '')
    
    # Obtain the secure writable path
    user_data_dir = os.environ.get('ANDROID_PRIVATE', '')
    if not user_data_dir:
        # Fallback for testing on desktop environments
        user_data_dir = '.'
        
    img_path = os.path.join(user_data_dir, 'frame.png')
    result_path = os.path.join(user_data_dir, 'result.json')
    
    # Continuous Foreground Service Loop
    while True:
        process_frame(api_key, img_path, result_path)
        # Prevent tight looping; Android services should yield
        time.sleep(1)

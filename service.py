import os
from jnius import autoclass
import time

if __name__ == '__main__':
    args = os.environ.get('PYTHON_SERVICE_ARGUMENT', '')
    if '|||' in args:
        parts = args.split('|||')
        api_key = parts[0]
        w = int(parts[1])
        h = int(parts[2])
        dpi = int(parts[3])
        intent_uri = parts[4]
        
        try:
            # Reconstruct the MediaProjection Intent
            Intent = autoclass('android.content.Intent')
            intent = Intent.parseUri(intent_uri, 0)
            
            # Use the active PythonService context
            PythonService = autoclass('org.kivy.android.PythonService')
            mService = PythonService.mService
            
            # Start the completely Native Java Loop
            NativeHelper = autoclass('org.ai.tools.aivisionguide.NativeHelper')
            NativeHelper.startContinuousAnalysis(mService, api_key, intent, w, h, dpi)
            
            # Keep Python Service shell alive without eating CPU
            while True:
                time.sleep(30)
                
        except Exception as e:
            print(f"Service Native Launcher Failed: {e}")
            import traceback
            traceback.print_exc()
    else:
        print("Invalid Service arguments. Expected API_KEY|||w|||h|||dpi|||INTENT_URI")


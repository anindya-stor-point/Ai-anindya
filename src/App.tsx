/** AI Vision Guide - App Simulator **/
import React, { useState } from 'react';
import { Play, Square, Smartphone } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// Using a clean home screen image as the "Live Background" for simulation
const LIVE_BACKGROUND = "https://picsum.photos/seed/android-home/720/1280";

export default function App() {
  const [isActive, setIsActive] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [guidance, setGuidance] = useState<{ x: number, y: number, msg: string } | null>(null);

  const performAnalysis = async () => {
    if (!isActive) return;
    setIsAnalyzing(true);
    setGuidance(null);

    // Mocking AI response for the simulation environment
    try {
      await new Promise(r => setTimeout(r, 1500));
      const res = {
        x: 30 + Math.random() * 40,
        y: 40 + Math.random() * 40,
        msg: "এই অ্যাপটি ওপেন করুন"
      };
      setGuidance(res);
    } catch (e) {
      console.error(e);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const startService = () => {
    setIsActive(true);
    setTimeout(performAnalysis, 500);
  };

  const handleBoxClick = () => {
    setGuidance(null);
    setTimeout(performAnalysis, 600);
  };

  return (
    <div className="min-h-screen bg-black flex items-center justify-center p-6 select-none font-sans">
      
      {/* Phone Frame */}
      <div className="relative w-full max-w-[340px] aspect-[9/19] bg-black rounded-[3rem] border-[8px] border-[#222] shadow-2xl overflow-hidden flex flex-col">
        
        {/* Notch Area */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-6 bg-[#222] rounded-b-2xl z-50" />

        {/* Content Area */}
        <div className="flex-1 relative flex flex-col bg-black overflow-hidden">
          
          <AnimatePresence>
            {!isActive ? (
               /* Initial App Screen */
               <motion.div 
                 key="app-ui"
                 initial={{ opacity: 0 }}
                 animate={{ opacity: 1 }}
                 exit={{ scale: 1.1, opacity: 0 }}
                 className="absolute inset-0 z-40 bg-black flex flex-col items-center justify-center p-10"
               >
                 <motion.button
                   whileTap={{ scale: 0.9 }}
                   onClick={startService}
                   className="w-full h-14 bg-green-700 text-white rounded-xl font-black text-xs tracking-widest shadow-lg active:bg-green-800 transition-all flex items-center justify-center gap-2 uppercase"
                 >
                   <Play size={16} fill="currentColor" />
                   START SERVICE
                 </motion.button>
               </motion.div>
            ) : (
               /* Background Mode Simulator */
               <motion.div 
                 key="live-bg"
                 initial={{ opacity: 0 }}
                 animate={{ opacity: 1 }}
                 className="absolute inset-0 z-0 h-full w-full"
               >
                 {/* Live Background (Mobile Home Screen Sim) */}
                 <img src={LIVE_BACKGROUND} className="w-full h-full object-cover opacity-50 grayscale-[0.2]" alt="Live Screen" />
                 
                 {/* The Red Box Overlay */}
                 <AnimatePresence>
                    {guidance && (
                      <motion.div
                        initial={{ opacity: 0, scale: 0.8 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 1.5 }}
                        className="absolute cursor-pointer z-30"
                        style={{ left: `${guidance.x}%`, top: `${guidance.y}%`, transform: 'translate(-50%, -50%)' }}
                        onClick={handleBoxClick}
                      >
                         <div className="w-24 h-24 border-[4px] border-red-600 bg-red-600/10 rounded shadow-[0_0_20px_rgba(220,0,0,0.4)]" />
                      </motion.div>
                    )}
                 </AnimatePresence>

                 {/* Simulated Snackbar at bottom */}
                 <AnimatePresence>
                   {guidance && (
                     <motion.div
                       initial={{ y: 100 }}
                       animate={{ y: 0 }}
                       exit={{ y: 100 }}
                       className="absolute bottom-10 left-4 right-4 bg-zinc-900 border border-zinc-800 text-white p-3 rounded shadow-xl z-50 text-[10px] font-medium"
                     >
                        AI গাইড: {guidance.msg}
                     </motion.div>
                   )}
                 </AnimatePresence>

                 {/* Analyzing Indicator (Subtle) */}
                 {isAnalyzing && (
                   <div className="absolute top-10 w-full flex justify-center z-50">
                     <div className="bg-black/50 backdrop-blur px-3 py-1 rounded-full flex items-center gap-2">
                        <div className="w-2 h-2 border border-red-500 border-t-transparent rounded-full animate-spin" />
                        <span className="text-[8px] font-bold text-white/50 tracking-tighter uppercase whitespace-nowrap">Analysing...</span>
                     </div>
                   </div>
                 )}

                 {/* Exit Simulator Button */}
                 <button 
                  onClick={() => setIsActive(false)}
                  className="absolute bottom-4 left-1/2 -translate-x-1/2 p-2 bg-white/5 rounded-full text-white/10 hover:text-white/40 transition-all z-50"
                 >
                   <Square size={12} fill="currentColor" />
                 </button>
               </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

    </div>
  );
}

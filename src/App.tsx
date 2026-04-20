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
      await new Promise(r => setTimeout(r, 1200));
      const res = {
        x: 20 + Math.random() * 60,
        y: 20 + Math.random() * 60,
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
    setTimeout(performAnalysis, 400);
  };

  return (
    <div className="min-h-screen bg-[#0a0a0a] flex items-center justify-center p-6 select-none font-sans">
      
      {/* Phone Frame */}
      <div className="relative w-full max-w-[340px] aspect-[9/19] bg-black rounded-[3rem] border-[10px] border-[#1a1a1a] shadow-[0_50px_100px_-20px_rgba(0,0,0,0.9)] overflow-hidden flex flex-col">
        
        {/* Notch Area */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-28 h-6 bg-[#1a1a1a] rounded-b-2xl z-50 flex items-center justify-center">
            <div className="w-10 h-1 bg-white/5 rounded-full" />
        </div>

        {/* Content Area */}
        <div className="flex-1 relative flex flex-col bg-black overflow-hidden">
          
          <AnimatePresence>
            {!isActive ? (
               /* Initial App Screen */
               <motion.div 
                 key="app-ui"
                 initial={{ opacity: 0 }}
                 animate={{ opacity: 1 }}
                 exit={{ opacity: 0 }}
                 className="absolute inset-0 z-40 bg-[#050505] flex flex-col items-center justify-center p-8"
               >
                 <motion.button
                   whileTap={{ scale: 0.95 }}
                   onClick={startService}
                   className="w-full h-16 bg-green-600 text-white rounded-2xl font-black text-sm tracking-[0.2em] shadow-xl shadow-green-600/20 active:bg-green-700 transition-colors flex items-center justify-center gap-3 uppercase"
                 >
                   <Play size={18} fill="currentColor" />
                   START SERVICE
                 </motion.button>
               </motion.div>
            ) : (
               /* Background Guidance Mode */
               <motion.div 
                 key="live-bg"
                 initial={{ opacity: 0 }}
                 animate={{ opacity: 1 }}
                 className="absolute inset-0 z-0 h-full w-full"
               >
                 {/* Live Background Snapshot */}
                 <img src={LIVE_BACKGROUND} className="w-full h-full object-cover opacity-60 grayscale-[0.3]" alt="Live Screen" />
                 
                 {/* AI Overlay Layer */}
                 <AnimatePresence>
                    {guidance && (
                      <motion.div
                        initial={{ opacity: 0, scale: 0.8 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 1.2 }}
                        className="absolute cursor-pointer z-30"
                        style={{ left: `${guidance.x}%`, top: `${guidance.y}%`, transform: 'translate(-50%, -50%)' }}
                        onClick={handleBoxClick}
                      >
                         <div className="relative flex flex-col items-center">
                           <div className="bg-red-600 text-white text-[9px] font-bold px-2 py-1 rounded mb-2 whitespace-nowrap shadow-lg border border-white/20 uppercase tracking-tighter">
                             {guidance.msg}
                           </div>
                           <div className="w-24 h-24 border-[5px] border-red-600 bg-red-600/10 rounded-lg shadow-[0_0_30px_rgba(220,38,38,0.5)]">
                             <div className="absolute inset-0 animate-ping border border-red-600/30 rounded-lg opacity-40" />
                           </div>
                         </div>
                      </motion.div>
                    )}
                 </AnimatePresence>

                 {/* Analyzing Indicator */}
                 {isAnalyzing && (
                   <div className="absolute inset-x-0 bottom-12 flex justify-center z-50">
                     <div className="bg-white/5 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
                        <div className="w-4 h-4 border-2 border-red-500 border-t-transparent rounded-full animate-spin" />
                        <span className="text-[10px] font-black text-white/50 uppercase tracking-widest italic">AI Analyzing...</span>
                     </div>
                   </div>
                 )}

                 {/* Control Overlay (Only for Simulator Reset) */}
                 <button 
                  onClick={() => setIsActive(false)}
                  className="absolute top-10 right-4 p-2 bg-white/5 rounded-full text-white/20 hover:text-white/60 transition-colors z-50"
                 >
                   <Square size={14} fill="currentColor" />
                 </button>
               </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Bottom Navigation (Simulated) */}
        <div className="h-10 w-full bg-transparent flex items-center justify-center gap-12 pb-2 opacity-20 pointer-events-none">
           <div className="w-12 h-1 bg-white/50 rounded-full" />
        </div>
      </div>

    </div>
  );
}

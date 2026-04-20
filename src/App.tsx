/** AI Vision Guide - App Simulator **/
import React, { useState } from 'react';
import { Play, Square, Smartphone } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

export default function App() {
  const [isActive, setIsActive] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [guidance, setGuidance] = useState<{ x: number, y: number, msg: string } | null>(null);

  const performAnalysis = async () => {
    if (!isActive) return;
    setIsAnalyzing(true);
    setGuidance(null);

    // Mocking AI response for simulation
    try {
      await new Promise(r => setTimeout(r, 1500));
      const res = {
        x: 30 + Math.random() * 40,
        y: 40 + Math.random() * 40,
        msg: "উদাহরণ: এই অ্যাপটি ওপেন করুন"
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
    <div className="min-h-screen bg-[#050505] flex flex-col select-none font-sans overflow-hidden">
      
      <AnimatePresence mode="wait">
        {!isActive ? (
            /* Initial App Screen - Full Screen */
            <motion.div 
              key="app-ui"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="fixed inset-0 z-40 bg-black flex flex-col items-center justify-center p-8"
            >
              <div className="mb-16 text-center">
                <motion.div
                  initial={{ y: 20, opacity: 0 }}
                  animate={{ y: 0, opacity: 1 }}
                  transition={{ delay: 0.2 }}
                >
                  <Smartphone className="w-24 h-24 text-green-500 mx-auto mb-6 opacity-40" />
                  <h1 className="text-white font-black text-4xl tracking-tighter sm:text-5xl">AI Vision Guide</h1>
                  <p className="text-zinc-500 text-xs sm:text-sm mt-4 uppercase tracking-[0.3em] font-medium">System Overlay Engine Ready</p>
                </motion.div>
              </div>

              <motion.button
                whileHover={{ scale: 1.05, backgroundColor: '#16a34a' }}
                whileTap={{ scale: 0.95 }}
                onClick={startService}
                className="w-full max-w-sm h-16 bg-green-600 text-white rounded-2xl font-black text-sm tracking-widest shadow-[0_20px_50px_rgba(22,163,74,0.3)] transition-all flex items-center justify-center gap-3 uppercase"
              >
                <Play size={20} fill="currentColor" />
                START SERVICE
              </motion.button>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-12 w-full max-w-3xl">
                <div className="bg-zinc-900/50 p-4 rounded-xl border border-white/5">
                  <h4 className="text-white text-[10px] font-black uppercase tracking-widest mb-3 opacity-60">System Features</h4>
                  <ul className="text-[11px] text-zinc-500 space-y-2">
                    <li className="flex items-center gap-2">• Real-time Screen Analysis</li>
                    <li className="flex items-center gap-2">• Bengali Voice Guidance (TTS)</li>
                    <li className="flex items-center gap-2">• Draggable Control Bubble</li>
                  </ul>
                </div>
                <div className="bg-zinc-900/50 p-4 rounded-xl border border-white/5">
                  <h4 className="text-white text-[10px] font-black uppercase tracking-widest mb-3 opacity-60">Success Tips</h4>
                  <ul className="text-[11px] text-zinc-500 space-y-2">
                    <li className="flex items-center gap-2">• Enable "Overlay" Permission</li>
                    <li className="flex items-center gap-2">• Disable Battery Optimization</li>
                    <li className="flex items-center gap-2">• Use voice for complex tasks</li>
                  </ul>
                </div>
              </div>

              <p className="fixed bottom-10 text-zinc-700 text-[10px] uppercase font-bold tracking-widest">v0.1.5-stable</p>
            </motion.div>
        ) : (
            /* Active State (System Monitor Simulator) - Full Screen */
            <motion.div 
              key="live-bg"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="fixed inset-0 z-0 bg-zinc-950 flex flex-col"
            >
              {/* Top Status Bar */}
              <div className="p-6 pt-12 border-b border-white/5 bg-zinc-900/50 flex justify-between items-center backdrop-blur-md">
                <div className="flex items-center gap-3">
                    <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse shadow-[0_0_10px_rgba(34,197,94,0.5)]" />
                    <span className="text-xs text-white/80 font-black uppercase tracking-widest">Observation Mode Active</span>
                </div>
                <div className="flex items-center gap-4 text-white/30 font-mono text-[10px] tracking-tight">
                    <span>UPTIME: 00:04:21</span>
                    <span className="hidden sm:inline">PID: 8842</span>
                </div>
              </div>

              {/* Main Monitoring Area (Abstract) */}
              <div className="flex-1 flex flex-col items-center justify-center p-10 text-center relative overflow-hidden">
                {/* Background Grid Pattern */}
                <div className="absolute inset-0 opacity-[0.03] pointer-events-none" 
                     style={{ backgroundImage: 'radial-gradient(circle, #fff 1px, transparent 1px)', backgroundSize: '40px 40px' }} />
                
                <div className="w-32 h-32 border border-white/10 rounded-[2.5rem] flex items-center justify-center mb-10 relative z-10">
                    <div className="absolute inset-0 border-2 border-green-500/20 rounded-[2.5rem] animate-ping" />
                    <Smartphone className="w-12 h-12 text-white/10" />
                </div>
                
                <div className="max-w-md mx-auto z-10">
                    <h3 className="text-white/60 text-lg font-black uppercase tracking-[0.2em] mb-4">Background Monitoring</h3>
                    <p className="text-white/30 text-xs sm:text-sm leading-relaxed font-medium">
                        সিস্টেম এখন আপনার ফোনের মেইন স্ক্রিন মনিটর করছে। এটি ব্যাকগ্রাউন্ড থেকে কাজ করবে এবং আপনাকে গাইডেন্স দেবে।
                    </p>
                </div>

                {/* Important Note for Web Preview */}
                <motion.div 
                  initial={{ y: 20, opacity: 0 }}
                  animate={{ y: 0, opacity: 1 }}
                  className="mt-12 bg-amber-500/10 border border-amber-500/20 p-6 rounded-2xl max-w-lg z-10"
                >
                    <p className="text-amber-500 text-xs leading-relaxed font-bold">
                        বিঃদ্রঃ: রিয়েল টাইম স্ক্রিন অবজারভেশন এবং ওভারলে শুধুমাত্র আপনার ফোনে APK ইন্সটল করলেই কাজ করবে। প্রিভিউতে এটি সিস্টেম স্ট্যাটাস ড্যাশবোর্ড হিসেবে দেখানো হচ্ছে।
                    </p>
                </motion.div>
              </div>
              
              {/* The Red Box Overlay Simulation (Abstract Position) */}
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
                      <div className="w-32 h-32 border-[4px] border-red-600 bg-red-600/10 rounded-2xl shadow-[0_0_30px_rgba(220,0,0,0.5)]" />
                      <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-red-600 text-white text-[10px] font-black py-1 px-3 rounded-full whitespace-nowrap uppercase tracking-tighter">
                          Interaction Point
                      </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Simulated Snackbar at bottom */}
              <AnimatePresence>
                {guidance && (
                  <motion.div
                    initial={{ y: 100, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: 100, opacity: 0 }}
                    className="fixed bottom-24 left-1/2 -translate-x-1/2 w-full max-w-xl px-4 z-50"
                  >
                    <div className="bg-zinc-900 border border-white/10 text-white p-5 rounded-2xl shadow-2xl backdrop-blur-xl">
                        <div className="flex items-center gap-4">
                            <div className="w-3 h-3 bg-red-500 rounded-full shrink-0" />
                            <div className="space-y-1">
                                <p className="text-[10px] text-white/30 uppercase font-black tracking-widest">AI Vision Suggestion</p>
                                <p className="text-sm sm:text-base font-bold leading-tight">{guidance.msg}</p>
                            </div>
                        </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Analyzing Indicator (System-like) */}
              <AnimatePresence>
                {isAnalyzing && (
                    <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 bg-black/80 backdrop-blur-[4px] z-20 flex flex-col items-center justify-center"
                    >
                        <div className="flex flex-col items-center gap-6">
                            <div className="w-12 h-12 border-4 border-green-500 border-t-transparent rounded-full animate-spin" />
                            <span className="text-xs font-black text-white/60 uppercase tracking-[0.4em]">Analyzing Screen Frame...</span>
                        </div>
                    </motion.div>
                )}
              </AnimatePresence>

              {/* Reset/Stop Button */}
              <div className="p-8 pb-12 flex justify-center bg-zinc-900/30 border-t border-white/5 backdrop-blur-md">
                <motion.button 
                  whileHover={{ backgroundColor: 'rgba(255,255,255,0.1)' }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setIsActive(false)}
                  className="px-10 py-4 bg-white/5 rounded-2xl text-white/40 hover:text-white transition-all text-xs font-black tracking-widest uppercase flex items-center gap-3 border border-white/5"
                >
                  <Square size={14} fill="currentColor" />
                  Stop Service
                </motion.button>
              </div>
            </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}

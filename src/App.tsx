/** AI Vision Guide - Web Simulator & Dashboard **/
import React, { useState, useRef } from 'react';
import { 
  Zap, 
  Smartphone, 
  Cpu, 
  ArrowRight, 
  Code2, 
  Download, 
  Eye, 
  Play, 
  Square,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// --- Components ---

const Header = () => (
  <header className="py-8 px-6 border-b border-white/5 flex items-center justify-between bg-[#0f172a]/80 backdrop-blur-xl sticky top-0 z-50">
    <div className="flex items-center gap-3">
      <div className="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-indigo-600/20">
        <Zap size={20} fill="currentColor" />
      </div>
      <div>
        <h1 className="text-xl font-bold text-white tracking-tight">ScreenGuide AI</h1>
        <p className="text-xs text-slate-400 font-medium tracking-wide">Professional Android Development</p>
      </div>
    </div>
    <div className="flex items-center gap-4">
      <span className="flex items-center gap-2 px-3 py-1 bg-emerald-500/10 text-emerald-400 rounded-lg text-[10px] font-bold border border-emerald-500/20 uppercase tracking-wider">
        <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
        Live Engine Active
      </span>
    </div>
  </header>
);

const FeatureCard = ({ icon: Icon, title, description }: { icon: any, title: string, description: string }) => (
  <motion.div 
    whileHover={{ y: -5, backgroundColor: "rgba(255, 255, 255, 0.08)" }}
    className="p-8 bg-white/5 backdrop-blur-xl rounded-[2rem] border border-white/10 shadow-sm transition-all duration-300 group"
  >
    <div className="w-14 h-14 bg-indigo-500/10 rounded-2xl flex items-center justify-center text-indigo-400 mb-6 group-hover:bg-indigo-600 group-hover:text-white transition-all duration-300">
      <Icon size={28} />
    </div>
    <h3 className="text-xl font-bold text-white mb-3 tracking-tight">{title}</h3>
    <p className="text-sm text-slate-400 leading-relaxed font-medium">{description}</p>
  </motion.div>
);

const Simulator = () => {
  const [image, setImage] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [guide, setGuide] = useState<{ x: number, y: number, text: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setImage(event.target?.result as string);
        setGuide(null);
      };
      reader.readAsDataURL(file);
    }
  };

  const runAnalysis = async () => {
    if (!image) return;
    setAnalyzing(true);
    setGuide(null);

    try {
      const result = await ai.models.generateContent({
        model: "gemini-3-flash-preview",
        contents: [
          {
            inlineData: {
              mimeType: "image/png",
              data: image.split(',')[1],
            },
          },
          {
            text: "This is a mobile screen screenshot. Identify where the user should click next for general navigation. Return JSON: { x: percent, y: percent, reason: string }",
          },
        ],
        config: {
          responseMimeType: "application/json"
        }
      });

      const data = JSON.parse(result.text);
      setGuide({ x: data.x, y: data.y, text: data.reason });
    } catch (error) {
      console.error("Simulation error:", error);
    } finally {
      setAnalyzing(false);
    }
  };

  return (
    <section className="py-20 px-6 relative z-10">
      <div className="max-w-6xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 gap-4">
          <div>
            <h2 className="text-3xl font-bold text-white mb-2 tracking-tight">Live Observation Simulator</h2>
            <p className="text-slate-400 font-medium">অ্যান্ড্রয়েড অ্যাপটি কীভাবে কাজ করবে তা এখানে পরীক্ষা করুন</p>
          </div>
          <button 
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center gap-2 px-6 py-3 bg-white/5 border border-white/10 text-white rounded-xl hover:bg-white/10 transition-all font-semibold backdrop-blur-md"
          >
            <Smartphone size={18} />
            স্ক্রিনশট আপলোড করুন
          </button>
          <input 
            type="file" 
            ref={fileInputRef} 
            onChange={handleFileUpload} 
            className="hidden" 
            accept="image/*"
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Phone Frame */}
          <div className="relative group perspective-1000">
            <div className="w-[320px] h-[640px] mx-auto bg-slate-950 rounded-[3.5rem] border-[10px] border-slate-800 shadow-2xl relative overflow-hidden ring-1 ring-white/10">
              {/* Screen Content */}
              <div className="absolute inset-0 bg-slate-900">
                {image ? (
                  <div className="relative w-full h-full">
                    <img src={image} alt="Simulator Screen" className="w-full h-full object-cover" />
                    {guide && (
                      <motion.div 
                        initial={{ scale: 0, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        className="absolute pointer-events-none z-20"
                        style={{ left: `${guide.x}%`, top: `${guide.y}%`, transform: 'translate(-50%, -50%)' }}
                      >
                        <div className="relative flex flex-col items-center">
                           <div className="bg-amber-400 text-slate-950 text-[10px] font-bold px-2 py-0.5 rounded shadow-lg mb-1 whitespace-nowrap">CLICK HERE</div>
                           <div className="w-12 h-12 border-2 border-amber-400 rounded-lg shadow-[0_0_20px_rgba(251,191,36,0.6)] flex items-center justify-center bg-amber-400/10">
                              <div className="w-2.5 h-2.5 bg-amber-400 rounded-full animate-pulse shadow-[0_0_10px_rgba(251,191,36,1)]" />
                           </div>
                        </div>
                      </motion.div>
                    )}
                  </div>
                ) : (
                  <div className="w-full h-full flex flex-col items-center justify-center p-8 text-center bg-slate-900">
                    <div className="w-20 h-20 rounded-full bg-slate-800/50 flex items-center justify-center mb-6">
                      <Eye size={36} className="text-slate-600 opacity-50" />
                    </div>
                    <p className="text-sm font-bold text-slate-500 tracking-wide">NO INPUT SIGNAL</p>
                    <p className="text-xs text-slate-600 mt-2">Upload a screenshot to begin</p>
                  </div>
                )}
              </div>
              {/* Notch */}
              <div className="absolute top-0 left-1/2 -translate-x-1/2 w-32 h-7 bg-slate-800 rounded-b-3xl z-10" />
              {/* Reflection */}
              <div className="absolute inset-0 pointer-events-none bg-gradient-to-tr from-white/5 to-transparent opacity-30" />
            </div>
          </div>

          {/* Controls & Output */}
          <div className="space-y-6">
            <div className="bg-white/5 backdrop-blur-xl p-8 rounded-[2.5rem] border border-white/10 shadow-2xl space-y-6">
              <div className="flex items-center justify-between">
                 <h4 className="font-bold text-white flex items-center gap-2 text-lg">
                    <Cpu size={20} className="text-indigo-400" />
                    Gemini Vision Engine
                 </h4>
                 <div className="px-2 py-1 bg-emerald-500/10 text-emerald-400 text-[10px] font-bold rounded-lg border border-emerald-500/20 uppercase tracking-widest">
                    v1.5 Flash
                 </div>
              </div>
              
              <p className="text-sm text-slate-400 leading-relaxed font-medium">
                Gemini 1.5 Flash স্ক্রিনশট বিশ্লেষণ করে পরবর্তী পদক্ষেপ ঠিক করে। এটি অ্যান্ড্রয়েড অ্যাপে লাইভ কাজ করবে।
              </p>
              
              <button 
                disabled={!image || analyzing}
                onClick={runAnalysis}
                className="w-full flex items-center justify-center gap-3 py-5 bg-indigo-600 text-white rounded-2xl font-black text-sm uppercase tracking-widest disabled:opacity-30 transition-all hover:bg-indigo-500 shadow-xl shadow-indigo-600/20 active:scale-95"
              >
                {analyzing ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    Analyzing Frame...
                  </>
                ) : (
                  <>
                    <Play size={18} fill="currentColor" />
                    Analyze Frame
                  </>
                )}
              </button>

              <AnimatePresence>
                {guide && (
                  <motion.div 
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="p-6 bg-indigo-500/10 rounded-3xl border border-indigo-500/20"
                  >
                    <h5 className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mb-3">AI Reasoning Output</h5>
                    <p className="text-sm text-indigo-200/90 leading-relaxed italic">"{guide.text}"</p>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            <div className="bg-slate-950 p-8 rounded-[2.5rem] border border-white/5 overflow-hidden relative group shadow-2xl">
              <div className="flex items-center justify-between mb-6">
                <h4 className="font-bold text-indigo-300 flex items-center gap-2 text-sm tracking-widest uppercase">
                  <Code2 size={16} />
                  Development Logs
                </h4>
                <div className="flex gap-1.5">
                  <div className="w-2.5 h-2.5 rounded-full bg-slate-800" />
                  <div className="w-2.5 h-2.5 rounded-full bg-slate-800" />
                  <div className="w-2.5 h-2.5 rounded-full bg-slate-800" />
                </div>
              </div>
              <div className="space-y-4 font-mono text-[11px] text-slate-400">
                <div className="flex items-center gap-3">
                  <span className="text-indigo-500">INIT</span>
                  <span>main.py logic initialized</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-indigo-500">CONF</span>
                  <span>buildozer.spec architecture: arm64-v8a</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-emerald-500">SUCCESS</span>
                  <span>Requirements verified</span>
                </div>
              </div>
              <div className="absolute -right-12 -bottom-12 opacity-5 scale-125">
                <Smartphone size={200} />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default function App() {
  return (
    <div className="min-h-screen bg-[#0f172a] font-sans text-slate-100 selection:bg-indigo-500/30">
      <Header />
      
      <main>
        {/* Decorative Background Elements */}
        <div className="fixed inset-0 overflow-hidden pointer-events-none">
          <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-600/10 blur-[120px] rounded-full" />
          <div className="absolute bottom-[10%] right-[-5%] w-[30%] h-[30%] bg-emerald-500/5 blur-[100px] rounded-full" />
        </div>

        {/* Hero Section */}
        <section className="py-24 px-6 text-center max-w-4xl mx-auto relative z-10">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/5 border border-white/10 rounded-full text-[10px] font-bold tracking-widest text-indigo-400 uppercase mb-6">
              <Zap size={12} className="fill-current" />
              Buildozer Ready • V 1.0.4
            </div>
            <h2 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-6 bg-gradient-to-br from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
              AI Vision Guidance
            </h2>
            <p className="text-lg md:text-xl text-slate-400 mb-10 leading-relaxed max-w-2xl mx-auto">
              অ্যান্ড্রয়েডের জন্য এমন একটি স্মার্ট সিস্টেম যা আপনার স্ক্রিন পর্যবেক্ষণ করবে এবং 
              AI এর সাহায্যে ব্যবহারকারীকে গাইড করবে।
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <button className="w-full sm:w-auto px-8 py-4 bg-indigo-600 text-white rounded-2xl font-bold shadow-lg shadow-indigo-600/20 hover:bg-indigo-500 transition-all flex items-center justify-center gap-2">
                <Download size={20} />
                Android প্রজেক্ট ডাউনলোড করুন
              </button>
              <button className="w-full sm:w-auto px-8 py-4 bg-white/5 backdrop-blur-md text-white border border-white/10 rounded-2xl font-bold hover:bg-white/10 transition-all">
                ডকুমেন্টেশন দেখুন
              </button>
            </div>
          </motion.div>
        </section>

        {/* Features Grid */}
        <section className="py-20 px-6 relative z-10">
          <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-6">
            <FeatureCard 
              icon={Smartphone}
              title="MediaProjection API"
              description="লাইভ স্ক্রিন ফ্রেম ক্যাপচার করার জন্য ইন-বিল্ট অ্যান্ড্রয়েড সিস্টেম ইন্টিগ্রেশন।"
            />
            <FeatureCard 
              icon={Eye}
              title="Accessibility Service"
              description="অন্য অ্যাপের ওপর ওভারলে আঁকা এবং স্ক্রিনের কনটেক্সট বোঝার উন্নত প্রযুক্তি।"
            />
            <FeatureCard 
              icon={Zap}
              title="Gemini 1.5 Intelligence"
              description="Google Gemini 1.5 Flash ব্যবহার করে রিয়েল-টাইমে ভিজ্যুয়াল ডেটা বিশ্লেষণ।"
            />
          </div>
        </section>

        {/* Simulator Area */}
        <Simulator />

        {/* Build Steps */}
        <section className="py-24 px-6 max-w-4xl mx-auto relative z-10">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold mb-4 text-white">কীভাবে বিল্ড করবেন?</h2>
            <div className="h-1 w-20 bg-indigo-500 mx-auto rounded-full" />
          </div>
          <div className="space-y-6">
            {[
              { step: "01", text: "প্রথমে 'main.py', 'buildozer.spec' এবং 'requirements.txt' ফাইলগুলো আপনার লোকাল কম্পিউটারে নিন।" },
              { step: "02", text: "Buildozer এনভায়রনমেন্ট সেটআপ করুন (Linux বা Google Colab সবচেয়ে ভালো কাজ করে)।" },
              { step: "03", text: "'buildozer -v android debug' কমান্ডটি রান করুন।" },
              { step: "04", text: "তৈরি হওয়া APK ফাইলটি আপনার অ্যান্ড্রয়েড ফোনে ইন্সটল করুন এবং Accessibility পারমিশন দিয়ে শুরু করুন।" }
            ].map((item, idx) => (
              <div key={idx} className="flex gap-6 items-start p-8 bg-white/5 backdrop-blur-xl border border-white/10 rounded-[2rem] shadow-sm group hover:border-indigo-500/30 transition-all">
                <span className="text-4xl font-black text-indigo-500/20 group-hover:text-indigo-500/40 transition-colors tabular-nums">{item.step}</span>
                <p className="text-slate-300 font-medium pt-2 leading-relaxed">{item.text}</p>
              </div>
            ))}
          </div>
        </section>
      </main>

      <footer className="py-16 border-t border-white/5 px-6 text-center text-slate-500 text-sm font-medium">
        <div className="flex items-center justify-center gap-2 mb-4">
          <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
          System Operational • Build 1.0.4-beta
        </div>
        © {new Date().getFullYear()} AI Vision Guide Project. Built for Professional Developers.
      </footer>
    </div>
  );
}

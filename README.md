# AI Vision Guide - Professional Android App Implementation 🚀

AI Vision Guide একটি পেশাদার অ্যান্ড্রয়েড অ্যাপ্লিকেশন যা **Gemini 1.5 Flash** ব্যবহার করে লাইভ স্ক্রিন পর্যবেক্ষণ এবং ইউজারকে ধাপে ধাপে নির্দেশনা প্রদান করে।

## প্রধান বৈশিষ্ট্যসমূহ (Features) 🌟
- **অতি সাধারণ ইন্টারফেস**: অ্যাপ ওপেন করলেই শুধু একটি 'START SERVICE' বাটন থাকবে।
- **লাইভ স্ক্রিন অবজারভেশন**: MediaProjection API (সিমুলেটেড) ব্যবহার করে লাইভ ফ্রেম রিড করে।
- **এআই বিশ্লেষণ**: স্ক্রিন বিশ্লেষণ করে ইউজারকে পরবর্তী পদক্ষেপ গ্রহণে সাহায্য করে।
- **লাল বর্গক্ষেত্র ওভারলে**: এআই যদি কোনো নির্দিষ্ট জায়গা চিহ্নিত করে, সেখানে একটি লাল স্কয়ার বক্স দেখাবে।
- **ইন্টারঅ্যাক্টিভ গাইডেন্স**: লাল বক্সে ক্লিক করলে অ্যাপটি তৎক্ষণাৎ পরবর্তী পদক্ষেপের জন্য স্ক্রিন বিশ্লেষণ শুরু করবে।

## প্রজেক্টের গঠন (Project Structure) 📁
- `main.py`: Kivy/KivyMD ব্যবহার করে তৈরি কোর লজিক।
- `buildozer.spec`: অ্যান্ড্রয়েড APK প্যাকেজিং কনফিগারেশন।
- `.github/workflows/android.yml`: GitHub Actions এর মাধ্যমে অটোমেটেড বিল্ড সেটআপ।

## কিভাবে বিল্ড করবেন (How to Build) 🛠️

১. **Github এ পুশ করুন**: আপনার রিপোজিটরিতে কোড পুশ করলে স্বয়ংক্রিয়ভাবে বিল্ড শুরু হবে।
২. **API Key সেট করুন**: এনভায়রনমেন্টে `GEMINI_API_KEY` নিশ্চিত করুন।
৩. **APK ফাইল**: Github Actions এর Artifacts থেকে `AI-Vision-Guide-APK` ডাউনলোড করে অ্যান্ড্রয়েড ফোনে ইন্সটল করুন।

---
*Last Sync Prepared by AI Engine on: 2026-04-19 09:00 PM*
*Status: Feature Revamp - Minimal UI, Red Square Overlay, and Interactive Guidance implemented.*

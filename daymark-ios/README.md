# Daymark for iOS (Progressive Web App)

A 100% offline-first, private PWA replica of Daymark designed specifically for iPhone Safari with **"Add to Home Screen"** support.

---

## 🌟 Key Features

* **iOS Standalone Experience**: Runs full-screen with no Safari browser bars, custom app icon, and native iOS safe-area handling.
* **100% Offline & Private**: Uses **IndexedDB** locally in the browser—zero cloud, zero tracking, your data never leaves your device.
* **Voice Action Engine**: Uses native iOS Safari `webkitSpeechRecognition` to dictate tasks and natural speech dates/times (e.g., *"Call Mom tomorrow at 6pm urgent"*).
* **Tactile Task Preview Card**: Live editable title before auto-saving, date/time chips, priority badges.
* **Airbnb / Apple Design System**: Obsidian charcoal background, Sunset Coral (`#FF385C`) indicators, and clean typography.
* **4 Core Tabs**: Today, Tasks (primary default: Today), Calendar (with clear dot badges and legend), Habits (single-line aligned routines).
* **Sidebar Navigation Drawer**: Settings, Insights & Analytics, Weekly Review, Day Templates, and Data Export/Backup.
* **Focus Timer**: 25m focus / 5m break circular progress timer with celebratory confetti.

---

## 📱 How to Run & Test on Your iPhone

### Option 1: Instant Local Wi-Fi Testing (No Deployment Needed)

1. Make sure your iPhone and Mac are connected to the same Wi-Fi network.
2. In the `daymark-ios` folder, run:
   ```bash
   npm run dev -- --host
   ```
3. Vite will display your local IP (e.g. `http://192.168.1.X:5173/`).
4. Open Safari on your iPhone, enter that address, tap the **Share** button (box with arrow pointing up), and select **"Add to Home Screen"**.

---

### Option 2: 1-Click Free Deployment (Recommended for HTTPS & Anywhere Access)

PWA voice recognition and service workers work best over `https://`. You can deploy this in 60 seconds using Vercel, Netlify, or Cloudflare Pages:

#### Using Vercel CLI (Free, instant):
```bash
npx vercel
```
Follow the prompts (accept defaults). Vercel will give you a live `https://daymark-ios.vercel.app` URL.

#### Or Using GitHub:
1. Push `daymark-ios` to a GitHub repository.
2. Import into [Vercel](https://vercel.com) or [Cloudflare Pages](https://pages.cloudflare.com).
3. Framework Preset: **Vite**, Build Command: `npm run build`, Output Directory: `dist`.

---

## 📲 How to Install from Safari on iPhone

1. Open your Daymark URL in **Safari**.
2. Tap the **Share** icon at the bottom of Safari (square with arrow pointing up).
3. Scroll down and tap **Add to Home Screen** (`+`).
4. Tap **Add** in the top-right corner.
5. Tap the new **Daymark** icon on your iPhone home screen!


# MyRemote 📱⚡

**MyRemote** is a powerful, developer-friendly Android IR Remote application designed to control generic and "hard-to-find" audio systems.

## 🎯 Why this app?
**To reduce Electronic Waste and keep the music playing.** 🌍♻️

I built this application to solve a specific problem: owning multiple imported or generic audio systems (amplifiers, DACs, preamp boards) where the original remote controls were fragile, easily lost, or stopped working after a few months.

Because these devices often use undocumented or non-standard IR codes, standard "Universal Remote" apps rarely work. This often leads to functional audio equipment being thrown away (e-waste) simply because it can no longer be controlled.

**MyRemote** is the solution. It is not just a remote; it is a **Reverse Engineering Tool** that lets you find the codes for *any* NEC-based device and save them permanently.

## 🚀 Key Features
*   **Universal IR Control**: Standard NEC protocol interaction.
*   **Physical Button Mapping**: Use your phone's **Volume keys** to control volume or skip tracks (Double Click) without looking at the screen.
*   **Modern UI**: Sleek "Liquid Energy" animations, haptic feedback, and a Pure Black Dark Mode for OLED efficiency.
*   **Fully Configurable**: Edit every button's hexadecimal code in Settings.

---

## 🛠️ The Troubleshooter Utilities
This is the core "Superpower" of the app. When you have a device with **NO** documentation and **NO** remote, use these tools inside the **Settings** page:

### 1. Frequency Scanner 📡
IR receivers are tuned to specific "Carrier Frequencies" (usually spanning 36kHz to 40kHz). If your phone transmits at 38kHz but the device listens at 40kHz, it might not respond even if the code is correct.
*   **The Problem**: You don't know what frequency your obscure amp uses.
*   **The Solution**: This tool transmits a standard "Power" signal while looping through all common frequencies (36kHz, 37kHz, 38kHz, 38.4kHz, 40kHz).
*   **Usage**: Point phone at device -> Start Scan -> Wait for device to react -> **Found your frequency!**

### 2. Code Scanner (Brute Force) 🔓
Once you have the frequency, you still need the specific "Command Codes" (e.g., Which hex code means "Volume Up"?).
*   **The Problem**: The original remote is lost, and the codes are not online.
*   **The Solution**: This tool acts like a lock picker. It systematically iterates through every possible 8-bit command (from `00` to `FF`) for a target address.
*   **Usage**:
    1.  Start Scan.
    2.  Watch your audio system closely.
    3.  When the volume jumps or the mute toggles, hit **STOP**.
    4.  The app displays the **Hex Code** (e.g., `0x00FF02FD`) that triggered the action.
    5.  Copy this code and paste it into the **Button Configuration** settings.

## 👨‍💻 Tech Stack
*   **Language**: Kotlin
*   **UI**: Jetpack Compose (Material3)
*   **Hardware**: Android `ConsumerIrManager` API
*   **Architecture**: MVVM with `StateFlow`
*   **Storage**: Shared Preferences (for persisting custom mapcodes)

## 👤 Author
**Kalaiselvan Arumugam**
*   GitHub: [kalaiselvan-arumugam](https://github.com/kalaiselvan-arumugam/)

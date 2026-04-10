# 🚗 Vehicle Control Hub (Android Automotive / AAOS)

Vehicle Control Hub is an Android Automotive OS (AAOS) application that demonstrates interaction with vehicle properties such as speed, battery, and basic controls using `CarPropertyManager`.

This project is built as part of learning and exploring **Automotive AOSP / AAOS development**.

---

## 📌 Features Implemented

### ✅ Telemetry Module
- Displays real-time **vehicle speed**
- Displays **battery percentage / energy data**
- Uses `CarPropertyManager` with subscription-based updates

### ✅ Multi-Tab Dashboard UI
- Built using `ViewPager2` + `TabLayout`
- Tabs:
  - Telemetry
  - HVAC (in progress)
  - Camera
  - Placeholder for future modules

### ✅ Camera Integration (Basic)
- Launch camera intent (depends on emulator/device support)

---

## ⚠️ Limitations

Due to **AAOS permission model**, several features require privileged access:

| Feature | Status | Reason |
|--------|--------|--------|
| HVAC Control | ❌ Limited | Requires privileged permissions |
| Seat Control | ❌ Not implemented | Requires OEM-level access |
| Light / Body Controls | ❌ Not implemented | Restricted properties |
| Energy Ports | ❌ Limited | Non-changeable permission |

---

## 🔐 Important Note on Permissions

Many `android.car.*` permissions are:
- **Signature / Privileged permissions**
- Not grantable via `adb pm grant`
- Only available to:
  - System apps
  - OEM / platform-signed apps

Example:
```text
android.car.permission.CAR_ENERGY_PORTS → not changeable
android.car.permission.CAR_INFO → not changeable
🛠 Tech Stack
Kotlin
Android Studio (Panda / Latest Stable)
Android Automotive OS Emulator
View Binding
Car API (android.car.*)
📂 Project Structure
VehicleControlHub/
 ├── MainActivity.kt
 ├── fragments/
 │   ├── TelemetryFragment.kt
 │   ├── HvacFragment.kt
 │   ├── CameraFragment.kt
 │   └── PlaceholderFragment.kt
 ├── res/layout/
 │   ├── activity_main.xml
 │   ├── fragment_telemetry.xml
 │   ├── fragment_hvac.xml
 │   ├── fragment_camera.xml
 │   └── fragment_placeholder.xml
🚀 How to Run
1. Prerequisites
Android Studio (latest stable)
AAOS Emulator (Android Automotive image)
JDK 11+
2. Setup
Clone repo:
git clone https://github.com/<your-username>/VehicleControlHub.git
Open in Android Studio
Sync Gradle
Run on Automotive Emulator
⚙️ Required Setup (Important)
Enable car library dependency
compileOnly(files("$ANDROID_HOME/platforms/android-35/optional/android.car.jar"))
📊 Telemetry Data Testing

Use AAOS emulator commands:

adb shell dumpsys activity service com.android.car inject-vhal-event 0x11600207 0 60

Property example: PERF_VEHICLE_SPEED

📷 Camera Note

Camera behavior depends on emulator:

Some AAOS images do not include camera apps
On Mac, webcam may not be supported directly
🧠 Learnings from this Project
AAOS architecture and Car Service
Vehicle property subscription model
Permission model differences vs Android mobile
Multi-user behavior in Automotive OS
Emulator limitations vs real vehicle hardware
🔜 Future Improvements
Run app as privileged/system app
Enable full HVAC control
Add:
Seat control
Lighting control
Vehicle diagnostics
Integrate CAN / vehicle simulation data
Improve UI/UX for automotive HMI
👨‍💻 Author

Abhishek Mandal

Backend Engineer (Java / Spring Boot)
Exploring Automotive / AAOS domain
Interested in Connected Vehicles & OTA systems


📌 Note
This project is for learning and experimentation purposes and does not represent a production-ready automotive system.

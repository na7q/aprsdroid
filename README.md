# 📡 NA7Q APRSdroid
### *The Ultimate APRS Companion for Android - Enhanced by NA7Q*

> 🌟 **This is the NA7Q enhanced build** of the [original APRSdroid](https://aprsdroid.org/) developed by NA7Q with extensive additional features and improvements specifically designed for advanced amateur radio operators. This is a **work in progress** with active development and regular updates.

---

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://na7q.com/aprsdroid-osm/)
[![License](https://img.shields.io/badge/License-GPLv2-blue?style=for-the-badge)](https://www.gnu.org/licenses/gpl-2.0.html)
[![Scala](https://img.shields.io/badge/Built_with-Scala-DC322F?style=for-the-badge&logo=scala&logoColor=white)](https://www.scala-lang.org/)
[![Open Source](https://img.shields.io/badge/Open-Source-FF6B6B?style=for-the-badge)](https://github.com/na7q/aprsdroid)
[![Work in Progress](https://img.shields.io/badge/Status-Work_in_Progress-orange?style=for-the-badge)](https://na7q.com/aprsdroid-changelog/)

**[🌐 NA7Q APRSdroid](https://na7q.com/aprsdroid-osm/)** • **[📋 Changelog](https://na7q.com/aprsdroid-changelog/)** • **[🗺️ Original APRSdroid](https://aprsdroid.org/)**

</div>

---

## ✨ **What is NA7Q APRSdroid?**

NA7Q APRSdroid is a powerful, extensively enhanced Android application developed by **NA7Q** based on the original APRSdroid. This version adds numerous professional-grade features while maintaining seamless integration with the [**APRS (Automatic Packet Reporting System)**](http://aprs.org/) network. This build is a work in progress, and some features may be incomplete or broken at the time of download as development actively continues.

### 🎯 **Core Features**
- 📍 **Real-time Position Reporting** - Share your location with the APRS network
- 🗺️ **Interactive Station Map** - Visualize nearby amateur radio stations with offline mapping
- 💬 **APRS Messaging** - Send and receive messages through the network
- 🔄 **Network Integration** - Full compatibility with APRS infrastructure
- 🎨 **Modern Android UI** - Clean, intuitive interface designed for mobile use

### 🚀 **NA7Q Enhanced Features (Not in Official APRSdroid)**

Based on the official feature list from NA7Q:

#### 📡 **RF & Networking**
- 🔄 **Digipeater** - Direct or full digipeating capabilities
- 🌐 **2-Way IGating** - Full Internet Gateway functionality
- 📶 **Flexible Packet Routing** - Send packets via RF and APRS-IS, or RF only while IGating
- 🎚️ **Radio Control** - Support for Vero, BTech, Radioddity, and other radios
- 📻 **DigiRig Support** - Seamless integration with DigiRig interfaces
- 🔵 **Bluetooth Low Energy** - Stable BLE support (now stable and near completion!)

#### 🗺️ **Advanced Offline Mapping**
- 🗺️ **Offline Maps with MBTiles** - Complete offline operation capability
- 🆕 **MapsForge V3 Support** - NEW! Enhanced offline mapping with MapsForge
- 🌍 **OpenStreetMap Integration** - Full OSM compatibility for mapping
- ⚠️ **Note**: This version does not include the Google Maps API for enhanced privacy and reduced dependencies

#### 📊 **Data & Compression**
- 🗜️ **Mic-E Compression** - Efficient position encoding
- 🚨 **Mic-E Emergency Status** - Including EMERGENCY status support
- 📈 **Standard Compression** - Multiple compression formats supported

#### ⚙️ **User Experience Enhancements**
- 📏 **Unit Options** - Choose between Metric or Imperial units
- 🔧 **Hardware Control** - Option to disable hardware acceleration
- 📊 **Enhanced Station Viewer** - Added speed and course information
- 💬 **Advanced Messaging Tweaks** - Features for power users
- 🆔 **Message ID Control** - Option to disable Message ID
- 📋 **Smart Hub Log** - Sort by distance or newest stations
- 🔍 **Under-the-Hood Improvements** - Numerous performance and stability enhancements

---

### 🔮 **Development Roadmap**

NA7Q is actively developing additional features, including:

- 🌤️ **Improved APRS Parser** - Enhanced data parsing capabilities
- ☁️ **Weather Readability** - Better weather data display
- 📏 **Altitude in Hub Log** - Show altitude information
- 🖥️ **Full Screen Mode** - Immersive display option
- 📱 **Mobile HUD Integration** - Enhanced heads-up display
- 🆔 **Device Identifier in Hub** - Better station identification
- 🔧 **BLE Bug Fixes** - Crash fixes for BLE device selection
- 📟 **APRS Message Query Commands** - Support for ?APRSM and similar
- 🐭 **MICE Code Cleanup** - Improved Mic-E implementation
- 📋 **Enhanced Beacon Types** - List menu for beacon selection
- 🚨 **MICE Emergency Alerts** - Alert system for emergency status
- 📡 **Station Path Display** - Show direct vs digipeated stations
- ➕ **And Much More!**

## 🚀 **Quick Start**

### 📲 **Installation**

> ⚠️ **Important**: Uninstall any previous OFFICIAL version of APRSdroid before installing NA7Q's version

1. **Download the latest APK** from release page
2. **Optional**: Download the Mobile HUD APK from [**https://na7q.com/aprsdroid-osm/**](https://na7q.com/aprsdroid-osm/) (experimental, landscape mode recommended)
3. **Install** both APKs on your Android device

### 🗺️ **Setting Up Offline Maps**

For Android 11+ devices, manual storage permissions are required for offline mapping files:

1. In APRSdroid settings, go to **OSM Maps** category
2. Tap **"Grant Storage Permissions"**
3. Grant **ALL file permissions** for device storage access
4. Set map viewer to **OpenStreetMap.org** to use offline maps
5. Configure offline maps in the **OSM Maps** preferences section

#### 🗺️ **Getting Maps**

NA7Q provides several tools for downloading offline maps:

- 🌍 [**World Map**](https://na7q.com/wp-content/uploads/2024/12/map.mbtiles) - Ready-to-use world map
- 🖥️ [**OSM Map Maker (Windows)**](https://downloads.aprs.wiki/APRSdroid/gui7-concurrency.exe) - Windows GUI tool
- 🐍 [**Python Map Maker**](https://na7q.com/wp-content/uploads/2025/01/gui7-concurrency.py) - Python script version
- 🗺️ [**Multi-Map Maker**](https://na7q.com/wp-content/uploads/2025/02/mapmaker-0.2.exe) - Advanced mapping tool
- 👁️ [**Map Viewer**](https://na7q.com/wp-content/uploads/2025/02/mapviewer.exe) - Preview downloaded maps
- 🌐 [**BBBike MapsForge**](https://extract.bbbike.org/) - Alternative map source

**Map Requirements**: 
- Use **MBTiles format** (PNG or JPG, NOT Vector/PBF)
- Specify precise locations like "Portland, Oregon" or "Texas USA"
- Zoom levels 1-18 (recommend 13-14 for states)
- Note: Large areas at high zoom can be 2-5GB+

### 📚 **Documentation & Support**
- 🌐 [**NA7Q APRSdroid Homepage**](https://na7q.com/aprsdroid-osm/)
- 📋 [**Changelog**](https://na7q.com/aprsdroid-changelog/)
- 📖 [**Original APRSdroid FAQ**](https://github.com/ge0rg/aprsdroid/wiki/Frequently-Asked-Questions)
- ⚙️ [**Original APRSdroid Configuration Guide**](https://github.com/ge0rg/aprsdroid/wiki/Settings)
- 📧 **Contact NA7Q** - Check QRZ for contact information

---

## 💖 **Support NA7Q's Development**

NA7Q APRSdroid has had over 1851 downloads and the Mobile HUD has had 400 downloads, making it a valuable resource for the amateur radio community. This project is developed and maintained by **NA7Q** as a labor of love for fellow amateur radio enthusiasts.

<div align="center">

[![Support on Patreon](https://img.shields.io/badge/Support-Patreon-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/c/NA7Q/home)

**Your support helps fund continued development, new features, and keeps NA7Q APRSdroid free for the amateur radio community! 🙏**

*If you like the work NA7Q has put into this enhanced APRSdroid, please consider supporting on Patreon!*

</div>

---

## 🛠️ **Development & Compilation**

### 🏗️ **Build Environment**
APRSdroid is crafted in **Scala** using the [gradle-android-scala-plugin](https://github.com/AllBus/scala-plugin). While the compilation process is robust, please note:
- ⏱️ Full builds take approximately **3 minutes**
- 🔄 Incremental builds may occasionally produce non-functional APKs
- 🗺️ **Google Maps API key required** for map functionality

### 📋 **Prerequisites**
- ☕ **Java 8 JDK**
- 🐙 **Git** for version control
- 🗺️ **Google Maps API Key** ([Get yours here](https://developers.google.com/maps/documentation/android-sdk/start)) - *Optional, only if you want Google Maps support*

### 🗝️ **Important Notice: Google Maps**
> ⚠️ **This enhanced fork does NOT include the Google Maps API** for improved privacy and reduced dependencies. The app uses offline mapping solutions (MBTiles and MapsForge) instead. If you require Google Maps functionality, you can build the app from source and add your own API key following the build instructions below.

### 🚀 **Complete Build Instructions**

```bash
# 📦 Install required dependencies
sudo apt-get install -y git openjdk-8-jdk vim-nox wget unzip

# 🔧 Setup Android SDK
cmdline_tool_file="commandlinetools-linux-6609375_latest.zip"
export ANDROID_SDK_ROOT="$(pwd)/android"
mkdir -p "${ANDROID_SDK_ROOT}"

# 📥 Download and setup Android command-line tools
wget "https://dl.google.com/android/repository/${cmdline_tool_file}"
unzip "${cmdline_tool_file}" -d "${ANDROID_SDK_ROOT}/cmdline-tools"
rm -f "${cmdline_tool_file}"

# 🛤️ Configure PATH variables
export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/tools/bin:${PATH}"
export PATH="${ANDROID_SDK_ROOT}/platform-tools:${PATH}"
export PATH="${ANDROID_SDK_ROOT}/emulator:${PATH}"

# 📄 Accept Android SDK licenses
mkdir "${ANDROID_SDK_ROOT}/licenses"
echo 24333f8a63b6825ea9c5514f83c2829b004d1fee > "${ANDROID_SDK_ROOT}/licenses/android-sdk-license"
echo 84831b9409646a918e30573bab4c9c91346d8abd > "${ANDROID_SDK_ROOT}/licenses/android-sdk-preview-license"

# 📱 Install required Android components
sdkmanager --install emulator 'system-images;android-24;default;armeabi-v7a'

# 🔗 Clone the repository
git clone https://github.com/na7q/aprsdroid/
cd aprsdroid
git submodule update --init --recursive

# 🗝️ Configure Google Maps API key (OPTIONAL - only if you want Google Maps)
# Replace AI... with your actual API key, or skip this step for offline maps only:
# echo "mapsApiKey=AI..." > local.properties

# 🔨 Build APK
# For debug build:
./gradlew assembleDebug

# For release build:
./gradlew assembleRelease
```

---

## 📜 **License**

This project is licensed under the **GNU General Public License v2.0** - see the [LICENSE](LICENSE) file for details.

---

## 🤝 **Contributing**

We welcome contributions from the amateur radio community! Whether you're fixing bugs, adding features, or improving documentation, your help makes APRSdroid better for everyone.

---

<div align="center">

**Made with ❤️ by Amateur Radio operators, for Amateur Radio operators**

*73 and happy APRSing!* 📡

</div>

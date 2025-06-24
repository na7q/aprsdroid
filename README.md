# 📡 APRSdroid
### *The Ultimate APRS Companion for Android*

---

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://aprsdroid.org/)
[![License](https://img.shields.io/badge/License-GPLv2-blue?style=for-the-badge)](https://www.gnu.org/licenses/gpl-2.0.html)
[![Scala](https://img.shields.io/badge/Built_with-Scala-DC322F?style=for-the-badge&logo=scala&logoColor=white)](https://www.scala-lang.org/)
[![Open Source](https://img.shields.io/badge/Open-Source-FF6B6B?style=for-the-badge)](https://github.com/ge0rg/aprsdroid)

**[🌐 Official Website](https://aprsdroid.org/)** • **[📱 Download on Google Play](https://play.google.com/store/apps/details?id=org.aprsdroid.app)** • **[💬 Follow on Twitter](http://twitter.com/aprsdroid)**

</div>

---

## ✨ **What is APRSdroid?**

APRSdroid is a powerful, feature-rich Android application designed specifically for **Amateur Radio operators**. It seamlessly integrates with the [**APRS (Automatic Packet Reporting System)**](http://aprs.org/) network, providing real-time position reporting, station tracking, and message exchange capabilities—all from the convenience of your Android device.

### 🎯 **Key Features**
- 📍 **Real-time Position Reporting** - Share your location with the APRS network
- 🗺️ **Interactive Station Map** - Visualize nearby amateur radio stations
- 💬 **APRS Messaging** - Send and receive messages through the network
- 🔄 **Network Integration** - Full compatibility with APRS infrastructure
- 🎨 **Modern Android UI** - Clean, intuitive interface designed for mobile use

---

## 🚀 **Quick Start**

### 📲 **Installation**
Get started in seconds:

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=org.aprsdroid.app)

### 📚 **Documentation & Support**
- 📖 [**Frequently Asked Questions**](https://github.com/ge0rg/aprsdroid/wiki/Frequently-Asked-Questions)
- ⚙️ [**Configuration Guide**](https://github.com/ge0rg/aprsdroid/wiki/Settings)
- 🐛 [**Report Issues**](https://github.com/ge0rg/aprsdroid/issues)

---

## 💖 **Support the Developer**

APRSdroid is a labor of love developed and maintained by dedicated amateur radio enthusiasts. If you find this app valuable for your ham radio activities, consider supporting continued development:

<div align="center">

[![Support on Patreon](https://img.shields.io/badge/Support-Patreon-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/c/NA7Q/home)

**Your support helps maintain servers, implement new features, and keep APRSdroid free for the amateur radio community! 🙏**

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
- 🗺️ **Google Maps API Key** ([Get yours here](https://developers.google.com/maps/documentation/android-sdk/start))

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

# 🗝️ Configure Google Maps API key
# Replace AI... with your actual API key:
echo "mapsApiKey=AI..." > local.properties

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

## 📬 **Connect with Us**

- 🌐 **Website**: [aprsdroid.org](https://aprsdroid.org/)
- 🐦 **Twitter**: [@aprsdroid](http://twitter.com/aprsdroid)
- 💰 **Support**: [Patreon](https://www.patreon.com/c/NA7Q/home)
- 📧 **Issues**: [GitHub Issues](https://github.com/ge0rg/aprsdroid/issues)

---

<div align="center">

**Made with ❤️ by Amateur Radio operators, for Amateur Radio operators**

*73 and happy APRSing!* 📡

</div>

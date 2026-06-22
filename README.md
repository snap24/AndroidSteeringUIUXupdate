<h1 align="center">Android as Steering Wheel</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Available_on-GitHub-black?style=for-the-badge&logo=github" alt="Available on GitHub" />
  <img src="https://img.shields.io/badge/App_Version-2.3.0-blue?style=for-the-badge" alt="App Version" />
  <img src="https://img.shields.io/badge/Platform-Windows_%7C_Android-green?style=for-the-badge" alt="Platforms" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge" alt="License" /></a>
</p>

<div align="center">
  <img src="Assets/2.gif" alt="Demo" />
</div>

<br>

A massive UI/UX update to the incredible **AndroidSteering** project originally created by [teamclouday](https://github.com/teamclouday). This application turns your Android phone into a fully functional PC racing wheel and gamepad via Wi-Fi or Bluetooth. 

This fork provides an updated interface, improved multi-touch support, and customizable control layouts.

---

### Requirements  
* Android mobile phone with bluetooth (or Wifi)  
* Windows PC with bluetooth (or Wifi/LAN)  
* If bluetooth, phone and PC should be paired once  
* If Wifi, phone should be under same network as PC (e.g. connect to same router)
* vJoy driver installed on PC  

---

### How to Use

<details>
  <summary>vJoy Driver</summary>

1. Make sure no physical controller is attached  
2. Download `vJoySetup.exe` from [vJoy sourceforge](https://sourceforge.net/projects/vjoystick/files/Beta%202.x/2.1.8.39-270518/) and install (Recommended to check all 4 programs)  
3. Make sure no other program is capturing controller (e.g. Steam)  
4. Launch `Configure vJoy` and configure the device as following:  
   <img src="Assets/vjoy.png" width="300" alt="vjoy configuration">

</details>

<details>
  <summary>Android App</summary>

  1. After installing apk on Android phone, tap upper left corner to see all options  
  2. Connection modes:  
     1. Bluetooth requires the phone to enable bluetooth and has already paired with  target PC at least once  
     2. Wifi/LAN requires the phone to connect to the the same local network as Windows PC  
        To connect, enter the same IP address displayed on PC app  
        If connection failed, check if [Windows PC firewall is blocking the app](https://pureinfotech.com/allow-apps-firewall-windows-10/)  
  3. Control options:  
     1. `Default`: phone motion controls both acceleration and steering  
        Two changing numbers [left indicates horizontal angle (`Roll`), right  indicates vertical angle (`Pitch`)]  
        See [Android README](Android/README.md) for more information  
     2. `Alternative`: phone motion only control steering  
        Acceleration is controlled by pressing `LT` / `RT`  
        Used if default mode is too hard to control  
     3. `GamePad`: no steering is enabled, but has more buttons  
        Used to control the rest of a game (such as menu, car view, etc.)  

</details>

<details>
  <summary>Windows App</summary>
 
  1. Install Windows application  
  2. Minimizing the app will hide app to system tray (with a notification)  
  3. Left textbox contains all essential program notifications  
  4. Be sure to check information in textbox that vJoy controller is initialized  correctly and valid device is found  
  5. To connect to phone:  
     1. Select correct connection mode  
     2. Tap `Connect` button  
     3. Server will start listening  
     4. Tap `Listening` will stop server  
     5. If connected, tap `Connected` will disconnect device and stop server  
  6. `Controller` button leads to the xbox controller mapping page  
     It is used to map buttons and stick axis in a game or steam when vJoy controller is not recognized or has wrong settings
  7. `Configure` button opens the motion angle configuration window
     "Steering Angles" control the left and right limit of the steering wheel angle (in degrees)
     "Acceleration Angles" control the forward and backward limit of the phone angle for acceleration
     "Acceleration Angles (Rest)" is also for acceleration, but defines the range of angles where phone is considered at rest (no acceleration)

Finally, the first run of Windows app after installation may be slow or not responding.\
This is because the app will need to be initialized for the first time, a [well-known issue](https://stackoverflow.com/a/1308732/11397618) of ClickOnce deployment.\
In this case, close the app and open again.

</details>

---

## What's New in this Overhaul

- Editable UI elements
- Set mapping mode
- Import and export of layouts
- Accurate pedal physics with automatic spring-back logic
- Bug fixes for simultaneous multi-touch joystick & trigger input
- A modern, sleek controller logo and glassmorphism styling

> **Note:** Included are pre-configured layouts in [Assets/steering_layouts.json](Assets/steering_layouts.json). The layout elements may not be perfectly aligned for every device's screen size or aspect ratio. Please make sure to enter edit mode and adjust them to your preference! This app is fully tested on Forza Horizon 6.

---

## Interface Gallery

<p align="center">
  <img src="Assets/gallery/1.jpeg" width="400" /> &nbsp;&nbsp;&nbsp; <img src="Assets/gallery/2.jpeg" width="400" />
</p>
<br>
<p align="center">
  <img src="Assets/gallery/3.jpeg" width="400" /> &nbsp;&nbsp;&nbsp; <img src="Assets/gallery/4.jpeg" width="400" />
</p>
<br>
<p align="center">
  <img src="Assets/gallery/5.jpeg" width="400" />
</p>

---

### For Games  

By default, vJoy will use settings of xbox controller, for most modern games. Make sure to tweak in-game steering settings, because most games have been using stabilization algorithms to smooth out the steering axis input. Some may result in a lagging of steering, others may make steering extremely slow or fast.  

See [Game Steering Wheel Settings](SETTINGS.md) for more details.

<details>
  <summary>Game Play Demos (Original)</summary>

* [Assetto Corsa](https://www.bilibili.com/video/BV1XJ411C7R9) (version [1.0.0.6](https://github.com/teamclouday/AndroidSteering/releases/tag/1.0.0.6))  
* [Assetto Corsa](https://www.bilibili.com/video/BV1Ee411s7Zr) (version [1.1.0.4](https://github.com/teamclouday/AndroidSteering/releases/tag/1.1.0.4))  
* [Forza Horizon 4](https://www.bilibili.com/video/BV1oM4y1V7NK/) (version [2.0.0](https://github.com/teamclouday/AndroidSteering/releases/tag/2.0.0))  
* [Assetto Corsa](https://www.bilibili.com/video/BV1jq4y1D7Ed/) (version [2.1.0](https://github.com/teamclouday/AndroidSteering/releases/tag/2.1.0))  

</details>

---

### Original Windows Side Screenshots

<img src="Assets/pc1.png" width="600" alt="pc1">
<img src="Assets/pc2.png" width="600" alt="pc2">
<img src="Assets/pc3.png" width="600" alt="pc3">

---

## Special Thanks & Credits
**All core networking, math logic, and Windows PC server infrastructure were originally engineered by [teamclouday](https://github.com/teamclouday/AndroidSteering).** 

This project would not exist without their brilliant foundation. This fork serves to build upon their hard work by providing a completely redesigned frontend and fixing some edge-case control bugs, aiming to provide the ultimate plug-and-play racing experience.

---

## License

Distributed under the Apache License 2.0. See [LICENSE](LICENSE) for further information.

---


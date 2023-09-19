# IKEA Zigbee drivers

Supported devices:
* [Tradfri Control Outlet (E1603)](#tradfri-control-outlet-e1603)
* [Tradfri On/Off Switch (E1743)](#tradfri-onoff-switch-e1743)
* [Tradfri Motion Sensor (E1745)](#tradfri-motion-sensor-e1745)
* [Tradfri Remote Control (E1810)](#tradfri-remote-control-e1810)
* [Tradfri Shortcut Button (E1812)](#tradfri-shortcut-button-e1812)
* [Styrbar Remote Control N2 (E2002)](#styrbar-remote-control-n2-e2002)
* [Symfonisk Sound Remote Gen2 (E2123)](#symfonisk-sound-remote-gen2-e2123)
* [Rodret Dimmer (E2201)](#rodret-dimmer-e2201)

## Driver Install
### Install using HPM (offers automatic updates)
Follow the steps below if you already have the "Hubitat Package Manager" app installed in your Hubitat hub:
   * In the Hubitat interface, go to "Apps" and select "Hubitat Package Manager"
   * Select "Install", then "Search by Keywords"
   * Enter "IKEA Zigbee drivers" in the search box and click "Next"
   * Select "IKEA Zigbee drivers by Dan Danache" and click "Next"
   * Select the driver(s) you need from the dropdown list and follow the install instructions

### Manual Install
Follow the steps below if you don't know what "Hubitat Package Manager" is:
   * In the Hubitat interface, go to "Drivers code"
   * Click "New Driver" in the top right, then Click "Import" in the top right
   * Search below for your device, look for the "Manual install file" property and enter it in the URL field
   * Click "Import", then click "OK" and the code should load in the editor
   * Click "Save" in the top right


## Tradfri Control Outlet (E1603)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1603.webp" style="width: 200px"> |
| Product Code | `304.883.63` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1603.groovy` |
| Tested firmwares | `2.0.0244`｜`2.3.089` |

### Features
* Commands: On, Off, Toggle, On with Timed Off
* Configure what happens after a power outage (Power On, Power Off, Restore previous state)
* Health status (online / offline)
* Refresh switch state on demand
* Report Zigbee Neighbors and Routing Tables (device acts as a Zigbee router)

### Device Pairing
Follow the steps below in order to pair the Tradfri Control Outlet with your Hubitat hub:
   * Find the small reset hole on the side of the device and make sure you have at hand a pin that can fit the reset hole (e.g.: a paper clip or SIM card eject pin)
   * Plug the device in an outlet
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * Insert the pin into the reset hole and press it for at least 5 seconds; upon release, the LED light will start blinking
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * That's it, Have fun!


## Tradfri On/Off Switch (E1743)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1743.webp" style="width: 200px"> |
| Product Code | `203.563.82`｜`404.677.65`｜`403.563.81` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1743.groovy` |
| Tested firmwares | `2.2.010`｜`24.4.6` |

### Features
* Button Push events for: both buttons
* Button Hold events for: both buttons
* Button Release events for: both buttons
* Battery indicator (%)
* Health status (online / offline)

### Device Pairing
Follow the steps below in order to pair the Tradfri On/Off Switch with your Hubitat hub:
   * Using a small screwdriver, open the battery compartiment and you should see the small pair button (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!


## Tradfri Motion Sensor (E1745)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1745.webp" style="width: 200px"> |
| Product Code | `704.299.13` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1745.groovy` |
| Tested firmwares | `24.4.5` |

> IMPORTANT: Old firmware versions (below 24.4.5) suppport binding to groups only and this functionality is not supported by the Hubitat hub. Please update the remote to the latest version.

### Features
* Motion detection
* Battery indicator (%)
* Health status (online / offline)

### Device Pairing
Follow the steps below in order to pair the Tradfri Motion Sensor with your Hubitat hub:
   * Open the back compartiment and you should see the small pair button (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!


## Tradfri Remote Control (E1810)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1810.webp" style="width: 200px"> |
| Product Code | `304.431.24`｜`004.431.30` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1810.groovy` |
| Tested firmwares | `24.4.5` |

> IMPORTANT: Old firmware versions suppport binding to groups only and this functionality is not supported by the Hubitat hub. Please update the remote to the latest version.

### Features
* Button Push events for: all buttons
* Button Hold events for: Button 2 (🔆), Button 3 (🔅), Button 4 (Next) and Button 5 (Prev)
* Button Release events for: Button 2 (🔆), Button 3 (🔅), Button 4 (Next) and Button 5 (Prev)
* Button 1 (Play) acts as a switch (on / off)
* Button 2 (🔆) and Button 3 (🔅) act as a switch level (0 - 100%)
* Battery indicator (%)
* Health status (online / offline)

### Device Pairing
Follow the steps below in order to pair the Tradfri Remote Control with your Hubitat hub:
   * Open the battery compartiment and you should see the small pair button (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!


## Tradfri Shortcut Button (E1812)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1812.webp" style="width: 200px"> |
| Product Code | `203.563.82`｜`404.677.65`｜`403.563.81` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1812.groovy` |
| Tested firmwares | `2.3.015`｜`24.4.6` |

### Features
* Button Push event
* Button Double-Tap event (only on firmware `24.4.6` and above)
* Button Hold event
* Button Release event
* Battery indicator (%)
* Health status (online / offline)

### Device Pairing
Follow the steps below in order to pair the Tradfri Shortcut Button with your Hubitat hub:
   * Using a small screwdriver, open the battery compartiment and you should see the small pair button (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!


## Styrbar Remote Control N2 (E2002)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E2002.webp" style="width: 200px"> |
| Product Code | `304.883.63` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2002.groovy` |
| Tested firmwares | `1.0.024`｜`2.4.5` |

### Features
* Button Push events for: all buttons
* Button Hold events for: Button 1 (🔆) and Button 2 (🔅)
* Button Release events for: Button 1 (🔆), Button 2 (🔅)
* Battery indicator (%)
* Health status (online / offline)

### Issues
* The Hold / Release events don't work correctly on the Next and Prev buttons

### Device Pairing
Follow the steps below in order to pair the Styrbar Remote Control with your Hubitat hub:
   * Using a small screwdriver, open the battery compartiment and you should see the small pair button (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!


## Symfonisk Sound Remote Gen2 (E2123)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E2123.webp" style="width: 200px"> |
| Product Code | `305.273.12` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2123.groovy` |
| Tested firmwares | `1.0.012`｜`1.0.35` |

### Features
* Button Push events for: all buttons
* Button Hold events for: Button 2 (Plus), Button 3 (Minus), Button 6 (•) and Button 7 (••)
* Button Release events for: Button 6 (•) and Button 7 (••)
* Button Double-Tap events for: Button 6 (•) and Button 7 (••)
* Battery indicator (%)
* Health status (online / offline)

### Device Pairing
Follow the steps below in order to pair the IKEA Sound Remote Gen2 with your Hubitat hub:
   * Open the battery compartiment and you should see the small pair button - with two chain links on it (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub until the LED stops blinking and turns off
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!


## Rodret Dimmer (E2201)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E2201.webp" style="width: 200px"> |
| Product Code | `205.281.28`｜`805.597.96` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2201.groovy` |
| Tested firmwares | `1.0.47` |

### Features
* Button Push events for: both buttons
* Button Hold events for: both buttons
* Button Release events for: both buttons
* Battery indicator (%)
* Health status (online / offline)

### Device Pairing
Follow the steps below in order to pair the Rodret Dimmer with your Hubitat hub:
   * Open the battery compartiment and you should see the small pair button (don't push it!)
   * In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room (optional)
   * Close the device battery compartiment
   * That's it, Have fun!

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)

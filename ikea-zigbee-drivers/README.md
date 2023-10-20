# IKEA Zigbee drivers

Supported devices:
* [Tradfri Control Outlet (E1603)](#tradfri-control-outlet-e1603)
* [Tradfri On/Off Switch (E1743)](#tradfri-onoff-switch-e1743)
* [Tradfri Motion Sensor (E1745)](#tradfri-motion-sensor-e1745)
* [Tradfri Remote Control (E1810)](#tradfri-remote-control-e1810)
* [Tradfri Shortcut Button (E1812)](#tradfri-shortcut-button-e1812)
* [Askvader On/Off Switch (E1836)](#askvader-onoff-switch-e1836)
* [Styrbar Remote Control N2 (E2002)](#styrbar-remote-control-n2-e2002)
* [Symfonisk Sound Remote Gen2 (E2123)](#symfonisk-sound-remote-gen2-e2123)
* [Rodret Dimmer (E2201)](#rodret-dimmer-e2201)
* [Tradfri LED Driver (ICPSHC24)](#tradfri-led-driver-icpshc24)

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
1. Find the small reset hole on the side of the device and make sure you have at hand a pin that can fit the reset hole
   (e.g.: a paper clip or SIM card eject pin)
1. If the device is already plugged in, take it out for 20 seconds (power-cycle); do this before each pair attempt
1. Plug the device in an outlet
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. Insert the pin into the reset hole and press it for at least 5 seconds; upon release, the LED light will start
   blinking
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. That's it, Have fun!


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
1. Using a small screwdriver, open the battery compartiment and you should see the small pair button (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


## Tradfri Motion Sensor (E1745)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1745.webp" style="width: 200px"> |
| Product Code | `704.299.13` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1745.groovy` |
| Tested firmwares | `24.4.5` |

### Features
* Motion detection
* Illumination detection ("dim" or "bright")
* Option to detect motion only when dark
* Battery indicator (%)
* Health status (online / offline)

### Known Issues
* Old firmware versions (below 24.4.5) suppport binding to groups only and this functionality is not supported by the
  Hubitat hub.
* You can update the device to the latest version (24.4.5) using via Hubitat

### Device Pairing
1. Open the back compartiment and you should see the small pair button (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


## Tradfri Remote Control (E1810)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1810.webp" style="width: 200px"> |
| Product Code | `304.431.24`｜`004.431.30` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1810.groovy` |
| Tested firmwares | `24.4.5` |

### Features
* Button Push events for: all buttons
* Button Hold events for: Button 2 (🔆), Button 3 (🔅), Button 4 (Next) and Button 5 (Prev)
* Button Release events for: Button 2 (🔆), Button 3 (🔅), Button 4 (Next) and Button 5 (Prev)
* Button 1 (Play) acts as a switch (on / off)
* Button 2 (🔆) and Button 3 (🔅) act as a switch level (0 - 100%)
* Battery indicator (%)
* Health status (online / offline)

### Known Issues
* Old firmware versions suppport binding to groups only and this functionality is not supported by the Hubitat hub.
  Please update the remote to the latest version.

### Device Pairing
1. Open the battery compartiment and you should see the small pair button (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


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
1. Using a small screwdriver, open the battery compartiment and you should see the small pair button (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**; the red LED light can be seen from the back / battery side
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


## Askvader On/Off Switch (E1836)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1836.webp" style="width: 200px"> |
| Product Code | `504.638.80` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1836.groovy` |
| Tested firmwares | `1.0.002` |

### Features
* Commands: On, Off, Toggle, On with Timed Off
* Configure what happens after a power outage (Power On, Power Off, Restore previous state)
* Health status (online / offline)
* Refresh switch state on demand
* Report Zigbee Neighbors and Routing Tables (device acts as a Zigbee router)

### Device Pairing
1. Find the small reset hole (between the On/Off button and the LED light) and make sure you have at hand a pin or
   pencil that can fit the reset hole
1. If the device is already plugged in, take it out for 20 seconds (power-cycle); do this before each pair attempt
1. Plug the device in an outlet
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. Insert the pin into the reset hole and press it for at least 5 seconds; upon release, the LED light will start
   blinking
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. That's it, Have fun!


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

### Known Issues
* The Hold / Release events don't work correctly on the Next and Prev buttons

### Device Pairing
1. Using a small screwdriver, open the battery compartiment and you should see the small pair button (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


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
1. Open the battery compartiment and you should see the small pair button - with two chain links on it (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub until the LED stops blinking and turns off
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


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
1. Open the battery compartiment and you should see the small pair button (don't push it!)
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within
   > 5 seconds**
1. > IMPORTANT: Immediately after the device LED starts blinking red, keep the device **as close as you can** against
   > your Hubitat hub for **at least 30 seconds** (after the LED stops blinking and turns off)
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. Close the device battery compartiment
1. That's it, Have fun!


## Tradfri LED Driver (ICPSHC24)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_ICPSHC24-30EU-IL-1.webp" style="width: 200px"> |
| Product Code | `603.426.56`｜`503.561.87` |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/ICPSHC24.groovy` |
| Tested firmwares | 10EU-IL-1: `1.2.245` |
|| 30EU-IL-2: `1.0.002` |

### Features
1. Commands: On, Off, Toggle, On with Timed Off
1. Configure what happens after a power outage (Power On, Power Off, Restore previous state)
1. Brightness control: Set brightness level, Start/Stop level change, Level up/down
1. Configure brightness level when turned on (Always the same value, Restore last level)
1. Can set the brightness level when the lights are off (and they stay off). When the lights are turned on, they will start at the specified level.
1. Health status (online / offline)
1. Refresh switch state on demand
1. Report Zigbee Neighbors and Routing Tables (device acts as a Zigbee router)

### Known Issues
* Smaller (10W) drivers do not honor the Power On Behavior

### Device Pairing
1. Have a light attached to the LED Driver device
1. Find the small reset hole on the device and make sure you have at hand a pin that can fit the reset hole (e.g.: a
   paper clip or SIM card eject pin)
1. If the device is already plugged in, take it out for 20 seconds (power-cycle); do this before each pair attempt
1. Plug the device in an outlet
1. In the Hubitat interface, go to "Devices", click "Add Device" in the top right, click "Zigbee", then click "Start
   Zigbee pairing"
1. Insert the pin into the reset hole and press it for at least 5 seconds; the attached light will blink then stay on
1. Return to the pairing page and give your device a name and assign it to a room (optional)
1. That's it, Have fun!

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)

# IKEA Zigbee drivers

Supported devices:
* [IKEA Tradfri Remote Control (E1810)](#symfonisk-sound-remote-gen2-e2123)
* [IKEA Tradfri Remote Control (E1810)](#tradfri-remote-control-e1810)

## Driver Install
### Install using HPM (offers automatic updates)
Follow the steps below if you already have the "Hubitat Package Manager" app installed in your Hubitat hub:
   * Go to "Apps" and select "Hubitat Package Manager"
   * Select "Install"
   * Select "Search by Keywords"
   * Enter "IKEA Zigbee drivers" in the search box and click "Next"
   * Select "IKEA Zigbee drivers by Dan Danache" and click "Next"
   * Select the required driver from the dropdown
   * Follow the install instructions

### Manual Install
Follow the steps below if you don't know what "Hubitat Package Manager" is:
   * Go to "Drivers code"
   * Click "New Driver" in the top right
   * Click "Import" in the top right
   * Search below for your device, look for the "Manual install file" property and enter it in the URL field
   * Click "Import", then click "OK"
   * Code should load in the editor
   * Click "Save" in the top right

## Symfonisk Sound Remote Gen2 (E2123)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://www.ikea.com/us/en/images/products/symfonisk-sound-remote-gen-2__1112597_pe871228_s5.jpg?f=xl" style="width: 200px"> |
| Product Code | [305.273.12](https://www.ikea.com/us/en/p/symfonisk-sound-remote-gen-2-30527312/) |
| Zigbee ID | SYMFONISK sound remote gen2 |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2123.groovy` |
| Tested firmwares | `1.0.012` `1.0.35` |

### Device Pairing
Follow the steps below in order to pair your IKEA Sound Remote with your Hubitat hub:
   * Open the battery compartiment of the IKEA Sound Remote; you should see the small pair button - with two chain links on it (don't push it!)
   * Go to "Devices"
   * Click "Add Device" in the top right
   * Click "Zigbee"
   * Click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the IKEA Sound Remote **as close as you can** against your Hubitat hub until the LED stops blinking and turns off
   * Return to the pairing page and give your device a name and assign it to a room
   * Close the device battery compartiment
   * That's it, Have fun!

## Tradfri Remote Control (E1810)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://zigbee.blakadder.com/assets/images/devices/Ikea_E1810.webp" style="width: 200px"> |
| Product Code | 304.431.24 |
| Zigbee ID | TRADFRI remote control |
| Manual install file | `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1810.groovy` |
| Tested firmwares | `24.4.5` |

> IMPORTANT: Old firmware versions allow only group bindings and this functionality is not supported by the Hubitat hub. Please update the remote to the latest version.

### Device Pairing
Follow the steps below in order to pair your Tradfri Remote with your Hubitat hub:
   * Open the battery compartiment of the IKEA Sound Remote; you should see the small pair button (don't push it!)
   * Go to "Devices"
   * Click "Add Device" in the top right
   * Click "Zigbee"
   * Click "Start Zigbee pairing"
   * > IMPORTANT: Move close to your Hubitat hub, then click the pair button in the battery compartiment **4 times within 5 seconds**
   * > IMPORTANT: Immediately after the device LED starts blinking red, keep the Tradfri Remote **as close as you can** against your Hubitat hub for at least 1 minute (after the LED stops blinking and turns off)
   * Return to the pairing page and give your device a name and assign it to a room
   * Close the device battery compartiment
   * That's it, Have fun!

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)

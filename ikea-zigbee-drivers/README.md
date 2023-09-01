# IKEA Zigbee drivers

## Symfonisk Sound Remote Gen2 (E2123)

| Parameter | Details |
|-----------|-------------|
| Product Image | <img src="https://www.ikea.com/us/en/images/products/symfonisk-sound-remote-gen-2__1112597_pe871228_s5.jpg?f=xl" style="width: 200px"> |
| Product Code | [305.273.12](https://www.ikea.com/us/en/p/symfonisk-sound-remote-gen-2-30527312/) |
| Zigbee ID | SYMFONISK sound remote gen2 |
| Hubitat Capabilities | Configuration, Battery, PushableButton, DoubleTapableButton, HoldableButton, Switch, SwitchLevel |

### Driver Install
#### Install using HPM (offers automatic updates)
Follow the steps below if you already have the "Hubitat Package Manager" app installed in your Hubitat hub:
   * Go to "Apps" and select "Hubitat Package Manager"
   * Select "Install"
   * Select "Search by Keywords"
   * Enter "IKEA Zigbee drivers" in the search box and click "Next"
   * Select "IKEA Zigbee drivers by Dan Danache" and click "Next"
   * Follow the install instructions

#### Manual Install
Follow the steps below if you don't know what "Hubitat Package Manager" is:
   * Go to "Drivers code"
   * Click "New Driver" in the top right
   * Click "Import" in the top right
   * Enter `https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2123.groovy` in the URL field
   * Click "Import", then click "OK"
   * Code should load in the editor
   * Click "Save" in the top right

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

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)

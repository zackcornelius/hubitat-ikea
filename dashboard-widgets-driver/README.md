# Dashboard Widgets

## Available widgets

| Widget | Preview | Parameters |
|--------|---------|------------|
| Clock | [preview](https://dan-danache.github.io/hubitat/dashboard-widgets-driver/widgets/clock.html?mock=true) | [parameters](https://github.com/dan-danache/hubitat/blob/main/dashboard-widgets-driver/widgets/clock.html) |
| Fan | [preview](https://dan-danache.github.io/hubitat/dashboard-widgets-driver/widgets/fan.html?mock=true) | [parameters](https://github.com/dan-danache/hubitat/blob/main/dashboard-widgets-driver/widgets/fan.html) |
| Wind | [preview](https://dan-danache.github.io/hubitat/dashboard-widgets-driver/widgets/wind.html?mock=true) | [parameters](https://github.com/dan-danache/hubitat/blob/main/dashboard-widgets-driver/widgets/wind.html) |

## Install and usage
1. **Install "Dashboard Widgets" package using [HPM](https://hubitatpackagemanager.hubitatcommunity.com/)**\
   This action will:
   * install the "Dashboard Widgets" device driver
   * add HTML files for each widget into the Hubitat File Manager
3. **Create a new Virtual Device**
   * Go to "Devices"
   * Click "Add Device" in the top right
   * Select "Virtual"
   * Give the device a name (e.g.: "Dashboard Widgets")
   * From the "Type" dropdown, select "Dashboard Widgets"
   * Click "Save Device"
4. **Enable Maker API integration**
   * Go to the newly added device page
   * In the "Preferences" section, enable the "Use Maker API" option
   * Click "Save Preferences"
   * Make sure that the "Maker API link" field is corectly configured; the driver will try to guess this value but, if it fails, follow the provided instructions to retreive the correct value
   * Click again "Save Preferences"
5. **Assign widgets to the device attributes**\
   The newly added device exports a fixed list of HTML attributes "Alfa", "Bravo", "Charlie", etc. For each of these attributes, you can configure and assign widgets (one per attribute).
   > TODO: Add more info here!
6. **Authorize dashboard to access the newly added device**
   * Go to "Apps"
   * Select one of the existing dashboards
   * In the "Choose Devices" section, check the newly added device name
   * Click "Done" in the bottom right
7. **Add widget tile to dashboard**
   * Go to "Dashboards"
   * Select the dashboard you authorized in Step 5
   * Click "+" in the top right to add a new dashboard tile
   * In the "Pick a Device" section, select the newly added device
   * In the "Pick a Template" section, select "Attribute"
   * In the "Pick an Attribute" section, select one of the attributes you configured in Step 4 (e.g.: "Alfa")
  
## Responsive tiles
Widgets are responsive, they will automatically scale if you modify the dashboard tile size. There is however a small bug that prevents this from happening; to fix it, follow these steps:
* Go to "Dashboards"
* Select the dashboard you authorized in Step 5
* Click the cogwheel icon in the top right to open dashboard settings
* Click "Advanced"
* Click "CSS"
* Enter the following code in the text area below:
  ```css
  .tile-primary { height: 100% }
  ```
* Finally, click "Save CSS"

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)

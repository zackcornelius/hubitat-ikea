# Dashboard Widgets

Available widgets:
* [Clock](https://dan-danache.github.io/hubitat/dashboard-widgets-driver/widgets/clock.html)
* [Fan](https://dan-danache.github.io/hubitat/dashboard-widgets-driver/widgets/fan.html)
* [Wind](https://dan-danache.github.io/hubitat/dashboard-widgets-driver/widgets/wind.html)

## Install and usage
1. **Install "Dashboard Widgets" package from HPM**\
   This action will install the "Dashboard Widgets" device driver and the HTML files for each widget into Hubitat File Manager
2. **Create a new Virtual Device**
   * Go to "Devices"
   * Click "Add Device" in the top right
   * Select "Virtual"
   * Give the device a name (e.g.: "Dashboard Widgets")
   * From the "Type" dropdown, select "Dashboard Widgets"
   * Click "Save Device"
3. **Assign widgets to the device attributes**\
   The newly added device exports a fixed list of HTML attributes "Alfa", "Bravo", "Charlie", etc. For each of these attributes, you can configure and assign a widget.
   > TODO: Add more info here!
4. **Authorize dashboard to access the newly added device**\
   * Go to "Apps"
   * Select one of the existing dashboards
   * In the "Choose Devices" section, check the newly added device name
   * Click "Done" in the bottom right
5. **Add widget to dashboard**
   * Go to "Dashboards"
   * Select the dashboard you authorized in Step 4
   * Click "+" in the top right to add a new dashboard tile
   * In the "Pick a Device" section, select the newly added device
   * In the "Pick a Template" section, select "Attribute"
   * In the "Pick an Attribute" section, select one of the attributes you configured in Step 3 (e.g.: "Alfa")
   * Note: Widgets are responsive, they will automatically scale if you modify the dashbaord tile size
   

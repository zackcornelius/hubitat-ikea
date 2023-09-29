# Knockturn Alley

Simple toolkit driver to help developers peer deep into the guts of Zigbee devices.

**Note:** This driver is useless to non-developers as it cannot actually control any smart device.

| Spells | Attributes Report |
|--------|-------------------|
| ![Spells](img/Screenshot_1.png) | ![Attributes Report](img/Screenshot_2.png) |

> Skulking around Knockturn Alley? Dodgy place. Don't want no one to see you there. \
> -- Hagrid

## Usage
This driver has no registered fingerprints and no configuration / initialization procedure so it does not support the pairing process for new devices. Regular hacking workflow goes like this:

1. If you have a new Zigbee device, pair it as usual with the correct driver. For already paired devices, there's nothing to do here.
2. If you want to know more about a Zigbee device, go to the device details page and change the driver to `Knockturn Alley`.
3. No initialization / configuration is required.
4. Cast whatever spells you want using the `Knockturn Alley` driver. Have the `Logs` section opened in a separate tab since the driver talks to you mostly via log entries.
5. When you decided you had enough fun, cast the `Obliviate` spell with option `1` to get rid of the `ka_*` device state entries (we clean our own mess).
6. From the device details page, change back to the original driver. Everything should work without the need to reconfigure / re-pair the device.
7. Pick a new Zigbee device to torture and go back to Step 2 :)


## Spells
The following functionalities are currently implemented:
- [A01 - Legilimens](#a01---legilimens)
- [A02 - Scourgify](#a02---scourgify)
- [B01 - Accio](#b01---accio)
- [C01 - Obliviate](#c01---obliviate)
- [C02 - Imperio](#c02---imperio)
- [C03 - Bombarda](#c03---bombarda)

### A01 - Legilimens
<img src="img/Legilimens.gif" height="200px"/>

`Legilimens` spell automatically collects information on all Zigbee attributes that the device exposes. When cast, it will:
1. Retrieve all Zigbee endpoints (e.g.: 0x01 = Default endpoint)
2. For each endpoint, retrieve in and out clusters (e.g.: 0x0006 = On/Off Cluster)
3. For each in cluster, discover attributes (e.g.: 0x0400 = SWBuildID - for cluster 0x0000)
4. For each attribute, ask the device to send its current value
5. If an attribute is known to be reportable, ask the device to send its current reporting configuration

Before casting the spell, have the Logs section open in order to take a peak at the chatty conversation that the driver is having with the device. Be patient, the discovering process will take about 1 minute to finish (depending on the number of endpoints/clusters/attributes). Keep your eyes on the Logs to see when the driver stops adding log entries.

> **Important**: If the device is battery-powered, press any button to wake it before casting the `Legilimens` spell; then, keep on pressing buttons every second or so in order to prevent the device from going back to sleep.

When the discovery process is complete, refresh the device details page to see what data was gathered. This data will be hard to follow in its raw form, so you should continue with casting the next spell.

### A02 - Scourgify
<img src="img/Scourgify.webp" height="200px"/>

`Scourgify` spell cleans up the data mess we got after casting the `Legilimens` spell. When cast, it will:
1. Read data gathered using the `Legilimens` spell. You need to first cast the `Legilimens`, otherwise nothing will happen.
2. Use the raw data to create a friendly attributes report.

After casting the spell, refresh the device details page to see the attributes report. Have fun!

### B01 - Accio
<img src="img/Accio.gif" height="200px"/>

`Accio` spell retrieves information about the Zigbee attribute identified by the endpoint / cluster / attribute coordinates. When cast, it can:
1. Read the current value of the specified attribute.
2. Read the reporting configuration for the specified attribute.

Before casting the spell, have the Logs section open in order to see the device response.

### C01 - Obliviate
<img src="img/Obliviate.gif" height="200px"/>

`Obliviate` spell is used to forget specific information present in the device details page. When cast, it can remove:
1. Our state variables (ka_*) - Remove only information that was added by this driver, so that you can go back to using the original driver.
2. All state variables - Remove all stored state data. You may use this if you want to switch drivers and start with a clean state.
3. Device data - Remove all information present in the `Device Details -> Data` section. Useful when switching drivers.
4. Scheduled jobs configured by the previous driver. Useful when switching drivers.
5. Everything - Forget everything, start anew.

After casting the spell, refresh the device details page to see that the specified information vanished into the void.

### C02 - Imperio
<img src="img/Imperio.gif" height="200px"/>

`Imperio` spell updates the value for the specified Zigbee attribute. You can now fight back and do some real damage to your devices!

After casting the spell, you may want to cast `Accio` to query the device for the updated attribute value.

### C03 - Bombarda
<img src="img/Bombarda.gif" height="200px"/>

`Bombarda` spell executes the specified Zigbee command. Keep an eye on the Logs section to see if you got the command payload right!

---
[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 162px !important">](https://www.buymeacoffee.com/dandanache)

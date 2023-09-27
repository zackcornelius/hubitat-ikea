# Knockturn Alley driver

Simple toolkit to peer into the guts of any Zigbee device.

## Spells
The following functionalities are currently implemented:
- [A01 - Legilimens](#a01---legilimens)
- [A02 - Legilimens](#a02---scourgify)

### A01 - Legilimens
The Legilimens spell is trying to automatically discover all available attributes of a Zigbee device. When cast, it will:
1. Retrieve all available Zigbee endpoints (e.g.: 0x01 - Default endpoint)
2. For each endpoint, retrieve all in and out clusters (e.g.: 0x0006 - On/Off Cluster)
3. For each cluster, discover all attributes (e.g.: 0x0400 - SWBuildID (for cluster 0x0000))
4. For each attribute, ask the device to send its current value
5. If an attributes is known to be reportable, as the device to send it current reporting configuration

Be patient, the discovering process will take about 1 minute to finish (depending on the number of endpoints/clusters/attributes). Keep your eyes on the Logs to see when the device stops adding log entries.

> **Important**: If the device is battery-powered, press any button to wake it before casting the `Legilimens` spell, and keep on pressing buttons every second or so in order to prevent the device from going back to sleep.

When the discovery process is complete, refresh the device details page in order to see what data was gathered. Data will be hard to follow, so you should continue with the next spell.

### A02 - Scourgify
The Scourgify spell will try to cleanup the data mess we got after casting the Legilimens spell. When cast it will:
1. Read the data gathered during the `Legilimens` spell
2. Create a friendly attributes report

After casting the spell, refresh the device details page in order to render the attributes report. Have fun!


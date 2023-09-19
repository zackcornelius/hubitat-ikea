{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "Battery"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# configure }}

// Configuration for capability.Battery
cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
{{/ configure }}
{{!--------------------------------------------------------------------------}}

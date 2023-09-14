// Switch state was changed
case { contains it, [clusterInt:0x0006, commandInt:0x0B] }:
    def newState = msg.data[0] == "00" ? "off" : "on"
    Log.debug "Reported switch status as ${newState}"
    return Utils.updateSwitch(newState)

// Device sent value for the OnOff attribute
case { contains it, [clusterInt:0x0006, attrInt: 0x0000] }:
    def newState = msg.value == "00" ? "off" : "on"
    Log.debug "Reported switch status as ${newState}"
    return Utils.updateSwitch(newState)

// Device_annce
case { contains it, [clusterInt:0x0013, commandInt:0x00] }:
    Log.info "Has been plugged back in. Querying its switch status..."
    return Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))

// Switch state was changed
case { contains it, [clusterInt:0x0006, commandInt:0x0B] }:
    def newState = msg.data[0] == "00" ? "off" : "on"
    Log.debug "Reported OnOff status as ${newState}"
    return Utils.updateSwitch(newState)

// Read Attribute Response: OnOff
case { contains it, [clusterInt:0x0006, attrInt: 0x0000] }:
    def newState = msg.value == "00" ? "off" : "on"
    Log.debug "Reported OnOff status as ${newState}"
    return Utils.updateSwitch(newState)

// Read Attribute Response: StartupOnOff
case { contains it, [clusterInt:0x0006, attrInt: 0x4003] }:
    def newValue = ""
    switch (Integer.parseInt(msg.value, 16)) {
        case 0x00: newValue = "OFF"; break
        case 0x01: newValue = "ON"; break
        case 0xFF: newValue = "PREV"; break
        default: return Log.warn("Received attribute value: StartupOnOff=${msg.value}")
    }
    startupOnOff = newValue
    device.clearSetting "startupOnOff"
    device.removeSetting "startupOnOff"
    device.updateSetting "startupOnOff", newValue
    return Log.debug("Reported StartupOnOff as ${newValue}")

// Device reported that the one of the on(), off() or toggle() command was processed
// Examples:
// - On:     0104 0006 01 01 0040 00 7F1B 00 00 0000 0B 01 0100
// - Off:    0104 0006 01 01 0040 00 7F1B 00 00 0000 0B 01 0000
// - Toggle: 0104 0006 01 01 0040 00 7F1B 00 00 0000 0B 01 0200
case { contains it, [clusterInt:0x0006, commandInt:0x0B] }:
    Log.debug "on/off/toggle command response: data=${msg.data}"

    // Toggle?
    if (msg.data[0] == "02") {
        state.isPhysical = false
        return Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
    }

    def newState = msg.data[0] == "00" ? "off" : "on"
    return Utils.sendEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:"digital", isStateChange:false)

// Read Attribute Response: OnOff
case { contains it, [clusterInt:0x0006, attrInt: 0x0000] }:
    Log.debug "Reported OnOff attribute: value=${msg.value}"

    def newState = msg.value == "00" ? "off" : "on"
    def type = state.containsKey("isPhysical") && state.isPhysical == false ? "digital" : "physical"
    state.remove("isPhysical")
    return Utils.sendEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:type, isStateChange:false)

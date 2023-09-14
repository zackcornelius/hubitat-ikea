// Button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    def button = BUTTONS.ONOFF
    Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button was pushed")

    // Also act as a switch
    return Utils.toggleSwitch()

// Button was double tapped
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
    def button = BUTTONS.ONOFF;
    return Utils.sendPhysicalEvent(name:"doubleTapped", value:button[0], isStateChange:false, descriptionText:"Button was double tapped")

// Button was held
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = BUTTONS.ONOFF;
    return Utils.sendPhysicalEvent(name:"held", value:button[0], isStateChange:false, descriptionText:"Button was held")

// Button was released
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    def button = BUTTONS.ONOFF;
    return Utils.sendPhysicalEvent(name:"released", value:button[0], isStateChange:false, descriptionText:"Button was released")

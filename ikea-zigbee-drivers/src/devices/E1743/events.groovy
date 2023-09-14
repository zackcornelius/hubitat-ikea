// I/O button was pressed
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    def button = msg.commandInt == 0x00 ? BUTTONS.OFF : BUTTONS.ON
    Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

    // Also act as a switch
    Utils.updateSwitch(msg.commandInt == 0x00 ? "off" : "on")

    // Also act as a dimmer
    return button == BUTTONS.ON ? Utils.levelUp() : Utils.levelDown()

// I/O button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = msg.commandInt == 0x01 ? BUTTONS.OFF : BUTTONS.ON
    return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

// I/O button was released
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    def button = device.currentValue("held", true) == 1 ? BUTTONS.ON : BUTTONS.OFF
    return Utils.sendPhysicalEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")

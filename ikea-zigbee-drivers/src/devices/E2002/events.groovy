// Plus/Minus button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    def button = msg.commandInt == 0x00 ? BUTTONS.MINUS : BUTTONS.PLUS
    Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

    // Also act as a dimmer
    return button == BUTTONS.PLUS ? Utils.levelUp() : Utils.levelDown()

// Plus/Minus button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = msg.commandInt == 0x01 ? BUTTONS.MINUS : BUTTONS.PLUS
    return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

// Plus/Minus button was released
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    def button = device.currentValue("held", true) == 1 ? BUTTONS.PLUS : BUTTONS.MINUS
    return Utils.sendPhysicalEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")

// Next/Prev button was pushed
case { contains it, [clusterInt:0x0005, commandInt:0x07] }:
    def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

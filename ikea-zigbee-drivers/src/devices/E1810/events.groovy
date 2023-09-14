// Button Prev/Next was pressed
case { contains it, [clusterInt:0x0005, commandInt:0x07] }:
    def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Button Prev/Next was held
case { contains it, [clusterInt:0x0005, commandInt:0x08] }:
    def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

// Button Prev/Next was released
case { contains it, [clusterInt:0x0005, commandInt:0x09] }:
    def button = device.currentValue("held") == 4 ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendPhysicalEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")

// Set switch state
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    def newState = msg.commandInt == 0x00 ? "off" : "on"
    return Utils.sendPhysicalEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")

// Power button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    def button = BUTTONS.POWER
    Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

    // Also act as a switch
    return Utils.toggleSwitch()

// Plus/Minus button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
case { contains it, [clusterInt:0x0008, commandInt:0x06] }:
    def button = msg.commandInt == 0x02 ? BUTTONS.MINUS : BUTTONS.PLUS
    Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

    // Also act as a dimmer
    return button == BUTTONS.PLUS ? Utils.levelUp() : Utils.levelDown()

// Plus/Minus button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = msg.commandInt == 0x01 ? BUTTONS.MINUS : BUTTONS.PLUS
    return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

// Plus/Minus button was released
case { contains it, [clusterInt:0x0008, commandInt:0x03] }:
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    def button = msg.commandInt == 0x03 ? BUTTONS.MINUS : BUTTONS.PLUS
    return Utils.sendPhysicalEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")

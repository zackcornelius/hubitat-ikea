// Button Prev/Next was pressed
case { contains it, [clusterInt:0x0005, commandInt:0x07] }:
    def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Button Prev/Next was held
case { contains it, [clusterInt:0x0005, commandInt:0x08] }:
    def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")

// Button Prev/Next was released
case { contains it, [clusterInt:0x0005, commandInt:0x09] }:
    def button = device.currentValue("held") == 4 ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")

// Power button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    def button = BUTTONS.POWER
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Plus/Minus button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
case { contains it, [clusterInt:0x0008, commandInt:0x06] }:
    def button = msg.commandInt == 0x02 ? BUTTONS.MINUS : BUTTONS.PLUS
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Plus/Minus button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = msg.commandInt == 0x01 ? BUTTONS.MINUS : BUTTONS.PLUS
    return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")

// Plus/Minus button was released
case { contains it, [clusterInt:0x0008, commandInt:0x03] }:
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    def button = msg.commandInt == 0x03 ? BUTTONS.MINUS : BUTTONS.PLUS
    return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")

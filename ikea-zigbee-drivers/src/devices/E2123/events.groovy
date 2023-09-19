// Play button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
    def button = BUTTONS.PLAY
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Plus/Minus button was held
case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
    def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
    return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")

// Next/Prev button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
    def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Plus/Minus button was pushed
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// Undocumented cluster (0xFC7F) - Used by firmware 1.0.012 (20211214)
case { contains it, [clusterInt:0xFC7F] }:
    def button = msg.data[0] == "01" ? BUTTONS.DOT_1 : BUTTONS.DOT_2

    // 1 Dot / 2 Dots button was pushed
    if (msg.data[1] == "01") {
        return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pressed")
    }

    // 1 Dot / 2 Dots button was double tapped
    if (msg.data[1] == "02") {
        return Utils.sendEvent(name:"doubleTapped", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
    }

    // 1 Dot / 2 Dots button was held
    if (msg.data[1] == "03") {
        return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")
    }

// Undocumented cluster (0xFC80) - Used by firmware 1.0.35 (20230411)
case { contains it, [clusterInt:0xFC80] }:
    def button = msg.sourceEndpoint == "02" ? BUTTONS.DOT_1 : BUTTONS.DOT_2

    // IGNORED: 1 Dot / 2 Dots button was pressed-down
    if (msg.commandInt == 0x01) {
        return Log.debug("Button ${button[0]} (${button[1]}) was pressed-down (ignored as we wait for the next message to distinguish between click, double tap and hold)")
    }

    // 1 Dot / 2 Dots button was held
    // Commands are issued in this order: 01 (key-down = ignored) -> 02 (button is held = update "held" attribute) -> 04 (button released = update "released" attribute)
    if (msg.commandInt == 0x02) {
        return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was held")
    }

    // 1 Dot / 2 Dots button was pushed
    if (msg.commandInt == 0x03) {
        return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pressed")
    }

    // IGNORED: 1 Dot / 2 Dots button was released
    if (msg.commandInt == 0x04) {
        return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")
    }

    // 1 Dot / 2 Dots button was double tapped
    if (msg.commandInt == 0x06) {
        return Utils.sendEvent(name:"doubleTapped", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
    }

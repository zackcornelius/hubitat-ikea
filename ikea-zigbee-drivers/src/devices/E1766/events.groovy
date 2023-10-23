// I/O button was pressed
case { contains it, [clusterInt:0x0102, commandInt:0x00] }:
case { contains it, [clusterInt:0x0102, commandInt:0x01] }:
    def button = msg.commandInt == 0x00 ? BUTTONS.OPEN : BUTTONS.CLOSE
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

// I/O button was released
case { contains it, [clusterInt:0x0102, commandInt:0x02] }:
    def button = device.currentValue("pushed", true) == 1 ? BUTTONS.OPEN : BUTTONS.CLOSE
    return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button ${button[0]} (${button[1]}) was released")

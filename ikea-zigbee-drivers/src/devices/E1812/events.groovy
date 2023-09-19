// Button was pushed
case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
    def button = BUTTONS.ONOFF
    return Utils.sendEvent(name:"pushed", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button 1 was pushed")

// Button was double tapped
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
    def button = BUTTONS.ONOFF;
    return Utils.sendEvent(name:"doubleTapped", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button 1 was double tapped")

// Button was held
case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
    def button = BUTTONS.ONOFF;
    return Utils.sendEvent(name:"held", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button 1 was held")

// Button was released
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    def button = BUTTONS.ONOFF;
    return Utils.sendEvent(name:"released", value:button[0], type:"physical", isStateChange:true, descriptionText:"Button 1 was released")

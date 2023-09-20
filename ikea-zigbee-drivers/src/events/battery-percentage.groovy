// General::Power (0x0001) / Battery report (0x0021)
case { contains it, [clusterInt:0x0001, attrInt:0x0021] }:
    def percentage =  Integer.parseInt(msg.value, 16)

    // (0xFF) 255 is an invalid value for the battery percentage attribute, so we just ignore it
    if (percentage == 255) {
        Log.warn "Ignored invalid battery percentage value: 0xFF (255)"
        return
    }

    {{# params.half }}
    percentage =  Math.round(percentage / 2)
    {{/ params.half }}
    return Utils.sendEvent(name:"battery", value:percentage, unit:"%", type:"physical", isStateChange:true, descriptionText:"Battery is ${percentage}% full")

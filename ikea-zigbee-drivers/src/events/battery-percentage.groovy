// General::Power (0x0001) / Battery report (0x0021)
case { contains it, [clusterInt:0x0001, attrInt:0x0021] }:
    def percentage =  Integer.parseInt(msg.value, 16)
    {{# params.half }}
    percentage =  Math.round(percentage / 2)
    {{/ params.half }}
    return Utils.sendPhysicalEvent(name:"battery", value:percentage, unit:"%", descriptionText:"Battery is ${percentage}% full")

{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Battery"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.Battery
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0001 0x0021 0x20 0x0000 0xA8C0 {01} {}" // Report battery at least every 12 hours
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}" // Power Configuration cluster
cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.Battery

// Report Attributes: BatteryPercentage
// Read Attributes Reponse: BatteryPercentage
case { contains it, [clusterInt:0x0001, commandInt:0x0A, attrInt:0x0021] }:
case { contains it, [clusterInt:0x0001, commandInt:0x01, attrInt:0x0021] }:
    Integer percentage = Integer.parseInt(msg.value, 16)

    // (0xFF) 255 is an invalid value for the battery percentage attribute, so we just ignore it
    if (percentage == 255) {
        Log.warn "Ignored invalid reported battery percentage value: 0xFF (255)"
        return
    }

    {{# params.half }}
    percentage =  percentage / 2
    {{/ params.half }}
    Utils.sendEvent name:"battery", value:percentage, unit:"%", type:"physical", descriptionText:"Battery is ${percentage}% full"
    return Utils.processedZclMessage("Report/Read Attributes Response", "BatteryPercentage=${percentage}")

// Other events that we expect but are not usefull for capability.Battery behavior

// ConfigureReportingResponse := { 08:Status, 08:Direction, 16:AttributeIdentifier }
// Success example: [00] -> status = SUCCESS
case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
    if (msg.data[0] != "00") return Utils.failedZclMessage("Configure Reporting Response", msg.data[0], msg)
    return Utils.processedZclMessage("Configure Reporting Response", "cluster=0x${msg.clusterId}, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}

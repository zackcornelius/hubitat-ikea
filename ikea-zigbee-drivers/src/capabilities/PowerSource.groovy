{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "PowerSource"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.PowerSource
sendEvent name:"powerSource", value:"unknown", type:"digital", descriptionText:"Power source initialized to unknown"
cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Read Attributes Reponse: PowerSource
case { contains it, [clusterInt:0x0000, commandInt:0x01, attrInt:0x0007] }:
    String powerSource = "unknown"

    // PowerSource := { 0x00:Unknown, 0x01:MainsSinglePhase, 0x02:MainsThreePhase, 0x03:Battery, 0x04:DC, 0x05:EmergencyMainsConstantlyPowered, 0x06:EmergencyMainsAndTransferSwitch }
    switch (msg.value) {
        case "01":
        case "02":
        case "05":
        case "06":
            powerSource = "mains"
            break
        case "03":
            powerSource = "battery"
            break
        case "04":
            powerSource = "dc"
            break
    }
    Utils.sendEvent name:"powerSource", value:powerSource, type:"digital", descriptionText:"Power source is ${powerSource}"
    return Utils.processedZclMessage("Read Attributes Response", "PowerSource=${msg.value}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}

// General::Basic cluster (0x0000) - Read Attribute Response (0x01)
case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
    Utils.processedZigbeeMessage("Read Attribute Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
    switch (msg.attrInt) {
        case 0x0001: return Utils.zigbeeDataValue("application", msg.value)
        case 0x0003: return Utils.zigbeeDataValue("hwVersion", msg.value)
        case 0x0004: return Utils.zigbeeDataValue("manufacturer", msg.value)
        case 0x0005:
            if (msg.value == "{{ device.zigbeeId }}") updateDataValue "type", "{{ device.type }}"
            return Utils.zigbeeDataValue("model", msg.value)
        case 0x4000: return Utils.zigbeeDataValue("softwareBuild", msg.value)
    }
    return Log.warn("Unexpected Zigbee attribute: cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, msg=${msg}")

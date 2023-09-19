// Active_EP_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:ActiveEPCount, n*08:ActiveEPList }
// Three endpoints example: [83, 00, 18, 4A, 03, 01, 02, 03] -> endpointIds=[01, 02, 03]
case { contains it, [clusterInt:0x8005] }:
    if (msg.data[1] != "00") {
        return Utils.failedZigbeeMessage("Active Endpoints Response", msg)
    }

    def cmds = []
    def endpointIds = []

    def count = Integer.parseInt(msg.data[4], 16)
    if (count > 0) {
        (1..count).each() { i ->
            def endpointId = msg.data[4 + i]
            endpointIds.add endpointId
            
            // Query simple descriptor data
            cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} ${endpointId}} {0x0000}"
        }
        Utils.sendZigbeeCommands cmds
    }

    // Add "endpointIds" only if device exposes more then one
    if (count > 1) {
        Utils.zigbeeDataValue "endpointIds", endpointIds.join(",")
    }
    return Utils.processedZigbeeMessage("Active Endpoints Response", "endpointIds=${endpointIds}")

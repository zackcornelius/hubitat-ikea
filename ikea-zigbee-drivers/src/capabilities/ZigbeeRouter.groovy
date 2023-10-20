{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "HealthCheck"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.ZigbeeRouter
attribute "neighbors", "STRING"
attribute "routes", "STRING"
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.ZigbeeRouter
command "requestRoutingData"
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send its Neighbors Table and the Routing Table data ..."
    Utils.sendZigbeeCommands([
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {00} {0x00}",
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {00} {0x00}"
    ])
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.ZigbeeRouter

// Mgmt_Lqi_rsp := { 08:Status, 08:NeighborTableEntries, 08:StartIndex, 08:NeighborTableListCount, n*176:NeighborTableList }
// NeighborTableList := { 64:ExtendedPanId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
// Example: [6E, 00, 08, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 24, 02, 00, CF, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 05, 02, 0F, CD, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 36]
case { contains it, [endpointInt:0x00, clusterInt:0x8031, commandInt:0x00] }:
    if (msg.data[1] != "00") return Utils.failedZdoMessage("Neighbors Table Response", msg.data[1], msg)
    Integer entriesCount = Integer.parseInt(msg.data[4], 16)

    // Use base64 encoding instead of hex encoding to make the message a bit shorter
    String base64 = msg.data.join().decodeHex().encodeBase64().toString() // Decode test: https://base64.guru/converter/decode/hex
    sendEvent name:"neighbors", value:"${entriesCount} entries", type:"digital", descriptionText:base64
    return Utils.processedZdoMessage("Neighbors Table Response", "entries=${entriesCount}, data=${msg.data}")

// Mgmt_Rtg_rsp := { 08:Status, 08:RoutingTableEntries, 08:StartIndex, 08:RoutingTableListCount, n*40:RoutingTableList }
// RoutingTableList := { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
// Example: [6F, 00, 0A, 00, 0A, 00, 00, 10, 00, 00, AD, 56, 00, AD, 56, ED, EE, 00, 4A, 16, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00]
case { contains it, [endpointInt:0x00, clusterInt:0x8032, commandInt:0x00] }:
    if (msg.data[1] != "00") return Utils.failedZdoMessage("Routing Table Response", msg.data[1], msg)
    Integer entriesCount = Integer.parseInt(msg.data[4], 16)

    // Use base64 encoding instead of hex encoding to make the message a bit shorter
    String base64 = msg.data.join().decodeHex().encodeBase64().toString()
    sendEvent name:"routes", value:"${entriesCount} entries", type:"digital", descriptionText:base64
    return Utils.processedZdoMessage("Routing Table Response", "entries=${entriesCount}, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}

// Mgmt_Rtg_rsp := { 08:Status, 08:RoutingTableEntries, 08:StartIndex, 08:RoutingTableListCount, n*40:RoutingTableList }
// RoutingTableList := { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
// Example: [6F, 00, 0A, 00, 0A, 00, 00, 10, 00, 00, AD, 56, 00, AD, 56, ED, EE, 00, 4A, 16, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00]
case { contains it, [clusterInt:0x8032, commandInt:0x00] }:
    if (msg.data[1] != "00") {
        return Utils.failedZigbeeMessage("Routing Table Response", msg)
    }
    def entriesCount = Integer.parseInt(msg.data[4], 16)

    // Use base64 encoding instead of hex encoding to make the message a bit shorter
    def base64 = msg.data.join().decodeHex().encodeBase64().toString()
    sendEvent name:"routes", value:"${entriesCount} entries", type:"digital", descriptionText:base64
    return Utils.processedZigbeeMessage("Routing Table Response", "entries=${entriesCount}, data=${msg.data}")

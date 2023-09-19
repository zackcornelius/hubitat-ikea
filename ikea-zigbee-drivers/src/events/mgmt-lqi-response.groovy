// Mgmt_Lqi_rsp := { 08:Status, 08:NeighborTableEntries, 08:StartIndex, 08:NeighborTableListCount, n*176:NeighborTableList }
// NeighborTableList := { 64:ExtendedPanId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
// Example: [6E, 00, 08, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 24, 02, 00, CF, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 05, 02, 0F, CD, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 36]
case { contains it, [clusterInt:0x8031, commandInt:0x00] }:
    if (msg.data[1] != "00") {
        return Utils.failedZigbeeMessage("Neighbors Table Response", msg)
    }
    def entriesCount = Integer.parseInt(msg.data[4], 16)

    // Use base64 encoding instead of hex encoding to make the message a bit shorter
    def base64 = msg.data.join().decodeHex().encodeBase64().toString() // Decode test: https://base64.guru/converter/decode/hex
    sendEvent name:"neighbors", value:"${entriesCount} entries", type:"digital", descriptionText:base64
    return Utils.processedZigbeeMessage("Neighbors Table Response", "entries=${entriesCount}, data=${msg.data}")

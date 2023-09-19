// Bind_rsp := { 08:Status }
// Success example: [26, 00] -> status = SUCCESS
// Fail example: [26, 82] -> status = INVALID_EP
case { contains it, [clusterInt:0x8021] }:
    if (msg.data[1] != "00") {
        return Utils.failedZigbeeMessage("Bind Response", msg)
    }
    return Utils.processedZigbeeMessage("Bind Response", "data=${msg.data}")

// Simple_Desc_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:Length, 08:Endpoint, 16:ApplicationProfileIdentifier, 16:ApplicationDeviceIdentifier, 08:Reserved, 16:InClusterCount, n*16:InClusterList, 16:OutClusterCount, n*16:OutClusterList }
// Example: [B7, 00, 18, 4A, 14, 03, 04, 01, 06, 00, 01, 03, 00,  00, 03, 00, 80, FC, 03, 03, 00, 04, 00, 80, FC] -> endpointId=03, inClusters=[0000, 0003, FC80], outClusters=[0003, 0004, FC80]
case { contains it, [clusterInt:0x8004] }:
    if (msg.data[1] != "00") {
        return Utils.failedZigbeeMessage("Simple Descriptor Response", msg)
    }

    def endpointId = msg.data[5]
    updateDataValue("profileId", msg.data[6..7].reverse().join())

    Integer count = Integer.parseInt(msg.data[11], 16)
    Integer position = 12
    Integer positionCounter = null
    def inClusters = []
    if (count > 0) {
        (1..count).each() { b->
            positionCounter = position+((b-1)*2)
            inClusters.add msg.data[positionCounter..positionCounter+1].reverse().join()
        }
    }
    position += count * 2
    count = Integer.parseInt(msg.data[position], 16)
    position += 1
    def outClusters = []
    if (count > 0) {
        (1..count).each() { b->
            positionCounter = position+((b-1)*2)
            outClusters.add msg.data[positionCounter..positionCounter+1].reverse().join()
        }
    }

    Utils.zigbeeDataValue "inClusters (${endpointId})", inClusters.join(",")
    Utils.zigbeeDataValue "outClusters (${endpointId})", outClusters.join(",")
    return Utils.processedZigbeeMessage("Simple Descriptor Response", "endpointId=${endpointId}, inClusters=${inClusters}, outClusters=${outClusters}")

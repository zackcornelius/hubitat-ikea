// Device_annce := { 16:NWKAddr, 64:IEEEAddr , 01:Capability }
// Example: [82, CF, A0, 71, 0F, 68, FE, FF, 08, AC, 70, 80] -> addr=A0CF, zigbeeId=70AC08FFFE680F71, capabilities=10000000
case { contains it, [clusterInt:0x0013, commandInt:0x00] }:
    def addr = msg.data[1..2].reverse().join()
    def zigbeeId = msg.data[3..10].reverse().join()
    def capabilities = Integer.toBinaryString(Integer.parseInt(msg.data[11], 16))
    Utils.processedZigbeeMessage("Device Announce Response", "addr=${addr}, zigbeeId=${zigbeeId}, capabilities=${capabilities}")
    {{# exec }}

    // Welcome back; let's sync state
    Log.debug("Rejoined the network. Executing {{ exec }} ...")
    {{ exec }}
    {{/ exec }}
    return

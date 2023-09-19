// OnWithTimedOff := { 08:OnOffControl, 16:OnTime, 16:OffWaitTime }
// OnOffControl := { 01:AcceptOnlyWhenOn, 07:Reserved }
// Example: [01, 08, 07, 00, 00] -> acceptOnlyWhenOn=true, onTime=180, offWaitTime=0
case { contains it, [clusterInt:0x0006, commandInt:0x42] }:
    def onTime = Math.round(Integer.parseInt(msg.data[1..2].reverse().join(), 16) / 10)
    runIn onTime, "motionInactive"
    return Utils.sendEvent(name:"motion", value:"active", type:"physical", isStateChange:true, descriptionText:"Is active")

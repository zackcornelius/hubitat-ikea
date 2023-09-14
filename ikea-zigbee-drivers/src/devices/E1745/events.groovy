// Motion detected
// data = [03, FD, FF, 04, 01, 01, 19, 00, 00]
case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
    return Utils.sendPhysicalEvent(name:"motion", value:"active", descriptionText:"Motion was detected")

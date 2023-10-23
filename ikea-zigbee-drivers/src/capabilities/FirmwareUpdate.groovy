{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.FirmwareUpdate
command "updateFirmware"
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.FirmwareUpdate
List updateFirmware() {
    def cmds = []
    cmds += zigbee.updateFirmware()
    if (debugEnable) log.debug "${device.displayName} updateFirmware $cmds"
    return cmds
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

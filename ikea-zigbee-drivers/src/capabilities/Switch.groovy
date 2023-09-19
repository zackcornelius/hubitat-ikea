{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "Switch"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# inputs }}
{{# startupBehavior}}

// Inputs for capability.Switch
input(
    name: "startupBehavior",
    type: "enum",
    title: "Behavior after a power outage",
    options: ["TURN_POWER_ON":"Turn power On", "TURN_POWER_OFF":"Turn power Off", "RESTORE_PREVIOUS_STATE":"Restore previous state"],
    defaultValue: "PREV",
    required: true
)
{{/ startupBehavior}}
{{/ inputs }}
{{!--------------------------------------------------------------------------}}
{{# commands }}

// Commands for capability.Switch
command "toggle"
{{# onWithTimedOff }}
command "onWithTimedOff", [[name:"On time*", type:"NUMBER", description:"After how many seconds power will be turned Off [1..6500]"]]
{{/ onWithTimedOff }}
{{/ commands }}
{{!--------------------------------------------------------------------------}}
{{# implementation }}

// Implementation for capability.Switch
def on() {
    Log.debug "Sending On command"
    Utils.sendZigbeeCommands(zigbee.on())
    {{# onWithTimedOff }}
    unschedule "onWithTimedOff_Completed"
    {{/ onWithTimedOff }}
}
def off() {
    Log.debug "Sending Off command"
    Utils.sendZigbeeCommands(zigbee.off())
    {{# onWithTimedOff }}
    unschedule "onWithTimedOff_Completed"
    {{/ onWithTimedOff }}
}

def toggle() {
    Log.debug "Sending Toggle command"
    Utils.sendZigbeeCommands(zigbee.command(0x0006, 0x02))
    {{# onWithTimedOff }}
    unschedule "onWithTimedOff_Completed"
    {{/ onWithTimedOff }}
}
{{# onWithTimedOff }}

def onWithTimedOff(onTime = 0) {
    if (onTime <= 0 || onTime > 6500) return
    Log.debug "Sending OnWithTimedOff command"
    def onTimeHex = zigbee.swapOctets(zigbee.convertToHexString(onTime.intValue() * 10, 4))
    Utils.sendZigbeeCommands(zigbee.command(0x0006, 0x42, "00${onTimeHex}0000"))
    runIn onTime.intValue() + 2, "onWithTimedOff_Completed"
}

def onWithTimedOff_Completed() {
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
}
{{/ onWithTimedOff }}
{{/ implementation }}
{{!--------------------------------------------------------------------------}}
{{# updated }}
{{# startupBehavior}}

// Preferences for capability.Switch
Log.info "🛠️ startupBehavior = ${startupBehavior}"
Utils.sendZigbeeCommands zigbee.writeAttribute(0x0006, 0x4003, 0x30, startupBehavior == "TURN_POWER_OFF" ? 0x00 : (startupBehavior == "TURN_POWER_ON" ? 0x01 : 0xFF))
{{/ startupBehavior}}
{{/ updated }}
{{!--------------------------------------------------------------------------}}
{{# events }}
{{# startupBehavior}}

// Events for capability.Switch
case { contains it, [clusterInt:0x0006, attrInt: 0x4003] }:
    def newValue = ""
    switch (Integer.parseInt(msg.value, 16)) {
        case 0x00: newValue = "TURN_POWER_OFF"; break
        case 0x01: newValue = "TURN_POWER_ON"; break
        case 0xFF: newValue = "RESTORE_PREVIOUS_STATE"; break
        default: return Log.warn("Received attribute value: StartupBehavior=${msg.value}")
    }
    startupBehavior = newValue
    device.clearSetting "startupBehavior"
    device.removeSetting "startupBehavior"
    device.updateSetting "startupBehavior", newValue
    return Log.debug("Reported StartupBehavior as ${newValue}")
{{/ startupBehavior}}
{{/ events }}
{{!--------------------------------------------------------------------------}}

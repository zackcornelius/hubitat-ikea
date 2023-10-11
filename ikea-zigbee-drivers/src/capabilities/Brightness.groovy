{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "ChangeLevel"
capability "SwitchLevel"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.ChangeLevel
input(
    name: "levelStep",
    type: "enum",
    title: "Brightness up/down step",
    description: "<small>Level adjust when using the levelUp/levelDown commands</small>",
    options: [
        "1" : "1%",
        "2" : "2%",
        "5" : "5%",
        "10": "10%",
        "20": "20%",
        "25": "25%",
        "33": "33%"],
    defaultValue: "20",
    required: true
)
input(
    name: "startLevelChangeRate",
    type: "enum",
    title: "Brightness change rate",
    description: "<small>The rate of brightness change when using the startLevelChange() command</small>",
    options: [
        "10" : "10% / second : from 0% to 100% in 10 seconds",
        "20" : "20% / second : from 0% to 100% in 5 seconds",
        "33" : "33% / second : from 0% to 100% in 3 seconds",
        "50" : "50% / seconds : from 0% to 100% in 2 seconds",
        "100": "100% / second : from 0% to 100% in 1 seconds",
    ],
    defaultValue: "20",
    required: true
)

// Inputs for capability.SwitchLevel
input(
    name: "turnOnBehavior",
    type: "enum",
    title: "Turn On behavior",
    description: "<small>Select what happens when the device is turned On</small>",
    options: [
        "RESTORE_PREVIOUS_LEVEL": "Restore previous brightness",
        "FIXED_VALUE": "Always start with the same fixed brightness"
    ],
    defaultValue: "RESTORE_PREVIOUS_LEVEL",
    required: true
)
if (turnOnBehavior == "FIXED_VALUE") {
    input(
        name: "onLevelValue",
        type: "number",
        title: "Fixed brightness value",
        description: "<small>Range 0~100</small>",
        defaultValue: 50,
        range: "0..100",
        required: true
    )
}
input(
    name: "transitionTime",
    type: "enum",
    title: "On/Off transition time",
    description: "<small>Time taken to move to/from the target brightness when device is turned On/Off</small>",
    options: [
        "0" : "Instant",
        "5" : "0.5 seconds",
        "10": "1 second",
        "15": "1.5 seconds",
        "20": "2 seconds",
        "50": "5 seconds"
    ],
    defaultValue: "5",
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @commands }}

// Commands for capability.ChangeLevel
command "levelUp"
command "levelDown"
{{/ @commands }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ChangeLevel
def startLevelChange(direction) {
    Log.debug "Starting brightness change ${direction}wards with a rate of ${startLevelChangeRate}% / second"

    Integer mode = direction == "up" ? 0x00 : 0x01
    Integer rate = Integer.parseInt(startLevelChangeRate) * 2.54
    Utils.sendZigbeeCommands(zigbee.command(0x0008, 0x01,
        zigbee.convertToHexString(mode, 2),          // MoveMode (enum8)
        zigbee.convertToHexString(rate, 2)           // Rate (uint8)
    ))
}
def stopLevelChange() {
    Log.debug "Stopping brightness change"
    Utils.sendZigbeeCommands(zigbee.command(0x0008, 0x03))
}
def levelUp() {
    Log.debug "Moving brightness up by ${levelStep}%"

    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    Integer dur = 0
    Utils.sendZigbeeCommands(zigbee.command(0x0008, 0x02,
        zigbee.convertToHexString(0x00, 2),                       // StepMode (enum8)
        zigbee.convertToHexString(stepSize, 2),                   // StepSize (uint8)
        zigbee.swapOctets(zigbee.convertToHexString(dur, 4))      // TransitionTime (uint16)
    ))
}
def levelDown() {
    Log.debug "Moving brightness down by ${levelStep}%"

    Integer stepSize = Integer.parseInt(levelStep) * 2.54
    Integer dur = 0
    Utils.sendZigbeeCommands(zigbee.command(0x0008, 0x02,
        zigbee.convertToHexString(0x01, 2),                       // StepMode (enum8)
        zigbee.convertToHexString(stepSize, 2),                   // StepSize (uint8)
        zigbee.swapOctets(zigbee.convertToHexString(dur, 4))      // TransitionTime (uint16)
    ))
}

// Implementation for capability.SwitchLevel
def setLevel(level, duration = 0) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Log.debug "Setting brightness to ${newLevel}% during ${duration} seconds"

    // Device is On: use the Move To Level command
    if (device.currentValue("switch", true) == "on") {
        Integer lvl = newLevel * 2.54
        Integer dur = (duration > 1800 ? 1800 : (duration < 0 ? 0 : duration)) * 10         // Max transition time = 30 min
        return Utils.sendZigbeeCommands(zigbee.command(0x0008, 0x00,
            zigbee.convertToHexString(lvl, 2),                        // Level (uint8)
            zigbee.swapOctets(zigbee.convertToHexString(dur, 4))      // TransitionTime (uint16)
        ))
    }

    // Device is Off and onLevel is set to a fixed value: ignore command
    if (turnOnBehavior == "FIXED_VALUE") {
        return Log.info("Ignoring Set Level command because the device is turned Off and \"Turn On behavior\" preference is set to \"Always start with the same fixed brightness\"")
    }

    // Device is Off: keep the device turned Off, use the OnLevel attribute
    Log.debug("Device is turned Off so we prepare brightness for when the device will be turned On")
    setOnLevel(newLevel)
    Utils.sendEvent(name:"level", value:newLevel, descriptionText:"Brightness is ${newLevel}%", type:"digital", isStateChange:true)
}
def setOnLevel(level) {
    Integer newLevel = level > 100 ? 100 : (level < 0 ? 0 : level)
    Log.debug "Setting Turn On brightness to ${newLevel}%"
    Integer lvl = newLevel * 2.54
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0008, 0x0011, 0x20, lvl)
}
def turnOnCallback(switchState) {
    // Device was just turned on: Read the value of the OnLevel attribute to sync/update its value
    if (switchState == "on") {
        Utils.sendZigbeeCommands zigbee.readAttribute(0x0008, 0x0011)
    }
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.ChangeLevel
Log.info "🛠️ levelStep = ${levelStep}%"
Log.info "🛠️ startLevelChangeRate = ${startLevelChangeRate}% / second"

// Preferences for capability.SwitchLevel
Log.info "🛠️ turnOnBehavior = ${turnOnBehavior}"
if (turnOnBehavior == "FIXED_VALUE") {
    Integer lvl = onLevelValue == null ? 50 : onLevelValue.intValue()
    device.clearSetting "onLevelValue"
    device.removeSetting "onLevelValue"
    device.updateSetting "onLevelValue", lvl
    Log.info "🛠️ onLevelValue = ${lvl}%"
    setOnLevel(lvl)
} else {
    Log.debug "Disabling OnLevel (0xFF)"
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF)
}
Log.info "🛠️ transitionTime = ${Integer.parseInt(transitionTime) / 10} second(s)"
Utils.sendZigbeeCommands(zigbee.writeAttribute(0x0008, 0x0010, 0x21, Integer.parseInt(transitionTime)))
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.SwitchLevel
sendEvent name:"level", value:"100", type:"digital", descriptionText:"Brightness initialized to 100%"
cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0008 0x0000 0x20 0x0000 0x0258 {01} {}" // Report CurrentLevel at least every 10 minutes
cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Level Control cluster
//cmds += zigbee.readAttribute(0x0008, 0x0000) // CurrentLevel
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.SwitchLevel

// Report Attributes: CurrentLevel
// Read Attributes Reponse: CurrentLevel
case { contains it, [clusterInt:0x0008, commandInt:0x0A, attrInt:0x0000] }:
case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0000] }:
    Utils.processedZclMessage("Report/Read Attributes Response", "CurrentLevel=${msg.value}")

    Integer newLevel = msg.value == "00" ? 0 : Integer.parseInt(msg.value, 16) * 100 / 254
    if (device.currentValue("level", true) != newLevel) {
        Utils.sendEvent name:"level", value:newLevel, descriptionText:"Brightness is ${newLevel}%", type:"digital"
    }
    return

// Read Attributes Reponse: OnLevel
// This value is read immediately after the device is turned On
// @see turnOnCallback()
case { contains it, [clusterInt:0x0008, commandInt:0x01, attrInt:0x0011] }:
    Utils.processedZclMessage("Read Attributes Response", "OnLevel=${msg.value}")
    Integer onLevel = msg.value == "00" ? 0 : Integer.parseInt(msg.value, 16) * 100 / 254

    // Clear OnLevel attribute value (if previously set)
    if (turnOnBehavior != "FIXED_VALUE" && msg.value != "FF") {
        setLevel(device.currentValue("level", true))
        Log.debug "Disabling OnLevel (0xFF)"
        return Utils.sendZigbeeCommands(zigbee.writeAttribute(0x0008, 0x0011, 0x20, 0xFF))
    }

    // Set current level to OnLevel
    if (turnOnBehavior == "FIXED_VALUE") {
        setLevel(onLevel)
    }
    return

// Write Attribute Response (0x04)
case { contains it, [clusterInt:0x0008, commandInt:0x04] }:
    if (msg.data[0] != "00") {
        return Utils.failedZclMessage("Write Attribute Response", msg.data[0], msg)
    }
    return Utils.processedZclMessage("Write Attribute Response", "cluster=0x${msg.clusterId}")

// DefaultResponse (0x0B) := { 08:CommandIdentifier, 08:Status }
// Example: [00, 80] -> command = 0x00, status = MALFORMED_COMMAND (0x80)
case { contains it, [clusterInt:0x0008, commandInt:0x0B] }:
    if (msg.data[1] != "00") {
        return Utils.failedZclMessage("Default Response", msg.data[1], msg)
    }
    return Utils.processedZclMessage("Default Response", "cluster=0x${msg.clusterId}, command=0x${msg.data[0]}")

// Other events that we expect but are not usefull for capability.SwitchLevel behavior

// ConfigureReportingResponse := { 08:Status, 08:Direction, 16:AttributeIdentifier }
// Success example: [00] -> status = SUCCESS
case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
    if (msg.data[0] != "00") return Utils.failedZclMessage("Configure Reporting Response", msg.data[0], msg)
    return Utils.processedZclMessage("Configure Reporting Response", "cluster=0x${msg.clusterId}, data=${msg.data}")
{{/ @events }}
{{!--------------------------------------------------------------------------}}

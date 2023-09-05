/**
 * IKEA Tradfri Remote Control (E1810) Driver
 *
 * @see https://zigbee.blakadder.com/Ikea_E1810.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */

import groovy.transform.Field

@Field def DRIVER_NAME = "IKEA Tradfri Remote Control (E1810)"
@Field def DRIVER_VERSION = "1.2.0"
@Field def BUTTONS = [
    "POWER" : ["1", "Power"],
    "PLUS"  : ["2", "Plus"],
    "MINUS" : ["3", "Minus"],
    "NEXT"  : ["4", "Next"],
    "PREV"  : ["5", "Prev"]
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1810.groovy") {
        capability "Configuration"
        capability "Battery"
        capability "PushableButton"
        capability "HoldableButton"
        capability "ReleasableButton"
        capability "Switch"
        capability "SwitchLevel"

        // For firmware 24.4.5 (24040005)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57,FC7C", outClusters:"0003,0004,0005,0006,0008,0019,1000", model:"TRADFRI remote control", manufacturer:"IKEA of Sweden" 
    }

    preferences {
        input(
            name: "levelChange",
            type: "enum",
            title: "Plus/Minus buttons level adjust (+/- %)*",
            options: ["1":"1%", "2":"2%", "5":"5%", "10":"10%", "20":"20%", "25":"25%"],
            defaultValue: "5",
            required: true
        )
        input(
            name: "logLevel",
            type: "enum",
            title: "Select log verbosity*",
            options: [
                "1":"Debug - log everything",
                "2":"Info - log only important events",
                "3":"Warning - log only events that require attention",
                "4":"Error - log only errors"
            ],
            defaultValue: "2",
            required: true
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    info "Installing Zigbee device...."
    debug "IMPORTANT: Make sure that you keep your IKEA remote as close as you can to your Hubitat hub for at least 1 minute. Otherwise it will successfully pair but the buttons won't work!" 
}

// Called when the "save Preferences" button is clicked
def updated() {
    info "Applying preferences..."
    info "🛠️ logLevel: ${logLevel}"
    info "🛠️ levelChange: ${levelChange}%"

    if (logLevel == "1") runIn 1800, "logsOff"
    else unschedule()
}

// Handler method for scheduled job to disable debug logging
def logsOff() {
   info '⏲️ Automatically reverting log level to "Info"'
   device.clearSetting "logLevel"
   device.removeSetting "logLevel"
   device.updateSetting "logLevel", "2"
}

// ===================================================================================================================
// Implement Hubitat Capabilities
// ===================================================================================================================

// capability.configuration
// Note: This method is also called when the device is initially installed
def configure() {
    info "Configuring device..."
    info 'IMPORTANT: Click the "Configure" button immediately after pushing any button on the remote so that the Zigbee messages we send during configuration will reach the device before it goes to sleep!'

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION
    
    // Apply preferences first
    updated()

    // Clear state
    state.clear()

    // Set initial values for all attributes
    push 0
    hold 0
    release 0
    on()
    setLevel 25
    def numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons set to: ${numberOfButtons}"

    List<String> cmds = []

    // Report battery percentage every 24-48 hours
    cmds.addAll zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 86400, 172800, 0x00)

    // Add binds
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0005 {${device.zigbeeId}} {}" // Generic - Scenes cluster
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // Generic - On/Off cluster
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Generic - Level Control cluster

    // Query device attributes
    cmds.addAll zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds.addAll zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds.addAll zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    cmds.addAll zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage

    // Query simple descriptor data
    cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} 01} {0x0000}"
    sendZigbeeCommands cmds
}

// capability.pushableButton
def push(buttonNumber) {
    triggerUserEvent name:"pushed", value:buttonNumber, descriptionText:"Button ${buttonNumber} was pressed"
}

// capability.holdableButton
def hold(buttonNumber) {
    triggerUserEvent name:"held", value:buttonNumber, descriptionText:"Button ${buttonNumber} was held"
}

// capability.releasableButton
def release(buttonNumber) {
    triggerUserEvent name:"released", value:buttonNumber, descriptionText:"Button ${buttonNumber} was released"
}

// capability.switch
def on() {
    triggerUserEvent name:"switch", value:"on", descriptionText:"Was turned on"
}

// capability.switch
def off() {
    triggerUserEvent name:"switch", value:"off", descriptionText:"Was turned off"
}

// capability.switchLevel
def setLevel(level, duration = 0) {
    def newLevel = level < 0 ? 0 : (level > 100 ? 100 : level)
    triggerUserEvent name:"level", value:newLevel, unit:"%", descriptionText:"Was set to ${newLevel}%"
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap description

    // Extract cluster and command from message
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)
  
    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // General::Basic cluster (0x0000) - Read Attribute Response
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            switch (msg.attrInt) {
                case 0x0001: return zigbeeDataValue("application", msg.value)
                case 0x0003: return zigbeeDataValue("hwVersion", msg.value)
                case 0x0004: return zigbeeDataValue("manufacturer", msg.value)
                case 0x0005:
                    if (msg.value == "TRADFRI remote control") updateDataValue "type", "E1810"
                    return zigbeeDataValue("model", msg.value)
                case 0x4000: return zigbeeDataValue("softwareBuild", msg.value)
            }
            return warn("Unknown Zigbee attribute: attribute=${msg.attrInt}, msg=${msg}")

        // ---------------------------------------------------------------------------------------------------------------
        // General::Power cluster (0x0001)
        // ---------------------------------------------------------------------------------------------------------------
        
        // Battery report
        case { contains it, [clusterInt:0x0001, attrInt:0x0021] }:
            def percentage = Math.round(Integer.parseInt(msg.value, 16) / 2)
            return triggerZigbeeEvent(name:"battery", value:percentage, unit: "%", descriptionText: "Battery is ${percentage}% full")

        // Configure Reporting response
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            return ignoredZigbeeResponse("Configure Reporting Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // General::Identify cluster (0x0003)
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:
            return ignoredZigbeeResponse("Identify Query", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // General::Scenes cluster (0x0005)
        // ---------------------------------------------------------------------------------------------------------------

        // Button Prev/Next was pressed
        case { contains it, [clusterInt:0x0005, commandInt:0x07] }:
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return triggerZigbeeEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

        // Button Prev/Next was help
        case { contains it, [clusterInt:0x0005, commandInt:0x08] }:
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return triggerZigbeeEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

        // Button Prev/Next was released
        case { contains it, [clusterInt:0x0005, commandInt:0x09] }:
            //def button = I'm not smart enough to figure it out how to determine button number from msg.data!
            //return triggerZigbeeEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")
            return

        // ---------------------------------------------------------------------------------------------------------------v
        // General::On/Off cluster (0x0006)
        // ---------------------------------------------------------------------------------------------------------------
        
        // Set switch state
        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
            def newState = msg.commandInt == 0x00 ? "off" : "on"
            return triggerZigbeeEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")

        // Power button was pushed
        case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
            def button = BUTTONS.POWER
            triggerZigbeeEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

            // Also act as a switch
            def newState = device.currentState("switch") != null && device.currentState("switch").value != "on" ? "on" : "off"
            return triggerZigbeeEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")

        // ---------------------------------------------------------------------------------------------------------------
        // General::Level Control cluster (0x0008)
        // ---------------------------------------------------------------------------------------------------------------

        // Plus/Minus button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x06] }:
            def button = msg.commandInt == 0x02 ? BUTTONS.MINUS : BUTTONS.PLUS
            triggerZigbeeEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

            def curLevel = device.currentState("level") != null ? Integer.parseInt(device.currentState("level").value) : 25
            def newLevel = curLevel + (button == BUTTONS.PLUS ? Integer.parseInt(levelChange) : 0 - Integer.parseInt(levelChange))
            newLevel = newLevel < 0 ? 0 : (newLevel > 100 ? 100 : newLevel)
            if (curLevel != newLevel) triggerZigbeeEvent(name:"level", value:newLevel, descriptionText:"Was set to ${newLevel}%")
            return

        // Plus/Minus button was held
        case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
            def button = msg.commandInt == 0x01 ? BUTTONS.MINUS : BUTTONS.PLUS
            return triggerZigbeeEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

        // Plus/Minus button was released
        case { contains it, [clusterInt:0x0008, commandInt:0x03] }:
        case { contains it, [clusterInt:0x0008, commandInt:0x07] }:
            def button = msg.commandInt == 0x03 ? BUTTONS.MINUS : BUTTONS.PLUS
            return triggerZigbeeEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")

        // ---------------------------------------------------------------------------------------------------------------
        // Device_annce
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x0013] }:
            return ignoredZigbeeResponse("Device Announce Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Simple_Desc_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8004] }:

            // Copy-paste from https://github.com/birdslikewires/hubitat
            if (msg.data[1] == "00") {
                updateDataValue("profileId", msg.data[6..7].reverse().join())

                Integer inClusterNum = Integer.parseInt(msg.data[11], 16)
                Integer position = 12
                Integer positionCounter = null
                String inClusters = ""
                if (inClusterNum > 0) {
                    (1..inClusterNum).each() { b->
                        positionCounter = position+((b-1)*2)
                        inClusters += msg.data[positionCounter..positionCounter+1].reverse().join()
                        if (b < inClusterNum) {
                            inClusters += ","
                        }
                    }
                }
                position += inClusterNum * 2
                Integer outClusterNum = Integer.parseInt(msg.data[position], 16)
                position += 1
                String outClusters = ""
                if (outClusterNum > 0) {
                    (1..outClusterNum).each() { b->
                        positionCounter = position+((b-1)*2)
                        outClusters += msg.data[positionCounter..positionCounter+1].reverse().join()
                        if (b < outClusterNum) {
                            outClusters += ","
                        }
                    }
                }

                updateDataValue "inClusters", inClusters
                updateDataValue "outClusters", outClusters

                return zigbeeDebug("Simple Descriptor Response: inClusters=$inClusters, outClusters=$outClusters")
            }

        // ---------------------------------------------------------------------------------------------------------------
        // Active_EP_rsp 
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8005] }:
            return ignoredZigbeeResponse("Active Endpoints Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Bind_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8021] }:
            return ignoredZigbeeResponse("Bind Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Mgmt_Leave_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8034] }:
            return ignoredZigbeeResponse("Leave Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Unhandled Zigbee message
        // ---------------------------------------------------------------------------------------------------------------
        default:
            warn "Received unknown Zigbee message: description=${description}, msg=${msg}"
    }
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

void zigbeeDebug(message) {
    debug "Received Zigbee message: ${message}"
}

void debug(message) {
    if (logLevel == "1") log.debug message
}

void info(message) {
    if (logLevel <= "2") log.info message
}

void warn(message) {
    if (logLevel <= "3") log.warn message
}

void error(message) {
    log.error message
}

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

void sendZigbeeCommands(List<String> cmds) {
    debug "📶 Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
}

void triggerZigbeeEvent(event) {
    info "${device.displayName} ${event.descriptionText.uncapitalize()} [physical]"
    sendEvent event + [isStateChange:true, type:"physical"]
}

void triggerUserEvent(event) {
    info "${device.displayName} ${event.descriptionText.uncapitalize()} [digital]"
    sendEvent event + [isStateChange:true, type:"digital"]
}

void zigbeeDataValue(key, value) {
    zigbeeDebug "${key}: ${value}"
    updateDataValue key, value
}

void ignoredZigbeeResponse(rspName, msg) {
    zigbeeDebug "${rspName}: data=${msg.data}"
}

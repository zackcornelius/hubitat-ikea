/**
 * IKEA Symfonisk Sound Remote Gen2 (E2123) Driver
 *
 * @see https://zigbee.blakadder.com/Ikea_E2123.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */

import groovy.time.TimeCategory
import groovy.transform.Field

@Field def DRIVER_NAME = "IKEA Symfonisk Sound Remote Gen2 (E2123)"
@Field def DRIVER_VERSION = "1.3.0"
@Field def BUTTONS = [
    "PLAY"  : ["1", "Play"],
    "PLUS"  : ["2", "Plus"],
    "MINUS" : ["3", "Minus"],
    "NEXT"  : ["4", "Next"],
    "PREV"  : ["5", "Prev"],
    "DOT_1" : ["6", "One Dot"],
    "DOT_2" : ["7", "Two Dots"]
]

// Health Check config:
@Field def HEALTH_CHECK = [
    "schedule"   : "0 0 0/12 ? * * *",  // Check health status every 12 hours
    "thereshold" : 43200                // When checking, mark the device as offline if no message was received in the last 12 hours (43200 seconds)
]                                       // Device should report battery percent every 6 to 12 hours; offline status is cancelled also when any button is pushed

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2123.groovy") {
        capability "Battery"
        capability "Configuration"
        capability "DoubleTapableButton"
        capability "HealthCheck"
        capability "HoldableButton"
        capability "PushableButton"
        capability "ReleasableButton"
        capability "Switch"
        capability "SwitchLevel"

        // For firmware 1.0.012 (20211214)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57", outClusters:"0003,0004,0006,0008,0019,1000,FC7F", model:"SYMFONISK sound remote gen2", manufacturer:"IKEA of Sweden"
        
        // For firmware 1.0.35 (20230411)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC7C", outClusters:"0003,0004,0006,0008,0019,1000", model:"SYMFONISK sound remote gen2", manufacturer:"IKEA of Sweden"

        // Should be part of capability.healthCheck
        attribute "healthStatus", "enum", ["offline", "online", "unknown"]
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
            title: "Select log verbosity",
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
    warn "IMPORTANT: Make sure that you keep your IKEA remote as close as you can to your Hubitat hub until the LED stops blinking. Otherwise it will successfully pair but the buttons won't work!"
}

// Called when the "save Preferences" button is clicked
def updated() {
    info "Saving preferences..."
    info "🛠️ logLevel = ${logLevel}"
    info "🛠️ levelChange = ${levelChange}%"

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    schedule HEALTH_CHECK.schedule, "healthCheck"
}

// Handler method for scheduled job to disable debug logging
def logsOff() {
   info '⏲️ Automatically reverting log level to "Info"'
   device.clearSetting "logLevel"
   device.removeSetting "logLevel"
   device.updateSetting "logLevel", "2"
}

def healthCheck() {
    debug '⏲️ Automatically running health check'
    def healthStatus = state?.lastRx == 0 ? "unknown" : (now() - state.lastRx < HEALTH_CHECK.thereshold * 1000 ? "online" : "offline")
    if (device.currentState("healthStatus")?.value != healthStatus) {
        sendDigitalEvent name:"healthStatus", value:healthStatus, descriptionText:"Health status changed to ${healthStatus}"
    }
}

// ===================================================================================================================
// Implement Hubitat Capabilities
// ===================================================================================================================

// capability.configuration
// Note: This method is also called when the device is initially installed
def configure() {
    info "Configuring device..."
    debug 'IMPORTANT: Click the "Configure" button immediately after pushing any button on the remote so that the Zigbee messages we send during configuration will reach the device before it goes to sleep!'

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION

    // Apply preferences first
    updated()

    // init state
    state.clear()
    state.lastRx = 0
    state.lastTx = 0

    // Set initial values for all attributes
    push 0
    doubleTap 0
    hold 0
    release 0
    on()
    setLevel 5
    def numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons set to ${numberOfButtons}"
    sendEvent name:"healthStatus", value:"unknown", descriptionText:"Health status set to unknown"

    List<String> cmds = []

    // Report battery percentage every 6-12 hours
    cmds.addAll zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 21600, 43200, 0x00)

    // Add binds
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // Generic - On/Off
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // Generic - Level Control
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7F {${device.zigbeeId}} {}" // 64639 --> For firmware 1.0.012 (20211214)
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes --> For firmware 1.0.35 (20230411)
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes --> For firmware 1.0.35 (20230411)

    // Query device attributes
    cmds.addAll zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds.addAll zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds.addAll zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    cmds.addAll zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage

    // Query all active endpoints
    cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"

    sendZigbeeCommands cmds
}

// capability.doubleTapableButton
def doubleTap(buttonNumber) {
    sendDigitalEvent name:"doubleTapped", value:buttonNumber, descriptionText:"Button ${buttonNumber} was double tapped"
}

// capability.healthCheck
def ping() {
    if (state.lastRx == null || state.lastRx == 0) {
        return info("Did not sent any messages since it was last configured")
    }

    def now = new Date(Math.round(now() / 1000) * 1000)
    def lastRx = new Date(Math.round(state.lastRx / 1000) * 1000)
    def lastRxAgo = TimeCategory.minus(now, lastRx).toString().replace(".000 seconds", " seconds")    
    info "Sent last message at ${lastRx.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${lastRxAgo} ago)"

    def thereshold = new Date(Math.round(state.lastRx / 1000 + HEALTH_CHECK.thereshold) * 1000)
    def theresholdAgo = TimeCategory.minus(thereshold, lastRx).toString().replace(".000 seconds", " seconds")
    info "Has offline thereshold configured to ${theresholdAgo} (hardcoded)"
    
    def offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace(".000 seconds", " seconds")
    info "Will me marked as offline at ${thereshold.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${offlineMarkAgo} from now)"
}

// capability.holdableButton
def hold(buttonNumber) {
    sendDigitalEvent name:"held", value:buttonNumber, descriptionText:"Button ${buttonNumber} was held"
}

// capability.pushableButton
def push(buttonNumber) {
    sendDigitalEvent name:"pushed", value:buttonNumber, descriptionText:"Button ${buttonNumber} was pressed"
}

// capability.releasableButton
def release(buttonNumber) {
    sendDigitalEvent name:"released", value:buttonNumber, descriptionText:"Button ${buttonNumber} was released"
}

// capability.switch
def on() {
    sendDigitalEvent name:"switch", value:"on", descriptionText:"Was turned on"
}
def off() {
    sendDigitalEvent name:"switch", value:"off", descriptionText:"Was turned off"
}

// capability.switchLevel
def setLevel(level, duration = 0) {
    def newLevel = level < 0 ? 0 : (level > 100 ? 100 : level)
    sendDigitalEvent name:"level", value:newLevel, unit:"%", descriptionText:"Level was set to ${newLevel}%"
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap description
    
    // Update health status
    state.lastRx = now()
    if (device.currentState("healthStatus")?.value != "online") {
        sendDigitalEvent name:"healthStatus", value:"online", descriptionText:"Health status changed to online"
    }

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
                    if (msg.value == "SYMFONISK sound remote gen2") updateDataValue "type", "E2123"
                    return zigbeeDataValue("model", msg.value)
                case 0x4000: return zigbeeDataValue("softwareBuild", msg.value)
            }
            return warn("Unknown Zigbee attribute: attribute=${msg.attrInt}, msg=${msg}")

        // ---------------------------------------------------------------------------------------------------------------
        // General::Power cluster (0x0001)
        // ---------------------------------------------------------------------------------------------------------------

        // Battery report
        case { contains it, [clusterInt:0x0001, attrInt:0x0021] }:
            def percentage = Integer.parseInt(msg.value, 16)

            // On later firmware versions; battery is reported with a double value, upto "200" (but why?)
            if (getDataValue("softwareBuild")?.value != "1.0.012") {
                percentage = Math.round(percentage / 2);
            }

            return sendPhysicalEvent(name:"battery", value:percentage, unit:"%", descriptionText:"Battery is ${percentage}% full")

        // Configure Reporting response
        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            return ignoredZigbeeResponse("Configure Reporting Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // General::Identify cluster (0x0003)
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:
            return ignoredZigbeeResponse("Identify Query", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // General::On/Off cluster (0x0006)
        // ---------------------------------------------------------------------------------------------------------------

        // Set switch state
        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
            def newState = msg.commandInt == 0x00 ? "off" : "on"
            return sendPhysicalEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")

        // Play button was pushed
        case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
            def button = BUTTONS.PLAY
            sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

            // Also act as a switch
            def newState = device.currentState("switch")?.value != "on" ? "on" : "off"
            return sendPhysicalEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")

        // ---------------------------------------------------------------------------------------------------------------
        // General::Level Control cluster (0x0008)
        // ---------------------------------------------------------------------------------------------------------------

        // Plus/Minus button was held
        case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
            def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
            return sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")

        // Next/Prev button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

        // Plus/Minus button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
            def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
            sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")

            // Also act as a dimmer
            def curLevel = device.currentState("level") != null ? Integer.parseInt(device.currentState("level").value) : 25
            def newLevel = curLevel + (button == BUTTONS.PLUS ? Integer.parseInt(levelChange) : 0 - Integer.parseInt(levelChange))
            newLevel = newLevel < 0 ? 0 : (newLevel > 100 ? 100 : newLevel)
            if (curLevel != newLevel) sendPhysicalEvent(name:"level", value:newLevel, descriptionText:"Level was set to ${newLevel}%")
            return

        // ---------------------------------------------------------------------------------------------------------------
        // Device_annce 
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x0013] }:
            return ignoredZigbeeResponse("Device Announce Response: status=ERROR", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Simple_Desc_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8004] }:
            if (msg.data[1] != "00") {
                return ignoredZigbeeResponse("Simple Descriptor Response: Error", msg)
            }

            def endpointId = msg.data[5]
            updateDataValue("profileId", msg.data[6..7].reverse().join())

            Integer inClusterNum = Integer.parseInt(msg.data[11], 16)
            Integer position = 12
            Integer positionCounter = null
            String inClusters = ""
            if (inClusterNum > 0) {
                (1..inClusterNum).each() { b->
                    positionCounter = position+((b-1)*2)
                    inClusters += msg.data[positionCounter..positionCounter+1].reverse().join()
                    if (b < inClusterNum) inClusters += ","
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
                    if (b < outClusterNum) outClusters += ","
                }
            }

            zigbeeDataValue "inClusters (${endpointId})", inClusters
            zigbeeDataValue "outClusters (${endpointId})", outClusters
            return zigbeeDebug("Simple Descriptor Response: endpointId=${endpointId}, inClusters=${inClusters}, outClusters=${outClusters}")

        // ---------------------------------------------------------------------------------------------------------------
        // Active_EP_rsp 
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8005] }:
            if (msg.data[1] != "00") {
                return ignoredZigbeeResponse("Active Endpoints Response:: status=ERROR", msg)
            }

            List<String> cmds = []
            def endpointIds = []

            def count = Integer.parseInt(msg.data[4], 16)
            if (count > 0) {
                (1..count).each() { i ->
                    def endpointId = msg.data[4 + i]
                    endpointIds.add endpointId
                    
                    // Query simple descriptor data
                    cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} ${endpointId}} {0x0000}"
                }
            }

            zigbeeDataValue "endpointIds", endpointIds.join(", ")
            zigbeeDebug("Active Endpoints Response: endpointIds=${endpointIds}")
            return sendZigbeeCommands(cmds)

        // ---------------------------------------------------------------------------------------------------------------
        // Bind_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8021] }:
            return ignoredZigbeeResponse("Bind Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Mgmt_Rtg_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8032] }:
            return ignoredZigbeeResponse("Routing Table Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Mgmt_Leave_rsp
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0x8034] }:
            return ignoredZigbeeResponse("Leave Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Undocumented cluster (0xFC7F) - Used by firmware 1.0.012 (20211214)
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0xFC7F] }:
            def button = msg.data[0] == "01" ? BUTTONS.DOT_1 : BUTTONS.DOT_2

            // 1 Dot / 2 Dots button was pushed
            if (msg.data[1] == "01") {
                return sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pressed")
            }

            // 1 Dot / 2 Dots button was double tapped
            if (msg.data[1] == "02") {
                return sendPhysicalEvent(name:"doubleTapped", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
            }
        
            // 1 Dot / 2 Dots button was held
            if (msg.data[1] == "03") {
                return sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")
            }

        // ---------------------------------------------------------------------------------------------------------------
        // Undocumented cluster (0xFC80) - Used by firmware 1.0.35 (20230411)
        // ---------------------------------------------------------------------------------------------------------------
        case { contains it, [clusterInt:0xFC80] }:
            def button = msg.sourceEndpoint == "02" ? BUTTONS.DOT_1 : BUTTONS.DOT_2

            // IGNORED: 1 Dot / 2 Dots button was pressed-down
            if (msg.commandInt == 0x01) {
                return zigbeeDebug("Button ${button[0]} (${button[1]}) was pressed-down (ignored as we wait for the next message to distinguish between click, double tap and hold)")
            }

            // 1 Dot / 2 Dots button was held
            // Commands are issued in this order: 01 (key-down = ignored) -> 02 (button is held = update "held" attribute) -> 04 (button released = update "released" attribute)
            if (msg.commandInt == 0x02) {
                return sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")
            }
        
            // 1 Dot / 2 Dots button was pushed
            if (msg.commandInt == 0x03) {
                return sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pressed")
            }

            // IGNORED: 1 Dot / 2 Dots button was released
            if (msg.commandInt == 0x04) {
                return sendPhysicalEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")
            }

            // 1 Dot / 2 Dots button was double tapped
            if (msg.commandInt == 0x06) {
                return sendPhysicalEvent(name:"doubleTapped", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
            }

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
    if (logLevel == "1") log.debug "${device.displayName} ${message.uncapitalize()}"
}

void info(message) {
    if (logLevel <= "2") log.info "${device.displayName} ${message.uncapitalize()}"
}

void warn(message) {
    if (logLevel <= "3") log.warn "${device.displayName} ${message.uncapitalize()}"
}

void error(message) {
    log.error "${device.displayName} ${message.uncapitalize()}"
}

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

void sendZigbeeCommands(List<String> cmds) {
    state.lastTx = now()
    debug "📶 Sending Zigbee messages: ${cmds}"
    sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
}

void sendPhysicalEvent(event) {
    info "${event.descriptionText} [physical]"
    sendEvent event + [isStateChange:true, type:"physical"]
}

void sendDigitalEvent(event) {
    info "${event.descriptionText} [digital]"
    sendEvent event + [isStateChange:true, type:"digital"]
}

void zigbeeDataValue(key, value) {
    zigbeeDebug "${key}: ${value}"
    updateDataValue key, value
}

void ignoredZigbeeResponse(rspName, msg) {
    zigbeeDebug "${rspName}: data=${msg.data}"
}

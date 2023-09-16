/**
 * IKEA Symfonisk Sound Remote Gen2 (E2123)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E2123.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 */

import groovy.time.TimeCategory
import groovy.transform.Field

@Field def DRIVER_NAME = "IKEA Symfonisk Sound Remote Gen2 (E2123)"
@Field def DRIVER_VERSION = "2.3.0"
@Field def ZDP_STATUS = ["00":"SUCCESS", "80":"INV_REQUESTTYPE", "81":"DEVICE_NOT_FOUND", "82":"INVALID_EP", "83":"NOT_ACTIVE", "84":"NOT_SUPPORTED", "85":"TIMEOUT", "86":"NO_MATCH", "88":"NO_ENTRY", "89":"NO_DESCRIPTOR", "8A":"INSUFFICIENT_SPACE", "8B":"NOT_PERMITTED", "8C":"TABLE_FULL", "8D":"NOT_AUTHORIZED", "8E":"DEVICE_BINDING_TABLE_FULL"]
@Field def BUTTONS = [
    "PLAY": ["1", "Play"],
    "PLUS": ["2", "Plus"],
    "MINUS": ["3", "Minus"],
    "NEXT": ["4", "Next"],
    "PREV": ["5", "Prev"],
    "DOT_1": ["6", "•"],
    "DOT_2": ["7", "••"],
]

// Health Check config
@Field def HEALTH_CHECK = [
    "schedule"   : "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold" : 43200              // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E2123.groovy") {
        capability "Battery"
        capability "Configuration"
        capability "DoubleTapableButton"
        capability "HealthCheck"
        capability "HoldableButton"
        capability "PowerSource"
        capability "PushableButton"
        capability "ReleasableButton"
        capability "Switch"
        capability "SwitchLevel"

        // For firmwares: 1.0.012
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57", outClusters:"0003,0004,0006,0008,0019,1000,FC7F", model:"SYMFONISK sound remote gen2", manufacturer:"IKEA of Sweden"

        // For firmwares: 1.0.35
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC7C", outClusters:"0003,0004,0006,0008,0019,1000", model:"SYMFONISK sound remote gen2", manufacturer:"IKEA of Sweden"

        // Should be part of capability.HealthCheck
        attribute "healthStatus", "ENUM", ["offline", "online", "unknown"]
    }

    preferences {
        input(
            name: "logLevel",
            type: "enum",
            title: "Select log verbosity",
            options: [
                "1":"Debug - log everything",
                "2":"Info - log important events",
                "3":"Warning - log events that require attention",
                "4":"Error - log errors"
            ],
            defaultValue: "2",
            required: true
        )
        input(
            name: "levelChange",
            type: "enum",
            title: "Level adjust on button press (+/- %)",
            options: ["1":"1%", "2":"2%", "5":"5%", "10":"10%", "20":"20%", "25":"25%", "33":"33%"],
            defaultValue: "5",
            required: true
        )
        input(
            name: "minLevel",
            type: "number",
            title: "Minimum level",
            description: "<small>Range 0~90</small>",
            defaultValue: 0,
            range: "0..90",
            required: true
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    Log.info "Installing Zigbee device...."
    Log.warn "[IMPORTANT] Make sure that you keep your IKEA device as close as you can to your Hubitat hub until the LED stops blinking. Otherwise it will successfully pair but it won't work properly!"
}

// Called when the "Save Preferences" button is clicked
def updated() {
    Log.info "Saving preferences..."
    Log.info "🛠️ logLevel = ${logLevel}"
    Log.info "🛠️ levelChange = ${levelChange}%"
    minLevel = minLevel.toDouble().trunc().toInteger()
    device.clearSetting "minLevel"
    device.removeSetting "minLevel"
    device.updateSetting "minLevel", minLevel
    Log.info "🛠️ minLevel = ${minLevel}"

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    schedule HEALTH_CHECK.schedule, "healthCheck"
}

// Handler method for scheduled job to disable debug logging
def logsOff() {
   Log.info '⏲️ Automatically reverting log level to "Info"'
   device.clearSetting "logLevel"
   device.removeSetting "logLevel"
   device.updateSetting "logLevel", "2"
}

// Handler method for scheduled job to check health status
def healthCheck() {
   Log.debug '⏲️ Automatically running health check'
    def healthStatus = state?.lastRx == 0 ? "unknown" : (now() - state.lastRx < HEALTH_CHECK.thereshold * 1000 ? "online" : "offline")
    if (device.currentValue("healthStatus") != healthStatus) {
        Utils.sendDigitalEvent name:"healthStatus", value:healthStatus, descriptionText:"Health status changed to ${healthStatus}"
    }
}

// ===================================================================================================================
// Implement Hubitat Capabilities
// ===================================================================================================================

// capability.Configuration
// Note: This method is also called when the device is initially installed
def configure() {
    Log.info "Configuring device..."
    Log.debug '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the remote so that the Zigbee messages we send during configuration will reach the device before it goes to sleep!'

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION

    // Apply preferences first
    updated()

    // Clear state
    state.clear()
    state.lastRx = 0
    state.lastTx = 0

    // capability.PushableButton
    push 0
    def numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons set to ${numberOfButtons}"

    // capability.DoubleTapableButton
    doubleTap 0

    // capability.HoldableButton
    hold 0

    // capability.ReleasableButton
    release 0

    // capability.Switch
    on()

    // capability.SwitchLevel
    setLevel 5

    // capability.PowerSource
    sendEvent name:"powerSource", value:"battery", descriptionText:"Power source set to battery"

    // capability.HealthCheck
    sendEvent name:"healthStatus", value:"unknown", descriptionText:"Health status set to unknown"
    sendEvent name:"checkInterval", value:3600, descriptionText:"Health check interval set to 3600 seconds"

    List<String> cmds = []

    // Configure Zigbee reporting
    cmds.addAll zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 21600, 43200, 0x00) // Report battery level every 6 to 12 hours

    // Add Zigbee binds
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // General - On/Off cluster
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}" // General - Level Control cluster
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7F {${device.zigbeeId}} {}" // Unknown 64639 cluster --&gt; For firmware 1.0.012
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes cluster --&gt; For firmware 1.0.35
    cmds.add "zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0xFC80 {${device.zigbeeId}} {}" // Heiman - Specific Scenes cluster --&gt; For firmware 1.0.35

    // Query Zigbee attributes
    cmds.addAll zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds.addAll zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds.addAll zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    cmds.addAll zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage

    // Query all active endpoints
    cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"

    Utils.sendZigbeeCommands cmds
}

// capability.DoubleTapableButton
def doubleTap(buttonNumber) {
    Utils.sendDigitalEvent name:"doubleTapped", value:buttonNumber, descriptionText:"Button ${buttonNumber} was double tapped"
}

// capability.HealthCheck
def ping() {
    pingExecute()
}

def pingExecute() {
    if (state.lastRx == null || state.lastRx == 0) {
        return info("Did not sent any messages since it was last configured")
    }

    def now = new Date(Math.round(now() / 1000) * 1000)
    def lastRx = new Date(Math.round(state.lastRx / 1000) * 1000)
    def lastRxAgo = TimeCategory.minus(now, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Sent last message at ${lastRx.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${lastRxAgo} ago)"

    def thereshold = new Date(Math.round(state.lastRx / 1000 + HEALTH_CHECK.thereshold) * 1000)
    def theresholdAgo = TimeCategory.minus(thereshold, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received for ${theresholdAgo} (hardcoded)"

    def offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received until ${thereshold.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${offlineMarkAgo} from now)"
}

// capability.HoldableButton
def hold(buttonNumber) {
    Utils.sendDigitalEvent name:"held", value:buttonNumber, descriptionText:"Button ${buttonNumber} was held"
}

// capability.PushableButton
def push(buttonNumber) {
    Utils.sendDigitalEvent name:"pushed", value:buttonNumber, descriptionText:"Button ${buttonNumber} was pressed"
}

// capability.ReleasableButton
def release(buttonNumber) {
    Utils.sendDigitalEvent name:"released", value:buttonNumber, descriptionText:"Button ${buttonNumber} was released"
}

// capability.Switch
def on() {
    Utils.sendDigitalEvent name:"switch", value:"on", descriptionText:"Was turned on"
}
def off() {
    Utils.sendDigitalEvent name:"switch", value:"off", descriptionText:"Was turned off"
}

// capability.SwitchLevel
def setLevel(level, duration = 0) {
    def newLevel = level < 0 ? 0 : (level > 100 ? 100 : level)
    Utils.sendDigitalEvent name:"level", value:newLevel, unit:"%", descriptionText:"Level was set to ${newLevel}%"
}


// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap description
    Log.debug "description=[${description}]"
    Log.debug "msg=[${msg}]"
    state.lastRx = now()

    // Update health status
    if (device.currentValue("healthStatus") != "online") {
        Utils.sendDigitalEvent name:"healthStatus", value:"online", descriptionText:"Health status changed to online"
    }

    // Extract cluster and command from message
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle device specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // Set switch state
        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01] }:
            return Utils.updateSwitch(msg.commandInt == 0x00 ? "off" : "on")
        
        // Play button was pushed
        case { contains it, [clusterInt:0x0006, commandInt:0x02] }:
            def button = BUTTONS.PLAY
            Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
            // Also act as a switch
            return Utils.toggleSwitch()
        
        // Plus/Minus button was held
        case { contains it, [clusterInt:0x0008, commandInt:0x01] }:
            def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
            return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")
        
        // Next/Prev button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x02] }:
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
        // Plus/Minus button was pushed
        case { contains it, [clusterInt:0x0008, commandInt:0x05] }:
            def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
            Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pushed")
        
            // Also act as a dimmer
            return button == BUTTONS.PLUS ? Utils.levelUp() : Utils.levelDown()
        
        // Undocumented cluster (0xFC7F) - Used by firmware 1.0.012 (20211214)
        case { contains it, [clusterInt:0xFC7F] }:
            def button = msg.data[0] == "01" ? BUTTONS.DOT_1 : BUTTONS.DOT_2
        
            // 1 Dot / 2 Dots button was pushed
            if (msg.data[1] == "01") {
                return Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pressed")
            }
        
            // 1 Dot / 2 Dots button was double tapped
            if (msg.data[1] == "02") {
                return Utils.sendPhysicalEvent(name:"doubleTapped", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
            }
        
            // 1 Dot / 2 Dots button was held
            if (msg.data[1] == "03") {
                return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")
            }
        
        // Undocumented cluster (0xFC80) - Used by firmware 1.0.35 (20230411)
        case { contains it, [clusterInt:0xFC80] }:
            def button = msg.sourceEndpoint == "02" ? BUTTONS.DOT_1 : BUTTONS.DOT_2
        
            // IGNORED: 1 Dot / 2 Dots button was pressed-down
            if (msg.commandInt == 0x01) {
                return Log.debug("Button ${button[0]} (${button[1]}) was pressed-down (ignored as we wait for the next message to distinguish between click, double tap and hold)")
            }
        
            // 1 Dot / 2 Dots button was held
            // Commands are issued in this order: 01 (key-down = ignored) -> 02 (button is held = update "held" attribute) -> 04 (button released = update "released" attribute)
            if (msg.commandInt == 0x02) {
                return Utils.sendPhysicalEvent(name:"held", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was held")
            }
        
            // 1 Dot / 2 Dots button was pushed
            if (msg.commandInt == 0x03) {
                return Utils.sendPhysicalEvent(name:"pushed", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was pressed")
            }
        
            // IGNORED: 1 Dot / 2 Dots button was released
            if (msg.commandInt == 0x04) {
                return Utils.sendPhysicalEvent(name:"released", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was released")
            }
        
            // 1 Dot / 2 Dots button was double tapped
            if (msg.commandInt == 0x06) {
                return Utils.sendPhysicalEvent(name:"doubleTapped", value:button[0], descriptionText:"Button ${button[0]} (${button[1]}) was double tapped")
            }

        // General::Power (0x0001) / Battery report (0x0021)
        case { contains it, [clusterInt:0x0001, attrInt:0x0021] }:
            def percentage =  Integer.parseInt(msg.value, 16)
        
            // (0xFF) 255 is an invalid value for the battery percentage attribute, so we just ignore it
            if (percentage == 255) {
                Log.warn "Ignored invalid battery percentage value: 0xFF (255)"
                return
            }
        
            percentage =  Math.round(percentage / 2)
            return Utils.sendPhysicalEvent(name:"battery", value:percentage, unit:"%", descriptionText:"Battery is ${percentage}% full")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // General::Basic cluster (0x0000) - Read Attribute Response (0x01)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZigbeeMessage("Read Attribute Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
            switch (msg.attrInt) {
                case 0x0001: return Utils.zigbeeDataValue("application", msg.value)
                case 0x0003: return Utils.zigbeeDataValue("hwVersion", msg.value)
                case 0x0004: return Utils.zigbeeDataValue("manufacturer", msg.value)
                case 0x0005:
                    if (msg.value == "SYMFONISK sound remote gen2") updateDataValue "type", "E2123"
                    return Utils.zigbeeDataValue("model", msg.value)
                case 0x4000: return Utils.zigbeeDataValue("softwareBuild", msg.value)
            }
            return Log.warn("Unexpected Zigbee attribute: cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, msg=${msg}")

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

        // Active_EP_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:ActiveEPCount, n*08:ActiveEPList }
        // Three endpoints example: [83, 00, 18, 4A, 03, 01, 02, 03] -> endpointIds=[01, 02, 03]
        case { contains it, [clusterInt:0x8005] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Active Endpoints Response", msg)
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
                Utils.sendZigbeeCommands cmds
            }

            // Add "endpointIds" only if device exposes more then one
            if (count > 1) {
                Utils.zigbeeDataValue "endpointIds", endpointIds.join(",")
            }
            return Utils.processedZigbeeMessage("Active Endpoints Response", "endpointIds=${endpointIds}")

        // Device_annce := { 16:NWKAddr, 64:IEEEAddr , 01:Capability }
        // Example: [82, CF, A0, 71, 0F, 68, FE, FF, 08, AC, 70, 80] -> addr=A0CF, zigbeeId=70AC08FFFE680F71, capabilities=10000000
        case { contains it, [clusterInt:0x0013, commandInt:0x00] }:
            def addr = msg.data[1..2].reverse().join()
            def zigbeeId = msg.data[3..10].reverse().join()
            def capabilities = Integer.toBinaryString(Integer.parseInt(msg.data[11], 16))
            return Utils.processedZigbeeMessage("Device Announce Response", "addr=${addr}, zigbeeId=${zigbeeId}, capabilities=${capabilities}")

        // Bind_rsp := { 08:Status }
        // Success example: [26, 00] -> status = SUCCESS
        // Fail example: [26, 82] -> status = INVALID_EP
        case { contains it, [clusterInt:0x8021] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Bind Response", msg)
            }
            return Utils.processedZigbeeMessage("Bind Response", "data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // Ignored Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            return Utils.ignoredZigbeeMessage("Configure Reporting Response", msg)

        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:
            return Utils.ignoredZigbeeMessage("Identify Query Response", msg)

        case { contains it, [clusterInt:0x8034] }:
            return Utils.ignoredZigbeeMessage("Leave Response", msg)

        // ---------------------------------------------------------------------------------------------------------------
        // Unexpected Zigbee message
        // ---------------------------------------------------------------------------------------------------------------
        default:
            Log.warn "Sent unexpected Zigbee message: description=${description}, msg=${msg}"
    }
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

@Field def Map Log = [
    debug: { message -> if (logLevel == "1") log.debug "${device.displayName} ${message.uncapitalize()}" },
    info:  { message -> if (logLevel <= "2") log.info  "${device.displayName} ${message.uncapitalize()}" },
    warn:  { message -> if (logLevel <= "3") log.warn  "${device.displayName} ${message.uncapitalize()}" },
    error: { log.error "${device.displayName} ${message.uncapitalize()}" },
]

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

@Field def Utils = [
    sendZigbeeCommands: { List<String> cmds ->
        Log.debug "◀ Sending Zigbee messages: ${cmds}"
        state.lastTx = now()
        sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    },
    
    sendPhysicalEvent: { Map event ->
        Log.info "${event.descriptionText} [physical]"
        sendEvent event + [isStateChange:true, type:"physical"]
    },
    
    sendDigitalEvent: { Map event ->
        Log.info "${event.descriptionText} [digital]"
        sendEvent event + [isStateChange:true, type:"digital"]
    },
    
    zigbeeDataValue: { String key, String value ->
        Log.debug "Update driver data value: ${key}=${value}"
        updateDataValue key, value
    },
    
    processedZigbeeMessage: { String type, String details ->
        Log.debug "▶ Processed Zigbee message: type=${type}, status=SUCCESS, ${details}"
    },
    
    ignoredZigbeeMessage: { String type, Map msg ->
        Log.debug "▶ Ignored Zigbee message: type=${type}, status=SUCCESS, data=${msg.data}"
    },
    
    failedZigbeeMessage: { String type, Map msg ->
        Log.warn "▶ Received Zigbee message: type=${type}, status=${ZDP_STATUS[msg.data[1]]}, data=${msg.data}"
    },
    
    updateSwitch: { String newState ->
        def curState = device.currentValue("switch", true) == "off" ? "off" : "on"
        if (curState != newState) Utils.sendPhysicalEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")
    },
    
    toggleSwitch: {
        def newState = device.currentValue("switch", true) != "on" ? "on" : "off"
        Utils.sendPhysicalEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}")
    },
    
    levelUp: {
        def curLevel = device.currentValue("level", true)
        def delta = Integer.parseInt(levelChange)
        def newLevel = curLevel + delta
        newLevel = newLevel < minLevel ? minLevel : (newLevel > 100 ? 100 : newLevel)
        if (curLevel != newLevel) Utils.sendPhysicalEvent(name:"level", value:newLevel, descriptionText:"Level was set to ${newLevel}%")
    },
    
    levelDown: {
        def curLevel = device.currentValue("level", true)
        def delta = Integer.parseInt(levelChange)
        def newLevel = curLevel - delta
        newLevel = newLevel < minLevel ? minLevel : (newLevel > 100 ? 100 : newLevel)
        if (curLevel != newLevel) Utils.sendPhysicalEvent(name:"level", value:newLevel, descriptionText:"Level was set to ${newLevel}%")
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

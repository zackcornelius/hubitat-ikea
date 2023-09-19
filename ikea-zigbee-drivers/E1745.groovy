/**
 * IKEA Tradfri Motion Sensor (E1745)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E1745.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 */

import groovy.time.TimeCategory
import groovy.transform.Field

@Field def DRIVER_NAME = "IKEA Tradfri Motion Sensor (E1745)"
@Field def DRIVER_VERSION = "3.0.0"
@Field def ZDP_STATUS = ["00":"SUCCESS", "80":"INV_REQUESTTYPE", "81":"DEVICE_NOT_FOUND", "82":"INVALID_EP", "83":"NOT_ACTIVE", "84":"NOT_SUPPORTED", "85":"TIMEOUT", "86":"NO_MATCH", "88":"NO_ENTRY", "89":"NO_DESCRIPTOR", "8A":"INSUFFICIENT_SPACE", "8B":"NOT_PERMITTED", "8C":"TABLE_FULL", "8D":"NOT_AUTHORIZED", "8E":"DEVICE_BINDING_TABLE_FULL"]

// Fields for capability.HealthCheck
@Field def HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": 43200 // When checking, mark the device as offline if no Zigbee message was received in the last 43200 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1745.groovy") {
        capability "Configuration"
        capability "Battery"
        capability "HealthCheck"
        capability "MotionSensor"
        capability "PowerSource"

        // For firmwares: 24.4.5
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57,FC7C", outClusters:"0003,0004,0006,0008,0019,1000", model:"TRADFRI motion sensor", manufacturer:"IKEA of Sweden"
        
        // Attributes for capability.HealthCheck
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
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    Log.info "Installing Zigbee device...."
}

// Called when the "Save Preferences" button is clicked
def updated() {
    Log.info "Saving preferences..."

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    Log.info "🛠️ logLevel = ${logLevel}"
    
    // Preferences for capability.HealthCheck
    schedule HEALTH_CHECK.schedule, "healthCheck"
}

// ===================================================================================================================
// Capabilities helpers
// ===================================================================================================================

// Handler method for scheduled job to disable debug logging
def logsOff() {
   Log.info '⏲️ Automatically reverting log level to "Info"'
   device.clearSetting "logLevel"
   device.removeSetting "logLevel"
   device.updateSetting "logLevel", "2"
}

// Helpers for capability.HealthCheck
def healthCheck() {
   Log.debug '⏲️ Automatically running health check'
    def healthStatus = state?.lastRx == 0 ? "unknown" : (now() - state.lastRx < HEALTH_CHECK.thereshold * 1000 ? "online" : "offline")
    Utils.sendEvent name:"healthStatus", value:healthStatus, type:"physical", descriptionText:"Health status is ${healthStatus}"
}

// ===================================================================================================================
// Implement Hubitat Capabilities
// ===================================================================================================================

// capability.Configuration
// Note: This method is also called when the device is initially installed
def configure() {
    Log.info "Configuring device..."
    Log.debug '[IMPORTANT] For battery-powered devices, click the "Configure" button immediately after pushing any button on the device so that the Zigbee messages we send during configuration will reach the device before it goes to sleep!'

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION

    // Apply preferences first
    updated()

    // Clear state
    state.clear()
    state.lastRx = 0
    state.lastTx = 0

    def cmds = []

    // Configure Zigbee reporting
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 21600, 43200, 0x00) // Report battery level every 6 to 12 hours

    // Add Zigbee binds
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // General - On/Off cluster

    // Query Zigbee attributes
    cmds += zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds += zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds += zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds += zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds += zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    
    // Configuration for capability.Battery
    cmds += zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
    
    // Configuration for capability.HealthCheck
    sendEvent name:"healthStatus", value:"unknown", descriptionText:"Health status is unknown"
    sendEvent name:"checkInterval", value:3600, unit:"second", descriptionText:"Health check interval is 3600 seconds"
    
    // Configuration for capability.PowerSource
    sendEvent name:"powerSource", value:"battery", descriptionText:"Power source is battery"

    // Query all active endpoints
    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.HealthCheck
def ping() {
    Log.info "ping ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    Log.debug "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    runIn 5, "pingExecute"
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

// Implementation for capability.MotionSensor
def motionInactive() {
    return Utils.sendEvent(name:"motion", value:"inactive", type:"digital", descriptionText:"Is inactive")
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap description
    Log.debug "description=[${description}]"
    Log.debug "msg=[${msg}]"
    state.lastRx = now()
    
    // Parse for capability.HealthCheck
    if (device.currentValue("healthStatus", true) != "online") {
        Utils.sendEvent name:"healthStatus", value:"online", type:"digital", descriptionText:"Health status changed to online"
    }

    // Extract cluster and command from message
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle E1745 specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // OnWithTimedOff := { 08:OnOffControl, 16:OnTime, 16:OffWaitTime }
        // OnOffControl := { 01:AcceptOnlyWhenOn, 07:Reserved }
        // Example: [01, 08, 07, 00, 00] -> acceptOnlyWhenOn=true, onTime=180, offWaitTime=0
        case { contains it, [clusterInt:0x0006, commandInt:0x42] }:
            def onTime = Math.round(Integer.parseInt(msg.data[1..2].reverse().join(), 16) / 10)
            runIn onTime, "motionInactive"
            return Utils.sendEvent(name:"motion", value:"active", type:"physical", isStateChange:true, descriptionText:"Is active")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.HealthCheck
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            return Log.info("... pong")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle standard Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // General::Power (0x0001) / Battery report (0x0021)
        case { contains it, [clusterInt:0x0001, attrInt:0x0021] }:
            def percentage =  Integer.parseInt(msg.value, 16)
        
            // (0xFF) 255 is an invalid value for the battery percentage attribute, so we just ignore it
            if (percentage == 255) {
                Log.warn "Ignored invalid battery percentage value: 0xFF (255)"
                return
            }
        
            percentage =  Math.round(percentage / 2)
            return Utils.sendEvent(name:"battery", value:percentage, unit:"%", type:"physical", isStateChange:true, descriptionText:"Battery is ${percentage}% full")

        // General::Basic cluster (0x0000) - Read Attribute Response (0x01)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZigbeeMessage("Read Attribute Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
            switch (msg.attrInt) {
                case 0x0001: return Utils.zigbeeDataValue("application", msg.value)
                case 0x0003: return Utils.zigbeeDataValue("hwVersion", msg.value)
                case 0x0004: return Utils.zigbeeDataValue("manufacturer", msg.value)
                case 0x0005:
                    if (msg.value == "TRADFRI motion sensor") updateDataValue "type", "E1745"
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
        
            def cmds = []
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
            Utils.processedZigbeeMessage("Device Announce Response", "addr=${addr}, zigbeeId=${zigbeeId}, capabilities=${capabilities}")
            return

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

        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
            return Utils.ignoredZigbeeMessage("Unknown Zigbee Response with weird payload", msg)

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

    sendEvent: { Map event ->
        Log.info "${event.descriptionText} [${event.type}]"
        sendEvent event
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
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

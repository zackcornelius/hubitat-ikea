/**
 * IKEA Tradfri Control Outlet (E1603)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E1603.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 */

import groovy.time.TimeCategory
import groovy.transform.Field

@Field def DRIVER_NAME = "IKEA Tradfri Control Outlet (E1603)"
@Field def DRIVER_VERSION = "1.3.0"
@Field def ZDP_STATUS = ["00":"SUCCESS", "80":"INV_REQUESTTYPE", "81":"DEVICE_NOT_FOUND", "82":"INVALID_EP", "83":"NOT_ACTIVE", "84":"NOT_SUPPORTED", "85":"TIMEOUT", "86":"NO_MATCH", "88":"NO_ENTRY", "89":"NO_DESCRIPTOR", "8A":"INSUFFICIENT_SPACE", "8B":"NOT_PERMITTED", "8C":"TABLE_FULL", "8D":"NOT_AUTHORIZED", "8E":"DEVICE_BINDING_TABLE_FULL"]

// Health Check config
@Field def HEALTH_CHECK = [
    "schedule"   : "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold" : 3600              // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1603.groovy") {
        capability "Configuration"
        capability "HealthCheck"
        capability "Outlet"
        capability "PowerSource"
        capability "Refresh"

        // For firmwares: 2.0.024
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,1000,FC7C", outClusters:"0005,0019,0020,1000", model:"TRADFRI control outlet", manufacturer:"IKEA of Sweden"

        // For firmwares: 2.3.089
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,1000,FC7C", outClusters:"0019,0020,1000", model:"TRADFRI control outlet", manufacturer:"IKEA of Sweden"

        // Should be part of capability.HealthCheck
        attribute "healthStatus", "ENUM", ["offline", "online", "unknown"]

        // Attributes for capability.ZigbeeRouter
        attribute "neighbors", "STRING"
        attribute "routes", "STRING"
    }

    // Commands for capability.ZigbeeRouter
    command "requestRoutingData"

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
    Log.info "🛠️ logLevel = ${logLevel}"

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

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION

    // Apply preferences first
    updated()

    // Clear state
    state.clear()
    state.lastRx = 0
    state.lastTx = 0

    // capability.PowerSource
    sendEvent name:"powerSource", value:"mains", descriptionText:"Power source set to mains"

    // capability.HealthCheck
    sendEvent name:"healthStatus", value:"unknown", descriptionText:"Health status set to unknown"
    sendEvent name:"checkInterval", value:3600, descriptionText:"Health check interval set to 3600 seconds"

    List<String> cmds = []

    // Configure Zigbee reporting
    cmds.addAll zigbee.configureReporting(0x0006, 0x0000, 0x10, 300, 600, 0x00) // Report On/Off status every 5 to 10 minutes

    // Add Zigbee binds
    // -- No binds needed

    // Query Zigbee attributes
    cmds.addAll zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds.addAll zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds.addAll zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID

    // Query all active endpoints
    cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"

    Utils.sendZigbeeCommands cmds
}

// capability.HealthCheck
def ping() {
    // Request the device to send the value for the OnOff attribute
    Log.info "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
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

// capability.Outlet
def on() {
    Log.debug "Sending on command"
    Utils.sendZigbeeCommands(zigbee.on())
}
def off() {
    Log.debug "Sending off command"
    Utils.sendZigbeeCommands(zigbee.off())
}

// capability.Refresh
def refresh() {
    Log.debug "Asking device to send its switch status ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
}

// capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send the Neighbors Table and the Routing Table data ..."
    List<String> cmds = []

    // Ask for the Neighbors Table and the Routing Table
    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0031 {00} {0x00}"
    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0032 {00} {0x00}"
    Utils.sendZigbeeCommands cmds
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

        // Switch state was changed
        case { contains it, [clusterInt:0x0006, commandInt:0x0B] }:
            def newState = msg.data[0] == "00" ? "off" : "on"
            Log.debug "Reported switch status as ${newState}"
            return Utils.updateSwitch(newState)
        
        // Device sent value for the OnOff attribute
        case { contains it, [clusterInt:0x0006, attrInt: 0x0000] }:
            def newState = msg.value == "00" ? "off" : "on"
            Log.debug "Reported switch status as ${newState}"
            return Utils.updateSwitch(newState)
        
        // Device_annce
        case { contains it, [clusterInt:0x0013, commandInt:0x00] }:
            Log.info "Has been plugged back in. Querying its switch status..."
            return Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // General::Basic cluster (0x0000) - Read Attribute Response (0x01)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZigbeeMessage("Read Attribute Response", "attribute=${msg.attrId}")
            switch (msg.attrInt) {
                case 0x0001: return Utils.zigbeeDataValue("application", msg.value)
                case 0x0003: return Utils.zigbeeDataValue("hwVersion", msg.value)
                case 0x0004: return Utils.zigbeeDataValue("manufacturer", msg.value)
                case 0x0005:
                    if (msg.value == "TRADFRI control outlet") updateDataValue "type", "E1603"
                    return Utils.zigbeeDataValue("model", msg.value)
                case 0x4000: return Utils.zigbeeDataValue("softwareBuild", msg.value)
            }
            return warn("Unexpected Zigbee attribute: attribute=${msg.attrInt}, msg=${msg}")

        // Simple_Desc_rsp = { 08:Status, 16:NWKAddrOfInterest, 08:Length, 08:Endpoint, 16:ApplicationProfileIdentifier, 16:ApplicationDeviceIdentifier, 08:Reserved, 16:InClusterCount, n*16:InClusterList, 16:OutClusterCount, n*16:OutClusterList }
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

        // Active_EP_rsp = { 08:Status, 16:NWKAddrOfInterest, 08:ActiveEPCount, n*08:ActiveEPList }
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

        // Device_annce = { 16:NWKAddr, 64:IEEEAddr , 01:Capability }
        // Example : [82, CF, A0, 71, 0F, 68, FE, FF, 08, AC, 70, 80] -> addr=A0CF, zigbeeId=70AC08FFFE680F71, capabilities=10000000
        case { contains it, [clusterInt:0x0013] }:
            def addr = msg.data[1..2].reverse().join()
            def zigbeeId = msg.data[3..10].reverse().join()
            def capabilities = Integer.toBinaryString(Integer.parseInt(msg.data[11], 16))
            return Utils.processedZigbeeMessage("Device Announce Response", "addr=${addr}, zigbeeId=${zigbeeId}, capabilities=${capabilities}")

        // Bind_rsp = { 08:Status }
        // Success example : [26, 00] -> status = SUCCESS
        // Fail example    : [26, 82] -> status = INVALID_EP
        case { contains it, [clusterInt:0x8021] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Bind Response", msg)
            }
            return Utils.processedZigbeeMessage("Bind Response", "data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // capability.ZigbeeRouter
        // ---------------------------------------------------------------------------------------------------------------

        // Mgmt_Lqi_rsp = { 08:Status, 08:NeighborTableEntries, 08:StartIndex, 08:NeighborTableListCount, n*176:NeighborTableList }
        // NeighborTableList = { 64:ExtendedPanId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
        // Example: [6E, 00, 08, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 24, 02, 00, CF, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 05, 02, 0F, CD, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 36]
        case { contains it, [clusterInt:0x8031, commandInt:0x00] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Neighbors Table Response", msg)
            }
            def entriesCount = Integer.parseInt(msg.data[4], 16)

            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            def base64 = msg.data.join().decodeHex().encodeBase64().toString() // Decode test: https://base64.guru/converter/decode/hex
            sendEvent name:"neighbors", value:"${entriesCount} entries", descriptionText:base64, isStateChange:true, type:"physical"
            return Utils.processedZigbeeMessage("Neighbors Table Response", "entries=${entriesCount}, data=${msg.data}")

        // Mgmt_Rtg_rsp = { 08:Status, 08:RoutingTableEntries, 08:StartIndex, 08:RoutingTableListCount, n*40:RoutingTableList }
        // Example: [6F, 00, 0A, 00, 0A, 00, 00, 10, 00, 00, AD, 56, 00, AD, 56, ED, EE, 00, 4A, 16, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00]
        // RoutingTableList = { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
        case { contains it, [clusterInt:0x8032, commandInt:0x00] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Routing Table Response", msg)
            }
            def entriesCount = Integer.parseInt(msg.data[4], 16)

            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            def base64 = msg.data.join().decodeHex().encodeBase64().toString()
            sendEvent name:"routes", value:"${entriesCount} entries", descriptionText:base64, isStateChange:true, type:"physical"
            return Utils.processedZigbeeMessage("Routing Table Response", "entries=${entriesCount}, data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // Ignored Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        case { contains it, [clusterInt:0x0001, commandInt:0x07] }:
            return Utils.ignoredZigbeeMessage("Configure Reporting Response", msg)

        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:
            return Utils.ignoredZigbeeMessage("Identify Query Response", msg)

        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            return Utils.ignoredZigbeeMessage("Reporting Configuration Response", msg)

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

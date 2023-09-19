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
@Field def DRIVER_VERSION = "3.0.0"
@Field def ZDP_STATUS = ["00":"SUCCESS", "80":"INV_REQUESTTYPE", "81":"DEVICE_NOT_FOUND", "82":"INVALID_EP", "83":"NOT_ACTIVE", "84":"NOT_SUPPORTED", "85":"TIMEOUT", "86":"NO_MATCH", "88":"NO_ENTRY", "89":"NO_DESCRIPTOR", "8A":"INSUFFICIENT_SPACE", "8B":"NOT_PERMITTED", "8C":"TABLE_FULL", "8D":"NOT_AUTHORIZED", "8E":"DEVICE_BINDING_TABLE_FULL"]

// Fields for capability.HealthCheck
@Field def HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": 3600 // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1603.groovy") {
        capability "Configuration"
        capability "HealthCheck"
        capability "PowerSource"
        capability "Refresh"
        capability "Switch"
        capability "HealthCheck"

        // For firmwares: 2.0.024
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,1000,FC7C", outClusters:"0005,0019,0020,1000", model:"TRADFRI control outlet", manufacturer:"IKEA of Sweden"

        // For firmwares: 2.3.089
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0008,1000,FC7C", outClusters:"0019,0020,1000", model:"TRADFRI control outlet", manufacturer:"IKEA of Sweden"
        
        // Attributes for capability.HealthCheck
        attribute "healthStatus", "ENUM", ["offline", "online", "unknown"]
        
        // Attributes for capability.ZigbeeRouter
        attribute "neighbors", "STRING"
        attribute "routes", "STRING"
    }
    
    // Commands for capability.Switch
    command "toggle"
    command "onWithTimedOff", [[name:"On time*", type:"NUMBER", description:"After how many seconds power will be turned Off [1..6500]"]]
    
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
        
        // Inputs for capability.Switch
        input(
            name: "startupBehavior",
            type: "enum",
            title: "Behavior after a power outage",
            options: ["TURN_POWER_ON":"Turn power On", "TURN_POWER_OFF":"Turn power Off", "RESTORE_PREVIOUS_STATE":"Restore previous state"],
            defaultValue: "PREV",
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
    
    // Preferences for capability.Switch
    Log.info "🛠️ startupBehavior = ${startupBehavior}"
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0006, 0x4003, 0x30, startupBehavior == "TURN_POWER_OFF" ? 0x00 : (startupBehavior == "TURN_POWER_ON" ? 0x01 : 0xFF))
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
    cmds += zigbee.configureReporting(0x0006, 0x0000, 0x10, 300, 600, 0x00) // Report On/Off status every 5 to 10 minutes

    // Add Zigbee binds
    // -- No binds needed

    // Query Zigbee attributes
    cmds += zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds += zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds += zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds += zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds += zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    
    // Configuration for capability.HealthCheck
    sendEvent name:"healthStatus", value:"unknown", descriptionText:"Health status is unknown"
    sendEvent name:"checkInterval", value:3600, unit:"second", descriptionText:"Health check interval is 3600 seconds"
    
    // Configuration for capability.PowerSource
    sendEvent name:"powerSource", value:"mains", descriptionText:"Power source is mains"
    
    // Configure for capability.Refresh
    cmds += zigbee.readAttribute(0x0006, 0x0000)  // OnOff := { 0x00:Off, 0x01:On }
    cmds += zigbee.readAttribute(0x0006, 0x4003)  // StartupBehavior := { 0x00:TurnPowerOff, 0x01:TurnPowerOn, 0xFF:RestorePreviousState }

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

// Implementation for capability.Refresh
def refresh(isPhysical = false) {
    state.isPhysical = isPhysical
    cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000)  // OnOff := { 0x00:Off, 0x01:On }
    cmds += zigbee.readAttribute(0x0006, 0x4003)  // StartupBehavior := { 0x00:TurnPowerOff, 0x01:TurnPowerOn, 0xFF:RestorePreviousState }
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.Switch
def on() {
    Log.debug "Sending On command"
    Utils.sendZigbeeCommands(zigbee.on())
    unschedule "onWithTimedOff_Completed"
}
def off() {
    Log.debug "Sending Off command"
    Utils.sendZigbeeCommands(zigbee.off())
    unschedule "onWithTimedOff_Completed"
}

def toggle() {
    Log.debug "Sending Toggle command"
    Utils.sendZigbeeCommands(zigbee.command(0x0006, 0x02))
    unschedule "onWithTimedOff_Completed"
}

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

// Implementation for capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send the Neighbors Table and the Routing Table data ..."
    Utils.sendZigbeeCommands([
        "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0031 {00} {0x00}",
        "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0032 {00} {0x00}"
    ])
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
        // Handle E1603 specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // Device reported that the one of the on(), off() or toggle() command was processed
        // Examples:
        // - On:     0104 0006 01 01 0040 00 7F1B 00 00 0000 0B 01 0100
        // - Off:    0104 0006 01 01 0040 00 7F1B 00 00 0000 0B 01 0000
        // - Toggle: 0104 0006 01 01 0040 00 7F1B 00 00 0000 0B 01 0200
        case { contains it, [clusterInt:0x0006, commandInt:0x0B] }:
            Log.debug "on/off/toggle command response: data=${msg.data}"
        
            // Toggle?
            if (msg.data[0] == "02") {
                state.isPhysical = false
                return Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
            }
        
            def newState = msg.data[0] == "00" ? "off" : "on"
            return Utils.sendEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:"digital", isStateChange:false)
        
        // Read Attribute Response: OnOff
        case { contains it, [clusterInt:0x0006, attrInt: 0x0000] }:
            Log.debug "Reported OnOff attribute: value=${msg.value}"
        
            def newState = msg.value == "00" ? "off" : "on"
            def type = state.containsKey("isPhysical") && state.isPhysical == false ? "digital" : "physical"
            state.remove("isPhysical")
            return Utils.sendEvent(name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:type, isStateChange:false)

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.HealthCheck
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            return Log.info("... pong")
        
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
        
        // Events for capability.ZigbeeRouter
        
        // Mgmt_Lqi_rsp := { 08:Status, 08:NeighborTableEntries, 08:StartIndex, 08:NeighborTableListCount, n*176:NeighborTableList }
        // NeighborTableList := { 64:ExtendedPanId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
        // Example: [6E, 00, 08, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 24, 02, 00, CF, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 05, 02, 0F, CD, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 36]
        case { contains it, [clusterInt:0x8031, commandInt:0x00] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Neighbors Table Response", msg)
            }
            def entriesCount = Integer.parseInt(msg.data[4], 16)
        
            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            def base64 = msg.data.join().decodeHex().encodeBase64().toString() // Decode test: https://base64.guru/converter/decode/hex
            sendEvent name:"neighbors", value:"${entriesCount} entries", type:"digital", descriptionText:base64
            return Utils.processedZigbeeMessage("Neighbors Table Response", "entries=${entriesCount}, data=${msg.data}")
        
        // Mgmt_Rtg_rsp := { 08:Status, 08:RoutingTableEntries, 08:StartIndex, 08:RoutingTableListCount, n*40:RoutingTableList }
        // RoutingTableList := { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
        // Example: [6F, 00, 0A, 00, 0A, 00, 00, 10, 00, 00, AD, 56, 00, AD, 56, ED, EE, 00, 4A, 16, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00]
        case { contains it, [clusterInt:0x8032, commandInt:0x00] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Routing Table Response", msg)
            }
            def entriesCount = Integer.parseInt(msg.data[4], 16)
        
            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            def base64 = msg.data.join().decodeHex().encodeBase64().toString()
            sendEvent name:"routes", value:"${entriesCount} entries", type:"digital", descriptionText:base64
            return Utils.processedZigbeeMessage("Routing Table Response", "entries=${entriesCount}, data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle standard Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // General::Basic cluster (0x0000) - Read Attribute Response (0x01)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZigbeeMessage("Read Attribute Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
            switch (msg.attrInt) {
                case 0x0001: return Utils.zigbeeDataValue("application", msg.value)
                case 0x0003: return Utils.zigbeeDataValue("hwVersion", msg.value)
                case 0x0004: return Utils.zigbeeDataValue("manufacturer", msg.value)
                case 0x0005:
                    if (msg.value == "TRADFRI control outlet") updateDataValue "type", "E1603"
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
        
            // Welcome back; let's sync state
            Log.debug("Rejoined the network. Executing refresh(false) ...")
            refresh(false)
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

        case { contains it, [clusterInt:0x0006, commandInt:0x04] }:
            return Utils.ignoredZigbeeMessage("Attribute Write Response", msg)

        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            return Utils.ignoredZigbeeMessage("Reporting Configuration Response", msg)

        case { contains it, [clusterInt:0x0006, commandInt:0x00] }:
            return Utils.ignoredZigbeeMessage("Unknown Zigbee Response with weird payload", msg)

        case { contains it, [clusterInt:0x8001] }:
            return Utils.ignoredZigbeeMessage("IEEE Address Response", msg)

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

/**
 * IKEA Tradfri Control Outlet (E1603)
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_E1603.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://static.homesmart.ikea.com/releaseNotes/
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final String DRIVER_NAME = "IKEA Tradfri Control Outlet (E1603)"
@Field static final String DRIVER_VERSION = "3.2.0"
@Field static final Map<String, String> ZDP_STATUS = ["00":"SUCCESS", "80":"INV_REQUESTTYPE", "81":"DEVICE_NOT_FOUND", "82":"INVALID_EP", "83":"NOT_ACTIVE", "84":"NOT_SUPPORTED", "85":"TIMEOUT", "86":"NO_MATCH", "88":"NO_ENTRY", "89":"NO_DESCRIPTOR", "8A":"INSUFFICIENT_SPACE", "8B":"NOT_PERMITTED", "8C":"TABLE_FULL", "8D":"NOT_AUTHORIZED", "8E":"DEVICE_BINDING_TABLE_FULL"]
@Field static final Map<String, String> ZCL_STATUS = ["00":"SUCCESS", "01":"FAILURE", "7E":"NOT_AUTHORIZED", "7F":"RESERVED_FIELD_NOT_ZERO", "80":"MALFORMED_COMMAND", "81":"UNSUP_CLUSTER_COMMAND", "82":"UNSUP_GENERAL_COMMAND", "83":"UNSUP_MANUF_CLUSTER_COMMAND", "84":"UNSUP_MANUF_GENERAL_COMMAND", "85":"INVALID_FIELD", "86":"UNSUPPORTED_ATTRIBUTE", "87":"INVALID_VALUE", "88":"READ_ONLY", "89":"INSUFFICIENT_SPACE", "8A":"DUPLICATE_EXISTS", "8B":"NOT_FOUND", "8C":"UNREPORTABLE_ATTRIBUTE", "8D":"INVALID_DATA_TYPE", "8E":"INVALID_SELECTOR", "8F":"WRITE_ONLY", "90":"INCONSISTENT_STARTUP_STATE", "91":"DEFINED_OUT_OF_BAND", "92":"INCONSISTENT", "93":"ACTION_DENIED", "94":"TIMEOUT", "95":"ABORT", "96":"INVALID_IMAGE", "97":"WAIT_FOR_DATA", "98":"NO_IMAGE_AVAILABLE", "99":"REQUIRE_MORE_IMAGE", "9A":"NOTIFICATION_PENDING", "C0":"HARDWARE_FAILURE", "C1":"SOFTWARE_FAILURE", "C2":"CALIBRATION_ERROR", "C3":"UNSUPPORTED_CLUSTER"]

// Fields for capability.HealthCheck
@Field static final Map<String, String> HEALTH_CHECK = [
    "schedule": "0 0 0/1 ? * * *", // Health will be checked using this cron schedule
    "thereshold": "3600" // When checking, mark the device as offline if no Zigbee message was received in the last 3600 seconds
]

metadata {
    definition(name:DRIVER_NAME, namespace:"dandanache", author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1603.groovy") {
        capability "Configuration"
        capability "Switch"
        capability "HealthCheck"
        capability "PowerSource"
        capability "Refresh"
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
            title: "Log verbosity",
            description: "<small>Select what type of messages are added in the \"Logs\" section</small>",
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
            name: "powerOnBehavior",
            type: "enum",
            title: "Power On behaviour",
            description: "<small>Select what happens after a power outage</small>",
            options: [
                "TURN_POWER_ON": "Turn power On",
                "TURN_POWER_OFF": "Turn power Off",
                "RESTORE_PREVIOUS_STATE": "Restore previous state"
            ],
            defaultValue: "RESTORE_PREVIOUS_STATE",
            required: true
        )
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    Log.info "Installing device...."
    Log.warn "[IMPORTANT] For battery-powered devices, make sure that you keep your IKEA device as close as you can to your Hubitat hub (or any other Zigbee router device) for at least 20 seconds. Otherwise it will successfully pair but it won't work properly!"
}

// Called when the "Save Preferences" button is clicked
def updated() {
    Log.info "Saving device preferences..."

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    Log.info "🛠️ logLevel = ${logLevel}"
    
    // Preferences for capability.Switch
    Log.info "🛠️ powerOnBehavior = ${powerOnBehavior}"
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0006, 0x4003, 0x30, powerOnBehavior == "TURN_POWER_OFF" ? 0x00 : (powerOnBehavior == "TURN_POWER_ON" ? 0x01 : 0xFF))
    
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
    def healthStatus = state.lastRx == 0 ? "unknown" : (now() - state.lastRx < Integer.parseInt(HEALTH_CHECK.thereshold) * 1000 ? "online" : "offline")
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

    def cmds = []

    // Configure E1603 specific Zigbee reporting
    // -- No reporting needed

    // Add E1603 specific Zigbee binds
    // -- No binds needed
    
    // Configuration for capability.Switch
    sendEvent name:"switch", value:"on", type:"digital", descriptionText:"Switch initialized to on"
    cmds += "he cr 0x${device.deviceNetworkId} 0x01 0x0006 0x0000 0x10 0x0000 0x0258 {01} {}" // Report battery at least every 10 minutes
    cmds += "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}" // On/Off cluster
    
    // Configuration for capability.HealthCheck
    state.lastRx == 0
    state.lastTx == 0
    sendEvent name:"healthStatus", value:"online", descriptionText:"Health status initialized to online"
    sendEvent name:"checkInterval", value:3600, unit:"second", descriptionText:"Health check interval is 3600 seconds"
    
    // Configuration for capability.PowerSource
    sendEvent name:"powerSource", value:"unknown", type:"digital", descriptionText:"Power source initialized to unknown"
    cmds += zigbee.readAttribute(0x0000, 0x0007) // PowerSource
    
    // Configure for capability.Refresh
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff := { 0x00:Off, 0x01:On }
    cmds += zigbee.readAttribute(0x0006, 0x4003) // PowerOnBehavior := { 0x00:TurnPowerOff, 0x01:TurnPowerOn, 0xFF:RestorePreviousState }

    // Query Basic cluster attributes
    cmds += zigbee.readAttribute(0x0000, [0x0001, 0x0003, 0x0004, 0x0005, 0x000A, 0x4000]) // ApplicationVersion, HWVersion, ManufacturerName, ModelIdentifier, IKEAType, SWBuildID

    // Query all active endpoints
    cmds += "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.Switch
def on() {
    Log.debug "Sending On command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114301}"])
}
def off() {
    Log.debug "Sending Off command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114300}"])
}

def toggle() {
    Log.debug "Sending Toggle command"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114302}"])
}

def onWithTimedOff(onTime = 1) {
    Integer delay = onTime < 1 ? 1 : (onTime > 6500 ? 6500 : onTime)
    Log.debug "Sending OnWithTimedOff command"

    String payload = "00 ${zigbee.swapOctets(zigbee.convertToHexString(delay * 10, 4))} 0000"
    Utils.sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {114342 ${payload}}"])
}

// Implementation for capability.HealthCheck
def ping() {
    Log.info "ping ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    Log.debug "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    runIn 5, "pingExecute"
}

def pingExecute() {
    if (state.lastRx == 0) {
        return Log.info("Did not sent any messages since it was last configured")
    }

    def now = new Date(Math.round(now() / 1000) * 1000)
    def lastRx = new Date(Math.round(state.lastRx / 1000) * 1000)
    def lastRxAgo = TimeCategory.minus(now, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Sent last message at ${lastRx.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${lastRxAgo} ago)"

    def thereshold = new Date(Math.round(state.lastRx / 1000 + Integer.parseInt(HEALTH_CHECK.thereshold)) * 1000)
    def theresholdAgo = TimeCategory.minus(thereshold, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received for ${theresholdAgo} (hardcoded)"

    def offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received until ${thereshold.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${offlineMarkAgo} from now)"
}

// Implementation for capability.Refresh
def refresh() {
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff := { 0x00:Off, 0x01:On }
    cmds += zigbee.readAttribute(0x0006, 0x4003) // PowerOnBehavior := { 0x00:TurnPowerOff, 0x01:TurnPowerOn, 0xFF:RestorePreviousState }
    Utils.sendZigbeeCommands cmds
}

// Implementation for capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send its Neighbors Table and the Routing Table data ..."
    Utils.sendZigbeeCommands([
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0031 {00} {0x00}",
        "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0032 {00} {0x00}"
    ])
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    Log.debug "description=[${description}]"

    // Extract msg
    def msg = zigbee.parseDescriptionAsMap description
    if (msg.containsKey("endpoint")) msg.endpointInt = Integer.parseInt(msg.endpoint, 16)
    if (msg.containsKey("sourceEndpoint")) msg.endpointInt = Integer.parseInt(msg.sourceEndpoint, 16)
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)
    Log.debug "msg=[${msg}]"

    state.lastRx = now()
    
    // Parse for capability.HealthCheck
    if (device.currentValue("healthStatus", true) != "online") {
        Utils.sendEvent name:"healthStatus", value:"online", type:"digital", descriptionText:"Health status changed to online"
    }

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle E1603 specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------

        // No specific events

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        
        // Events for capability.Switch
        
        // Report Attributes: OnOff
        // Read Attributes Response: OnOff
        case { contains it, [clusterInt:0x0006, commandInt:0x0A, attrInt: 0x0000] }:
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt: 0x0000] }:
        
            // If we sent a Zigbee command in the last 3 seconds, we assume that this On/Off state change is a consequence of this driver
            // Therefore, we mark this event as "digital"
            String type = state.containsKey("lastTx") && (now() - state.lastTx < 3000) ? "digital" : "physical"
        
            String newState = msg.value == "00" ? "off" : "on"
            if (device.currentValue("switch", true) != newState) {
                Utils.sendEvent name:"switch", value:newState, descriptionText:"Was turned ${newState}", type:type
            }
            return Utils.processedZclMessage("Report/Read Attributes Response", "OnOff=${newState}")
        
        // Read Attributes Response: powerOnBehavior
        case { contains it, [clusterInt:0x0006, commandInt:0x01, attrInt: 0x4003] }:
            String newValue = ""
            switch (Integer.parseInt(msg.value, 16)) {
                case 0x00: newValue = "TURN_POWER_OFF"; break
                case 0x01: newValue = "TURN_POWER_ON"; break
                case 0xFF: newValue = "RESTORE_PREVIOUS_STATE"; break
                default: return Log.warn("Received attribute value: powerOnBehavior=${msg.value}")
            }
        
            powerOnBehavior = newValue
            device.clearSetting "powerOnBehavior"
            device.removeSetting "powerOnBehavior"
            device.updateSetting "powerOnBehavior", newValue
            return Utils.processedZclMessage("Read Attributes Response", "powerOnBehavior=${newValue}")
        
        // Other events that we expect but are not usefull for capability.Switch behavior
        
        // Write Attribute Response (0x04)
        case { contains it, [clusterInt:0x0006, commandInt:0x04] }:
            if (msg.data[0] != "00") return Utils.failedZclMessage("Write Attribute Response", msg.data[0], msg)
            return Utils.processedZclMessage("Write Attribute Response", "cluster=0x${msg.clusterId}")
        
        // ConfigureReportingResponse := { 08:Status, 08:Direction, 16:AttributeIdentifier }
        // Success example: [00] -> status = SUCCESS
        case { contains it, [clusterInt:0x0006, commandInt:0x07] }:
            if (msg.data[0] != "00") return Utils.failedZclMessage("Configure Reporting Response", msg.data[0], msg)
            return Utils.processedZclMessage("Configure Reporting Response", "cluster=0x${msg.clusterId}, data=${msg.data}")
        
        // Events for capability.HealthCheck
        case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
            return Log.info("... pong")
        
        // Read Attributes Reponse: PowerSource
        case { contains it, [clusterInt:0x0000, commandInt:0x01, attrInt:0x0007] }:
            String powerSource = "unknown"
        
            // PowerSource := { 0x00:Unknown, 0x01:MainsSinglePhase, 0x02:MainsThreePhase, 0x03:Battery, 0x04:DC, 0x05:EmergencyMainsConstantlyPowered, 0x06:EmergencyMainsAndTransferSwitch }
            switch (msg.value) {
                case "01":
                case "02":
                case "05":
                case "06":
                    powerSource = "mains"
                    break
                case "03":
                    powerSource = "battery"
                    break
                case "04":
                    powerSource = "dc"
                    break
            }
            Utils.sendEvent name:"powerSource", value:powerSource, type:"digital", descriptionText:"Power source is ${powerSource}"
            return Utils.processedZclMessage("Read Attributes Response", "PowerSource=${msg.value}")
        
        // Events for capability.ZigbeeRouter
        
        // Mgmt_Lqi_rsp := { 08:Status, 08:NeighborTableEntries, 08:StartIndex, 08:NeighborTableListCount, n*176:NeighborTableList }
        // NeighborTableList := { 64:ExtendedPanId, 64:IEEEAddress, 16:NetworkAddress, 02:DeviceType, 02:RxOnWhenIdle, 03:Relationship, 01:Reserved, 02:PermitJoining, 06:Reserved, 08:Depth, 08:LQI }
        // Example: [6E, 00, 08, 00, 03, 50, 53, 3A, 0D, 00, DF, 66, 15, E9, A6, C9, 17, 00, 6F, 0D, 00, 00, 00, 24, 02, 00, CF, 50, 53, 3A, 0D, 00, DF, 66, 15, 80, BF, CA, 6B, 6A, 38, C1, A4, 4A, 16, 05, 02, 0F, CD, 50, 53, 3A, 0D, 00, DF, 66, 15, D3, FA, E1, 25, 00, 4B, 12, 00, 64, 17, 25, 02, 0F, 36]
        case { contains it, [endpointInt:0x00, clusterInt:0x8031, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Neighbors Table Response", msg.data[1], msg)
            Integer entriesCount = Integer.parseInt(msg.data[4], 16)
        
            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            String base64 = msg.data.join().decodeHex().encodeBase64().toString() // Decode test: https://base64.guru/converter/decode/hex
            sendEvent name:"neighbors", value:"${entriesCount} entries", type:"digital", descriptionText:base64
            return Utils.processedZdoMessage("Neighbors Table Response", "entries=${entriesCount}, data=${msg.data}")
        
        // Mgmt_Rtg_rsp := { 08:Status, 08:RoutingTableEntries, 08:StartIndex, 08:RoutingTableListCount, n*40:RoutingTableList }
        // RoutingTableList := { 16:DestinationAddress, 03:RouteStatus, 01:MemoryConstrained, 01:ManyToOne, 01:RouteRecordRequired, 02:Reserved, 16:NextHopAddress }
        // Example: [6F, 00, 0A, 00, 0A, 00, 00, 10, 00, 00, AD, 56, 00, AD, 56, ED, EE, 00, 4A, 16, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00, 00, 00, 03, 00, 00]
        case { contains it, [endpointInt:0x00, clusterInt:0x8032, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Routing Table Response", msg.data[1], msg)
            Integer entriesCount = Integer.parseInt(msg.data[4], 16)
        
            // Use base64 encoding instead of hex encoding to make the message a bit shorter
            String base64 = msg.data.join().decodeHex().encodeBase64().toString()
            sendEvent name:"routes", value:"${entriesCount} entries", type:"digital", descriptionText:base64
            return Utils.processedZdoMessage("Routing Table Response", "entries=${entriesCount}, data=${msg.data}")

        // ---------------------------------------------------------------------------------------------------------------
        // Handle common messages (e.g.: received during pairing when we query the device for information)
        // ---------------------------------------------------------------------------------------------------------------

        // Device_annce := { 16:NWKAddr, 64:IEEEAddr , 01:Capability }
        // Event is issued by the device when it is powered up (after battery change, plugged in to a power outlet, etc.)
        // Example: [82, CF, A0, 71, 0F, 68, FE, FF, 08, AC, 70, 80] -> addr=A0CF, zigbeeId=70AC08FFFE680F71, capabilities=10000000
        case { contains it, [endpointInt:0x00, clusterInt:0x0013, commandInt:0x00] }:
            String addr = msg.data[1..2].reverse().join()
            String zigbeeId = msg.data[3..10].reverse().join()
            String capabilities = Integer.toBinaryString(Integer.parseInt(msg.data[11], 16))
            Utils.processedZdoMessage("Device Announce Response", "addr=${addr}, zigbeeId=${zigbeeId}, capabilities=${capabilities}")

            // Welcome back; let's sync state
            Log.info("Rejoined the Zigbee mesh; auto-executing refresh() in 5 seconds to sync its state ...")
            runIn 5, "tryToRefresh"
            return

        // Read Attributes Response (Basic cluster)
        case { contains it, [clusterInt:0x0000, commandInt:0x01] }:
            Utils.processedZclMessage("Read Attributes Response", "cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, value=${msg.value}")
            Utils.zigbeeDataValue(msg.attrInt, msg.value)
            msg.additionalAttrs?.each { Utils.zigbeeDataValue(it.attrInt, it.value) }
            return

        // Identify Query Command
        case { contains it, [clusterInt:0x0003, commandInt:0x01] }:
            return Utils.processedZclMessage("Identify Query Command", "Ignored!")

        // Mgmt_Bind_rsp := { 08:Status }
        // Success example: [26, 00] -> status = SUCCESS
        // Fail example: [26, 82] -> status = INVALID_EP
        case { contains it, [endpointInt:0x00, clusterInt:0x8021, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Bind Response", msg.data[1], msg)
            return Utils.processedZdoMessage("Bind Response", "data=${msg.data}")

        // Mgmt_Leave_rsp := { 08:Status }
        // Success example: [26, 00] -> status = SUCCESS
        // Fail example: [26, 82] -> status = INVALID_EP
        case { contains it, [endpointInt:0x00, clusterInt:0x8034, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Leave Response", msg.data[1], msg)
            Log.info "Device is leaving the Zigbee mesh. See you later, Aligator!"
            return Utils.processedZdoMessage("Leave Response", "data=${msg.data}")

        // MatchDescriptorRequest := { 08:SequenceNumber, 16:NetworkAddress, 16:ProfileId, 08:NumberOfInputClusters, 16*n:InputClusterList, 08:NumberOfOutputClusters, 16*n:OutputClusterList }
        // Example: [02, FD, FF, 04, 01, 01, 19, 00, 00] -> SequenceNumber=2, NetworkAddress=FFFD (Broadcast), ProfileId=0104 (Home Automation), NumberOfInputClusters=1, InputClusterList=[0x0019], NumberOfOutputClusters=0, OutputClusterList=[]
        case { contains it, [endpointInt:0x00, clusterInt:0x0006, commandInt:0x00] }:

            // Maybe we should not ignore this and send back a MatchDescriptorResponse
            // See https://www.digi.com/resources/documentation/digidocs/90001539/reference/r_zdo_match_response.htm
            return Utils.processedZdoMessage("Match Descriptor Request", "data=${msg.data}")

        // Active_EP_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:ActiveEPCount, n*08:ActiveEPList }
        // Three endpoints example: [83, 00, 18, 4A, 03, 01, 02, 03] -> endpointIds=[01, 02, 03]
        case { contains it, [endpointInt:0x00, clusterInt:0x8005, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Active Endpoints Response", msg.data[1], msg)

            List<String> cmds = []
            List<String> endpointIds = []

            Integer count = Integer.parseInt(msg.data[4], 16)
            if (count > 0) {
                (1..count).each() { i ->
                    String endpointId = msg.data[4 + i]
                    endpointIds.add endpointId
                    
                    // Query simple descriptor data
                    cmds.add "he raw 0x${device.deviceNetworkId} 0x00 0x00 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} ${endpointId}} {0x0000}"
                }
                Utils.sendZigbeeCommands cmds
            }

            // Add "endpointIds" only if device exposes more then one
            if (count > 1) {
                Utils.dataValue "endpointIds", endpointIds.join(",")
            }
            return Utils.processedZdoMessage("Active Endpoints Response", "endpointIds=${endpointIds}")

        // Simple_Desc_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:Length, 08:Endpoint, 16:ApplicationProfileIdentifier, 16:ApplicationDeviceIdentifier, 08:Reserved, 16:InClusterCount, n*16:InClusterList, 16:OutClusterCount, n*16:OutClusterList }
        // Example: [B7, 00, 18, 4A, 14, 03, 04, 01, 06, 00, 01, 03, 00,  00, 03, 00, 80, FC, 03, 03, 00, 04, 00, 80, FC] -> endpointId=03, inClusters=[0000, 0003, FC80], outClusters=[0003, 0004, FC80]
        case { contains it, [endpointInt:0x00, clusterInt:0x8004, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Simple Descriptor Response", msg.data[1], msg)

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

            Utils.dataValue "inClusters (${endpointId})", inClusters.join(",")
            Utils.dataValue "outClusters (${endpointId})", outClusters.join(",")
            return Utils.processedZdoMessage("Simple Descriptor Response", "endpointId=${endpointId}, inClusters=${inClusters}, outClusters=${outClusters}")

        // IEEE_addr_rsp := { 08:Status, 64:IEEEAddrRemoteDev, 16:NWKAddrRemoteDev, ... }
        // Example: [84, 00, C6, 9C, FE, FE, FF, F9, E3, B4, E3, 1F] Status=SUCCESS, IEEEAddrRemoteDev=B4E3F9FFFEFE9CC6, NWKAddrRemoteDev=1FE3
        case { contains it, [endpointInt:0x00, clusterInt:0x8001, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("IEEE Address Response", msg.data[1], msg)
            String zigbeeId = msg.data[2..9].reverse().join()
            String networkId = msg.data[10..11].reverse().join()
            return Utils.processedZdoMessage("IEEE Address Response", "zigbeeId=${zigbeeId}, networkId=${networkId}")

        // Mgmt_NWK_Update_notify := { 08:Status, 32:ScannedChannels, 16:TotalTransmissions, 16:TransmissionFailures, 08:ScannedChannelsListCount, n*08:EnergyValues }
        // Example: [00, 00, 00, F8, FF, 07,  1C, 00,  09, 00,  10,  BC, B1, C8, D0, E2, BD, CD, B2, AC, BD, AD, CE, B8, B3, AC, C0]
        // Example: [00, 00, 00, F8, FF, 07, 23, 00, 09, 00, 10, A2, AB, BD, CF, C9, BA, CE, AC, B0, CF, B8, C4, B6, AA, B5, B6]
        // Status=SUCCESS, IEEEAddrRemoteDev=B4E3F9FFFEFE9CC6, NWKAddrRemoteDev=1FE3
        case { contains it, [endpointInt:0x00, clusterInt:0x8038, commandInt:0x00] }:
            if (msg.data[1] != "00") return Utils.failedZdoMessage("Network Update Notify", msg.data[1], msg)
            String scannedChannels = msg.data[2..5].collect { Integer.toBinaryString(Integer.parseInt(it, 16)) }.join()
            Integer totalTransmissions = Integer.parseInt(msg.data[6..7].reverse().join(), 16)
            Integer transmissionFailures = Integer.parseInt(msg.data[8..9].reverse().join(), 16)
            Integer scannedChannelsListCount = Integer.parseInt(msg.data[10], 16)
            List<Integer> energyValues = msg.data[11..msg.data.size()-1].collect { Integer.parseInt(it, 16) }
            return Utils.processedZdoMessage("Network Update Notify", "scannedChannels=${scannedChannels}, totalTransmissions=${totalTransmissions}, transmissionFailures=${transmissionFailures}, energyValues=${energyValues}")

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
    error: { message -> log.error "${device.displayName} ${message.uncapitalize()}" },
]

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

@Field def Utils = [
    sendZigbeeCommands: { List<String> cmds ->
        List<String> send = delayBetween(cmds.findAll { !it.startsWith("delay") }, 2000)
        Log.debug "◀ Sending Zigbee messages: ${send}"
        state.lastTx = now()
        sendHubCommand new hubitat.device.HubMultiAction(send, hubitat.device.Protocol.ZIGBEE)
    },

    sendEvent: { Map event ->
        if (device.currentValue(event.name, true) != event.value || event.isStateChange) {
            Log.info "${event.descriptionText} [${event.type}]"
        } else {
            Log.debug "${event.descriptionText} [${event.type}]"
        }
        sendEvent event
    },

    dataValue: { String key, String value ->
        Log.debug "Update data value: ${key}=${value}"
        updateDataValue key, value
    },

    zigbeeDataValue: { Integer attrInt, String value ->
        switch (attrInt) {
            case 0x0001: return Utils.dataValue("application", value)
            case 0x0003: return Utils.dataValue("hwVersion", value)
            case 0x0004: return Utils.dataValue("manufacturer", value)
            case 0x000A: return Utils.dataValue("type", "${!value ? "" : (value.split("") as List).collate(2).collect { "${Integer.parseInt(it.join(), 16) as char}" }.join()}")
            case 0x0005: return Utils.dataValue("model", value)
            case 0x4000: return Utils.dataValue("softwareBuild", value)
        }
    },

    processedZclMessage: { String type, String details ->
        Log.debug "▶ Processed ZCL message: type=${type}, status=SUCCESS, ${details}"
    },

    processedZdoMessage: { String type, String details ->
        Log.debug "▶ Processed ZDO message: type=${type}, status=SUCCESS, ${details}"
    },

    failedZclMessage: { String type, String status, Map msg ->
        Log.warn "▶ Received ZCL message: type=${type}, status=${ZCL_STATUS[status]}, data=${msg.data}"
    },

    failedZdoMessage: { String type, String status, Map msg ->
        Log.warn "▶ Received ZDO message: type=${type}, status=${ZDP_STATUS[status]}, data=${msg.data}"
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

// Call refresh() if available
private tryToRefresh() {
    try { refresh() } catch(e) {}
}

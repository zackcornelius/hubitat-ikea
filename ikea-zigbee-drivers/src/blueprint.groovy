/**
 * {{ device.model }} ({{ device.type }})
 *
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 * @see https://zigbee.blakadder.com/Ikea_{{ device.type }}.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 */

import groovy.time.TimeCategory
import groovy.transform.Field

@Field def DRIVER_NAME = "{{ device.model }} ({{ device.type }})"
@Field def DRIVER_VERSION = "{{ driver.version }}"
@Field def ZDP_STATUS = ["00":"SUCCESS", "80":"INV_REQUESTTYPE", "81":"DEVICE_NOT_FOUND", "82":"INVALID_EP", "83":"NOT_ACTIVE", "84":"NOT_SUPPORTED", "85":"TIMEOUT", "86":"NO_MATCH", "88":"NO_ENTRY", "89":"NO_DESCRIPTOR", "8A":"INSUFFICIENT_SPACE", "8B":"NOT_PERMITTED", "8C":"TABLE_FULL", "8D":"NOT_AUTHORIZED", "8E":"DEVICE_BINDING_TABLE_FULL"]
{{# device.buttons }}
@Field def BUTTONS = [
    {{# device.buttons.list }}
    "{{ id }}": ["{{ number }}", "{{ name }}"],
    {{/ device.buttons.list }}
]
{{/ device.buttons }}
{{# device.capabilities.HealthCheck }}

// Health Check config
@Field def HEALTH_CHECK = [
    "schedule"   : "{{ schedule }}", // Health will be checked using this cron schedule
    "thereshold" : {{ thereshold }}              // When checking, mark the device as offline if no Zigbee message was received in the last {{ thereshold }} seconds
]

{{/ device.capabilities.HealthCheck }}
metadata {
    definition(name:DRIVER_NAME, namespace:"{{ driver.namespace }}", author:"{{ driver.author }}", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/{{ device.type }}.groovy") {
        {{# device.capabilities.Battery }}
        capability "Battery"
        {{/ device.capabilities.Battery }}
        capability "Configuration"
        {{# device.capabilities.DoubleTapableButton }}
        capability "DoubleTapableButton"
        {{/ device.capabilities.DoubleTapableButton }}
        {{# device.capabilities.HealthCheck }}
        capability "HealthCheck"
        {{/ device.capabilities.HealthCheck }}
        {{# device.capabilities.HoldableButton }}
        capability "HoldableButton"
        {{/ device.capabilities.HoldableButton }}
        {{# device.capabilities.MotionSensor }}
        capability "MotionSensor"
        {{/ device.capabilities.MotionSensor }}
        {{# device.capabilities.Outlet }}
        capability "Outlet"
        {{/ device.capabilities.Outlet }}
        {{# device.capabilities.PowerSource }}
        capability "PowerSource"
        {{/ device.capabilities.PowerSource }}
        {{# device.capabilities.PushableButton }}
        capability "PushableButton"
        {{/ device.capabilities.PushableButton }}
        {{# device.capabilities.Refresh }}
        capability "Refresh"
        {{/ device.capabilities.Refresh }}
        {{# device.capabilities.ReleasableButton }}
        capability "ReleasableButton"
        {{/ device.capabilities.ReleasableButton }}
        {{# device.capabilities.Switch }}
        capability "Switch"
        {{/ device.capabilities.Switch }}
        {{# device.capabilities.SwitchLevel }}
        capability "SwitchLevel"
        {{/ device.capabilities.SwitchLevel }}
        {{# zigbee.fingerprints }}

        // For firmwares: {{ firmwares }}
        {{{ value }}}
        {{/ zigbee.fingerprints }}
        {{# device.capabilities.HealthCheck }}

        // Should be part of capability.HealthCheck
        attribute "healthStatus", "ENUM", ["offline", "online", "unknown"]
        {{/ device.capabilities.HealthCheck }}
        {{# device.capabilities.ZigbeeRouter }}

        // Attributes for capability.ZigbeeRouter
        attribute "neighbors", "STRING"
        attribute "routes", "STRING"
        {{/ device.capabilities.ZigbeeRouter }}
    }
    {{# device.capabilities.ZigbeeRouter }}

    // Commands for capability.ZigbeeRouter
    command "requestRoutingData"
    {{/ device.capabilities.ZigbeeRouter }}

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
        {{# device.capabilities.SwitchLevel }}
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
        {{/ device.capabilities.SwitchLevel }}
        {{# device.capabilities.StartupOnOff }}
        input(
            name: "startupOnOff",
            type: "enum",
            title: "Behavior after a power outage",
            options: ["ON":"Turn power On", "OFF":"Turn power Off", "PREV":"Restore previous state"],
            defaultValue: "PREV",
            required: true
        )
        {{ /device.capabilities.StartupOnOff }}
    }
}

// ===================================================================================================================
// Implement default methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    Log.info "Installing Zigbee device...."
    {{# device.capabilities.Battery }}
    Log.warn "[IMPORTANT] Make sure that you keep your IKEA device as close as you can to your Hubitat hub until the LED stops blinking. Otherwise it will successfully pair but it won't work properly!"
    {{/ device.capabilities.Battery }}
}

// Called when the "Save Preferences" button is clicked
def updated() {
    Log.info "Saving preferences..."
    Log.info "🛠️ logLevel = ${logLevel}"
    {{# device.capabilities.SwitchLevel }}
    Log.info "🛠️ levelChange = ${levelChange}%"
    minLevel = minLevel.toDouble().trunc().toInteger()
    device.clearSetting "minLevel"
    device.removeSetting "minLevel"
    device.updateSetting "minLevel", minLevel
    Log.info "🛠️ minLevel = ${minLevel}"
    {{/ device.capabilities.SwitchLevel }}

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    {{# device.capabilities.HealthCheck }}
    schedule HEALTH_CHECK.schedule, "healthCheck"
    {{/ device.capabilities.HealthCheck }}
    {{# device.capabilities.StartupOnOff }}

    // Configure StartupOnOff
    Log.info "🛠️ startupOnOff = ${startupOnOff}"
    Utils.sendZigbeeCommands zigbee.writeAttribute(0x0006, 0x4003, 0x30, startupOnOff == "OFF" ? 0x00 : (startupOnOff == "ON" ? 0x01 : 0xFF))
    {{/ device.capabilities.StartupOnOff }}
}

// Handler method for scheduled job to disable debug logging
def logsOff() {
   Log.info '⏲️ Automatically reverting log level to "Info"'
   device.clearSetting "logLevel"
   device.removeSetting "logLevel"
   device.updateSetting "logLevel", "2"
}
{{# device.capabilities.HealthCheck }}

// Handler method for scheduled job to check health status
def healthCheck() {
   Log.debug '⏲️ Automatically running health check'
    def healthStatus = state?.lastRx == 0 ? "unknown" : (now() - state.lastRx < HEALTH_CHECK.thereshold * 1000 ? "online" : "offline")
    if (device.currentValue("healthStatus") != healthStatus) {
        Utils.sendDigitalEvent name:"healthStatus", value:healthStatus, descriptionText:"Health status changed to ${healthStatus}"
    }
}
{{/ device.capabilities.HealthCheck }}

// ===================================================================================================================
// Implement Hubitat Capabilities
// ===================================================================================================================

// capability.Configuration
// Note: This method is also called when the device is initially installed
def configure() {
    Log.info "Configuring device..."
    {{# device.capabilities.Battery }}
    {{# device.capabilities.PushableButton }}
    Log.debug '[IMPORTANT] Click the "Configure" button immediately after pushing any button on the remote so that the Zigbee messages we send during configuration will reach the device before it goes to sleep!'
    {{/ device.capabilities.PushableButton }}
    {{/ device.capabilities.Battery }}

    // Advertise driver name and value
    updateDataValue "driverName", DRIVER_NAME
    updateDataValue "driverVersion", DRIVER_VERSION

    // Apply preferences first
    updated()

    // Clear state
    state.clear()
    state.lastRx = 0
    state.lastTx = 0
    {{# device.capabilities.PushableButton }}

    // capability.PushableButton
    push 0
    {{# device.buttons }}
    def numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons set to ${numberOfButtons}"
    {{/ device.buttons }}
    {{/ device.capabilities.PushableButton }}
    {{# device.capabilities.DoubleTapableButton }}

    // capability.DoubleTapableButton
    doubleTap 0
    {{/ device.capabilities.DoubleTapableButton }}
    {{# device.capabilities.HoldableButton }}

    // capability.HoldableButton
    hold 0
    {{/ device.capabilities.HoldableButton }}
    {{# device.capabilities.ReleasableButton }}

    // capability.ReleasableButton
    release 0
    {{/ device.capabilities.ReleasableButton }}
    {{# device.capabilities.Switch }}

    // capability.Switch
    on()
    {{/ device.capabilities.Switch }}
    {{# device.capabilities.SwitchLevel }}

    // capability.SwitchLevel
    setLevel 5
    {{/ device.capabilities.SwitchLevel }}
    {{# device.capabilities.PowerSource }}

    // capability.PowerSource
    {{# battery }}
    sendEvent name:"powerSource", value:"battery", descriptionText:"Power source set to battery"
    {{/ battery }}
    {{# mains }}
    sendEvent name:"powerSource", value:"mains", descriptionText:"Power source set to mains"
    {{/ mains }}
    {{/ device.capabilities.PowerSource }}
    {{# device.capabilities.HealthCheck }}

    // capability.HealthCheck
    sendEvent name:"healthStatus", value:"unknown", descriptionText:"Health status set to unknown"
    sendEvent name:"checkInterval", value:{{ checkInterval }}, descriptionText:"Health check interval set to {{ checkInterval }} seconds"
    {{/ device.capabilities.HealthCheck }}

    List<String> cmds = []

    // Configure Zigbee reporting
    {{# zigbee.reporting }}
    cmds.addAll zigbee.configureReporting({{ cluster }}, {{ attribute }}, {{ type }}, {{ min }}, {{ max }}, {{ delta }}) // {{ reason }}
    {{/ zigbee.reporting }}
    {{^ zigbee.reporting }}
    // -- No reporting needed
    {{/ zigbee.reporting }}

    // Add Zigbee binds
    {{# zigbee.binds }}
    cmds.add "zdo bind 0x${device.deviceNetworkId} {{ endpoint }} 0x01 {{ cluster }} {${device.zigbeeId}} {}" // {{ reason }}
    {{/ zigbee.binds }}
    {{^ zigbee.binds }}
    // -- No binds needed
    {{/ zigbee.binds }}

    // Query Zigbee attributes
    cmds.addAll zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds.addAll zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds.addAll zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds.addAll zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    {{# device.capabilities.Battery }}
    cmds.addAll zigbee.readAttribute(0x0001, 0x0021)  // BatteryPercentage
    {{/ device.capabilities.Battery }}

    // Query all active endpoints
    cmds.add "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"

    Utils.sendZigbeeCommands cmds
}
{{# device.capabilities.DoubleTapableButton }}

// capability.DoubleTapableButton
def doubleTap(buttonNumber) {
    Utils.sendDigitalEvent name:"doubleTapped", value:buttonNumber, descriptionText:"Button ${buttonNumber} was double tapped"
}
{{/ device.capabilities.DoubleTapableButton }}
{{# device.capabilities.HealthCheck }}

// capability.HealthCheck
def ping() {
    {{# device.capabilities.PowerSource.mains }}
    // Request the device to send the value for the OnOff attribute
    Log.info "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
    runIn 5, "pingExecute"
    {{/ device.capabilities.PowerSource.mains }}
    {{# device.capabilities.PowerSource.battery }}
    pingExecute()
    {{/ device.capabilities.PowerSource.battery }}
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
{{/ device.capabilities.HealthCheck }}
{{# device.capabilities.HoldableButton }}

// capability.HoldableButton
def hold(buttonNumber) {
    Utils.sendDigitalEvent name:"held", value:buttonNumber, descriptionText:"Button ${buttonNumber} was held"
}
{{/ device.capabilities.HoldableButton }}
{{# device.capabilities.Outlet }}

// capability.Outlet
def on() {
    Log.debug "Sending on command"
    Utils.sendZigbeeCommands(zigbee.on())
}
def off() {
    Log.debug "Sending off command"
    Utils.sendZigbeeCommands(zigbee.off())
}
{{/ device.capabilities.Outlet }}
{{# device.capabilities.PushableButton }}

// capability.PushableButton
def push(buttonNumber) {
    Utils.sendDigitalEvent name:"pushed", value:buttonNumber, descriptionText:"Button ${buttonNumber} was pressed"
}
{{/ device.capabilities.PushableButton }}
{{# device.capabilities.Refresh }}

// capability.Refresh
def refresh() {
    List<String> cmds = [];

    {{# readAttributes }}
    cmds += zigbee.readAttribute({{ cluster }}, {{ attr }})  // {{ description }}
    {{/ readAttributes }}

    Utils.sendZigbeeCommands(cmds)
}
{{/ device.capabilities.Refresh }}
{{# device.capabilities.ReleasableButton }}

// capability.ReleasableButton
def release(buttonNumber) {
    Utils.sendDigitalEvent name:"released", value:buttonNumber, descriptionText:"Button ${buttonNumber} was released"
}
{{/ device.capabilities.ReleasableButton }}
{{# device.capabilities.Switch }}

// capability.Switch
def on() {
    Utils.sendDigitalEvent name:"switch", value:"on", descriptionText:"Was turned on"
}
def off() {
    Utils.sendDigitalEvent name:"switch", value:"off", descriptionText:"Was turned off"
}
{{/ device.capabilities.Switch }}
{{# device.capabilities.SwitchLevel }}

// capability.SwitchLevel
def setLevel(level, duration = 0) {
    def newLevel = level < 0 ? 0 : (level > 100 ? 100 : level)
    Utils.sendDigitalEvent name:"level", value:newLevel, unit:"%", descriptionText:"Level was set to ${newLevel}%"
}
{{/ device.capabilities.SwitchLevel }}
{{# device.capabilities.ZigbeeRouter }}

// capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send the Neighbors Table and the Routing Table data ..."
    List<String> cmds = []

    // Ask for the Neighbors Table and the Routing Table
    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0031 {00} {0x00}"
    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0032 {00} {0x00}"
    Utils.sendZigbeeCommands cmds
}
{{/ device.capabilities.ZigbeeRouter }}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap description
    Log.debug "description=[${description}]"
    Log.debug "msg=[${msg}]"
    state.lastRx = now()
    {{# device.capabilities.HealthCheck }}

    // Update health status
    if (device.currentValue("healthStatus") != "online") {
        Utils.sendDigitalEvent name:"healthStatus", value:"online", descriptionText:"Health status changed to online"
    }
    {{/ device.capabilities.HealthCheck }}

    // Extract cluster and command from message
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle device specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        {{# zigbee.messages.handle }}

        {{ > file }}
        {{/ zigbee.messages.handle }}

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
                    if (msg.value == "{{ device.zigbeeId }}") updateDataValue "type", "{{ device.type }}"
                    return Utils.zigbeeDataValue("model", msg.value)
                case 0x4000: return Utils.zigbeeDataValue("softwareBuild", msg.value)
            }
            return Log.warn("Unexpected Zigbee attribute: cluster=0x${msg.cluster}, attribute=0x${msg.attrId}, msg=${msg}")

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
        case { contains it, [clusterInt:0x0013, commandInt:0x00] }:
            def addr = msg.data[1..2].reverse().join()
            def zigbeeId = msg.data[3..10].reverse().join()
            def capabilities = Integer.toBinaryString(Integer.parseInt(msg.data[11], 16))
            {{# device.capabilities.Refresh }}

            // Welcome back; let's sync state
            Log.debug("Device rejoined the network. Calling refresh() to sync state ...")
            refresh()
            {{/ device.capabilities.Refresh }}
            return Utils.processedZigbeeMessage("Device Announce Response", "addr=${addr}, zigbeeId=${zigbeeId}, capabilities=${capabilities}")

        // Bind_rsp = { 08:Status }
        // Success example : [26, 00] -> status = SUCCESS
        // Fail example    : [26, 82] -> status = INVALID_EP
        case { contains it, [clusterInt:0x8021] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Bind Response", msg)
            }
            return Utils.processedZigbeeMessage("Bind Response", "data=${msg.data}")
        {{# device.capabilities.ZigbeeRouter }}

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
        {{/ device.capabilities.ZigbeeRouter }}

        // ---------------------------------------------------------------------------------------------------------------
        // Ignored Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        {{# zigbee.messages.ignore }}

        case { contains it, [clusterInt:{{ cluster }}{{# command }}, commandInt:{{ command }}{{/ command }}] }:
            return Utils.ignoredZigbeeMessage("{{ name }}", msg)
        {{/ zigbee.messages.ignore }}

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

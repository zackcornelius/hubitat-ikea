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
{{# device.capabilities }}
{{> file@fields }}
{{/ device.capabilities }}

metadata {
    definition(name:DRIVER_NAME, namespace:"{{ driver.namespace }}", author:"{{ driver.author }}", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/{{ device.type }}.groovy") {
        capability "Configuration"
        {{# device.capabilities }}
        {{> file@definition }}
        {{/ device.capabilities }}
        {{# zigbee.fingerprints }}

        // For firmwares: {{ firmwares }}
        {{{ value }}}
        {{/ zigbee.fingerprints }}
        {{# device.capabilities }}
        {{> file@attributes }}
        {{/ device.capabilities }}
    }
    {{# device.capabilities }}
    {{> file@commands }}
    {{/ device.capabilities }}

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
        {{# device.capabilities }}
        {{> file@inputs }}
        {{/ device.capabilities }}
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

    unschedule()
    if (logLevel == "1") runIn 1800, "logsOff"
    Log.info "🛠️ logLevel = ${logLevel}"
    {{# device.capabilities }}
    {{> file@updated }}
    {{/ device.capabilities }}
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
{{# device.capabilities }}
{{> file@helpers }}
{{/ device.capabilities }}

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
    {{# zigbee.reporting }}
    cmds += zigbee.configureReporting({{ cluster }}, {{ attribute }}, {{ type }}, {{ min }}, {{ max }}, {{ delta }}) // {{ reason }}
    {{/ zigbee.reporting }}
    {{^ zigbee.reporting }}
    // -- No reporting needed
    {{/ zigbee.reporting }}

    // Add Zigbee binds
    {{# zigbee.binds }}
    cmds += "zdo bind 0x${device.deviceNetworkId} {{ endpoint }} 0x01 {{ cluster }} {${device.zigbeeId}} {}" // {{ reason }}
    {{/ zigbee.binds }}
    {{^ zigbee.binds }}
    // -- No binds needed
    {{/ zigbee.binds }}

    // Query Zigbee attributes
    cmds += zigbee.readAttribute(0x0000, 0x0001)  // ApplicationVersion
    cmds += zigbee.readAttribute(0x0000, 0x0003)  // HWVersion
    cmds += zigbee.readAttribute(0x0000, 0x0004)  // ManufacturerName
    cmds += zigbee.readAttribute(0x0000, 0x0005)  // ModelIdentifier
    cmds += zigbee.readAttribute(0x0000, 0x4000)  // SWBuildID
    {{# device.capabilities }}
    {{> file@configure }}
    {{/ device.capabilities }}

    // Query all active endpoints
    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"
    Utils.sendZigbeeCommands cmds
}
{{# device.capabilities }}
{{> file@implementation }}
{{/ device.capabilities }}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap description
    Log.debug "description=[${description}]"
    Log.debug "msg=[${msg}]"
    state.lastRx = now()
    {{# device.capabilities }}
    {{> file@parse }}
    {{/ device.capabilities }}

    // Extract cluster and command from message
    if (msg.clusterInt == null) msg.clusterInt = Integer.parseInt(msg.cluster, 16)
    msg.commandInt = Integer.parseInt(msg.command, 16)

    switch (msg) {

        // ---------------------------------------------------------------------------------------------------------------
        // Handle {{ device.type }} specific Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        {{# zigbee.messages.specific }}

        {{ > file }}
        {{/ zigbee.messages.specific }}

        // ---------------------------------------------------------------------------------------------------------------
        // Handle capabilities Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        {{# device.capabilities }}
        {{> file@events }}
        {{/ device.capabilities }}

        // ---------------------------------------------------------------------------------------------------------------
        // Handle standard Zigbee messages
        // ---------------------------------------------------------------------------------------------------------------
        {{# zigbee.messages.standard }}

        {{ > file }}
        {{/ zigbee.messages.standard }}

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

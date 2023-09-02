/**
 * IKEA Tradfri Remote Control (E1810) Driver
 * Ver: 1.1.0
 *
 * @see https://zigbee.blakadder.com/Ikea_E1810.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */

import groovy.transform.Field

@Field def BUTTONS = [
    "PLAY":  ["1", "Play"],
    "PLUS":  ["2", "Plus"],
    "MINUS": ["3", "Minus"],
    "NEXT":  ["4", "Next"],
    "PREV":  ["5", "Prev"]
]

metadata {
    definition(name: "IKEA Tradfri Remote Control (E1810)", namespace: "dandanache", author: "Dan Danache", importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/E1810.groovy") {
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
            options: ["1": "1 %", "2": "2 %", "5": "5 %", "10": "10 %", "20":"20 %", "25":"25 %"],
            defaultValue: "5",
            required: true
        )
        input(
            name: "logLevel",
            type: "enum",
            title: "Select log verbosity*",
            options: [
                "1": "Debug (log everything)",
                "2": "Info (log only important events)",
                "3": "Warning (log only events that require attention)",
                "4": "Error (log only errors)"
            ],
            defaultValue: "2",
            required: true
        )
    }
}

// ===================================================================================================================
// Driver required methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    info("Installing Zigbee device....")
    warn("IMPORTANT: Make sure that you keep the IKEA TRADFRI remote as close as you can to your Hubitat hub! Otherwise it will successfully pair but the buttons won't work.")
}

// Called when the "save Preferences" button is clicked
def updated() {
    info("Applying preferences...")
    info("🛠️ logLevel: ${logLevel}")
    info("🛠️ levelChange: ${levelChange}%")

    if (logLevel == "1") runIn(1800, "logsOff")
    else unschedule()
}

// Handler method for scheduled job to disable debug logging
def logsOff() {
   info('⏲️ Automatically reverting log level to "Info"')
   device.clearSetting("logLevel")
   device.removeSetting("logLevel")
   device.updateSetting("logLevel", "2")
}

// ===================================================================================================================
// Capabilities implementation
// ===================================================================================================================

// capability.configuration
// Note: also called when the device is initially installed
def configure() {
    info("Configuring device...")
    warn('IMPORTANT: Click the "Configure" button immediately after pushing any button on the remote so that the Zigbee messages we send during configuration will reach the device before it goes to sleep!')

    // Apply preferences first
    updated()

    // Clear state
    state.clear()

    // Set initial values for all attributes
    push(0)
    hold(0)
    release(0)
    on()
    setLevel(25)
    def numberOfButtons = BUTTONS.count{_ -> true}
    sendEvent(name: "numberOfButtons", value: numberOfButtons, descriptionText: "Number of buttons set to: ${numberOfButtons}")

    List<String> cmds = []

    // Configure reporting
    cmds.addAll(zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 86400, 172800, 0x00)) // Report battery percentage every 24-48 hours

    // Add binds
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0005 {${device.zigbeeId}} {}") // Generic - Scenes cluster
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}") // Generic - On/Off cluster
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}") // Generic - Level Control cluster

    // Query device attributes
    cmds.addAll(zigbee.readAttribute(0x0000, 0x0001))  // ApplicationVersion
    cmds.addAll(zigbee.readAttribute(0x0000, 0x0003))  // HWVersion
    cmds.addAll(zigbee.readAttribute(0x0000, 0x0004))  // ManufacturerName
    cmds.addAll(zigbee.readAttribute(0x0000, 0x0005))  // ModelIdentifier
    cmds.addAll(zigbee.readAttribute(0x0000, 0x4000))  // SWBuildID
    cmds.addAll(zigbee.readAttribute(0x0001, 0x0021))  // BatteryPercentage

    // Query simple descriptor data
    cmds.add("he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} 01} {0x0000}")
    sendZigbeeCommands(cmds)
}

// capability.pushableButton
def push(buttonNumber) {
    triggerUserEvent(name: "pushed", value: buttonNumber, descriptionText: "Button #${buttonNumber} was pressed")
}

// capability.holdableButton
def hold(buttonNumber) {
    triggerUserEvent(name: "held", value: buttonNumber, descriptionText: "Button #${buttonNumber} was held")
}

// capability.releasableButton
def release(buttonNumber) {
    triggerUserEvent(name: "released", value: buttonNumber, descriptionText: "Button #${buttonNumber} was released")
}

// capability.switch
def on() {
    triggerUserEvent(name: "switch", value: "on", descriptionText: "Switch is on")
}

// capability.switch
def off() {
    triggerUserEvent(name: "switch", value: "off", descriptionText: "Switch is off")
}

// capability.switchLevel
def setLevel(level, duration = 0) {
    def newLevel = newLevel < 0 ? 0 : (newLevel > 100 ? 100 : newLevel)
    triggerUserEvent(name: "level", value: newLevel, unit: "%", descriptionText: "Level changed to: ${newLevel}")
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    def msg = zigbee.parseDescriptionAsMap(description)
    
    // Extract cluster from message
    def clusterId = msg.clusterInt;
    if (clusterId == null) clusterId = Integer.parseInt(msg.cluster, 16)
    
    // General::Basic cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0000) {
        if (msg.attrInt == 0x0001) {
            zigbeeDebug("ApplicationVersion: ${msg.value}")
            updateDataValue("application", "${msg.value}")
            return
        }

        if (msg.attrInt == 0x0003) {
            zigbeeDebug("HWVersion: ${msg.value}")
            updateDataValue("hwVersion", "${msg.value}")
            return
        }
        
        if (msg.attrInt == 0x0004) {
            zigbeeDebug("ManufacturerName: ${msg.value}")
            updateDataValue("manufacturer", "${msg.value}")
            return
        }

        if (msg.attrInt == 0x0005) {
            zigbeeDebug("ModelIdentifier: ${msg.value}")
            updateDataValue("model", "${msg.value}")
            if (msg.value == "TRADFRI remote control") {
                updateDataValue("type", "E1810")
            }
            return
        }

        if (msg.attrInt == 0x4000) {
            zigbeeDebug("SWBuildID: ${msg.value}")
            updateDataValue("softwareBuild", "${msg.value}")
            return
        }
    }

    // General::Power cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0001) {
        
        // BatteryPercentage
        if (msg.attrInt == 0x0021) {
            def percentage = Integer.parseInt(msg.value, 16)
            
            // On later firmware versions; battery is reported with a double value, upto "200" (but why?)
            if (getDataValue("softwareBuild") != null && getDataValue("softwareBuild").value != "1.0.012") {
                percentage = Math.round(percentage / 2);
            }

            return triggerZigbeeEvent(name: "battery", value: percentage, unit: "%", descriptionText: "Battery is ${percentage}% full")
        }

        // Configuration?
        if (msg.command == "07") {
            if (msg.data[0] == "00") zigbeeDebug("Device successfully processed the configuration")
            else zigbeeDebug("Device may not have processed the configuration correctly")
            return
        }
    }

    // General::Identify cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0003) {
        if (msg.command == "01") {
            return zigbeeDebug("Identify Query Command: ${msg}")
        }
    }

    // General::Scenes cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0005) {

        // Next / Prev button was pushed
        if (msg.command == "07") {
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
        }
        
        // Next / Prev button was held
        if (msg.command == "08") {
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return triggerZigbeeEvent(name: "held", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was held")
        }
        
        // Next / Prev button was released
        if (msg.command == "09") {
            //def button = I'm not smart enough to figure it out how to determine button number from msd.data!
            //return triggerZigbeeEvent(name: "released", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was released")
            return
        }
    }
    
    // General::On/Off cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0006) {

        // "Off" and "On" command
        if (msg.command == "00" || msg.command == "01") {
            def newState = msg.command == "00" ? "off" : "on"
            return triggerZigbeeEvent(name: "switch", value: newState, descriptionText: "Switch is ${newState}")
        }
        
        // "Toggle" command
        if (msg.command == "02") {
            def button = BUTTONS.PLAY
            triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
            
            // Also act as a switch
            def newState = device.currentState("switch") != null && device.currentState("switch").value != "on" ? "on" : "off"
            return triggerZigbeeEvent(name: "switch", value: newState, descriptionText: "Switch is ${newState}")
        }
    }

    // General::Level Control cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0008) {

        // Plus / Minus button was pushed
        if (msg.command == "02" || msg.command == "06") {
            def button = msg.command == "02" ? BUTTONS.MINUS : BUTTONS.PLUS
            triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
            
            def curLevel = device.currentState("level") != null ? Integer.parseInt(device.currentState("level").value) : 25
            def newLevel = curLevel + (button == BUTTONS.PLUS ? Integer.parseInt(levelChange) : 0 - Integer.parseInt(levelChange))
            newLevel = newLevel < 0 ? 0 : (newLevel > 100 ? 100 : newLevel)
            if (curLevel != newLevel) triggerZigbeeEvent(name: "level", value: newLevel, descriptionText: "Level changed to: ${newLevel}")
            return
        }
        
        // Plus / Minus button was held
        if (msg.command == "01" || msg.command == "05") {
            def button = msg.command == "01" ? BUTTONS.MINUS : BUTTONS.PLUS
            return triggerZigbeeEvent(name: "held", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was held")
        }

        // Plus / Minus button was released
        if (msg.command == "03" || msg.command == "07") {
            def button = msg.command == "03" ? BUTTONS.MINUS : BUTTONS.PLUS
            return triggerZigbeeEvent(name: "held", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was released")
        }
    }

    // Undocumented cluster 
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0013) {
        return zigbeeDebug("Device Announce Broadcast: ${msg.data}")
    }
    
    // Undocumented cluster 
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x8005) {
        if (msg.command == "00") {
            return zigbeeDebug("Active Endpoints Response: endpoints count=(${Integer.parseInt(msg.data[5], 16)}): ${msg.data}")
        }
    }
    
    // Undocumented cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x8021) {
        if (msg.data[1] == "00") {
            return zigbeeDebug("Bind response with status:SUCCESS transaction:${msg.data[0]}")
        }

        if (msg.data[1] == "8C") {
            return zigbeeDebug("Bind response with status:TABLE_FULL transaction:${msg.data[0]}")
        }
    }

    // Undocumented cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x8004) {
        
        // Process the simple descriptors normally received from Zigbee Cluster 8004 into device data values
        // Copy-paste from https://github.com/birdslikewires/hubitat
        if (msg.data[1] == "00") {
            updateDataValue("profileId", msg.data[6..7].reverse().join())

            Integer inClusterNum = Integer.parseInt(msg.data[11], 16)
            Integer position = 12
            Integer positionCounter = null
            String inClusters = ""
            if (inClusterNum > 0) {
                (1..inClusterNum).each() {b->
                    positionCounter = position+((b-1)*2)
                    inClusters += msg.data[positionCounter..positionCounter+1].reverse().join()
                    if (b < inClusterNum) {
                        inClusters += ","
                    }
                }
            }
            position += inClusterNum*2
            Integer outClusterNum = Integer.parseInt(msg.data[position], 16)
            position += 1
            String outClusters = ""
            if (outClusterNum > 0) {
                (1..outClusterNum).each() {b->
                    positionCounter = position+((b-1)*2)
                    outClusters += msg.data[positionCounter..positionCounter+1].reverse().join()
                    if (b < outClusterNum) {
                        outClusters += ","
                    }
                }
            }

            updateDataValue("inClusters", inClusters)
            updateDataValue("outClusters", outClusters)

            zigbeeDebug("inClusters: $inClusters")
            zigbeeDebug("outClusters: $outClusters")
            return
        }
    }
    
    // Undocumented cluster
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x8034) {
        return zigbeeDebug("Leave Response: ${msg.data}")
    }
   
    // Unhandled Zigbee message
    warn("🔽 Received unknown Zigbee message: ${description}, msg: ${msg}")
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

void zigbeeDebug(message) {
    debug("🔽 Received Zigbee message: ${message}")
}

void debug(message) {
    if (logLevel == "1") log.debug(message)
}

void info(message) {
    if (logLevel <= "2") log.info(message)
}

void warn(message) {
    if (logLevel <= "3") log.warn(message)
}

void error(message) {
    log.error(message)
}

// ===================================================================================================================
// Helper methods
// ===================================================================================================================

void sendZigbeeCommands(List<String> cmds) {
    info("🔼 Sending Zigbee messages: ${cmds}")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

void triggerZigbeeEvent(event) {
    info("🔽 Zigbee event: ${event.descriptionText}")
    sendEvent(event + [isStateChange: true, type: "physical"])
}

void triggerUserEvent(event) {
    info("👆 User event: ${event.descriptionText}")
    sendEvent(event + [isStateChange: true, type: "digital"])
}

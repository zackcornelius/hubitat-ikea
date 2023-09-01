/**
 * IKEA Symfonisk Sound Remote (E2123) Driver
 *
 * @see https://www.ikea.com/us/en/p/symfonisk-sound-remote-gen-2-30527312/
 * @see https://zigbee.blakadder.com/Ikea_E2123.html
 * @see https://ww8.ikea.com/ikeahomesmart/releasenotes/releasenotes.html
 * @see https://dan-danache.github.io/hubitat/ikea-zigbee-drivers/
 */

import groovy.transform.Field

@Field def BUTTONS = [
    "PLAY":  ["1", "Play"],
    "PLUS":  ["2", "Plus"],
    "MINUS": ["3", "Minus"],
    "NEXT":  ["4", "Next"],
    "PREV":  ["5", "Prev"],
    "DOT_1": ["6", "One Dot"],
    "DOT_2": ["7", "Two Dots"]
]

metadata {
    definition(name: "IKEA Symfonisk Sound Remote E2123", namespace: "dandanache", author: "Dan Danache", importUrl: "TODO") {
        capability "Configuration"
        capability "Battery"
        capability "PushableButton"
        capability "DoubleTapableButton"
        capability "HoldableButton"
        capability "Switch"
        capability "SwitchLevel"

        // For remotes with firmware 1.0.012 (20211214)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC57", outClusters:"0003,0004,0006,0008,0019,1000,FC7F", model:"SYMFONISK sound remote gen2", manufacturer:"IKEA of Sweden"
        
        // For remotes with firmware 1.0.35 (20230411)
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,1000,FC7C", outClusters:"0003,0004,0006,0008,0019,1000", model:"SYMFONISK sound remote gen2", manufacturer:"IKEA of Sweden"
    }

    preferences {
        input(
            name: "levelChange",
            type: "enum",
            title: "Level change when Plus or Minus is pushed",
            defaultValue: 5,
            options: [1:"1 %", 5:"5 %", 10:"10 %", 20:"20 %", 25:"25 %"]
        )
        input(
            name: "logLevel",
            type: "enum",
            title: "Select log verbosity",
            options: [1:"Debug", 2:"Info", 3:"Warning", 4:"Error"], defaultValue: 1
        )
    }
}

// ===================================================================================================================
// Driver required methods
// ===================================================================================================================

// Called when the device is first added
def installed() {
    info("Installing Zigbee device....")
    warn("IMPORTANT: Make sure that you keep the IKEA SYMFONISK remote as close as you can to your Hubitat hub! Otherwise it will successfully pair but the buttons won't work.")
}

// Called when the "save Preferences" button is clicked
def updated() {
    info("Applying preferences...")
    info("🛠️ Preferences :: logLevel: ${logLevel}")
    info("🛠️ Preferences :: levelChange: ${levelChange}%")

    if (logLevel == "1") runIn(1800, "logsOff")
    else unschedule()
}

// Handler method for scheduled job to disable debug logging
def logsOff() {
   log.warn('Automatically reverting log level to "Info"')
   device.updateSetting("logLevel", [value: 2, type: "number"])
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
    doubleTap(0)
    hold(0)
    on()
    setLevel(25)
    sendEvent(name: "numberOfButtons", value: 7, descriptionText: "Number of buttons set to 7")

    List<String> cmds = []

    // Configure reporting
    cmds.addAll(zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 86400, 172800, 0x00)) // Report battery percentage every 24-48 hours

    // Add binds
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}") // Generic - On/Off
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}") // Generic - Level Control
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0xFC7F {${device.zigbeeId}} {}") // 64639 --> For remotes with firmware 1.0.012 (20211214)
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0xFC80 {${device.zigbeeId}} {}") // Heiman - Specific Scenes --> For remotes with firmware 1.0.35 (20230411)
    cmds.add("zdo bind 0x${device.deviceNetworkId} 0x03 0x01 0xFC80 {${device.zigbeeId}} {}") // Heiman - Specific Scenes --> For remotes with firmware 1.0.35 (20230411)

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

// capability.doubleTapableButton
def doubleTap(buttonNumber) {
    triggerUserEvent(name: "doubleTapped", value: buttonNumber, descriptionText: "Button #${buttonNumber} was double tapped")
}

// capability.holdableButton
def hold(buttonNumber) {
    triggerUserEvent(name: "held", value: buttonNumber, descriptionText: "Button #${buttonNumber} was held")
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
            if (msg.value == "SYMFONISK sound remote gen2") {
                updateDataValue("type", "E2123")
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
            
            // On later firmware versions; battery is reported with a double value (but why?)
            if (getDataValue("softwareBuild") != "1.0.012") {
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
        if (msg.command == "01" || msg.command == "05") {
            def button = msg.data[0] == "00" ? BUTTONS.PLUS : BUTTONS.MINUS
            triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
            
            def curLevel = device.currentState("level") != null ? Integer.parseInt(device.currentState("level").value) : 25;
            def newLevel = curLevel + (button == BUTTONS.PLUS ? Integer.parseInt(levelChange) : 0 - Integer.parseInt(levelChange))
            newLevel = newLevel < 0 ? 0 : (newLevel > 100 ? 100 : newLevel)
            if (curLevel != newLevel) triggerZigbeeEvent(name: "level", value: newLevel, descriptionText: "Level changed to: ${newLevel}")
            return
        }
        
        // Next / Prev button was pushed
        if (msg.command == "02" || msg.command == "05") {
            def button = msg.data[0] == "00" ? BUTTONS.NEXT : BUTTONS.PREV
            return triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
        }
    }

    // Undocumented cluster 
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0x0013) {
        return zigbeeDebug("Device Announce Broadcast: ${msg.data}")
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

    // Undocumented cluster - Used by firmware 1.0.012 (20211214)
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0xFC7F) {
        def button = msg.data[0] == "01" ? BUTTONS.DOT_1 : BUTTONS.DOT_2

        // 1 Dot / 2 Dots button was pushed
        if (msg.data[1] == "01") {
            return triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
        }

        // 1 Dot / 2 Dots button was double tapped
        if (msg.data[1] == "02") {
            return triggerZigbeeEvent(name: "doubleTapped", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was double tapped")
        }
        
        // 1 Dot / 2 Dots button was held
        if (msg.data[1] == "03") {
            return triggerZigbeeEvent(name: "held", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was held")
        }
    }

    // Undocumented cluster - Used by firmware 1.0.35 (20230411)
    // ---------------------------------------------------------------------------------------------------------------
    if (clusterId == 0xFC80) {
        def button = msg.sourceEndpoint == "02" ? BUTTONS.DOT_1 : BUTTONS.DOT_2

        // IGNORED: 1 Dot / 2 Dots button was pressed-down
        if (msg.command == "01") {
            return zigbeeDebug("Button #${button[0]} (${button[1]}) was pressed-down (ignored as we wait for the next message to distinguish between click, double tap and hold)")
        }

        // 1 Dot / 2 Dots button was held
        // Commands are issued in this order: 01 (key-down = ignored) -> 02 (button is held = update "held" attribute) -> 04 (button released = ignored)
        if (msg.command == "02") {
            return triggerZigbeeEvent(name: "held", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was held")
        }
        
        // 1 Dot / 2 Dots button was pushed
        if (msg.command == "03") {
            return triggerZigbeeEvent(name: "pushed", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was pressed")
        }

        // IGNORED: 1 Dot / 2 Dots button was released (ignored)
        if (msg.command == "04") {
            return zigbeeDebug("Button #${button[0]} (${button[1]}) was released after a long hold (ignored)")
        }
        
        // 1 Dot / 2 Dots button was double tapped
        if (msg.command == "06") {
            return triggerZigbeeEvent(name: "doubleTapped", value: button[0], descriptionText: "Button #${button[0]} (${button[1]}) was double tapped")
        }
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

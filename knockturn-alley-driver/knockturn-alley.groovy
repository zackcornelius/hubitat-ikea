/**
 * Knockturn Alley - Simple toolkit driver to help developers peer deep into the guts of Zigbee devices.
 *
 * @version 1.1.0
 * @see https://dan-danache.github.io/hubitat/knockturn-alley-driver/
 * @see https://dan-danache.github.io/hubitat/knockturn-alley-driver/CHANGELOG
 * @see https://community.hubitat.com/t/dev-knockturn-alley/125167
 */
import groovy.time.TimeCategory
import groovy.transform.Field

metadata {
    definition(name:"Knockturn Alley", namespace:"dandanache", singleThreaded:true, author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/knockturn-alley-driver/knockturn-alley.groovy") {
        command "a01Legilimens"
        command "a02Scourgify", [
            [name: "Raw data", type: "ENUM", constraints: [
                "1 - Keep raw data",
                "2 - Remove raw data",
            ]],
        ]
        command "b01Revelio", [
            [name: "What to reveal", type: "ENUM", constraints: [
                "1 - Get attribute current value",
                "2 - Check attribute reporting",
            ]],
            [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
            [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
        ]
        command "b02Obliviate", [
            [name: "What to forget", type: "ENUM", constraints: [
                "1 - Our state variables (ka_*) - Restore previous driver state",
                "2 - All state variables",
                "3 - Device data",
                "4 - Scheduled jobs configured by the previous driver",
                "5 - Everything",
            ]],
        ]
        command "c01Imperio", [
            [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
            [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Data type*", description: "Attribute data type", type: "ENUM", constraints: ZCL_DATA_TYPES.keySet().findAll { ZCL_DATA_TYPES[it].bytes != "0" && ZCL_DATA_TYPES[it].bytes != "var" }.sort().collect { "${Utils.hex it, 2}: ${ZCL_DATA_TYPES[it].name} (${ZCL_DATA_TYPES[it].bytes} bytes)" }],
            [name: "Value*", description: "Attribute value - hex format (e.g.: 0001 - for uint16)", type: "STRING"],
        ]
    }
}

// ===================================================================================================================
// Spells
// ===================================================================================================================

def a01Legilimens() {
    Log.info "🪄 Legilimens"

    // Discover active endpoints
    def cmd = "he raw 0x${device.deviceNetworkId} 0x0000 0x0000 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0x0000}"
    Utils.sendZigbeeCommands([cmd])
}

def a02Scourgify(operation) {
    Log.info "🪄 Scourgify: ${operation}"
    if (!state.ka_endpoints) {
        return Log.warn("Raw data is missing. Maybe you should run Legilimens first if you didn't do that already ...")
    }
    
    // Make data pretty
    String table = "<style>#ka_report { border-collapse:collapse; font-weight:normal } #ka_report th { background-color:#F2F2F2 } #ka_report pre { margin:0 } #ka_report_div { margin-left:-40px; width:calc(100% + 40px) } @media (max-width: 840px) { #ka_report_div { margin-left:-64px; width:calc(100% + 87px) } }</style>"
    table += "<div id=ka_report_div style='overflow-x:scroll; padding:0 1px'><table id=ka_report class='cell-border nowrap hover stripe'>"
    table += "<thead><tr><th>Attr ID</th><th>Name</th><th>Required</th><th>Access</th><th>Type</th><th>Bytes</th><th>Encoding</th><th>Value</th><th>Reporting</th></tr></thead><tbody>"
    
    state.ka_endpoints?.sort().each { endpoint ->
        table += "<tr><th colspan=9 style='background-color:SteelBlue; color:White'>Endpoint: ${Utils.hex endpoint, 2}</th></tr>"
        table += "<tr><th colspan=9>Out Clusters: ${state["ka_outClusters_${endpoint}"]?.sort().collect { "${Utils.hex it, 4} (${ZCL_CLUSTERS.get(it)?.name ?: "Unknown Cluster"})" }.join(", ")}</th></tr>"
        state["ka_inClusters_${endpoint}"]?.sort().each { cluster ->
            table += "<tr><th colspan=9>In Cluster: ${"${Utils.hex cluster, 4} (${ZCL_CLUSTERS.get(cluster)?.name ?: "Unknown Cluster"})"}</th></tr>"
            Set<Integer> attributes = []
            getState()?.each {
                if (it.key.startsWith("ka_attribute_${endpoint}_${cluster}") || it.key.startsWith("ka_attributeValue_${endpoint}_${cluster}")) {
                    attributes += Integer.parseInt it.key.split("_").last()
                }
            }
            attributes.sort().each { attribute ->
                def attributeSpec = ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(attribute)
                def attributeType = state["ka_attribute_${endpoint}_${cluster}_${attribute}"]
                def attributeValue = state["ka_attributeValue_${endpoint}_${cluster}_${attribute}"]
                def attributeReporting = state["ka_attributeReporting_${endpoint}_${cluster}_${attribute}"]
                table += "<tr>"
                table += "<td><b><pre>${Utils.hex attribute, 4}</pre></b></td>"
                table += "<td>${attributeSpec?.name ?: "--"}</td>"
                table += "<td>${attributeSpec?.req ?: "--"}</td>"
                table += "<td><pre>${attributeSpec?.acc ?: "--"}</pre></td>"
                table += "<td>${attributeType?.name ?: "--"}</td>"
                table += "<td>${attributeType?.bytes ?: "--"}</td>"
                table += "<td>${attributeValue?.encoding ?: "--"}</td>"
                table += "<td><b><pre>${attributeValue?.value ?: "--"}</pre></b></td>"
                table += "<td>${attributeReporting ?: "--"}</td>"
                table += "</tr>"
            }
        }
    }
    table += "</tbody></table></div>"
    table += "<script>window.addEventListener('load', () => { \$('#ka_report').dataTable() })</script>"

    // Cleanup raw data?
    if (operation.startsWith("2 - ")) {
       getState()?.findAll { it.key.startsWith("ka_") }?.collect { it.key }.each { state.remove it }
    }
    
    // Show report table
    state.ka_report = table
}

def b01Revelio(operation, endpointHex, clusterHex, attributeHex) {
    Log.info "🪄 Revelio: ${operation} (endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex})"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid value for Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid value for Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid value for Attribute ID: ${clusterHex}")
    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16

    switch (operation) {
        case { it.startsWith("1 - ") }:
            return Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} ${Utils.hex endpoint, 2} 0x01 ${Utils.hex cluster} {10 00 00 ${Utils.payload attribute}}"])

        case { it.startsWith("2 - ") }:
            return Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} ${Utils.hex endpoint, 2} 0x01 ${Utils.hex cluster} {10 00 08 00 ${Utils.payload attribute}}"])
    }
}

def b02Obliviate(operation) {
    Log.info "🪄 Obliviate: ${operation}"

    switch (operation) {
        case { it.startsWith("5 - ") }:
            state.clear()
            device.getData().collect { it.key }.each { device.removeDataValue it }
            return unschedule()

        case { it.startsWith("4 - ") }:
            return unschedule()

        case { it.startsWith("3 - ") }:
            return device.getData()?.collect { it.key }.each { device.removeDataValue it }

        case { it.startsWith("2 - ") }:
            return state.clear()
        
        case { it.startsWith("1 - ") }:
            return getState()?.findAll { it.key.startsWith("ka_") }?.collect { it.key }.each { state.remove it }

        default:
            Log.error "Don't know how to ${operation}"
    }
}

def c01Imperio(endpointHex, clusterHex, attributeHex, typeStr, valueHex) {
    Log.info "🪄 Imperio: endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex}, type=${typeStr}, value=${valueHex}"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid value for Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid value for Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid value for Attribute ID: ${clusterHex}")
    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16
    Integer type = Integer.parseInt typeStr.substring(2, 4), 16
    String value = valueHex.replaceAll " ", ""
    
    Integer typeLen = Integer.parseInt ZCL_DATA_TYPES[type].bytes
    if (value.size() != typeLen * 2) return Log.error("Invalid attribute Value: It must have exactly ${typeLen} bytes but you provided ${value.size()}: ${valueHex}")

    // Transform BE -> LE: "123456" -> ["1", "2", "3", "4", "5", "6"] -> [["1", "2"], ["3", "4"], ["5", "6"]] -> ["12", "34", "56"] -> ["56", "34", "12"] -> "563412"
    value = (value.split("") as List).collate(2).collect { it.join() }.reverse().join()
    
    // Send zigbee command
    return Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} ${Utils.hex endpoint, 2} 0x01 ${Utils.hex cluster} {10 00 02 ${Utils.payload attribute}${typeStr.substring(2, 4)}${value}}"])
}

// ===================================================================================================================
// Handle incoming Zigbee messages
// ===================================================================================================================

def parse(String description) {
    Log.debug "description=[${description}]"
    def msg = zigbee.parseDescriptionAsMap description

    // Extract cluster and command from message
    if (msg.clusterInt == null) msg.clusterInt = Utils.dec msg.cluster
    msg.commandInt = Utils.dec msg.command

    Log.debug "msg=[${msg}]"
    switch (msg) {

        // Read Attribute Response (0x01) & Report attributes (0x0A)
        case { contains it, [commandInt:0x01] }:
        case { contains it, [commandInt:0x0A] }:
            if (!msg.endpoint) {
                return Utils.failedZigbeeMessage("Read Attribute Response", msg, msg.data[2])
            }
            
            Integer endpoint = Utils.dec msg.endpoint
            Integer cluster = msg.clusterInt

            Map<Integer, Map<String, String>> attributesValues = [:]
            attributesValues[msg.attrInt] = [encoding: msg.encoding, value: msg.value]
            Utils.processedZigbeeMessage "Read Attribute Response", "endpoint=${Utils.hex endpoint}, cluster=${Utils.hex cluster}, attribute=${Utils.hex msg.attrInt}, value=${msg.value}"

            msg.additionalAttrs?.each {
                attributesValues[it.attrInt] = [encoding: it.encoding, value: it.value]
                Utils.processedZigbeeMessage "Additional Attribute", "endpoint=${Utils.hex endpoint}, cluster=${Utils.hex cluster}, attribute=${Utils.hex it.attrInt}, value=${it.value}"
            }
        
            return State.addAttributesValues(endpoint, cluster, attributesValues)

        // Write Attribute Response (0x04)
        case { contains it, [commandInt:0x04] }:
            if (msg.data[0] != "00") {
                return Utils.failedZigbeeMessage("Write Attribute Response", msg, msg.data[0])
            }
        return Utils.processedZigbeeMessage("Write Attribute Response", "data=${msg.data}")

        
        // Read Reporting Configuration Response (0x09)
        case { contains it, [commandInt:0x09] }:
            if (msg.data[0] != "00") {
                return Utils.failedZigbeeMessage("Read Reporting Configuration Response", msg, msg.data[0])
            }
            
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            Integer attribute = Utils.dec msg.data[2..3].reverse().join()
            Integer minPeriod = Utils.dec msg.data[5..6].reverse().join()
            Integer maxPeriod = Utils.dec msg.data[7..8].reverse().join()

            State.addAttributeReporting endpoint, cluster, attribute, minPeriod, maxPeriod
            return Utils.processedZigbeeMessage("Read Reporting Configuration Response", "endpoint=${Utils.hex endpoint}, cluster=${Utils.hex cluster}, attribute=${Utils.hex attribute}, minPeriod=${minPeriod}, maxPeriod=${maxPeriod}")
        
        // Active_EP_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:ActiveEPCount, n*08:ActiveEPList }
        // Three endpoints example: [83, 00, 18, 4A, 03, 01, 02, 03] -> endpointIds=[01, 02, 03]
        case { contains it, [clusterInt:0x8005] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Active Endpoints Response", msg)
            }
        
            Set<Integer> endpoints = []
            Integer count = Utils.dec msg.data[4]
            List<String> cmds = []
            if (count > 0) {
                (1..count).each() { i ->
                    String endpointStr = msg.data[4 + i]
                    Integer endpoint = Utils.dec endpointStr
                    endpoints += endpoint
                    
                    // Query simple descriptor data
                    cmds += "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0004 {00 ${zigbee.swapOctets(device.deviceNetworkId)} ${endpointStr}} {0x0000}"
                }
                State.addEndpoints endpoints
                Utils.sendZigbeeCommands delayBetween(cmds, 1000)
            }

            return Utils.processedZigbeeMessage("Active Endpoints Response", "endpoints=${endpoints}")

        // Simple_Desc_rsp := { 08:Status, 16:NWKAddrOfInterest, 08:Length, 08:Endpoint, 16:ApplicationProfileIdentifier, 16:ApplicationDeviceIdentifier, 08:Reserved, 16:InClusterCount, n*16:InClusterList, 16:OutClusterCount, n*16:OutClusterList }
        // Example: [B7, 00, 18, 4A, 14, 03, 04, 01, 06, 00, 01, 03, 00,  00, 03, 00, 80, FC, 03, 03, 00, 04, 00, 80, FC] -> endpointId=03, inClusters=[0000, 0003, FC80], outClusters=[0003, 0004, FC80]
        case { contains it, [clusterInt:0x8004] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Simple Descriptor Response", msg)
            }

            Integer endpoint = Utils.dec msg.data[5]
            Integer count = Utils.dec msg.data[11]
            Integer position = 12
            Integer positionCounter = null
            Set<Integer> inClusters = []
            List<String> cmds = []
            if (count > 0) {
                (1..count).each() { b->
                    positionCounter = position + ((b - 1) * 2)
                    Integer cluster = Utils.dec msg.data[positionCounter..positionCounter+1].reverse().join()
                    inClusters += cluster

                    // Discover cluster attributes
                    cmds += "he raw ${device.deviceNetworkId} ${Utils.hex endpoint, 2} 0x01 ${Utils.hex cluster} {10 00 0C 00 00 FF}"
                }
                State.addInClusters endpoint, inClusters
            }

            position += count * 2
            count = Utils.dec msg.data[position]
            position += 1
            Set<Integer> outClusters = []
            if (count > 0) {
                (1..count).each() { b->
                    positionCounter = position + ((b - 1) * 2)
                    Integer cluster = Utils.dec msg.data[positionCounter..positionCounter+1].reverse().join()
                    outClusters += cluster
                }
                State.addOutClusters endpoint, outClusters
            }

            if (cmds.size != 0) Utils.sendZigbeeCommands delayBetween(cmds, 1000)
            return Utils.processedZigbeeMessage("Simple Descriptor Response", "endpoint=${Utils.hex endpoint, 2}, inClusters=${Utils.hexs inClusters}, outClusters=${Utils.hexs outClusters}")

        // DiscoverAttributesResponse := { 08:Complete?, n*24:AttributeInformation }
        // AttributeInformation := { 16:AttributeIdentifier, 08:AttributeDataType }
        // AttributeDataType := @see @Field ZCL_DATA_TYPES
        // Example: [01, 00, 00, 20, 01, 00, 20, 02, 00, 20, 03, 00, 20, 04, 00, 42, 05, 00, 42, 06, 00, 42, 07, 00, 30, 08, 00, 30, 09, 00, 30, 0A, 00, 41, 00, 40, 42, FD, FF, 21]
        case { contains it, [commandInt:0x0D] }:
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt

            List<String> data = msg.data.drop 1
            Map<Integer, Integer> attributes = [:]
            while (data.size() >= 3) {
                List<String> chunk = data.take 3
                Integer attribute = Utils.dec chunk.take(2).reverse().join()
                Integer type = Utils.dec chunk[2]
                attributes[attribute] = type               

                data = data.drop 3
            }

            if (attributes.size() != 0) {
                List<String> cmds = []
                attributes.keySet().collate(3).each { attrs ->

                    // Read attribute value (use batches of 3 to reduce mesh traffic)
                    cmds += "he raw ${device.deviceNetworkId} ${Utils.hex endpoint, 2} 0x01 ${Utils.hex cluster} {10 00 00 ${attrs.collect { Utils.payload it }.join()}}"

                    // If attribute is reportable, also inquire its reporting status
                    attrs.each {
                        if (ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(it)?.get("acc")?.endsWith("P")) {
                            cmds += "he raw ${device.deviceNetworkId} ${Utils.hex endpoint, 2} 0x01 ${Utils.hex cluster} {10 00 08 00 ${Utils.payload it}}"
                        }
                    }
                }
                Utils.sendZigbeeCommands delayBetween(cmds, 1000)
                State.addAttributes endpoint, cluster, attributes
            }
            return Utils.processedZigbeeMessage("Discover Attributes Response", "endpoint=${Utils.hex endpoint, 2}, cluster=${Utils.hex cluster}, attributes=${attributes}")


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
    debug: { message -> log.debug "${device.displayName} ${message.uncapitalize()}" },
    info:  { message -> log.info  "${device.displayName} ${message.uncapitalize()}" },
    warn:  { message -> log.warn  "${device.displayName} ${message.uncapitalize()}" },
    error: { message -> log.error "${device.displayName} ${message.uncapitalize()}" },
]

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

@Field def Utils = [
    dec: { String value -> Integer.parseInt(value, 16) },
    hex: { Integer value, Integer chars = 4 -> "0x${zigbee.convertToHexString value, chars}" },
    hexs: { Collection<Integer> values, Integer chars = 4 -> values.collect { "0x${zigbee.convertToHexString it, chars}" } },
    payload: { Integer value -> zigbee.swapOctets(zigbee.convertToHexString(value, 4)) },

    sendZigbeeCommands: { List<String> cmds ->
        if (cmds.size() == 0) return
        Log.debug "◀ Sending Zigbee messages: ${cmds}"
        sendHubCommand new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE)
    },

    sendEvent: { Map event ->
        Log.info "${event.descriptionText} [${event.type}]"
        sendEvent event
    },

    dataValue: { String key, String value ->
        Log.debug "Update driver data value: ${key}=${value}"
        updateDataValue key, value
    },

    processedZigbeeMessage: { String type, String details ->
        Log.debug "▶ Processed Zigbee message: type=${type}, status=SUCCESS, ${details}"
    },

    ignoredZigbeeMessage: { String type, Map msg ->
        Log.debug "▶ Ignored Zigbee message: type=${type}, status=SUCCESS, data=${msg.data}"
    },

    failedZigbeeMessage: { String type, Map msg, String status = null ->
        String prettyStatus = ZCL_STATUS[Integer.parseInt(status == null ? msg.data[1] : status, 16)]
        Log.warn "▶ Received Zigbee message: type=${type}, status=${prettyStatus}, data=${msg.data}"
    }
]

// switch/case syntactic sugar
private boolean contains(Map msg, Map spec) {
    msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == msg[it.key] }
}

// ===================================================================================================================
// State helpers
// ===================================================================================================================

@Field Map<String, Closure> State = [
    addEndpoints: { Set<Integer> endpoints ->
        state.ka_endpoints = endpoints
    },

    addInClusters: { Integer endpoint, Collection<Integer> clusters ->
        state["ka_inClusters_${endpoint}"] = clusters
    },

    addOutClusters: { Integer endpoint, Collection<Integer> clusters ->
        state["ka_outClusters_${endpoint}"] = clusters
    },

    addAttributes: { Integer endpoint, Integer cluster, Map<Integer, Integer> attributes ->
        attributes.each {
            state["ka_attribute_${endpoint}_${cluster}_${it.key}"] = ZCL_DATA_TYPES[it.value]
        }
    },

    addAttributesValues: { Integer endpoint, Integer cluster, Map<Integer, Map<String, String>> attributesValues ->
        attributesValues.each {
            state["ka_attributeValue_${endpoint}_${cluster}_${it.key}"] = it.value
        }
    },
    
    addAttributeReporting: { Integer endpoint, Integer cluster, Integer attribute, Integer minPeriod, Integer maxPeriod ->
        state["ka_attributeReporting_${endpoint}_${cluster}_${attribute}"] = [ min:minPeriod, max:maxPeriod ]
    }
]

// ===================================================================================================================
// Constants
// ===================================================================================================================

@Field static final Map<String, String> ZCL_STATUS = [
    0x00: "SUCCESS",
    0x01: "FAILURE",
    0x7E: "NOT_AUTHORIZED",
    0x7F: "RESERVED_FIELD_NOT_ZERO",
    0x80: "MALFORMED_COMMAND",
    0x81: "UNSUP_CLUSTER_COMMAND",
    0x82: "UNSUP_GENERAL_COMMAND",
    0x83: "UNSUP_MANUF_CLUSTER_COMMAND",
    0x84: "UNSUP_MANUF_GENERAL_COMMAND",
    0x85: "INVALID_FIELD",
    0x86: "UNSUPPORTED_ATTRIBUTE",
    0x87: "INVALID_VALUE",
    0x88: "READ_ONLY",
    0x89: "INSUFFICIENT_SPACE",
    0x8A: "DUPLICATE_EXISTS",
    0x8B: "NOT_FOUND",
    0x8C: "UNREPORTABLE_ATTRIBUTE",
    0x8D: "INVALID_DATA_TYPE",
    0x8E: "INVALID_SELECTOR",
    0x8F: "WRITE_ONLY",
    0x90: "INCONSISTENT_STARTUP_STATE",
    0x91: "DEFINED_OUT_OF_BAND",
    0x92: "INCONSISTENT",
    0x93: "ACTION_DENIED",
    0x94: "TIMEOUT",
    0x95: "ABORT",
    0x96: "INVALID_IMAGE",
    0x97: "WAIT_FOR_DATA",
    0x98: "NO_IMAGE_AVAILABLE",
    0x99: "REQUIRE_MORE_IMAGE",
    0x9A: "NOTIFICATION_PENDING",
    0xC0: "HARDWARE_FAILURE",
    0xC1: "SOFTWARE_FAILURE",
    0xC2: "CALIBRATION_ERROR",
    0xC3: "UNSUPPORTED_CLUSTER"
]

@Field static final Map<Integer, Map<String, String>> ZCL_DATA_TYPES = [
    0x00: [name:"nodata",    bytes:"0"],
    0x08: [name:"data8",     bytes:"1"],
    0x09: [name:"data16",    bytes:"2"],
    0x0a: [name:"data24",    bytes:"3"],
    0x0b: [name:"data32",    bytes:"4"],
    0x0c: [name:"data40",    bytes:"5"],
    0x0d: [name:"data48",    bytes:"6"],
    0x0e: [name:"data56",    bytes:"7"],
    0x0f: [name:"data64",    bytes:"8"],
    0x10: [name:"bool",      bytes:"1"],
    0x18: [name:"map8",      bytes:"1"],
    0x19: [name:"map16",     bytes:"2"],
    0x1a: [name:"map24",     bytes:"3"],
    0x1b: [name:"map32",     bytes:"4"],
    0x1c: [name:"map40",     bytes:"5"],
    0x1d: [name:"map48",     bytes:"6"],
    0x1e: [name:"map56",     bytes:"7"],
    0x1f: [name:"map64",     bytes:"8"],
    0x20: [name:"uint8",     bytes:"1"],
    0x21: [name:"uint16",    bytes:"2"],
    0x22: [name:"uint24",    bytes:"3"],
    0x23: [name:"uint32",    bytes:"4"],
    0x24: [name:"uint40",    bytes:"5"],
    0x25: [name:"uint48",    bytes:"6"],
    0x26: [name:"uint56",    bytes:"7"],
    0x27: [name:"uint64",    bytes:"8"],
    0x28: [name:"int8",      bytes:"1"],
    0x29: [name:"int16",     bytes:"2"],
    0x2a: [name:"int24",     bytes:"3"],
    0x2b: [name:"int32",     bytes:"4"],
    0x2c: [name:"int40",     bytes:"5"],
    0x2d: [name:"int48",     bytes:"6"],
    0x2e: [name:"int56",     bytes:"7"],
    0x2f: [name:"int64",     bytes:"8"],
    0x30: [name:"enum8",     bytes:"1"],
    0x31: [name:"enum16",    bytes:"2"],
    0x38: [name:"semi",      bytes:"2"],
    0x39: [name:"single",    bytes:"4"],
    0x3a: [name:"double",    bytes:"8"],
    0x41: [name:"octstr",    bytes:"var"],
    0x42: [name:"string",    bytes:"var"],
    0x43: [name:"octstr16",  bytes:"var"],
    0x44: [name:"string16",  bytes:"var"],
    0x48: [name:"array",     bytes:"var"],
    0x4c: [name:"struct",    bytes:"var"],
    0x50: [name:"set",       bytes:"var"],
    0x51: [name:"bag",       bytes:"var"],
    0xe0: [name:"ToD",       bytes:"4"],
    0xe1: [name:"date",      bytes:"4"],
    0xe2: [name:"UTC",       bytes:"4"],
    0xe8: [name:"clusterId", bytes:"2"],
    0xe9: [name:"attribId",  bytes:"2"],
    0xea: [name:"bacOID",    bytes:"4"],
    0xf0: [name:"EUI64",     bytes:"8"],
    0xf1: [name:"key128",    bytes:"16"],
    0xff: [name:"unknown",   bytes:"0"],
]

@Field static final def ZCL_CLUSTERS = [
    0x0000: [
        name: "Basic Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"ZCLVersion" ],
            0x0001: [ req:"No",  acc:"R--", name:"ApplicationVersion" ],
            0x0002: [ req:"No",  acc:"R--", name:"StackVersion" ],
            0x0003: [ req:"No",  acc:"R--", name:"HWVersion" ],
            0x0004: [ req:"No",  acc:"R--", name:"ManufacturerName" ],
            0x0005: [ req:"No",  acc:"R--", name:"ModelIdentifier" ],
            0x0006: [ req:"Yes", acc:"R--", name:"DateCode" ],
            0x0007: [ req:"No",  acc:"R--", name:"PowerSource" ],
            0x0010: [ req:"No",  acc:"RW-", name:"LocationDescription" ],
            0x0011: [ req:"No",  acc:"RW-", name:"PhysicalEnvironment" ],
            0x0012: [ req:"No",  acc:"RW-", name:"DeviceEnabled" ],
            0x0013: [ req:"No",  acc:"RW-", name:"AlarmMask" ],
            0x0014: [ req:"No",  acc:"RW-", name:"DisableLocalConfig" ],
            0x4000: [ req:"No",  acc:"R--", name:"SWBuildID" ]
        ]
    ],
    0x0001: [
        name: "Power Configuration Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"MainsVoltage" ],
            0x0001: [ req:"No",  acc:"R--", name:"MainsFrequency" ],
            
            0x0010: [ req:"No",  acc:"RW-", name:"MainsAlarmMask" ],
            0x0011: [ req:"No",  acc:"RW-", name:"MainsVoltageMinThreshold" ],
            0x0012: [ req:"No",  acc:"RW-", name:"MainsVoltageMaxThreshold" ],
            0x0013: [ req:"No",  acc:"RW-", name:"MainsVoltageDwellTripPoint" ],
            
            0x0020: [ req:"No",  acc:"R--", name:"BatteryVoltage" ],
            0x0021: [ req:"No",  acc:"R-P", name:"BatteryPercentageRemaining" ],

            0x0030: [ req:"No",  acc:"RW-", name:"BatteryManufacturer" ],
            0x0031: [ req:"No",  acc:"RW-", name:"BatterySize" ],
            0x0032: [ req:"No",  acc:"RW-", name:"BatteryAHrRating" ],
            0x0033: [ req:"No",  acc:"RW-", name:"BatteryQuantity" ],
            0x0034: [ req:"No",  acc:"RW-", name:"BatteryRatedVoltage" ],
            0x0035: [ req:"No",  acc:"RW-", name:"BatteryAlarmMask" ],
            0x0036: [ req:"No",  acc:"RW-", name:"BatteryVoltageMinThreshold" ],
            0x0037: [ req:"No",  acc:"RW-", name:"BatteryVoltageThreshold1" ],
            0x0038: [ req:"No",  acc:"RW-", name:"BatteryVoltageThreshold2" ],
            0x0039: [ req:"No",  acc:"RW-", name:"BatteryVoltageThreshold3" ],
            0x003A: [ req:"No",  acc:"RW-", name:"BatteryPercentageMinThreshold" ],
            0x003B: [ req:"No",  acc:"RW-", name:"BatteryPercentageThreshold1" ],
            0x003C: [ req:"No",  acc:"RW-", name:"BatteryPercentageThreshold2" ],
            0x003D: [ req:"No",  acc:"RW-", name:"BatteryPercentageThreshold3" ],
            0x003E: [ req:"No",  acc:"R--", name:"BatteryAlarmState" ]
        ]
    ],
    0x0002: [
        name: "Temperature Configuration Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"CurrentTemperature" ],
            0x0001: [ req:"No",  acc:"R--", name:"MinTempExperienced" ],
            0x0002: [ req:"No",  acc:"R--", name:"MaxTempExperienced" ],
            0x0003: [ req:"No",  acc:"R--", name:"OverTempTotalDwell" ],

            0x0010: [ req:"No",  acc:"RW-", name:"DeviceTempAlarmMask" ],
            0x0011: [ req:"No",  acc:"RW-", name:"LowTempThreshold" ],
            0x0012: [ req:"No",  acc:"RW-", name:"HighTempThreshold" ],
            0x0013: [ req:"No",  acc:"RW-", name:"LowTempDwellTripPoint" ],
            0x0014: [ req:"No",  acc:"RW-", name:"HighTempDwellTripPoint" ]
        ]
    ],
    0x0003: [
        name: "Identify Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"IdentifyTime" ]
        ]
    ],
    0x0004: [
        name: "Groups Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"NameSupport" ]
        ]
    ],
    0x0005: [
        name: "Scenes Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"SceneCount" ],
            0x0001: [ req:"Yes", acc:"R--", name:"CurrentScene" ],
            0x0002: [ req:"Yes", acc:"R--", name:"CurrentGroup" ],
            0x0003: [ req:"Yes", acc:"R--", name:"SceneValid" ],
            0x0004: [ req:"Yes", acc:"R--", name:"NameSupport" ],
            0x0005: [ req:"No",  acc:"R--", name:"LastConfiguredBy" ]
        ]
    ],
    0x0006: [
        name: "On/Off Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"OnOff" ],
            0x4000: [ req:"No",  acc:"R--", name:"GlobalSceneControl" ],
            0x4001: [ req:"No",  acc:"RW-", name:"OnTime" ],
            0x4002: [ req:"No",  acc:"RW-", name:"OffWaitTime" ]
        ]
    ],
    0x0007: [
        name: "On/Off Switch Configuration Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"SwitchType" ],
            0x0010: [ req:"Yes", acc:"RW-", name:"SwitchActions" ]
        ]
    ],
    0x0008: [
        name: "Level Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"CurrentLevel" ],
            0x0001: [ req:"No",  acc:"R--", name:"RemainingTime" ],
            0x0010: [ req:"No",  acc:"RW-", name:"OnOffTransitionTime" ],
            0x0011: [ req:"No",  acc:"RW-", name:"OnLevel" ],
            0x0012: [ req:"No",  acc:"RW-", name:"OnTransitionTime" ],
            0x0013: [ req:"No",  acc:"RW-", name:"OffTransitionTime" ],
            0x0014: [ req:"No",  acc:"RW-", name:"DefaultMoveRate" ]
        ]
    ],
    0x0009: [
        name: "Alarms Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"AlarmCount" ]
        ]
    ],
    0x000A: [
        name: "Time Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"Time" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"TimeStatus" ],
            0x0002: [ req:"No",  acc:"RW-", name:"TimeZone" ],
            0x0003: [ req:"No",  acc:"RW-", name:"DstStart" ],
            0x0004: [ req:"No",  acc:"RW-", name:"DstEnd" ],
            0x0005: [ req:"No",  acc:"RW-", name:"DstShift" ],
            0x0006: [ req:"No",  acc:"R--", name:"StandardTime" ],
            0x0007: [ req:"No",  acc:"R--", name:"LocalTime" ],
            0x0008: [ req:"No",  acc:"R--", name:"LastSetTime" ],
            0x0009: [ req:"No",  acc:"RW-", name:"ValidUntilTime" ]
        ]
    ],
    0x000B: [
        name: "RSSI Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"LocationType" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"LocationMethod" ],
            0x0002: [ req:"No",  acc:"R--", name:"LocationAge" ],
            0x0003: [ req:"No",  acc:"R--", name:"QualityMeasure" ],
            0x0004: [ req:"No",  acc:"R--", name:"NumberOfDevices" ],

            0x0010: [ req:"No",  acc:"RW-", name:"Coordinate1" ],
            0x0011: [ req:"No",  acc:"RW-", name:"Coordinate2" ],
            0x0012: [ req:"No",  acc:"RW-", name:"Coordinate3" ],
            0x0013: [ req:"Yes", acc:"RW-", name:"Power" ],
            0x0014: [ req:"Yes", acc:"RW-", name:"PathLossExponent" ],
            0x0015: [ req:"No",  acc:"RW-", name:"ReportingPeriod" ],
            0x0016: [ req:"No",  acc:"RW-", name:"CalculationPeriod" ],
            0x0017: [ req:"No",  acc:"RW-", name:"NumberRSSIMeasurements" ]
        ]
    ],
    0x000C: [
        name: "Analog Input Cluster",
        attributes: [
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x0041: [ req:"No",  acc:"Rw-", name:"MaxPresentValue" ],
            0x0045: [ req:"No",  acc:"Rw-", name:"MinPresentValue" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"RWP", name:"PresentValue" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x006A: [ req:"No",  acc:"Rw-", name:"Resolution" ],
            0x006F: [ req:"Yes", acc:"R-P", name:"StatusFlags" ],
            0x0075: [ req:"No",  acc:"Rw-", name:"EngineeringUnits" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x000D: [
        name: "Analog Output Cluster",
        attributes: [
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x0041: [ req:"No",  acc:"Rw-", name:"MaxPresentValue" ],
            0x0045: [ req:"No",  acc:"Rw-", name:"MinPresentValue" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"RWP", name:"PresentValue" ],
            0x0057: [ req:"No",  acc:"RW-", name:"PriorityArray" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"RelinquishDefault" ],
            0x006A: [ req:"No",  acc:"Rw-", name:"Resolution" ],
            0x006F: [ req:"Yes", acc:"R-P", name:"StatusFlags" ],
            0x0075: [ req:"No",  acc:"Rw-", name:"EngineeringUnits" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x000E: [
        name: "Analog Value Cluster",
        attributes: [
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"PresentValue" ],
            0x0057: [ req:"No",  acc:"Rw-", name:"PriorityArray" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"RelinquishDefault" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0075: [ req:"No",  acc:"Rw-", name:"EngineeringUnits" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x000F: [
        name: "Binary Input Cluster",
        attributes: [
            0x0004: [ req:"No",  acc:"Rw-", name:"ActiveText" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x002E: [ req:"No",  acc:"Rw-", name:"InactiveText" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0054: [ req:"Yes", acc:"R--", name:"Polarity" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"PresentValue" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x0010: [
        name: "Binary Output Cluster",
        attributes: [
            0x0004: [ req:"No",  acc:"Rw-", name:"ActiveText" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x002E: [ req:"No",  acc:"Rw-", name:"InactiveText" ],
            0x0042: [ req:"No",  acc:"Rw-", name:"MinimumOffTime" ],
            0x0043: [ req:"No",  acc:"Rw-", name:"MinimumOnTime" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0054: [ req:"Yes", acc:"R--", name:"Polarity" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"PresentValue" ],
            0x0057: [ req:"No",  acc:"RW-", name:"PriorityArray" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"RelinquishDefault" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x0011: [
        name: "Binary Value Cluster",
        attributes: [
            0x0004: [ req:"No",  acc:"Rw-", name:"ActiveText" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x002E: [ req:"No",  acc:"Rw-", name:"InactiveText" ],
            0x0042: [ req:"No",  acc:"Rw-", name:"MinimumOffTime" ],
            0x0043: [ req:"No",  acc:"Rw-", name:"MinimumOnTime" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"PresentValue" ],
            0x0057: [ req:"No",  acc:"RW-", name:"PriorityArray" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"RelinquishDefault" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x0012: [
        name: "Multistate Input Cluster",
        attributes: [
            0x000E: [ req:"No",  acc:"Rw-", name:"StateText" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x004A: [ req:"Yes", acc:"Rw-", name:"NumberOfStates" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"PresentValue" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x0013: [
        name: "Multistate Output Cluster",
        attributes: [
            0x000E: [ req:"No",  acc:"Rw-", name:"StateText" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x004A: [ req:"Yes", acc:"Rw-", name:"NumberOfStates" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"RW-", name:"PresentValue" ],
            0x0057: [ req:"No",  acc:"R--", name:"PriorityArray" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"RelinquishDefault" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x0014: [
        name: "Multistate Value Cluster",
        attributes: [
            0x000E: [ req:"No",  acc:"Rw-", name:"StateText" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x004A: [ req:"Yes", acc:"Rw-", name:"NumberOfStates" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"OutOfService" ],
            0x0055: [ req:"Yes", acc:"RW-", name:"PresentValue" ],
            0x0057: [ req:"No",  acc:"RW-", name:"PriorityArray" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"RelinquishDefault" ],
            0x006F: [ req:"Yes", acc:"R--", name:"StatusFlags" ],
            0x0100: [ req:"No",  acc:"R--", name:"ApplicationType" ]
        ]
    ],
    0x0015: [
        name: "Commissioning Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"ShortAddress" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"ExtendedPANId" ],
            0x0002: [ req:"Yes", acc:"RW-", name:"PANId" ],
            0x0003: [ req:"Yes", acc:"RW-", name:"ChannelMask" ],
            0x0004: [ req:"Yes", acc:"RW-", name:"ProtocolVersion" ],
            0x0005: [ req:"Yes", acc:"RW-", name:"StackProfile" ],
            0x0006: [ req:"Yes", acc:"RW-", name:"StartupControl" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"TrustCenterAddress" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"TrustCenterMasterKey" ],
            0x0012: [ req:"Yes", acc:"RW-", name:"NetworkKey" ],
            0x0013: [ req:"Yes", acc:"RW-", name:"UseInsecureJoin" ],
            0x0014: [ req:"Yes", acc:"RW-", name:"PreconfiguredLinkKey" ],
            0x0015: [ req:"Yes", acc:"RW-", name:"NetworkKeySeqNum" ],
            0x0016: [ req:"Yes", acc:"RW-", name:"NetworkKeyType" ],
            0x0017: [ req:"Yes", acc:"RW-", name:"NetworkManagerAddress" ],
            
            0x0020: [ req:"No",  acc:"RW-", name:"ScanAttempts" ],
            0x0021: [ req:"No",  acc:"RW-", name:"TimeBetweenScans" ],
            0x0022: [ req:"No",  acc:"RW-", name:"RejoinInterval" ],
            
            0x0030: [ req:"No",  acc:"RW-", name:"IndirectPollRate" ],
            0x0031: [ req:"No",  acc:"R--", name:"ParentRetryThreshold" ],
            
            0x0040: [ req:"No",  acc:"RW-", name:"ConcentratorFlag" ],
            0x0041: [ req:"No",  acc:"RW-", name:"ConcentratorRadius" ],
            0x0042: [ req:"No",  acc:"RW-", name:"ConcentratorDiscoveryTime" ]
        ]
    ],
    0x0019: [
        name: "OTA Upgrade Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"UpgradeServerID" ],
            0x0001: [ req:"No",  acc:"R--", name:"FileOffset" ],
            0x0002: [ req:"No",  acc:"R--", name:"CurrentFileVersion" ],
            0x0003: [ req:"No",  acc:"R--", name:"CurrentZigBeeStackVersion" ],
            0x0004: [ req:"No",  acc:"R--", name:"DownloadedFileVersion" ],
            0x0005: [ req:"No",  acc:"R--", name:"DownloadedZigBeeStackVersion" ],
            0x0006: [ req:"Yes", acc:"R--", name:"ImageUpgradeStatus" ],
            0x0007: [ req:"No",  acc:"R--", name:"ManufacturerID" ],
            0x0008: [ req:"No",  acc:"R--", name:"ImageTypeID" ],
            0x0009: [ req:"No",  acc:"R--", name:"MinimumBlockPeriod" ],
            0x000A: [ req:"No",  acc:"R--", name:"ImageStamp" ],
        ]
    ],
    0x001A: [
        name: "Power Profile Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"TotalProfileNum" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MultipleScheduling" ],
            0x0002: [ req:"Yes", acc:"R--", name:"EnergyFormatting" ],
            0x0003: [ req:"Yes", acc:"R-P", name:"EnergyRemote" ],
            0x0004: [ req:"Yes", acc:"RWP", name:"ScheduleMode" ]
        ]
    ],
    0x0020: [
        name: "Poll Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"Check-inInterval" ],
            0x0001: [ req:"Yes", acc:"R--", name:"LongPollInterval" ],
            0x0002: [ req:"Yes", acc:"R--", name:"ShortPollInterval" ],
            0x0003: [ req:"Yes", acc:"RW-", name:"FastPollTimeout" ],
            0x0004: [ req:"No",  acc:"R--", name:"Check-inIntervalMin" ],
            0x0005: [ req:"No",  acc:"R--", name:"LongPollIntervalMin" ],
            0x0006: [ req:"No",  acc:"R--", name:"FastPollTimeoutMax" ]
        ]
    ],
    0x0100: [
        name: "Shade Configuration Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"PhysicalClosedLimit" ],
            0x0001: [ req:"No",  acc:"R--", name:"MotorStepSize" ],
            0x0002: [ req:"Yes", acc:"RW-", name:"Status" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"ClosedLimit" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"Mode" ]
        ]
    ],
    0x0101: [
        name: "Door Lock Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"LockState" ],
            0x0001: [ req:"Yes", acc:"R--", name:"LockType" ],
            0x0002: [ req:"Yes", acc:"R--", name:"ActuatorEnabled" ],
            0x0003: [ req:"No",  acc:"R-P", name:"DoorState" ],
            0x0004: [ req:"No",  acc:"RW-", name:"DoorOpenEvents" ],
            0x0005: [ req:"No",  acc:"RW-", name:"DoorClosedEvents" ],
            0x0006: [ req:"No",  acc:"RW-", name:"OpenPeriod" ],
            
            0x0010: [ req:"No",  acc:"R--", name:"NumberOfLogRecordsSupported" ],
            0x0011: [ req:"No",  acc:"R--", name:"NumberOfTotalUsersSupported" ],
            0x0012: [ req:"No",  acc:"R--", name:"NumberOfPINUsersSupported" ],
            0x0013: [ req:"No",  acc:"R--", name:"NumberOfRFIDUsersSupported" ],
            0x0014: [ req:"No",  acc:"R--", name:"NumberOfWeekDaySchedulesSupportedPerUser" ],
            0x0015: [ req:"No",  acc:"R--", name:"NumberOfYearDaySchedulesSupportedPerUser" ],
            0x0016: [ req:"No",  acc:"R--", name:"NumberOfHolidaySchedulesSupported" ],
            0x0017: [ req:"No",  acc:"R--", name:"MaxPINCodeLength" ],
            0x0018: [ req:"No",  acc:"R--", name:"MinPINCodeLength" ],
            0x0019: [ req:"No",  acc:"R--", name:"MaxRFIDCodeLength" ],
            0x001A: [ req:"No",  acc:"R--", name:"MinRFIDCodeLength" ],
            
            0x0020: [ req:"No",  acc:"RwP", name:"EnableLogging" ],
            0x0021: [ req:"No",  acc:"RwP", name:"Language" ],
            0x0022: [ req:"No",  acc:"RwP", name:"Settings" ],
            0x0023: [ req:"No",  acc:"RwP", name:"AutoRelockTime" ],
            0x0024: [ req:"No",  acc:"RwP", name:"SoundVolume" ],
            0x0025: [ req:"No",  acc:"RwP", name:"OperatingMode" ],
            0x0026: [ req:"No",  acc:"R--", name:"SupportedOperatingModes" ],
            0x0027: [ req:"No",  acc:"R-P", name:"DefaultConfigurationRegister" ],
            0x0028: [ req:"No",  acc:"RwP", name:"EnableLocalProgramming" ],
            0x0029: [ req:"No",  acc:"RWP", name:"EnableOneTouchLocking" ],
            0x002A: [ req:"No",  acc:"RWP", name:"EnableInsideStatusLED" ],
            0x002B: [ req:"No",  acc:"RWP", name:"EnablePrivacyModeButton" ],
            
            0x0030: [ req:"No",  acc:"RwP", name:"WrongCodeEntryLimit" ],
            0x0031: [ req:"No",  acc:"RwP", name:"UserCodeTemporaryDisableTime" ],
            0x0032: [ req:"No",  acc:"RwP", name:"SendPINOverTheAir" ],
            0x0033: [ req:"No",  acc:"RwP", name:"RequirePINforRFOperation" ],
            0x0034: [ req:"No",  acc:"R-P", name:"ZigBeeSecurityLevel" ],
            
            0x0040: [ req:"No",  acc:"RWP", name:"AlarmMask" ],
            0x0041: [ req:"No",  acc:"RWP", name:"KeypadOperationEventMask" ],
            0x0042: [ req:"No",  acc:"RWP", name:"RFOperationEventMask" ],
            0x0043: [ req:"No",  acc:"RWP", name:"ManualOperationEventMask" ],
            0x0044: [ req:"No",  acc:"RWP", name:"RFIDOperationEventMask" ],
            0x0045: [ req:"No",  acc:"RWP", name:"KeypadProgrammingEventMask" ],
            0x0046: [ req:"No",  acc:"RWP", name:"RFProgrammingEventMask" ],
            0x0047: [ req:"No",  acc:"RWP", name:"RFIDProgrammingEventMask" ]
        ]
    ],
    0x0102: [
        name: "Window Covering Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"WindowCoveringType" ],
            0x0001: [ req:"No",  acc:"R--", name:"PhysicalClosedLimit – Lift" ],
            0x0002: [ req:"No",  acc:"R--", name:"PhysicalClosedLimit – Tilt" ],
            0x0003: [ req:"No",  acc:"R--", name:"CurrentPosition – Lift" ],
            0x0004: [ req:"No",  acc:"R--", name:"Current Position – Tilt" ],
            0x0005: [ req:"No",  acc:"R--", name:"Number of Actuations – Lift" ],
            0x0006: [ req:"No",  acc:"R--", name:"Number of Actuations – Tilt" ],
            0x0007: [ req:"Yes", acc:"R--", name:"Config/Status" ],
            0x0008: [ req:"Yes", acc:"RSP", name:"Current Position Lift Percentage" ],
            0x0009: [ req:"Yes", acc:"RSP", name:"Current Position Tilt Percentage" ],

            0x0100: [ req:"Yes", acc:"R--", name:"InstalledOpenLimit – Lift" ],
            0x0101: [ req:"Yes", acc:"R--", name:"InstalledClosedLimit – Lift" ],
            0x0102: [ req:"Yes", acc:"R--", name:"InstalledOpenLimit – Tilt" ],
            0x0103: [ req:"Yes", acc:"R--", name:"InstalledClosedLimit – Tilt" ],
            0x0104: [ req:"No",  acc:"RW-", name:"Velocity – Lift" ],
            0x0105: [ req:"No",  acc:"RW-", name:"Acceleration Time – Lift" ],
            0x0106: [ req:"No",  acc:"RW-", name:"Deceleration Time – Lift" ],
            0x0107: [ req:"Yes", acc:"RW-", name:"Mode" ],
            0x0108: [ req:"No",  acc:"RW-", name:"Intermediate Setpoints – Lift" ],
            0x0109: [ req:"No",  acc:"RW-", name:"Intermediate Setpoints – Tilt" ]
        ]
    ],
    0x0200: [
        name: "Pump Configuration and Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"MaxPressure" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MaxSpeed" ],
            0x0002: [ req:"Yes", acc:"R--", name:"MaxFlow" ],
            0x0003: [ req:"No",  acc:"R--", name:"MinConstPressure" ],
            0x0004: [ req:"No",  acc:"R--", name:"MaxConstPressure" ],
            0x0005: [ req:"No",  acc:"R--", name:"MinCompPressure" ],
            0x0006: [ req:"No",  acc:"R--", name:"MaxCompPressure" ],
            0x0007: [ req:"No",  acc:"R--", name:"MinConstSpeed" ],
            0x0008: [ req:"No",  acc:"R--", name:"MaxConstSpeed" ],
            0x0009: [ req:"No",  acc:"R--", name:"MinConstFlow" ],
            0x000A: [ req:"No",  acc:"R--", name:"MaxConstFlow" ],
            0x000B: [ req:"No",  acc:"R--", name:"MinConstTemp" ],
            0x000C: [ req:"No",  acc:"R--", name:"MaxConstTemp" ],

            0x0010: [ req:"No",  acc:"R-P", name:"PumpStatus" ],
            0x0011: [ req:"Yes", acc:"R--", name:"EffectiveOperationMode" ],
            0x0012: [ req:"Yes", acc:"R--", name:"EffectiveControlMode" ],
            0x0013: [ req:"Yes", acc:"R-P", name:"Capacity" ],
            0x0014: [ req:"No",  acc:"R--", name:"Speed" ],
            0x0015: [ req:"No",  acc:"RW-", name:"LifetimeRunningHours" ],
            0x0016: [ req:"No",  acc:"RW-", name:"Power" ],
            0x0017: [ req:"No",  acc:"R--", name:"LifetimeEnergyConsumed" ],

            0x0020: [ req:"Yes", acc:"RW-", name:"OperationMode" ],
            0x0021: [ req:"No",  acc:"RW-", name:"ControlMode" ],
            0x0022: [ req:"No",  acc:"R--", name:"AlarmMask" ]
        ]
    ],
    0x0201: [
        name: "Thermostat Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"LocalTemperature" ],
            0x0001: [ req:"No",  acc:"R--", name:"OutdoorTemperature" ],
            0x0002: [ req:"No",  acc:"R--", name:"Occupancy" ],
            0x0003: [ req:"No",  acc:"R--", name:"AbsMinHeatSetpointLimit" ],
            0x0004: [ req:"No",  acc:"R--", name:"AbsMaxHeatSetpointLimit" ],
            0x0005: [ req:"No",  acc:"R--", name:"AbsMinCoolSetpointLimit" ],
            0x0006: [ req:"No",  acc:"R--", name:"AbsMaxCoolSetpointLimit" ],
            0x0007: [ req:"No",  acc:"R-P", name:"PICoolingDemand" ],
            0x0008: [ req:"No",  acc:"R-P", name:"PIHeatingDemand" ],
            0x0009: [ req:"No",  acc:"RW-", name:"HVACSystemTypeConfiguration" ],

            0x0010: [ req:"No",  acc:"RW-", name:"LocalTemperatureCalibration" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"OccupiedCoolingSetpoint" ],
            0x0012: [ req:"Yes", acc:"RWS", name:"OccupiedHeatingSetpoint" ],
            0x0013: [ req:"No",  acc:"RW-", name:"UnoccupiedCoolingSetpoint" ],
            0x0014: [ req:"No",  acc:"RW-", name:"UnoccupiedHeatingSetpoint" ],
            0x0015: [ req:"No",  acc:"RW-", name:"MinHeatSetpointLimit" ],
            0x0016: [ req:"No",  acc:"RW-", name:"MaxHeatSetpointLimit" ],
            0x0017: [ req:"No",  acc:"RW-", name:"MinCoolSetpointLimit" ],
            0x0018: [ req:"No",  acc:"RW-", name:"MaxCoolSetpointLimit" ],
            0x0019: [ req:"No",  acc:"RW-", name:"MinSetpointDeadBand" ],
            0x001A: [ req:"No",  acc:"RW-", name:"RemoteSensing" ],
            0x001B: [ req:"Yes", acc:"RW-", name:"ControlSequenceOfOperation" ],
            0x001C: [ req:"Yes", acc:"RWS", name:"SystemMode" ],
            0x001D: [ req:"No",  acc:"R--", name:"AlarmMask" ],
            0x001E: [ req:"No",  acc:"R--", name:"ThermostatRunningMode" ],

            0x0020: [ req:"No",  acc:"R--", name:"StartOfWeek" ],
            0x0021: [ req:"No",  acc:"R--", name:"NumberOfWeeklyTransitions" ],
            0x0022: [ req:"No",  acc:"R--", name:"NumberOfDailyTransitions" ],
            0x0023: [ req:"No",  acc:"RW-", name:"TemperatureSetpointHold" ],
            0x0024: [ req:"No",  acc:"RW-", name:"TemperatureSetpointHoldDuration" ],
            0x0025: [ req:"No",  acc:"RW-", name:"ThermostatProgrammingOperationMode" ],
            0x0029: [ req:"No",  acc:"R--", name:"ThermostatRunningState" ],
            
            0x0030: [ req:"No",  acc:"R--", name:"SetpointChangeSource" ],
            0x0031: [ req:"No",  acc:"R--", name:"SetpointChangeAmount" ],
            0x0032: [ req:"No",  acc:"R--", name:"SetpointChangeSourceTimestamp" ],
            
            0x0040: [ req:"No",  acc:"RW-", name:"ACType" ],
            0x0041: [ req:"No",  acc:"RW-", name:"ACCapacity" ],
            0x0042: [ req:"No",  acc:"RW-", name:"ACRefrigerantType" ],
            0x0043: [ req:"No",  acc:"RW-", name:"ACCompressorType" ],
            0x0044: [ req:"No",  acc:"RW-", name:"ACErrorCode" ],
            0x0045: [ req:"No",  acc:"RW-", name:"ACLouverPosition" ],
            0x0046: [ req:"No",  acc:"R--", name:"ACCoilTemperature" ],
            0x0047: [ req:"No",  acc:"RW-", name:"ACCapacityFormat" ]
        ]
    ],
    0x0202: [
        name: "Fan Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"FanMode" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"FanModeSequence" ]
        ]
    ],
    0x0203: [
        name: "Dehumidification Control Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"RelativeHumidity" ],
            0x0001: [ req:"Yes", acc:"R-P", name:"DehumidificationCooling" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"RHDehumidificationSetpoint" ],
            0x0011: [ req:"No",  acc:"RW-", name:"RelativeHumidityMode" ],
            0x0012: [ req:"No",  acc:"RW-", name:"DehumidificationLockout" ],
            0x0013: [ req:"Yes", acc:"RW-", name:"DehumidificationHysteresis" ],
            0x0014: [ req:"Yes", acc:"RW-", name:"DehumidificationMaxCool" ],
            0x0015: [ req:"No",  acc:"RW-", name:"RelativeHumidityDisplay" ]
        ]
    ],
    0x0204: [
        name: "Thermostat User Interface Configuration Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"TemperatureDisplayMode" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"KeypadLockout" ],
            0x0002: [ req:"No",  acc:"RW-", name:"ScheduleProgrammingVisibility" ]
        ]
    ],
    0x0300: [
        name: "Color Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"CurrentHue" ],
            0x0001: [ req:"Yes", acc:"R-P", name:"CurrentSaturation" ],
            0x0002: [ req:"No",  acc:"R--", name:"RemainingTime" ],
            0x0003: [ req:"Yes", acc:"R-P", name:"CurrentX" ],
            0x0004: [ req:"Yes", acc:"R-P", name:"CurrentY" ],
            0x0005: [ req:"No",  acc:"R--", name:"DriftCompensation" ],
            0x0006: [ req:"No",  acc:"R--", name:"CompensationText" ],
            0x0007: [ req:"Yes", acc:"R-P", name:"ColorTemperatureMireds" ],
            0x0008: [ req:"Yes", acc:"R--", name:"ColorMode" ],
            
            0x0010: [ req:"Yes", acc:"R--", name:"NumberOfPrimaries" ],
            0x0011: [ req:"Yes", acc:"R--", name:"Primary1X" ],
            0x0012: [ req:"No",  acc:"R--", name:"Primary1Y" ],
            0x0013: [ req:"Yes", acc:"R--", name:"Primary1Intensity" ],
            0x0015: [ req:"No",  acc:"R--", name:"Primary2X" ],
            0x0016: [ req:"No",  acc:"R--", name:"Primary2Y" ],
            0x0017: [ req:"Yes", acc:"R--", name:"Primary2Intensity" ],
            0x0019: [ req:"Yes", acc:"R--", name:"Primary3X" ],
            0x001A: [ req:"Yes", acc:"R--", name:"Primary3Y" ],
            0x001B: [ req:"Yes", acc:"R--", name:"Primary3Intensity" ],

            0x0020: [ req:"Yes", acc:"R--", name:"Primary4X" ],
            0x0021: [ req:"Yes", acc:"R--", name:"Primary4Y" ],
            0x0022: [ req:"No",  acc:"R--", name:"Primary4Intensity" ],
            0x0024: [ req:"No",  acc:"R--", name:"Primary2X" ],
            0x0025: [ req:"No",  acc:"R--", name:"Primary2Y" ],
            0x0026: [ req:"Yes", acc:"R--", name:"Primary2Intensity" ],
            0x0028: [ req:"Yes", acc:"R--", name:"Primary3X" ],
            0x0029: [ req:"Yes", acc:"R--", name:"Primary3Y" ],
            0x002A: [ req:"Yes", acc:"R--", name:"Primary3Intensity" ],

            0x0030: [ req:"Yes", acc:"RW-", name:"WhitePointX" ],
            0x0031: [ req:"Yes", acc:"RW-", name:"WhitePointY" ],
            0x0032: [ req:"No",  acc:"RW-", name:"ColorPointRX" ],
            0x0033: [ req:"No",  acc:"RW-", name:"ColorPointRY" ],
            0x0034: [ req:"No",  acc:"RW-", name:"ColorPointRIntensity" ],
            0x0036: [ req:"Yes", acc:"RW-", name:"ColorPointGX" ],
            0x0037: [ req:"Yes", acc:"RW-", name:"ColorPointGY" ],
            0x0038: [ req:"Yes", acc:"RW-", name:"ColorPointGIntensity" ],
            0x003A: [ req:"Yes", acc:"RW-", name:"ColorPointBX" ],
            0x003B: [ req:"Yes", acc:"RW-", name:"ColorPointBY" ],
            0x003C: [ req:"Yes", acc:"RW-", name:"ColorPointBIntensity" ],

            0x4000: [ req:"Yes", acc:"R--", name:"EnhancedCurrentHue" ],
            0x4001: [ req:"Yes", acc:"R--", name:"EnhancedColorMode" ],
            0x4002: [ req:"Yes", acc:"R--", name:"ColorLoopActive" ],
            0x4003: [ req:"Yes", acc:"R--", name:"ColorLoopDirection" ],
            0x4004: [ req:"Yes", acc:"R--", name:"ColorLoopTime" ],
            0x4005: [ req:"Yes", acc:"R--", name:"ColorLoopStartEnhancedHue" ],
            0x4006: [ req:"Yes", acc:"R--", name:"ColorLoopStoredEnhancedHue" ],
            0x400A: [ req:"Yes", acc:"R--", name:"ColorCapabilities" ],
            0x400B: [ req:"Yes", acc:"R--", name:"ColorTempPhysicalMinMireds" ],
            0x400C: [ req:"Yes", acc:"R--", name:"ColorTempPhysicalMaxMireds" ],
        ]
    ],
    0x0301: [
        name: "Ballast Configuration Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"PhysicalMinLevel" ],
            0x0001: [ req:"No",  acc:"R--", name:"PhysicalMaxLevel" ],
            0x0002: [ req:"Yes", acc:"R--", name:"BallastStatus" ],

            0x0010: [ req:"No",  acc:"RW-", name:"MinLevel" ],
            0x0011: [ req:"No",  acc:"RW-", name:"MaxLevel" ],
            0x0012: [ req:"No",  acc:"RW-", name:"PowerOnLevel" ],
            0x0013: [ req:"No",  acc:"RW-", name:"PowerOnFadeTime" ],
            0x0014: [ req:"No",  acc:"RW-", name:"IntrinsicBallastFactor" ],
            0x0015: [ req:"No",  acc:"RW-", name:"BallastFactorAdjustment" ],
            
            0x0020: [ req:"No",  acc:"R--", name:"LampQuantity" ],
            
            0x0030: [ req:"No",  acc:"RW-", name:"LampType" ],
            0x0031: [ req:"No",  acc:"RW-", name:"LampManufacturer" ],
            0x0032: [ req:"No",  acc:"RW-", name:"LampRatedHours" ],
            0x0033: [ req:"No",  acc:"RW-", name:"LampBurnHours" ],
            0x0034: [ req:"No",  acc:"RW-", name:"LampAlarmMode" ],
            0x0035: [ req:"No",  acc:"RW-", name:"LampBurnHoursTripPoint" ]
        ]
    ],
    0x0400: [
        name: "Illuminance Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"MeasuredValue" ],
            0x0001: [ req:"Yes", acc:"RP-", name:"MinMeasuredValue" ],
            0x0002: [ req:"Yes", acc:"R--", name:"MaxMeasuredValue" ],
            0x0003: [ req:"No",  acc:"R--", name:"Tolerance" ],
            0x0004: [ req:"No",  acc:"R--", name:"LightSensorType" ]
        ]
    ],
    0x0401: [
        name: "Illuminance Level Sensing Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"LevelStatus" ],
            0x0001: [ req:"No",  acc:"R--", name:"LightSensorType" ],

            0x0010: [ req:"Yes", acc:"RW-", name:"IlluminanceTargetLevel" ]
        ]
    ],
    0x0402: [
        name: "Temperature Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"MeasuredValue" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MinMeasuredValue" ],
            0x0002: [ req:"Yes", acc:"R--", name:"MaxMeasuredValue" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ]
        ]
    ],
    0x0403: [
        name: "Pressure Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"MeasuredValue" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MinMeasuredValue" ],
            0x0002: [ req:"Yes", acc:"R--", name:"MaxMeasuredValue" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ],

            0x0010: [ req:"No",  acc:"R--", name:"ScaledValue" ],
            0x0011: [ req:"No",  acc:"R--", name:"MinScaledValue" ],
            0x0012: [ req:"No",  acc:"R--", name:"MaxScaledValue" ],
            0x0013: [ req:"No",  acc:"R--", name:"ScaledTolerance" ],
            0x0014: [ req:"No",  acc:"R--", name:"Scale" ]
        ]
    ],
    0x0404: [
        name: "Flow Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"MeasuredValue" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MinMeasuredValue" ],
            0x0002: [ req:"Yes", acc:"R--", name:"MaxMeasuredValue" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ]
        ]
    ],
    0x0405: [
        name: "Relative Humidity Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"MeasuredValue" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MinMeasuredValue" ],
            0x0002: [ req:"Yes", acc:"R--", name:"MaxMeasuredValue" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ]
        ]
    ],
    0x0406: [
        name: "Occupancy Sensing Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Occupancy" ],
            0x0001: [ req:"Yes", acc:"R--", name:"OccupancySensorType" ],

            0x0010: [ req:"Yes", acc:"RW-", name:"PIROccupiedToUnoccupiedDelay" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"PIRUnoccupiedToOccupiedDelay" ],
            0x0012: [ req:"Yes", acc:"RW-", name:"PIRUnoccupiedToOccupiedThreshold" ],

            0x0020: [ req:"Yes", acc:"RW-", name:"UltrasonicOccupiedToUnoccupiedDelay" ],
            0x0021: [ req:"Yes", acc:"RW-", name:"UltrasonicUnoccupiedToOccupiedDelay" ],
            0x0022: [ req:"Yes", acc:"RW-", name:"UltrasonicUnoccupiedToOccupiedThreshold" ]
        ]
    ],
    0x0500: [
        name: "IAS Zone Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"ZoneState" ],
            0x0001: [ req:"Yes", acc:"R--", name:"ZoneType" ],
            0x0002: [ req:"Yes", acc:"R--", name:"ZoneStatus" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"IAS_CIE_Address" ],
            0x0011: [ req:"Yes", acc:"R--", name:"ZoneID" ],
            0x0012: [ req:"No",  acc:"R--", name:"NumberOfZoneSensitivityLevelsSupported" ],
            0x0013: [ req:"No",  acc:"RW-", name:"CurrentZoneSensitivityLevel" ]
        ]
    ],
    0x0502: [
        name: "IAS WD Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"MaxDuration" ]
        ]
    ],
    0x0B01: [
        name: "Meter Identification Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"CompanyName" ],
            0x0001: [ req:"Yes", acc:"R--", name:"MeterTypeID" ],
            0x0004: [ req:"Yes", acc:"R--", name:"DataQualityID" ],
            0x0005: [ req:"No",  acc:"RW-", name:"CustomerName" ],
            0x0006: [ req:"No",  acc:"R--", name:"Model" ],
            0x0007: [ req:"No",  acc:"R--", name:"PartNumber" ],
            0x0008: [ req:"No",  acc:"R--", name:"ProductRevision" ],
            0x000A: [ req:"Yes", acc:"R--", name:"SoftwareRevision" ],
            0x000B: [ req:"No",  acc:"R--", name:"UtilityName" ],
            0x000C: [ req:"Yes", acc:"R--", name:"POD" ],
            0x000D: [ req:"Yes", acc:"R--", name:"AvailablePower" ],
            0x000E: [ req:"Yes", acc:"R--", name:"PowerThreshold" ]
        ]
    ],
    0x0B04: [
        name: "Electrical Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"MeasurementType" ],
            
            0x0100: [ req:"No",  acc:"R--", name:"DCVoltage" ],
            0x0101: [ req:"No",  acc:"R--", name:"DCVoltageMin" ],
            0x0102: [ req:"No",  acc:"R--", name:"DCVoltageMax" ],
            0x0103: [ req:"No",  acc:"R--", name:"DCCurrent" ],
            0x0104: [ req:"No",  acc:"R--", name:"DCCurrentMin" ],
            0x0105: [ req:"No",  acc:"R--", name:"DCCurrentMax" ],
            0x0106: [ req:"No",  acc:"R--", name:"DCPower" ],
            0x0107: [ req:"No",  acc:"R--", name:"DCPowerMin" ],
            0x0108: [ req:"No",  acc:"R--", name:"DCPowerMax" ],
            
            0x0200: [ req:"No",  acc:"R--", name:"DCVoltageMultiplier" ],
            0x0201: [ req:"No",  acc:"R--", name:"DCVoltageDivisor" ],
            0x0202: [ req:"No",  acc:"R--", name:"DCCurrentMultiplier" ],
            0x0203: [ req:"No",  acc:"R--", name:"DCCurrentDivisor" ],
            0x0204: [ req:"No",  acc:"R--", name:"DCPowerMultiplier" ],
            0x0205: [ req:"No",  acc:"R--", name:"DCPowerDivisor" ],
            
            0x0300: [ req:"No",  acc:"R--", name:"ACFrequency" ],
            0x0301: [ req:"No",  acc:"R--", name:"ACFrequencyMin" ],
            0x0302: [ req:"No",  acc:"R--", name:"ACFrequencyMax" ],
            0x0303: [ req:"No",  acc:"R--", name:"NeutralCurrent" ],
            0x0304: [ req:"No",  acc:"R--", name:"TotalActivePower" ],
            0x0305: [ req:"No",  acc:"R--", name:"TotalReactivePower" ],
            0x0306: [ req:"No",  acc:"R--", name:"TotalApparentPower" ],
            
            0x0400: [ req:"No",  acc:"R--", name:"ACFrequencyMultiplier" ],
            0x0401: [ req:"No",  acc:"R--", name:"ACFrequencyDivisor" ],
            0x0402: [ req:"No",  acc:"R--", name:"PowerMultiplier" ],
            0x0403: [ req:"No",  acc:"R--", name:"PowerDivisor" ],
            0x0404: [ req:"No",  acc:"R--", name:"HarmonicCurrentMultiplier" ],
            0x0405: [ req:"No",  acc:"R--", name:"PhaseHarmonicCurrentMultiplier" ],
            
            0x0500: [ req:"No",  acc:"R--", name:"Reserved" ],
            0x0501: [ req:"No",  acc:"R--", name:"LineCurrent" ],
            0x0502: [ req:"No",  acc:"R--", name:"ActiveCurrent" ],
            0x0503: [ req:"No",  acc:"R--", name:"ReactiveCurrent" ],
            0x0505: [ req:"No",  acc:"R--", name:"RMSVoltage" ],
            0x0506: [ req:"No",  acc:"R--", name:"RMSVoltageMin" ],
            0x0507: [ req:"No",  acc:"R--", name:"RMSVoltageMax" ],
            0x0508: [ req:"No",  acc:"R--", name:"RMSCurrent" ],
            0x0509: [ req:"No",  acc:"R--", name:"RMSCurrentMin" ],
            0x050A: [ req:"No",  acc:"R--", name:"RMSCurrentMax" ],
            0x050B: [ req:"No",  acc:"R--", name:"ActivePower" ],
            0x050C: [ req:"No",  acc:"R--", name:"ActivePowerMin" ],
            0x050D: [ req:"No",  acc:"R--", name:"ActivePowerMax" ],
            0x050E: [ req:"No",  acc:"R--", name:"ReactivePower" ],
            0x050F: [ req:"No",  acc:"R--", name:"ApparentPower" ],
            0x0510: [ req:"No",  acc:"R--", name:"PowerFactor" ],
            0x0511: [ req:"No",  acc:"RW-", name:"AverageRMSVoltageMeasurementPeriod" ],
            0x0512: [ req:"No",  acc:"RW-", name:"AverageRMSOverVoltageCounter" ],
            0x0513: [ req:"No",  acc:"RW-", name:"AverageRMSUnderVoltageCounter" ],
            0x0514: [ req:"No",  acc:"RW-", name:"RMSExtremeOverVoltagePeriod" ],
            0x0515: [ req:"No",  acc:"RW-", name:"RMSExtremeUnderVoltagePeriod" ],
            0x0516: [ req:"No",  acc:"RW-", name:"RMSVoltageSagPeriod" ],
            0x0517: [ req:"No",  acc:"RW-", name:"RMSVoltageSwellPeriod" ],

            0x0600: [ req:"No",  acc:"R--", name:"ACVoltageMultiplier" ],
            0x0601: [ req:"No",  acc:"R--", name:"ACVoltageDivisor" ],
            0x0602: [ req:"No",  acc:"R--", name:"ACCurrentMultiplier" ],
            0x0603: [ req:"No",  acc:"R--", name:"ACCurrentDivisor" ],
            0x0604: [ req:"No",  acc:"R--", name:"ACPowerMultiplier" ],
            0x0605: [ req:"No",  acc:"R--", name:"ACPowerDivisor" ],

            0x0700: [ req:"No",  acc:"RW-", name:"DCOverloadAlarmsMask" ],
            0x0701: [ req:"No",  acc:"R--", name:"DCVoltageOverload" ],
            0x0702: [ req:"No",  acc:"R--", name:"DCCurrentOverload" ],

            0x0800: [ req:"No",  acc:"RW-", name:"ACAlarmsMask" ],
            0x0801: [ req:"No",  acc:"R--", name:"ACVoltageOverload" ],
            0x0802: [ req:"No",  acc:"R--", name:"ACCurrentOverload" ],
            0x0803: [ req:"No",  acc:"R--", name:"ACActivePowerOverload" ],
            0x0804: [ req:"No",  acc:"R--", name:"ACReactivePowerOverload" ],
            0x0805: [ req:"No",  acc:"R--", name:"AverageRMSOverVoltage" ],
            0x0806: [ req:"No",  acc:"R--", name:"AverageRMSUnderVoltage" ],
            0x0807: [ req:"No",  acc:"RW-", name:"RMSExtremeOverVoltage" ],
            0x0808: [ req:"No",  acc:"RW-", name:"RMSExtremeUnderVoltage" ],
            0x0809: [ req:"No",  acc:"RW-", name:"RMSVoltageSag" ],
            0x080A: [ req:"No",  acc:"RW-", name:"RMSVoltageSwell" ],

            0x0901: [ req:"No",  acc:"R--", name:"LineCurrentPhB" ],
            0x0902: [ req:"No",  acc:"R--", name:"ActiveCurrentPhB" ],
            0x0903: [ req:"No",  acc:"R--", name:"ReactiveCurrentPhB" ],
            0x0905: [ req:"No",  acc:"R--", name:"RMSVoltagePhB" ],
            0x0906: [ req:"No",  acc:"R--", name:"RMSVoltageMinPhB" ],
            0x0907: [ req:"No",  acc:"R--", name:"RMSVoltageMaxPhB" ],
            0x0908: [ req:"No",  acc:"R--", name:"RMSCurrentPhB" ],
            0x0909: [ req:"No",  acc:"R--", name:"RMSCurrentMinPhB" ],
            0x090A: [ req:"No",  acc:"R--", name:"RMSCurrentMaxPhB" ],
            0x090B: [ req:"No",  acc:"R--", name:"ActivePowerPhB" ],
            0x090C: [ req:"No",  acc:"R--", name:"ActivePowerMinPhB" ],
            0x090D: [ req:"No",  acc:"R--", name:"ActivePowerMaxPhB" ],
            0x090E: [ req:"No",  acc:"R--", name:"ReactivePowerPhB" ],
            0x090F: [ req:"No",  acc:"R--", name:"ApparentPowerPhB" ],
            0x0910: [ req:"No",  acc:"R--", name:"PowerFactorPhB" ],
            0x0911: [ req:"No",  acc:"RW-", name:"AverageRMSVoltageMeasurementPeriodPhB" ],
            0x0912: [ req:"No",  acc:"RW-", name:"AverageRMSOverVoltageCounterPhB" ],
            0x0913: [ req:"No",  acc:"RW-", name:"AverageRMSUnderVoltageCounterPhB" ],
            0x0914: [ req:"No",  acc:"RW-", name:"RMSExtremeOverVoltagePeriodPhB" ],
            0x0915: [ req:"No",  acc:"RW-", name:"RMSExtremeUnderVoltagePeriodPhB" ],
            0x0916: [ req:"No",  acc:"RW-", name:"RMSVoltageSagPeriodPhB" ],
            0x0917: [ req:"No",  acc:"RW-", name:"RMSVoltageSwellPeriodPhB" ],

            0x0A01: [ req:"No",  acc:"R--", name:"LineCurrentPhC" ],
            0x0A02: [ req:"No",  acc:"R--", name:"ActiveCurrentPhC" ],
            0x0A03: [ req:"No",  acc:"R--", name:"ReactiveCurrentPhC" ],
            0x0A05: [ req:"No",  acc:"R--", name:"RMSVoltagePhC" ],
            0x0A06: [ req:"No",  acc:"R--", name:"RMSVoltageMinPhC" ],
            0x0A07: [ req:"No",  acc:"R--", name:"RMSVoltageMaxPhC" ],
            0x0A08: [ req:"No",  acc:"R--", name:"RMSCurrentPhC" ],
            0x0A09: [ req:"No",  acc:"R--", name:"RMSCurrentMinPhC" ],
            0x0A0A: [ req:"No",  acc:"R--", name:"RMSCurrentMaxPhC" ],
            0x0A0B: [ req:"No",  acc:"R--", name:"ActivePowerPhC" ],
            0x0A0C: [ req:"No",  acc:"R--", name:"ActivePowerMinPhC" ],
            0x0A0D: [ req:"No",  acc:"R--", name:"ActivePowerMaxPhC" ],
            0x0A0E: [ req:"No",  acc:"R--", name:"ReactivePowerPhC" ],
            0x0A0F: [ req:"No",  acc:"R--", name:"ApparentPowerPhC" ],
            0x0A10: [ req:"No",  acc:"R--", name:"PowerFactorPhC" ],
            0x0A11: [ req:"No",  acc:"RW-", name:"AverageRMSVoltageMeasurementPeriodPhC" ],
            0x0A12: [ req:"No",  acc:"RW-", name:"AverageRMSOverVoltageCounterPhC" ],
            0x0A13: [ req:"No",  acc:"RW-", name:"AverageRMSUnderVoltageCounterPhC" ],
            0x0A14: [ req:"No",  acc:"RW-", name:"RMSExtremeOverVoltagePeriodPhC" ],
            0x0A15: [ req:"No",  acc:"RW-", name:"RMSExtremeUnderVoltagePeriodPhC" ],
            0x0A16: [ req:"No",  acc:"RW-", name:"RMSVoltageSagPeriodPhC" ],
            0x0A17: [ req:"No",  acc:"RW-", name:"RMSVoltageSwellPeriodPhC" ],
        ]
    ],
    0x0B05: [
        name: "Diagnostics Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"NumberOfResets" ],
            0x0001: [ req:"No",  acc:"R--", name:"PersistentMemoryWrites" ],

            0x0100: [ req:"No",  acc:"R--", name:"MacRxBcast" ],
            0x0101: [ req:"No",  acc:"R--", name:"MacTxBcast" ],
            0x0102: [ req:"No",  acc:"R--", name:"MacRxUcast" ],
            0x0103: [ req:"No",  acc:"R--", name:"MacTxUcast" ],
            0x0104: [ req:"No",  acc:"R--", name:"MacTxUcastRetry" ],
            0x0105: [ req:"No",  acc:"R--", name:"MacTxUcastFail" ],
            0x0106: [ req:"No",  acc:"R--", name:"APSRxBcast" ],
            0x0107: [ req:"No",  acc:"R--", name:"APSTxBcast" ],
            0x0108: [ req:"No",  acc:"R--", name:"APSRxUcast" ],
            0x0109: [ req:"No",  acc:"R--", name:"APSTxUcastSuccess" ],
            0x010A: [ req:"No",  acc:"R--", name:"APSTxUcastRetry" ],
            0x010B: [ req:"No",  acc:"R--", name:"APSTxUcastFail" ],
            0x010C: [ req:"No",  acc:"R--", name:"RouteDiscInitiated" ],
            0x010D: [ req:"No",  acc:"R--", name:"NeighborAdded" ],
            0x010E: [ req:"No",  acc:"R--", name:"NeighborRemoved" ],
            0x010F: [ req:"No",  acc:"R--", name:"NeighborStale" ],
            0x0110: [ req:"No",  acc:"R--", name:"JoinIndication" ],
            0x0111: [ req:"No",  acc:"R--", name:"ChildMoved" ],
            0x0112: [ req:"No",  acc:"R--", name:"NWKFCFailure" ],
            0x0113: [ req:"No",  acc:"R--", name:"APSFCFailure" ],
            0x0114: [ req:"No",  acc:"R--", name:"APSUnauthorizedKey" ],
            0x0115: [ req:"No",  acc:"R--", name:"NWKDecryptFailures" ],
            0x0116: [ req:"No",  acc:"R--", name:"APSDecryptFailures" ],
            0x0117: [ req:"No",  acc:"R--", name:"PacketBufferAllocateFailures" ],
            0x0118: [ req:"No",  acc:"R--", name:"RelayedUcast" ],
            0x0119: [ req:"No",  acc:"R--", name:"PhytoMACqueuelimitreached" ],
            0x011A: [ req:"No",  acc:"R--", name:"PacketValidatedropcount" ],
            0x011B: [ req:"No",  acc:"R--", name:"AverageMACRetryPerAPSMessageSent" ],
            0x011C: [ req:"No",  acc:"R--", name:"LastMessageLQI" ],
            0x011D: [ req:"No",  acc:"R--", name:"LastMessageRSSI" ]
        ]
    ],
]

/**
 * Knockturn Alley - Simple toolkit driver to help developers peer deep into the guts of Zigbee devices.
 *
 * @version 1.3.0
 * @see https://dan-danache.github.io/hubitat/knockturn-alley-driver/
 * @see https://dan-danache.github.io/hubitat/knockturn-alley-driver/CHANGELOG
 * @see https://community.hubitat.com/t/dev-knockturn-alley/125167
 */
import groovy.time.TimeCategory
import groovy.transform.Field

@Field static final def HEXADECIMAL_PATTERN = ~/\p{XDigit}+/

metadata {
    definition(name:"Knockturn Alley", namespace:"dandanache", singleThreaded:true, author:"Dan Danache", importUrl:"https://raw.githubusercontent.com/dan-danache/hubitat/master/knockturn-alley-driver/knockturn-alley.groovy") {
        command "a01Legilimens"
        command "a02Scourgify", [
            [name: "Raw data", type: "ENUM", constraints: [
                "1 - Keep raw data",
                "2 - Remove raw data",
            ]],
        ]
        command "b01Accio", [
            [name: "What to retrieve", type: "ENUM", constraints: [
                "1 - Get attribute current value",
                "2 - Check attribute reporting",
            ]],
            [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
            [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
        ]
        command "b02EverteStatum", [
            [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
            [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Attribute*", description: "Attribute ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
            [name: "Data type*", description: "Attribute data type", type: "ENUM", constraints:
                 ZCL_DATA_TYPES.keySet()
                     .findAll { ZCL_DATA_TYPES[it].bytes != "0" && ZCL_DATA_TYPES[it].bytes != "var" }
                     .sort()
                     .collect { "0x${Utils.hex it, 2}: ${ZCL_DATA_TYPES[it].name} (${ZCL_DATA_TYPES[it].bytes} bytes)" }
            ],
            [name: "Value*", description: "Attribute value - hex format (e.g.: 0001 - for uint16)", type: "STRING"],
        ]
        command "c01Imperio", [
            [name: "Endpoint*", description: "Endpoint ID - hex format (e.g.: 0x01)", type: "STRING"],
            [name: "Cluster*", description: "Cluster ID - hex format (e.g.: 0x0001)", type: "STRING"],
            [name: "Command*", description: "Command ID - hex format (e.g.: 0x01)", type: "STRING"],
            [name: "Manufacturer", description: "Manufacturer Code - hex format (e.g.: 0x117C)", type: "STRING"],
            [name: "Payload", description: "Raw payload - sent as is, spaces are removed", type: "STRING"],
        ]
        command "c02Obliviate", [
            [name: "What to forget", type: "ENUM", constraints: [
                "1 - Our state variables (ka_*) - Restore previous driver state",
                "2 - All state variables",
                "3 - Device data",
                "4 - Scheduled jobs configured by the previous driver",
                "5 - Everything",
            ]],
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
    table += "<thead>"
    //table += "<tr><th colspan=9><a href=\"javascript:void\" onclick=\"navigator.clipboard.writeText(document.getElementById('ka_report').outerHTML)\">Copy</a></tr>"
    table += "<tr><th>ID</th><th>Name</th><th>Required</th><th>Access</th><th>Type</th><th>Bytes</th><th>Encoding</th><th>Value</th><th>Reporting</th></tr>"
    table += "</thead><tbody>"
    
    state.ka_endpoints?.sort().each { endpoint ->
        table += "<tr><th colspan=9 style='background-color:SteelBlue; color:White'>Endpoint: 0x${Utils.hex endpoint, 2}</th></tr>"
        table += "<tr><th colspan=9>Out Clusters: ${state["ka_outClusters_${endpoint}"]?.sort().collect { "0x${Utils.hex it, 4} (${ZCL_CLUSTERS.get(it)?.name ?: "Unknown Cluster"})" }.join(", ")}</th></tr>"
        state["ka_inClusters_${endpoint}"]?.sort().each { cluster ->
            table += "<tr><th colspan=9>In Cluster: ${"0x${Utils.hex cluster, 4} (${ZCL_CLUSTERS.get(cluster)?.name ?: "Unknown Cluster"})"}</th></tr>"

            Set<Integer> attributes = []
            getState()?.each {
                if (it.key.startsWith("ka_attribute_${endpoint}_${cluster}") || it.key.startsWith("ka_attributeValue_${endpoint}_${cluster}")) {
                    attributes += Integer.parseInt it.key.split("_").last()
                }
            }
           
            if (attributes.size() > 0) {
                table += "<tr><td colspan=9><b>Attributes</b></td></tr>"
                attributes.sort().each { attribute ->
                    def attributeSpec = ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(attribute)
                    def attributeType = state["ka_attribute_${endpoint}_${cluster}_${attribute}"]
                    def attributeValue = state["ka_attributeValue_${endpoint}_${cluster}_${attribute}"]
                    def attributeReporting = state["ka_attributeReporting_${endpoint}_${cluster}_${attribute}"]
                    
                    // Cluster Revision global attribute
                    if (attribute == 0xFFFD) {
                        attributeSpec = [ type:0x21, req:"Yes", acc:"R--", name:"Cluster Revision" ]
                        attributeType = ZCL_DATA_TYPES[0x21]
                    }

                    // Pretty value
                    String value = "${attributeValue?.value ?: "--"}"
                    if (attributeValue?.value && attributeSpec?.constraints) {
                        value += " = ${attributeSpec.constraints[Utils.dec(attributeValue.value)]}"
                    }
                    if (attributeValue?.value && attributeSpec?.decorate) {
                        value += " = ${attributeSpec.decorate(attributeValue.value)}"
                    }
                    
                    table += "<tr>"
                    table += "<td><pre>0x${Utils.hex attribute, 4}</pre></td>"
                    table += "<td>${attributeSpec?.name ?: "--"}</td>"
                    table += "<td>${attributeSpec?.req ?: "--"}</td>"
                    table += "<td><pre>${attributeSpec?.acc ?: "--"}</pre></td>"
                    table += "<td>${attributeType?.name ?: "--"}</td>"
                    table += "<td>${attributeType?.bytes ?: "--"}</td>"
                    table += "<td>${attributeValue?.encoding ?: "--"}</td>"
                    table += "<td><pre>${value}</pre></td>"
                    table += "<td>${attributeReporting ?: "--"}</td>"
                    table += "</tr>"
                }
            }

            Set<Integer> commands = []
            getState()?.each {
                if (it.key.startsWith("ka_command_${endpoint}_${cluster}")) {
                    commands += Integer.parseInt it.key.split("_").last()
                }
            }
            if (commands.size() > 0) {
                table += "<tr><td colspan=9><b>Commands</b></td></tr>"
                commands.sort().each { command ->
                    def commandSpec = ZCL_CLUSTERS.get(cluster)?.get("commands")?.get(command)
                    table += "<tr>"
                    table += "<td><pre>0x${Utils.hex command, 2}</pre></td>"
                    table += "<td>${commandSpec?.name ?: "--"}</td>"
                    table += "<td>${commandSpec?.req ?: "--"}</td>"
                    table += "<td colspan=6></td>"
                    table += "</tr>"
                }
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

def b01Accio(operation, endpointHex, clusterHex, attributeHex, manufacturerHex="") {
    Log.info "🪄 Accio: ${operation} (endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex}, manufacturer=${manufacturerHex})"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid Attribute ID: ${clusterHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid Manufacturer Code: ${manufacturerHex}")
    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameStart = "10 75"
    if (manufacturer != null) {
        frameStart = "04 ${Utils.payload manufacturer} 75"
    }

    switch (operation) {
        case { it.startsWith("1 - ") }:
            return Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart} 00 ${Utils.payload attribute}}"])

        case { it.startsWith("2 - ") }:
            return Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart} 08 00 ${Utils.payload attribute}}"])
    }
}

def b02EverteStatum(endpointHex, clusterHex, attributeHex, manufacturerHex="", typeStr, valueHex) {
    Log.info "🪄 Everte Statum: endpoint=${endpointHex}, cluster=${clusterHex}, attribute=${attributeHex}, manufacturer=${manufacturerHex}, type=${typeStr}, value=${valueHex}"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!attributeHex.startsWith("0x") || attributeHex.size() != 6) return Log.error("Invalid Attribute ID: ${clusterHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid Manufacturer Code: ${manufacturerHex}")
    if (valueHex && !HEXADECIMAL_PATTERN.matcher(valueHex).matches()) return Log.error("Invalid Value: ${payload}")

    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer attribute = Integer.parseInt attributeHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameStart = "10 75"
    if (manufacturer != null) {
        frameStart = "04 ${Utils.payload manufacturer} 75"
    }
 
    Integer type = Integer.parseInt typeStr.substring(2, 4), 16
    String value = valueHex.replaceAll " ", ""
    Integer typeLen = Integer.parseInt ZCL_DATA_TYPES[type].bytes
    if (value.size() != typeLen * 2) return Log.error("Invalid Value: It must have exactly ${typeLen} bytes but you provided ${value.size()}: ${valueHex}")

    // Transform BE -> LE: "123456" -> ["1", "2", "3", "4", "5", "6"] -> [["1", "2"], ["3", "4"], ["5", "6"]] -> ["12", "34", "56"] -> ["56", "34", "12"] -> "563412"
    value = (value.split("") as List).collate(2).collect { it.join() }.reverse().join()
    
    // Send zigbee command
    Utils.sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {${frameStart} 02 ${Utils.payload attribute} ${typeStr.substring(2, 4)} ${value}}"])
}

def c01Imperio(endpointHex, clusterHex, commandHex, manufacturerHex="", payload="") {
    Log.info "🪄 Imperio: endpoint=${endpointHex}, cluster=${clusterHex}, command=${commandHex}, manufacturer=${manufacturerHex}, payload=${payload}"

    if (!endpointHex.startsWith("0x") || endpointHex.size() != 4) return Log.error("Invalid Endpoint ID: ${endpointHex}")
    if (!clusterHex.startsWith("0x") || clusterHex.size() != 6) return Log.error("Invalid Cluster ID: ${clusterHex}")
    if (!commandHex.startsWith("0x") || commandHex.size() != 4) return Log.error("Invalid Command ID: ${commandHex}")
    if (manufacturerHex && (!manufacturerHex.startsWith("0x") || manufacturerHex.size() != 6)) return Log.error("Invalid value for Manufacturer Code: ${manufacturerHex}")
    if (payload && !HEXADECIMAL_PATTERN.matcher(payload).matches()) return Log.error("Invalid Payload: ${payload}")
    
    Integer endpoint = Integer.parseInt endpointHex.substring(2), 16
    Integer cluster = Integer.parseInt clusterHex.substring(2), 16
    Integer command = Integer.parseInt commandHex.substring(2), 16

    Integer manufacturer = manufacturerHex ? Integer.parseInt(manufacturerHex.substring(2), 16) : null
    String frameTail = ""
    if (manufacturer != null) {
        frameTail = " {${Utils.hex manufacturer}}"
    }

    // Send zigbee command
    Utils.sendZigbeeCommands(["he cmd 0x${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x${Utils.hex cluster} 0x${Utils.hex command, 2} {${payload}}${frameTail}"])
}

def c02Obliviate(operation) {
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
            Utils.processedZigbeeMessage "Read Attribute Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attribute=0x${Utils.hex msg.attrInt}, value=${msg.value}"

            msg.additionalAttrs?.each {
                attributesValues[it.attrInt] = [encoding: it.encoding, value: it.value]
                Utils.processedZigbeeMessage "Additional Attribute", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attribute=0x${Utils.hex it.attrInt}, value=${it.value}"
            }
        
            return State.addAttributesValues(endpoint, cluster, attributesValues)

        // DefaultResponse (0x0B) :=  { 08:CommandIdentifier, 08:Status }
        // Example: [00, 80] -> command = 0x00, status = MALFORMED_COMMAND (0x80)
        case { contains it, [commandInt:0x0B] }:
            if (msg.data[1] != "00") {
                return Utils.failedZigbeeMessage("Default Response", msg, msg.data[1])
            }
            
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt
            Integer command = Utils.dec msg.data[0]
        
            return Utils.processedZigbeeMessage("Default Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, command=0x${Utils.hex command, 2}")
        
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
            return Utils.processedZigbeeMessage("Read Reporting Configuration Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attribute=0x${Utils.hex attribute}, minPeriod=${minPeriod}, maxPeriod=${maxPeriod}")
        
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
                    cmds += "he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {10 00 0C 00 00 FF}"
                    
                    // Discover cluster commands
                    cmds += "he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {10 00 11 00 FF}"
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
            return Utils.processedZigbeeMessage("Simple Descriptor Response", "endpoint=0x${Utils.hex endpoint, 2}, inClusters=${Utils.hexs inClusters}, outClusters=${Utils.hexs outClusters}")

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
                data = data.drop 3

                // Ignore trailing AttributeReportingStatus
                if (attribute == 0xFFFE) continue

                attributes[attribute] = type
            }

            if (attributes.size() != 0) {
                List<String> cmds = []
                attributes.keySet().collate(2).each { attrs ->

                    // Read attribute value (use batches of 3 to reduce mesh traffic)
                    cmds += "he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {10 00 00 ${attrs.collect { Utils.payload it }.join()}}"

                    // If attribute is reportable, also inquire its reporting status
                    attrs.each {
                        if (ZCL_CLUSTERS.get(cluster)?.get("attributes")?.get(it)?.get("acc")?.endsWith("P")) {
                            cmds += "he raw ${device.deviceNetworkId} 0x${Utils.hex endpoint, 2} 0x01 0x${Utils.hex cluster} {10 00 08 00 ${Utils.payload it}}"
                        }
                    }
                }
                Utils.sendZigbeeCommands delayBetween(cmds, 1000)
                State.addAttributes endpoint, cluster, attributes
            }
            return Utils.processedZigbeeMessage("Discover Attributes Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, attributes=${attributes}")

        // DiscoverCommandsReceivedResponse := { 08:Complete?, n*08:CommandIdentifier }
        // Example: [01, 00, 01, 40] -> commands: 0x00, 0x01, 0x40
        case { contains it, [commandInt:0x12] }:
            Integer endpoint = Utils.dec msg.sourceEndpoint
            Integer cluster = msg.clusterInt

            List<String> data = msg.data.drop 1
            List<Integer> commands = data.collect {Utils.dec it  }

            State.addCommands endpoint, cluster, commands
            return Utils.processedZigbeeMessage("Discover Commands Received Response", "endpoint=0x${Utils.hex endpoint, 2}, cluster=0x${Utils.hex cluster}, commands=${commands}")

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
    hex: { Integer value, Integer chars = 4 -> "${zigbee.convertToHexString value, chars}" },
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
        if (status == null && msg.data.size() >= 1) status = msg.data[0]
        String prettyStatus = status == null ? "NULL" : ZCL_STATUS[Integer.parseInt(status, 16)]
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

    addCommands: { Integer endpoint, Integer cluster, List<Integer> commands ->
        commands.each {
            state["ka_command_${endpoint}_${cluster}_${it}"] = ZCL_CLUSTERS.get(cluster)?.get("commands")?.get(it) ?: [:]
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
            0x0000: [ type:0x20, req:"Yes", acc:"R--", name:"ZCL Version" ],
            0x0001: [ type:0x20, req:"No",  acc:"R--", name:"Application Version" ],
            0x0002: [ type:0x20, req:"No",  acc:"R--", name:"Stack Version" ],
            0x0003: [ type:0x20, req:"No",  acc:"R--", name:"HW Version" ],
            0x0004: [ type:0x42, req:"No",  acc:"R--", name:"Manufacturer Name" ],
            0x0005: [ type:0x42, req:"No",  acc:"R--", name:"Model Identifier" ],
            0x0006: [ type:0x42, req:"Yes", acc:"R--", name:"Date Code" ],
            0x0007: [ type:0x30, req:"No",  acc:"R--", name:"Power Source", constraints: [
                0x00: "Unknown",
                0x01: "Mains (single phase)",
                0x02: "Mains (3 phase)",
                0x03: "Battery",
                0x04: "DC source",
                0x05: "Emergency mains constantly powered",
                0x06: "Emergency mains and transfer switch"
            ]],
            0x0010: [ type:0x42, req:"No",  acc:"RW-", name:"Location Description" ],
            0x0011: [ type:0x30, req:"No",  acc:"RW-", name:"Physical Environment" ],
            0x0012: [ type:0x10, req:"No",  acc:"RW-", name:"Device Enabled", constraints: [
                0x00: "Disabled",
                0x01: "Enabled"
            ]],
            0x0013: [ type:0x18, req:"No",  acc:"RW-", name:"Alarm Mask" ],
            0x0014: [ type:0x18, req:"No",  acc:"RW-", name:"Disable Local Config" ],
            0x4000: [ type:0x42, req:"No",  acc:"R--", name:"SW Build ID" ]
        ],
        commands: [
            0x00: [ req:"No", name:"Reset to Factory Defaults" ]
        ]
    ],
    0x0001: [
        name: "Power Configuration Cluster",
        attributes: [
            0x0000: [ type:0x21, req:"No",  acc:"R--", name:"Mains Voltage" ],
            0x0001: [ type:0x20, req:"No",  acc:"R--", name:"Mains Frequency" ],
            
            0x0010: [ type:0x18, req:"No",  acc:"RW-", name:"Mains Alarm Mask" ],
            0x0011: [ type:0x21, req:"No",  acc:"RW-", name:"Mains Voltage Min Threshold" ],
            0x0012: [ type:0x21, req:"No",  acc:"RW-", name:"Mains Voltage Max Threshold" ],
            0x0013: [ type:0x21, req:"No",  acc:"RW-", name:"Mains Voltage Dwell Trip Point" ],
            
            0x0020: [ type:0x20, req:"No",  acc:"R--", name:"Battery Voltage" ],
            0x0021: [ type:0x20, req:"No",  acc:"R-P", name:"Battery Percentage Remaining", decorate: { value -> "${Math.round(Integer.parseInt(value, 16) / 2) as Integer}%" } ],

            0x0030: [ type:0x42, req:"No",  acc:"RW-", name:"Battery Manufacturer" ],
            0x0031: [ type:0x30, req:"No",  acc:"RW-", name:"Battery Size", constraints: [
                0x00: "No battery",
                0x01: "Built in",
                0x02: "Other",
                0x03: "AA",
                0x04: "AAA",
                0x05: "C",
                0x06: "D",
                0x07: "CR2 (IEC: CR17355 / ANSI: 5046LC)",
                0x08: "CR123A (IEC: CR17345 / ANSI: 5018LC",
                0xFF: "Battery"
            ]],
            0x0032: [ type:0x21, req:"No",  acc:"RW-", name:"Battery AH Rating" ],
            0x0033: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Quantity" ],
            0x0034: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Rated Voltage" ],
            0x0035: [ type:0x18, req:"No",  acc:"RW-", name:"Battery Alarm Mask" ],
            0x0036: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Voltage Min Threshold" ],
            0x0037: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Voltage Threshold 1" ],
            0x0038: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Voltage Threshold 2" ],
            0x0039: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Voltage Threshold 3" ],
            0x003A: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Percentage Min Threshold" ],
            0x003B: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Percentage Threshold 1" ],
            0x003C: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Percentage Threshold 2" ],
            0x003D: [ type:0x20, req:"No",  acc:"RW-", name:"Battery Percentage Threshold 3" ],
            0x003E: [ type:0x1B, req:"No",  acc:"R--", name:"Battery Alarm State" ]
        ]
    ],
    0x0002: [
        name: "Temperature Configuration Cluster",
        attributes: [
            0x0000: [ type:0x29, req:"Yes", acc:"R--", name:"Current Temperature" ],
            0x0001: [ type:0x29, req:"No",  acc:"R--", name:"Min Temp Experienced" ],
            0x0002: [ type:0x29, req:"No",  acc:"R--", name:"Max Temp Experienced" ],
            0x0003: [ type:0x21, req:"No",  acc:"R--", name:"Over Temp Total Dwell" ],

            0x0010: [ type:0x18, req:"No",  acc:"RW-", name:"Device Temp Alarm Mask" ],
            0x0011: [ type:0x29, req:"No",  acc:"RW-", name:"Low Temp Threshold" ],
            0x0012: [ type:0x29, req:"No",  acc:"RW-", name:"High Temp Threshold" ],
            0x0013: [ type:0x22, req:"No",  acc:"RW-", name:"Low Temp Dwell Trip Point" ],
            0x0014: [ type:0x22, req:"No",  acc:"RW-", name:"High Temp Dwell Trip Point" ]
        ]
    ],
    0x0003: [
        name: "Identify Cluster",
        attributes: [
            0x0000: [ type:0x21, req:"Yes", acc:"RW-", name:"Identify Time" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Identify" ],
            0x01: [ req:"Yes", name:"Identify Query" ],
            0x40: [ req:"No",  name:"Trigger Effect" ]
        ]
    ],
    0x0004: [
        name: "Groups Cluster",
        attributes: [
            0x0000: [ type:0x18, req:"Yes", acc:"R--", name:"Name Support" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Add Group" ],
            0x01: [ req:"Yes", name:"View Group" ],
            0x02: [ req:"Yes", name:"Get Group Membership" ],
            0x03: [ req:"Yes", name:"Remove Group" ],
            0x04: [ req:"Yes", name:"Remove All Groups" ],
            0x05: [ req:"Yes", name:"Add Group If Identifying" ]
        ]
    ],
    0x0005: [
        name: "Scenes Cluster",
        attributes: [
            0x0000: [ type:0x20, req:"Yes", acc:"R--", name:"Scene Count" ],
            0x0001: [ type:0x20, req:"Yes", acc:"R--", name:"Current Scene" ],
            0x0002: [ type:0x21, req:"Yes", acc:"R--", name:"Current Group" ],
            0x0003: [ type:0x10, req:"Yes", acc:"R--", name:"Scene Valid" ],
            0x0004: [ type:0x18, req:"Yes", acc:"R--", name:"Name Support" ],
            0x0005: [ type:0xf0, req:"No",  acc:"R--", name:"Last Configured By" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Add Scene" ],
            0x01: [ req:"Yes", name:"View Scene" ],
            0x02: [ req:"Yes", name:"Remove Scene" ],
            0x03: [ req:"Yes", name:"Remove All Scenes" ],
            0x04: [ req:"Yes", name:"Store Scene" ],
            0x05: [ req:"Yes", name:"Recall Scene" ],
            0x06: [ req:"Yes", name:"Get Scene Membership" ],
            0x40: [ req:"No",  name:"Enhanced Add Scene" ],
            0x41: [ req:"No",  name:"Enhanced View Scene" ],
            0x42: [ req:"No",  name:"Copy Scene" ]
        ]
    ],
    0x0006: [
        name: "On/Off Cluster",
        attributes: [
            0x0000: [ type:0x10, req:"Yes", acc:"R-P", name:"On Off" ],
            0x4000: [ type:0x10, req:"No",  acc:"R--", name:"Global Scene Control" ],
            0x4001: [ type:0x21, req:"No",  acc:"RW-", name:"On Time" ],
            0x4002: [ type:0x21, req:"No",  acc:"RW-", name:"Off Wait Time" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Off" ],
            0x01: [ req:"Yes", name:"On" ],
            0x02: [ req:"Yes", name:"Toggle" ],
            0x40: [ req:"No",  name:"Off With Effect" ],
            0x41: [ req:"No",  name:"On With Recall Global Scene" ],
            0x42: [ req:"No",  name:"On With Timed Off" ]
        ]
    ],
    0x0007: [
        name: "On/Off Switch Configuration Cluster",
        attributes: [
            0x0000: [ type:0x30, req:"Yes", acc:"R--", name:"Switch Type", constraints: [
                0x00: "Toggle",
                0x01: "Momentary",
                0x02: "Multifunction"
            ]],
            0x0010: [ type:0x30, req:"Yes", acc:"RW-", name:"Switch Actions", constraints: [
                0x00: "On",
                0x01: "Off",
                0x02: "Toggle"
            ]]
        ]
    ],
    0x0008: [
        name: "Level Control Cluster",
        attributes: [
            0x0000: [ type:0x20, req:"Yes", acc:"R-P", name:"Current Level" ],
            0x0001: [ type:0x21, req:"No",  acc:"R--", name:"Remaining Time" ],
            0x0010: [ type:0x21, req:"No",  acc:"RW-", name:"On Off Transition Time" ],
            0x0011: [ type:0x20, req:"No",  acc:"RW-", name:"On Level" ],
            0x0012: [ type:0x21, req:"No",  acc:"RW-", name:"On Transition Time" ],
            0x0013: [ type:0x21, req:"No",  acc:"RW-", name:"Off Transition Time" ],
            0x0014: [ type:0x21, req:"No",  acc:"RW-", name:"Default Move Rate" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Move To Level" ],
            0x01: [ req:"Yes", name:"Move" ],
            0x02: [ req:"Yes", name:"Step" ],
            0x03: [ req:"Yes", name:"Stop" ],
            0x04: [ req:"Yes", name:"Move To Level With On/Off" ],
            0x05: [ req:"Yes", name:"Move With On/Off" ],
            0x06: [ req:"Yes", name:"Step With On/Off" ],
            0x07: [ req:"Yes", name:"Stop" ]
        ]
    ],
    0x0009: [
        name: "Alarms Cluster",
        attributes: [
            0x0000: [ type:0x21, req:"No",  acc:"R--", name:"Alarm Count" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Move To Level" ],
            0x01: [ req:"Yes", name:"Move" ],
            0x02: [ req:"Yes", name:"Step" ],
            0x03: [ req:"Yes", name:"Stop" ]
        ]
    ],
    0x000A: [
        name: "Time Cluster",
        attributes: [
            0x0000: [ type:0xE2, req:"Yes", acc:"RW-", name:"Time" ],
            0x0001: [ type:0x18, req:"Yes", acc:"RW-", name:"Time Status" ],
            0x0002: [ type:0x2B, req:"No",  acc:"RW-", name:"Time Zone" ],
            0x0003: [ type:0x23, req:"No",  acc:"RW-", name:"Dst Start" ],
            0x0004: [ type:0x23, req:"No",  acc:"RW-", name:"Dst End" ],
            0x0005: [ type:0x2B, req:"No",  acc:"RW-", name:"Dst Shift" ],
            0x0006: [ type:0x23, req:"No",  acc:"R--", name:"Standard Time" ],
            0x0007: [ type:0x23, req:"No",  acc:"R--", name:"Local Time" ],
            0x0008: [ type:0xE2, req:"No",  acc:"R--", name:"Last Set Time" ],
            0x0009: [ type:0xE2, req:"No",  acc:"RW-", name:"Valid Until Time" ]
        ]
    ],
    0x000B: [
        name: "RSSI Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"Location Type" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"Location Method" ],
            0x0002: [ req:"No",  acc:"R--", name:"Location Age" ],
            0x0003: [ req:"No",  acc:"R--", name:"Quality Measure" ],
            0x0004: [ req:"No",  acc:"R--", name:"Number Of Devices" ],

            0x0010: [ req:"No",  acc:"RW-", name:"Coordinate 1" ],
            0x0011: [ req:"No",  acc:"RW-", name:"Coordinate 2" ],
            0x0012: [ req:"No",  acc:"RW-", name:"Coordinate 3" ],
            0x0013: [ req:"Yes", acc:"RW-", name:"Power" ],
            0x0014: [ req:"Yes", acc:"RW-", name:"Path Loss Exponent" ],
            0x0015: [ req:"No",  acc:"RW-", name:"Reporting Period" ],
            0x0016: [ req:"No",  acc:"RW-", name:"Calculation Period" ],
            0x0017: [ req:"No",  acc:"RW-", name:"Number RSSI Measurements" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Set Absolute Location" ],
            0x01: [ req:"Yes", name:"Set Device Configuration" ],
            0x02: [ req:"Yes", name:"Get Device Configuration" ],
            0x03: [ req:"Yes", name:"Get Location Data" ],
            0x04: [ req:"No",  name:"RSSI Response" ],
            0x05: [ req:"No",  name:"Send Pings" ],
            0x06: [ req:"No",  name:"Anchor Node Announce" ]
        ]
    ],
    0x000C: [
        name: "Analog Input Cluster",
        attributes: [
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x0041: [ req:"No",  acc:"Rw-", name:"Max Present Value" ],
            0x0045: [ req:"No",  acc:"Rw-", name:"Min Present Value" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"RWP", name:"Present Value" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x006A: [ req:"No",  acc:"Rw-", name:"Resolution" ],
            0x006F: [ req:"Yes", acc:"R-P", name:"Status Flags" ],
            0x0075: [ req:"No",  acc:"Rw-", name:"Engineering Units" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x000D: [
        name: "Analog Output Cluster",
        attributes: [
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x0041: [ req:"No",  acc:"Rw-", name:"Max Present Value" ],
            0x0045: [ req:"No",  acc:"Rw-", name:"Min Present Value" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"RWP", name:"Present Value" ],
            0x0057: [ req:"No",  acc:"RW-", name:"Priority Array" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"Relinquish Default" ],
            0x006A: [ req:"No",  acc:"Rw-", name:"Resolution" ],
            0x006F: [ req:"Yes", acc:"R-P", name:"Status Flags" ],
            0x0075: [ req:"No",  acc:"Rw-", name:"Engineering Units" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x000E: [
        name: "Analog Value Cluster",
        attributes: [
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"Present Value" ],
            0x0057: [ req:"No",  acc:"Rw-", name:"Priority Array" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"Relinquish Default" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0075: [ req:"No",  acc:"Rw-", name:"Engineering Units" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x000F: [
        name: "Binary Input Cluster",
        attributes: [
            0x0004: [ req:"No",  acc:"Rw-", name:"Active Text" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x002E: [ req:"No",  acc:"Rw-", name:"Inactive Text" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0054: [ req:"Yes", acc:"R--", name:"Polarity" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"Present Value" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x0010: [
        name: "Binary Output Cluster",
        attributes: [
            0x0004: [ req:"No",  acc:"Rw-", name:"Active Text" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x002E: [ req:"No",  acc:"Rw-", name:"Inactive Text" ],
            0x0042: [ req:"No",  acc:"Rw-", name:"Minimum Off Time" ],
            0x0043: [ req:"No",  acc:"Rw-", name:"Minimum On Time" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0054: [ req:"Yes", acc:"R--", name:"Polarity" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"Present Value" ],
            0x0057: [ req:"No",  acc:"RW-", name:"Priority Array" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"Relinquish Default" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x0011: [
        name: "Binary Value Cluster",
        attributes: [
            0x0004: [ req:"No",  acc:"Rw-", name:"Active Text" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x002E: [ req:"No",  acc:"Rw-", name:"Inactive Text" ],
            0x0042: [ req:"No",  acc:"Rw-", name:"Minimum Off Time" ],
            0x0043: [ req:"No",  acc:"Rw-", name:"Minimum On Time" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"Present Value" ],
            0x0057: [ req:"No",  acc:"RW-", name:"Priority Array" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"Relinquish Default" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x0012: [
        name: "Multistate Input Cluster",
        attributes: [
            0x000E: [ req:"No",  acc:"Rw-", name:"State Text" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x004A: [ req:"Yes", acc:"Rw-", name:"Number Of States" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"Rw-", name:"Present Value" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x0013: [
        name: "Multistate Output Cluster",
        attributes: [
            0x000E: [ req:"No",  acc:"Rw-", name:"State Text" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x004A: [ req:"Yes", acc:"Rw-", name:"Number Of States" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"RW-", name:"Present Value" ],
            0x0057: [ req:"No",  acc:"R--", name:"Priority Array" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"Relinquish Default" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x0014: [
        name: "Multistate Value Cluster",
        attributes: [
            0x000E: [ req:"No",  acc:"Rw-", name:"State Text" ],
            0x001C: [ req:"No",  acc:"Rw-", name:"Description" ],
            0x004A: [ req:"Yes", acc:"Rw-", name:"Number Of States" ],
            0x0051: [ req:"Yes", acc:"Rw-", name:"Out Of Service" ],
            0x0055: [ req:"Yes", acc:"RW-", name:"Present Value" ],
            0x0057: [ req:"No",  acc:"RW-", name:"Priority Array" ],
            0x0067: [ req:"No",  acc:"Rw-", name:"Reliability" ],
            0x0068: [ req:"No",  acc:"Rw-", name:"Relinquish Default" ],
            0x006F: [ req:"Yes", acc:"R--", name:"Status Flags" ],
            0x0100: [ req:"No",  acc:"R--", name:"Application Type" ]
        ]
    ],
    0x0015: [
        name: "Commissioning Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"Short Address" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"Extended PAN ID" ],
            0x0002: [ req:"Yes", acc:"RW-", name:"PAN ID" ],
            0x0003: [ req:"Yes", acc:"RW-", name:"Channel Mask" ],
            0x0004: [ req:"Yes", acc:"RW-", name:"Protocol Version" ],
            0x0005: [ req:"Yes", acc:"RW-", name:"Stack Profile" ],
            0x0006: [ req:"Yes", acc:"RW-", name:"Startup Control" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"Trust Center Address" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"Trust Center Master Key" ],
            0x0012: [ req:"Yes", acc:"RW-", name:"Network Key" ],
            0x0013: [ req:"Yes", acc:"RW-", name:"Use Insecure Join" ],
            0x0014: [ req:"Yes", acc:"RW-", name:"Preconfigured Link Key" ],
            0x0015: [ req:"Yes", acc:"RW-", name:"Network Key Seq Num" ],
            0x0016: [ req:"Yes", acc:"RW-", name:"Network Key Type" ],
            0x0017: [ req:"Yes", acc:"RW-", name:"Network Manager Address" ],
            
            0x0020: [ req:"No",  acc:"RW-", name:"Scan Attempts" ],
            0x0021: [ req:"No",  acc:"RW-", name:"Time Between Scans" ],
            0x0022: [ req:"No",  acc:"RW-", name:"Rejoin Interval" ],
            
            0x0030: [ req:"No",  acc:"RW-", name:"Indirect Poll Rate" ],
            0x0031: [ req:"No",  acc:"R--", name:"Parent Retry Threshold" ],
            
            0x0040: [ req:"No",  acc:"RW-", name:"Concentrator Flag" ],
            0x0041: [ req:"No",  acc:"RW-", name:"Concentrator Radius" ],
            0x0042: [ req:"No",  acc:"RW-", name:"Concentrator Discovery Time" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Restart Device" ],
            0x01: [ req:"No",  name:"Save Startup Parameters" ],
            0x02: [ req:"No",  name:"Restore Startup Parameter" ],
            0x03: [ req:"Yes", name:"Reset Startup Parameters" ]
        ]
    ],
    0x0019: [
        name: "OTA Upgrade Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Upgrade Server ID" ],
            0x0001: [ req:"No",  acc:"R--", name:"File Offset" ],
            0x0002: [ req:"No",  acc:"R--", name:"Current File Version" ],
            0x0003: [ req:"No",  acc:"R--", name:"Current Zigbee Stack Version" ],
            0x0004: [ req:"No",  acc:"R--", name:"Downloaded File Version" ],
            0x0005: [ req:"No",  acc:"R--", name:"Downloaded Zigbee Stack Version" ],
            0x0006: [ req:"Yes", acc:"R--", name:"Image Upgrade Status" ],
            0x0007: [ req:"No",  acc:"R--", name:"Manufacturer ID" ],
            0x0008: [ req:"No",  acc:"R--", name:"Image Type ID" ],
            0x0009: [ req:"No",  acc:"R--", name:"Minimum Block Period" ],
            0x000A: [ req:"No",  acc:"R--", name:"Image Stamp" ]
        ],
        commands: [
            0x00: [ req:"No",  name:"Image Notify" ],
            0x01: [ req:"Yes", name:"Query Next Image Request" ],
            0x02: [ req:"Yes", name:"Query Next Image Response" ],
            0x03: [ req:"Yes", name:"Image Block Request" ],
            0x04: [ req:"No",  name:"Image Page Request" ],
            0x05: [ req:"Yes", name:"Image Block Response" ],
            0x06: [ req:"Yes", name:"Upgrade End Request" ],
            0x07: [ req:"Yes", name:"Upgrade End Response" ],
            0x08: [ req:"No",  name:"Query Device Specific File Request" ],
            0x09: [ req:"No",  name:"Query Device Specific File Response" ]
        ]
    ],
    0x0021: [
        name: "Green Power Cluster"
    ],
    0x001A: [
        name: "Power Profile Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Total Profile Num" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Multiple Scheduling" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Energy Formatting" ],
            0x0003: [ req:"Yes", acc:"R-P", name:"Energy Remote" ],
            0x0004: [ req:"Yes", acc:"RWP", name:"Schedule Mode" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Power Profile Request" ],
            0x01: [ req:"Yes", name:"Power Profile State Request" ],
            0x02: [ req:"Yes", name:"Get Power Profile Price Response" ],
            0x03: [ req:"Yes", name:"Get Overall Schedule Price Response" ],
            0x04: [ req:"Yes", name:"Energy Phases Schedule Notification" ],
            0x05: [ req:"Yes", name:"Energy Phases Schedule Response" ],
            0x06: [ req:"Yes", name:"Power Profile Schedule Constraints Request" ],
            0x07: [ req:"Yes", name:"Energy Phases Schedule State Request" ],
            0x08: [ req:"Yes", name:"Get Power Profile Price Extended Response" ]
        ]
    ],
    0x0020: [
        name: "Poll Cluster",
        attributes: [
            0x0000: [ type:0x23, req:"Yes", acc:"RW-", name:"Check-in Interval" ],
            0x0001: [ type:0x23, req:"Yes", acc:"R--", name:"Long Poll Interval" ],
            0x0002: [ type:0x21, req:"Yes", acc:"R--", name:"Short Poll Interval" ],
            0x0003: [ type:0x21, req:"Yes", acc:"RW-", name:"Fast Poll Timeout" ],
            0x0004: [ type:0x23, req:"No",  acc:"R--", name:"Check-in Interval Min" ],
            0x0005: [ type:0x23, req:"No",  acc:"R--", name:"Long Poll Interval Min" ],
            0x0006: [ type:0x21, req:"No",  acc:"R--", name:"Fast Poll Timeout Max" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Check-in" ]
        ]
    ],
    0x0100: [
        name: "Shade Configuration Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"Physical Closed Limit" ],
            0x0001: [ req:"No",  acc:"R--", name:"Motor Step Size" ],
            0x0002: [ req:"Yes", acc:"RW-", name:"Status" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"Closed Limit" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"Mode" ]
        ]
    ],
    0x0101: [
        name: "Door Lock Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Lock State" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Lock Type" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Actuator Enabled" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Door State" ],
            0x0004: [ req:"No",  acc:"RW-", name:"Door Open Events" ],
            0x0005: [ req:"No",  acc:"RW-", name:"Door Closed Events" ],
            0x0006: [ req:"No",  acc:"RW-", name:"Open Period" ],
            
            0x0010: [ req:"No",  acc:"R--", name:"Number Of Log Records Supported" ],
            0x0011: [ req:"No",  acc:"R--", name:"Number Of Total Users Supported" ],
            0x0012: [ req:"No",  acc:"R--", name:"Number Of PIN Users Supported" ],
            0x0013: [ req:"No",  acc:"R--", name:"Number Of RFID Users Supported" ],
            0x0014: [ req:"No",  acc:"R--", name:"Number Of Week Day Schedules Supported Per User" ],
            0x0015: [ req:"No",  acc:"R--", name:"Number Of Year Day Schedules Supported Per User" ],
            0x0016: [ req:"No",  acc:"R--", name:"Number Of Holiday Schedules Supported" ],
            0x0017: [ req:"No",  acc:"R--", name:"Max PIN Code Length" ],
            0x0018: [ req:"No",  acc:"R--", name:"Min PIN Code Length" ],
            0x0019: [ req:"No",  acc:"R--", name:"Max RFID Code Length" ],
            0x001A: [ req:"No",  acc:"R--", name:"Min RFID Code Length" ],
            
            0x0020: [ req:"No",  acc:"RwP", name:"Enable Logging" ],
            0x0021: [ req:"No",  acc:"RwP", name:"Language" ],
            0x0022: [ req:"No",  acc:"RwP", name:"Settings" ],
            0x0023: [ req:"No",  acc:"RwP", name:"Auto Relock Time" ],
            0x0024: [ req:"No",  acc:"RwP", name:"Sound Volume" ],
            0x0025: [ req:"No",  acc:"RwP", name:"Operating Mode" ],
            0x0026: [ req:"No",  acc:"R--", name:"Supported Operating Modes" ],
            0x0027: [ req:"No",  acc:"R-P", name:"Default Configuration Register" ],
            0x0028: [ req:"No",  acc:"RwP", name:"Enable Local Programming" ],
            0x0029: [ req:"No",  acc:"RWP", name:"Enable One Touch Locking" ],
            0x002A: [ req:"No",  acc:"RWP", name:"Enable Inside Status LED" ],
            0x002B: [ req:"No",  acc:"RWP", name:"Enable Privacy Mode Button" ],
            
            0x0030: [ req:"No",  acc:"RwP", name:"Wrong Code Entry Limit" ],
            0x0031: [ req:"No",  acc:"RwP", name:"User Code Temporary Disable Time" ],
            0x0032: [ req:"No",  acc:"RwP", name:"Send PIN Over The Air" ],
            0x0033: [ req:"No",  acc:"RwP", name:"Require PIN For RF Operation" ],
            0x0034: [ req:"No",  acc:"R-P", name:"Zigbee Security Level" ],
            
            0x0040: [ req:"No",  acc:"RWP", name:"Alarm Mask" ],
            0x0041: [ req:"No",  acc:"RWP", name:"Keypad Operation Event Mask" ],
            0x0042: [ req:"No",  acc:"RWP", name:"RF Operation Event Mask" ],
            0x0043: [ req:"No",  acc:"RWP", name:"Manual Operation Event Mask" ],
            0x0044: [ req:"No",  acc:"RWP", name:"RFID Operation Event Mask" ],
            0x0045: [ req:"No",  acc:"RWP", name:"Keypad Programming Event Mask" ],
            0x0046: [ req:"No",  acc:"RWP", name:"RF Programming Event Mask" ],
            0x0047: [ req:"No",  acc:"RWP", name:"RFID Programming Event Mask" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Lock Door" ],
            0x01: [ req:"Yes", name:"Unlock Door" ],
            0x02: [ req:"No",  name:"Toggle" ],
            0x03: [ req:"No",  name:"Unlock with Timeout" ],
            0x04: [ req:"No",  name:"Get Log Record" ],
            0x05: [ req:"No",  name:"Set PIN Code" ],
            0x06: [ req:"No",  name:"Get PIN Code" ],
            0x07: [ req:"No",  name:"Clear PIN Code" ],
            0x08: [ req:"No",  name:"Clear All PIN Codes" ],
            0x09: [ req:"No",  name:"Set User Status" ],
            0x0A: [ req:"No",  name:"Get User Status" ],
            0x0B: [ req:"No",  name:"Set Weekday Schedule" ],
            0x0C: [ req:"No",  name:"Get Weekday Schedule" ],
            0x0D: [ req:"No",  name:"Clear Weekday Schedule" ],
            0x0E: [ req:"No",  name:"Set Year Day Schedule" ],
            0x0F: [ req:"No",  name:"Get Year Day Schedule" ],
            0x10: [ req:"No",  name:"Clear Year Day Schedule" ],
            0x11: [ req:"No",  name:"Set Holiday Schedule" ],
            0x12: [ req:"No",  name:"Get Holiday Schedule" ],
            0x13: [ req:"No",  name:"Clear Holiday Schedule" ],
            0x14: [ req:"No",  name:"Set User Type" ],
            0x15: [ req:"No",  name:"Get User Type" ],
            0x16: [ req:"No",  name:"Set RFID Code" ],
            0x17: [ req:"No",  name:"Get RFID Code" ],
            0x18: [ req:"No",  name:"Clear RFID Code" ],
            0x19: [ req:"No",  name:"Clear All RFID Codes" ]
        ]
    ],
    0x0102: [
        name: "Window Covering Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Window Covering Type" ],
            0x0001: [ req:"No",  acc:"R--", name:"Physical Closed Limit – Lift" ],
            0x0002: [ req:"No",  acc:"R--", name:"Physical Closed Limit – Tilt" ],
            0x0003: [ req:"No",  acc:"R--", name:"Current Position – Lift" ],
            0x0004: [ req:"No",  acc:"R--", name:"Current Position – Tilt" ],
            0x0005: [ req:"No",  acc:"R--", name:"Number Of Actuations – Lift" ],
            0x0006: [ req:"No",  acc:"R--", name:"Number Of Actuations – Tilt" ],
            0x0007: [ req:"Yes", acc:"R--", name:"Config/Status" ],
            0x0008: [ req:"Yes", acc:"RSP", name:"Current Position Lift Percentage" ],
            0x0009: [ req:"Yes", acc:"RSP", name:"Current Position Tilt Percentage" ],

            0x0100: [ req:"Yes", acc:"R--", name:"Installed Open Limit – Lift" ],
            0x0101: [ req:"Yes", acc:"R--", name:"Installed Closed Limit – Lift" ],
            0x0102: [ req:"Yes", acc:"R--", name:"Installed Open Limit – Tilt" ],
            0x0103: [ req:"Yes", acc:"R--", name:"Installed Closed Limit – Tilt" ],
            0x0104: [ req:"No",  acc:"RW-", name:"Velocity – Lift" ],
            0x0105: [ req:"No",  acc:"RW-", name:"Acceleration Time – Lift" ],
            0x0106: [ req:"No",  acc:"RW-", name:"Deceleration Time – Lift" ],
            0x0107: [ req:"Yes", acc:"RW-", name:"Mode" ],
            0x0108: [ req:"No",  acc:"RW-", name:"Intermediate Setpoints – Lift" ],
            0x0109: [ req:"No",  acc:"RW-", name:"Intermediate Setpoints – Tilt" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Up / Open" ],
            0x01: [ req:"Yes", name:"Down / Close" ],
            0x02: [ req:"Yes", name:"Stop" ],
            0x04: [ req:"No",  name:"Go To Lift Value" ],
            0x05: [ req:"No",  name:"Go to Lift Percentage" ],
            0x07: [ req:"No",  name:"Go to Tilt Value" ],
            0x08: [ req:"No",  name:"Go to Tilt Percentage" ]
        ]
    ],
    0x0200: [
        name: "Pump Configuration and Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Max Pressure" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Max Speed" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Max Flow" ],
            0x0003: [ req:"No",  acc:"R--", name:"Min Const Pressure" ],
            0x0004: [ req:"No",  acc:"R--", name:"Max Const Pressure" ],
            0x0005: [ req:"No",  acc:"R--", name:"Min Comp Pressure" ],
            0x0006: [ req:"No",  acc:"R--", name:"Max Comp Pressure" ],
            0x0007: [ req:"No",  acc:"R--", name:"Min Const Speed" ],
            0x0008: [ req:"No",  acc:"R--", name:"Max Const Speed" ],
            0x0009: [ req:"No",  acc:"R--", name:"Min Const Flow" ],
            0x000A: [ req:"No",  acc:"R--", name:"Max Const Flow" ],
            0x000B: [ req:"No",  acc:"R--", name:"Min Const Temp" ],
            0x000C: [ req:"No",  acc:"R--", name:"Max Const Temp" ],

            0x0010: [ req:"No",  acc:"R-P", name:"Pump Status" ],
            0x0011: [ req:"Yes", acc:"R--", name:"Effective Operation Mode" ],
            0x0012: [ req:"Yes", acc:"R--", name:"Effective Control Mode" ],
            0x0013: [ req:"Yes", acc:"R-P", name:"Capacity" ],
            0x0014: [ req:"No",  acc:"R--", name:"Speed" ],
            0x0015: [ req:"No",  acc:"RW-", name:"Lifetime Running Hours" ],
            0x0016: [ req:"No",  acc:"RW-", name:"Power" ],
            0x0017: [ req:"No",  acc:"R--", name:"Lifetime Energy Consumed" ],

            0x0020: [ req:"Yes", acc:"RW-", name:"Operation Mode" ],
            0x0021: [ req:"No",  acc:"RW-", name:"Control Mode" ],
            0x0022: [ req:"No",  acc:"R--", name:"Alarm Mask" ]
        ]
    ],
    0x0201: [
        name: "Thermostat Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Local Temperature" ],
            0x0001: [ req:"No",  acc:"R--", name:"Outdoor Temperature" ],
            0x0002: [ req:"No",  acc:"R--", name:"Occupancy" ],
            0x0003: [ req:"No",  acc:"R--", name:"Abs Min Heat Setpoint Limit" ],
            0x0004: [ req:"No",  acc:"R--", name:"Abs Max Heat Setpoint Limit" ],
            0x0005: [ req:"No",  acc:"R--", name:"Abs Min Cool Setpoint Limit" ],
            0x0006: [ req:"No",  acc:"R--", name:"Abs Max Cool Setpoint Limit" ],
            0x0007: [ req:"No",  acc:"R-P", name:"PI Cooling Demand" ],
            0x0008: [ req:"No",  acc:"R-P", name:"PI Heating Demand" ],
            0x0009: [ req:"No",  acc:"RW-", name:"HVAC System Type Configuration" ],

            0x0010: [ req:"No",  acc:"RW-", name:"Local Temperature Calibration" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"Occupied Cooling Setpoint" ],
            0x0012: [ req:"Yes", acc:"RWS", name:"Occupied Heating Setpoint" ],
            0x0013: [ req:"No",  acc:"RW-", name:"Unoccupied Cooling Setpoint" ],
            0x0014: [ req:"No",  acc:"RW-", name:"Unoccupied Heating Setpoint" ],
            0x0015: [ req:"No",  acc:"RW-", name:"Min Heat Setpoint Limit" ],
            0x0016: [ req:"No",  acc:"RW-", name:"Max Heat Setpoint Limit" ],
            0x0017: [ req:"No",  acc:"RW-", name:"Min Cool Setpoint Limit" ],
            0x0018: [ req:"No",  acc:"RW-", name:"Max Cool Setpoint Limit" ],
            0x0019: [ req:"No",  acc:"RW-", name:"Min Setpoint Dead Band" ],
            0x001A: [ req:"No",  acc:"RW-", name:"Remote Sensing" ],
            0x001B: [ req:"Yes", acc:"RW-", name:"Control Sequence Of Operation" ],
            0x001C: [ req:"Yes", acc:"RWS", name:"System Mode" ],
            0x001D: [ req:"No",  acc:"R--", name:"Alarm Mask" ],
            0x001E: [ req:"No",  acc:"R--", name:"Thermostat Running Mode" ],

            0x0020: [ req:"No",  acc:"R--", name:"Start Of Week" ],
            0x0021: [ req:"No",  acc:"R--", name:"Number Of Weekly Transitions" ],
            0x0022: [ req:"No",  acc:"R--", name:"Number Of Daily Transitions" ],
            0x0023: [ req:"No",  acc:"RW-", name:"Temperature Setpoint Hold" ],
            0x0024: [ req:"No",  acc:"RW-", name:"Temperature Setpoint Hold Duration" ],
            0x0025: [ req:"No",  acc:"RW-", name:"Thermostat Programmin gOperation Mode" ],
            0x0029: [ req:"No",  acc:"R--", name:"Thermostat Running State" ],
            
            0x0030: [ req:"No",  acc:"R--", name:"Setpoint Change Source" ],
            0x0031: [ req:"No",  acc:"R--", name:"Setpoint Change Amount" ],
            0x0032: [ req:"No",  acc:"R--", name:"Setpoint Change Source Timestamp" ],
            
            0x0040: [ req:"No",  acc:"RW-", name:"AC Type" ],
            0x0041: [ req:"No",  acc:"RW-", name:"AC Capacity" ],
            0x0042: [ req:"No",  acc:"RW-", name:"AC Refrigerant Type" ],
            0x0043: [ req:"No",  acc:"RW-", name:"AC Compressor Type" ],
            0x0044: [ req:"No",  acc:"RW-", name:"AC Error Code" ],
            0x0045: [ req:"No",  acc:"RW-", name:"AC Louver Position" ],
            0x0046: [ req:"No",  acc:"R--", name:"AC Coil Temperature" ],
            0x0047: [ req:"No",  acc:"RW-", name:"AC Capacity Format" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Setpoint Raise/Lower" ],
            0x01: [ req:"No",  name:"Set Weekly Schedule" ],
            0x02: [ req:"No",  name:"Get Weekly Schedule" ],
            0x03: [ req:"No",  name:"Clear Weekly Schedule" ],
            0x04: [ req:"No",  name:"Get Relay Status Log" ]
        ]
    ],
    0x0202: [
        name: "Fan Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"Fan Mode" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"Fan Mode Sequence" ]
        ]
    ],
    0x0203: [
        name: "Dehumidification Control Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"Relative Humidity" ],
            0x0001: [ req:"Yes", acc:"R-P", name:"Dehumidificatio nCooling" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"RH Dehumidification Setpoint" ],
            0x0011: [ req:"No",  acc:"RW-", name:"Relative Humidity Mode" ],
            0x0012: [ req:"No",  acc:"RW-", name:"Dehumidification Lockout" ],
            0x0013: [ req:"Yes", acc:"RW-", name:"Dehumidification Hysteresis" ],
            0x0014: [ req:"Yes", acc:"RW-", name:"Dehumidification Max Cool" ],
            0x0015: [ req:"No",  acc:"RW-", name:"RelativeHumidity Display" ]
        ]
    ],
    0x0204: [
        name: "Thermostat User Interface Configuration Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Temperature Display Mode" ],
            0x0001: [ req:"Yes", acc:"RW-", name:"Keypad Lockout" ],
            0x0002: [ req:"No",  acc:"RW-", name:"Schedule Programming Visibility" ]
        ]
    ],
    0x0300: [
        name: "Color Control Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"CurrentHue" ],
            0x0001: [ req:"Yes", acc:"R-P", name:"Current Saturation" ],
            0x0002: [ req:"No",  acc:"R--", name:"Remaining Time" ],
            0x0003: [ req:"Yes", acc:"R-P", name:"CurrentX" ],
            0x0004: [ req:"Yes", acc:"R-P", name:"CurrentY" ],
            0x0005: [ req:"No",  acc:"R--", name:"Drift Compensation" ],
            0x0006: [ req:"No",  acc:"R--", name:"Compensation Text" ],
            0x0007: [ req:"Yes", acc:"R-P", name:"Color Temperature Mireds" ],
            0x0008: [ req:"Yes", acc:"R--", name:"Color Mode" ],
            
            0x0010: [ req:"Yes", acc:"R--", name:"Number Of Primaries" ],
            0x0011: [ req:"Yes", acc:"R--", name:"Primary 1 X" ],
            0x0012: [ req:"No",  acc:"R--", name:"Primary 1 Y" ],
            0x0013: [ req:"Yes", acc:"R--", name:"Primary 1 Intensity" ],
            0x0015: [ req:"No",  acc:"R--", name:"Primary 2 X" ],
            0x0016: [ req:"No",  acc:"R--", name:"Primary 2 Y" ],
            0x0017: [ req:"Yes", acc:"R--", name:"Primary 2 Intensity" ],
            0x0019: [ req:"Yes", acc:"R--", name:"Primary 3 X" ],
            0x001A: [ req:"Yes", acc:"R--", name:"Primary 3 Y" ],
            0x001B: [ req:"Yes", acc:"R--", name:"Primary 3 Intensity" ],

            0x0020: [ req:"Yes", acc:"R--", name:"Primary 4 X" ],
            0x0021: [ req:"Yes", acc:"R--", name:"Primary 4 Y" ],
            0x0022: [ req:"No",  acc:"R--", name:"Primary 4 Intensity" ],
            0x0024: [ req:"No",  acc:"R--", name:"Primary 2 X" ],
            0x0025: [ req:"No",  acc:"R--", name:"Primary 2 Y" ],
            0x0026: [ req:"Yes", acc:"R--", name:"Primary 2 Intensity" ],
            0x0028: [ req:"Yes", acc:"R--", name:"Primary 3 X" ],
            0x0029: [ req:"Yes", acc:"R--", name:"Primary 3 Y" ],
            0x002A: [ req:"Yes", acc:"R--", name:"Primary 3 Intensity" ],

            0x0030: [ req:"Yes", acc:"RW-", name:"White Point X" ],
            0x0031: [ req:"Yes", acc:"RW-", name:"White Point Y" ],
            0x0032: [ req:"No",  acc:"RW-", name:"Color Point R X" ],
            0x0033: [ req:"No",  acc:"RW-", name:"Color Point R Y" ],
            0x0034: [ req:"No",  acc:"RW-", name:"Color Point R Intensity" ],
            0x0036: [ req:"Yes", acc:"RW-", name:"Color Point G X" ],
            0x0037: [ req:"Yes", acc:"RW-", name:"Color Point G Y" ],
            0x0038: [ req:"Yes", acc:"RW-", name:"Color Point G Intensity" ],
            0x003A: [ req:"Yes", acc:"RW-", name:"Color Point B X" ],
            0x003B: [ req:"Yes", acc:"RW-", name:"Color Point B Y" ],
            0x003C: [ req:"Yes", acc:"RW-", name:"Color Point B Intensity" ],

            0x4000: [ req:"Yes", acc:"R--", name:"Enhanced Current Hue" ],
            0x4001: [ req:"Yes", acc:"R--", name:"Enhanced Color Mode" ],
            0x4002: [ req:"Yes", acc:"R--", name:"Color Loop Active" ],
            0x4003: [ req:"Yes", acc:"R--", name:"Color Loop Direction" ],
            0x4004: [ req:"Yes", acc:"R--", name:"Color Loop Time" ],
            0x4005: [ req:"Yes", acc:"R--", name:"Color Loop Start Enhanced Hue" ],
            0x4006: [ req:"Yes", acc:"R--", name:"Color Loop Stored Enhanced Hue" ],
            0x400A: [ req:"Yes", acc:"R--", name:"Color Capabilities" ],
            0x400B: [ req:"Yes", acc:"R--", name:"Color Temp Physical Min Mireds" ],
            0x400C: [ req:"Yes", acc:"R--", name:"Color Temp Physical Max Mireds" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Move to Hue" ],
            0x01: [ req:"Yes", name:"Move Hue" ],
            0x02: [ req:"Yes", name:"Step Hue" ],
            0x03: [ req:"Yes", name:"Move to Saturation" ],
            0x04: [ req:"Yes", name:"Move Saturation" ],
            0x05: [ req:"Yes", name:"Step Saturation" ],
            0x06: [ req:"Yes", name:"Move to Hue and Saturation" ],
            0x07: [ req:"Yes", name:"Move to Color" ],
            0x08: [ req:"Yes", name:"Move Color" ],
            0x09: [ req:"Yes", name:"Step Color" ],
            0x0A: [ req:"Yes", name:"Move to Color Temperature" ],
            
            0x40: [ req:"Yes", name:"Enhanced Move to Hue" ],
            0x41: [ req:"Yes", name:"Enhanced Move Hue" ],
            0x42: [ req:"Yes", name:"Enhanced Step Hue" ],
            0x43: [ req:"Yes", name:"Enhanced Move to Hue and Saturation" ],
            0x44: [ req:"Yes", name:"Color Loop Set" ],
            0x47: [ req:"Yes", name:"Stop Move Step" ],
            0x4B: [ req:"Yes", name:"Move Color Temperature" ],
            0x4C: [ req:"Yes", name:"Step Color Temperature" ]
        ]
    ],
    0x0301: [
        name: "Ballast Configuration Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"Physical Min Level" ],
            0x0001: [ req:"No",  acc:"R--", name:"Physical Max Level" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Ballast Status" ],

            0x0010: [ req:"No",  acc:"RW-", name:"Min Level" ],
            0x0011: [ req:"No",  acc:"RW-", name:"Max Level" ],
            0x0012: [ req:"No",  acc:"RW-", name:"Power On Level" ],
            0x0013: [ req:"No",  acc:"RW-", name:"Power On Fade Time" ],
            0x0014: [ req:"No",  acc:"RW-", name:"Intrinsic Ballast Factor" ],
            0x0015: [ req:"No",  acc:"RW-", name:"Ballast Factor Adjustment" ],
            
            0x0020: [ req:"No",  acc:"R--", name:"Lamp Quantity" ],
            
            0x0030: [ req:"No",  acc:"RW-", name:"Lamp Type" ],
            0x0031: [ req:"No",  acc:"RW-", name:"Lamp Manufacturer" ],
            0x0032: [ req:"No",  acc:"RW-", name:"Lamp Rated Hours" ],
            0x0033: [ req:"No",  acc:"RW-", name:"Lamp Burn Hours" ],
            0x0034: [ req:"No",  acc:"RW-", name:"Lamp Alarm Mode" ],
            0x0035: [ req:"No",  acc:"RW-", name:"Lamp Burn Hours Trip Point" ]
        ]
    ],
    0x0400: [
        name: "Illuminance Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Measured Value" ],
            0x0001: [ req:"Yes", acc:"RP-", name:"Min Measured Value" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Max Measured Value" ],
            0x0003: [ req:"No",  acc:"R--", name:"Tolerance" ],
            0x0004: [ req:"No",  acc:"R--", name:"Light Sensor Type" ]
        ]
    ],
    0x0401: [
        name: "Illuminance Level Sensing Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Level Status" ],
            0x0001: [ req:"No",  acc:"R--", name:"Light Sensor Type" ],

            0x0010: [ req:"Yes", acc:"RW-", name:"Illuminance Target Level" ]
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
            0x0000: [ req:"Yes", acc:"R-P", name:"Measured Value" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Min Measured Value" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Max Measured Value" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ],

            0x0010: [ req:"No",  acc:"R--", name:"Scaled Value" ],
            0x0011: [ req:"No",  acc:"R--", name:"Min Scaled Value" ],
            0x0012: [ req:"No",  acc:"R--", name:"Max Scaled Value" ],
            0x0013: [ req:"No",  acc:"R--", name:"Scaled Tolerance" ],
            0x0014: [ req:"No",  acc:"R--", name:"Scale" ]
        ]
    ],
    0x0404: [
        name: "Flow Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Measured Value" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Min Measured Value" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Max Measured Value" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ]
        ]
    ],
    0x0405: [
        name: "Relative Humidity Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Measured Value" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Min Measured Value" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Max Measured Value" ],
            0x0003: [ req:"No",  acc:"R-P", name:"Tolerance" ]
        ]
    ],
    0x0406: [
        name: "Occupancy Sensing Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R-P", name:"Occupancy" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Occupancy Sensor Type" ],

            0x0010: [ req:"Yes", acc:"RW-", name:"PIR Occupied To Unoccupied Delay" ],
            0x0011: [ req:"Yes", acc:"RW-", name:"PIR Unoccupied To Occupied Delay" ],
            0x0012: [ req:"Yes", acc:"RW-", name:"PIR Unoccupied To Occupied Threshold" ],

            0x0020: [ req:"Yes", acc:"RW-", name:"Ultrasonic Occupied To Unoccupied Delay" ],
            0x0021: [ req:"Yes", acc:"RW-", name:"Ultrasonic Unoccupied To Occupied Delay" ],
            0x0022: [ req:"Yes", acc:"RW-", name:"Ultrasonic Unoccupied To Occupied Threshold" ]
        ]
    ],
    0x0500: [
        name: "IAS Zone Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Zone State" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Zone Type" ],
            0x0002: [ req:"Yes", acc:"R--", name:"Zone Status" ],
            
            0x0010: [ req:"Yes", acc:"RW-", name:"IAS CIE Address" ],
            0x0011: [ req:"Yes", acc:"R--", name:"Zone ID" ],
            0x0012: [ req:"No",  acc:"R--", name:"Number Of Zone Sensitivity Levels Supported" ],
            0x0013: [ req:"No",  acc:"RW-", name:"Current Zone Sensitivity Level" ]
        ],
        commands: [
            0x00: [ req:"Yes", name:"Zone Enroll Response" ],
            0x01: [ req:"No",  name:"Initiate Normal Operation Mode" ],
            0x02: [ req:"No",  name:"Initiate Test Mode" ]
        ]
    ],
    0x0502: [
        name: "IAS WD Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"RW-", name:"Max Duration" ]
        ]
    ],
    0x0B01: [
        name: "Meter Identification Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Company Name" ],
            0x0001: [ req:"Yes", acc:"R--", name:"Meter Type ID" ],
            0x0004: [ req:"Yes", acc:"R--", name:"Data Quality ID" ],
            0x0005: [ req:"No",  acc:"RW-", name:"Customer Name" ],
            0x0006: [ req:"No",  acc:"R--", name:"Model" ],
            0x0007: [ req:"No",  acc:"R--", name:"Part Number" ],
            0x0008: [ req:"No",  acc:"R--", name:"Product Revision" ],
            0x000A: [ req:"Yes", acc:"R--", name:"Software Revision" ],
            0x000B: [ req:"No",  acc:"R--", name:"Utility Name" ],
            0x000C: [ req:"Yes", acc:"R--", name:"POD" ],
            0x000D: [ req:"Yes", acc:"R--", name:"Available Power" ],
            0x000E: [ req:"Yes", acc:"R--", name:"Power Threshold" ]
        ]
    ],
    0x0B04: [
        name: "Electrical Measurement Cluster",
        attributes: [
            0x0000: [ req:"Yes", acc:"R--", name:"Measurement Type" ],
            
            0x0100: [ req:"No",  acc:"R--", name:"DC Voltage" ],
            0x0101: [ req:"No",  acc:"R--", name:"DC Voltage Min" ],
            0x0102: [ req:"No",  acc:"R--", name:"DC Voltage Max" ],
            0x0103: [ req:"No",  acc:"R--", name:"DC Current" ],
            0x0104: [ req:"No",  acc:"R--", name:"DC Current Min" ],
            0x0105: [ req:"No",  acc:"R--", name:"DC Current Max" ],
            0x0106: [ req:"No",  acc:"R--", name:"DC Power" ],
            0x0107: [ req:"No",  acc:"R--", name:"DC Power Min" ],
            0x0108: [ req:"No",  acc:"R--", name:"DC Power Max" ],
            
            0x0200: [ req:"No",  acc:"R--", name:"DC Voltage Multiplier" ],
            0x0201: [ req:"No",  acc:"R--", name:"DC Voltage Divisor" ],
            0x0202: [ req:"No",  acc:"R--", name:"DC Current Multiplier" ],
            0x0203: [ req:"No",  acc:"R--", name:"DC Current Divisor" ],
            0x0204: [ req:"No",  acc:"R--", name:"DC Power Multiplier" ],
            0x0205: [ req:"No",  acc:"R--", name:"DC Power Divisor" ],
            
            0x0300: [ req:"No",  acc:"R--", name:"ACFrequency" ],
            0x0301: [ req:"No",  acc:"R--", name:"ACFrequencyMin" ],
            0x0302: [ req:"No",  acc:"R--", name:"ACFrequencyMax" ],
            0x0303: [ req:"No",  acc:"R--", name:"NeutralCurrent" ],
            0x0304: [ req:"No",  acc:"R--", name:"TotalActivePower" ],
            0x0305: [ req:"No",  acc:"R--", name:"TotalReactivePower" ],
            0x0306: [ req:"No",  acc:"R--", name:"TotalApparentPower" ],
            
            0x0400: [ req:"No",  acc:"R--", name:"AC Frequency Multiplier" ],
            0x0401: [ req:"No",  acc:"R--", name:"AC Frequency Divisor" ],
            0x0402: [ req:"No",  acc:"R--", name:"Power Multiplier" ],
            0x0403: [ req:"No",  acc:"R--", name:"Power Divisor" ],
            0x0404: [ req:"No",  acc:"R--", name:"Harmonic Current Multiplier" ],
            0x0405: [ req:"No",  acc:"R--", name:"Phase Harmonic Current Multiplier" ],
            
            0x0500: [ req:"No",  acc:"R--", name:"Reserved" ],
            0x0501: [ req:"No",  acc:"R--", name:"Line Current" ],
            0x0502: [ req:"No",  acc:"R--", name:"Active Current" ],
            0x0503: [ req:"No",  acc:"R--", name:"Reactive Current" ],
            0x0505: [ req:"No",  acc:"R--", name:"RMS Voltage" ],
            0x0506: [ req:"No",  acc:"R--", name:"RMS Voltag eMin" ],
            0x0507: [ req:"No",  acc:"R--", name:"RMS Voltage Max" ],
            0x0508: [ req:"No",  acc:"R--", name:"RMS Current" ],
            0x0509: [ req:"No",  acc:"R--", name:"RMS Current Min" ],
            0x050A: [ req:"No",  acc:"R--", name:"RMS Current Max" ],
            0x050B: [ req:"No",  acc:"R--", name:"Active Power" ],
            0x050C: [ req:"No",  acc:"R--", name:"Active Power Min" ],
            0x050D: [ req:"No",  acc:"R--", name:"Active Power Max" ],
            0x050E: [ req:"No",  acc:"R--", name:"Reactive Power" ],
            0x050F: [ req:"No",  acc:"R--", name:"Apparent Power" ],
            0x0510: [ req:"No",  acc:"R--", name:"Power Factor" ],
            0x0511: [ req:"No",  acc:"RW-", name:"Average RMS Voltage Measurement Period" ],
            0x0512: [ req:"No",  acc:"RW-", name:"Average RMS Over Voltage Counter" ],
            0x0513: [ req:"No",  acc:"RW-", name:"Average RMS Under Voltage Counter" ],
            0x0514: [ req:"No",  acc:"RW-", name:"RMS Extreme Over Voltage Period" ],
            0x0515: [ req:"No",  acc:"RW-", name:"RMS Extreme Under Voltage Period" ],
            0x0516: [ req:"No",  acc:"RW-", name:"RMS Voltage Sag Period" ],
            0x0517: [ req:"No",  acc:"RW-", name:"RMS Voltage Swell Period" ],

            0x0600: [ req:"No",  acc:"R--", name:"AC Voltage Multiplier" ],
            0x0601: [ req:"No",  acc:"R--", name:"AC Voltage Divisor" ],
            0x0602: [ req:"No",  acc:"R--", name:"AC Current Multiplier" ],
            0x0603: [ req:"No",  acc:"R--", name:"AC Current Divisor" ],
            0x0604: [ req:"No",  acc:"R--", name:"AC Power Multiplier" ],
            0x0605: [ req:"No",  acc:"R--", name:"AC Power Divisor" ],

            0x0700: [ req:"No",  acc:"RW-", name:"DC Overload Alarms Mask" ],
            0x0701: [ req:"No",  acc:"R--", name:"DC Voltage Overload" ],
            0x0702: [ req:"No",  acc:"R--", name:"DC Current Overload" ],

            0x0800: [ req:"No",  acc:"RW-", name:"AC Alarms Mask" ],
            0x0801: [ req:"No",  acc:"R--", name:"AC Voltage Overload" ],
            0x0802: [ req:"No",  acc:"R--", name:"AC Current Overload" ],
            0x0803: [ req:"No",  acc:"R--", name:"AC Active Power Overload" ],
            0x0804: [ req:"No",  acc:"R--", name:"AC Reactive Power Overload" ],
            0x0805: [ req:"No",  acc:"R--", name:"Average RMS Over Voltage" ],
            0x0806: [ req:"No",  acc:"R--", name:"Average RMS Under Voltage" ],
            0x0807: [ req:"No",  acc:"RW-", name:"RMS Extreme Over Voltage" ],
            0x0808: [ req:"No",  acc:"RW-", name:"RMS Extreme Unde rVoltage" ],
            0x0809: [ req:"No",  acc:"RW-", name:"RMS Voltage Sag" ],
            0x080A: [ req:"No",  acc:"RW-", name:"RMS Voltage Swell" ],

            0x0901: [ req:"No",  acc:"R--", name:"Line Current PhB" ],
            0x0902: [ req:"No",  acc:"R--", name:"Active Current PhB" ],
            0x0903: [ req:"No",  acc:"R--", name:"Reactive Current PhB" ],
            0x0905: [ req:"No",  acc:"R--", name:"RMS Voltage PhB" ],
            0x0906: [ req:"No",  acc:"R--", name:"RMS Voltage Min PhB" ],
            0x0907: [ req:"No",  acc:"R--", name:"RMS Voltage Max PhB" ],
            0x0908: [ req:"No",  acc:"R--", name:"RMS Current PhB" ],
            0x0909: [ req:"No",  acc:"R--", name:"RMS Current Min PhB" ],
            0x090A: [ req:"No",  acc:"R--", name:"RMS Current Max PhB" ],
            0x090B: [ req:"No",  acc:"R--", name:"Active Power PhB" ],
            0x090C: [ req:"No",  acc:"R--", name:"Active PowerMin PhB" ],
            0x090D: [ req:"No",  acc:"R--", name:"Active PowerMax PhB" ],
            0x090E: [ req:"No",  acc:"R--", name:"Reactive Power PhB" ],
            0x090F: [ req:"No",  acc:"R--", name:"Apparent Power PhB" ],
            0x0910: [ req:"No",  acc:"R--", name:"Power Factor PhB" ],
            0x0911: [ req:"No",  acc:"RW-", name:"Average RMS Voltage Measurement Period PhB" ],
            0x0912: [ req:"No",  acc:"RW-", name:"Average RMS Over Voltage Counter PhB" ],
            0x0913: [ req:"No",  acc:"RW-", name:"Average RMS Under Voltage Counter PhB" ],
            0x0914: [ req:"No",  acc:"RW-", name:"RMS Extreme Over Voltage Period PhB" ],
            0x0915: [ req:"No",  acc:"RW-", name:"RMS Extreme Under Voltage Period PhB" ],
            0x0916: [ req:"No",  acc:"RW-", name:"RMS Voltage Sag Period PhB" ],
            0x0917: [ req:"No",  acc:"RW-", name:"RMS Voltage Swell Period PhB" ],

            0x0A01: [ req:"No",  acc:"R--", name:"Line Current PhC" ],
            0x0A02: [ req:"No",  acc:"R--", name:"Active Current PhC" ],
            0x0A03: [ req:"No",  acc:"R--", name:"Reactive Current PhC" ],
            0x0A05: [ req:"No",  acc:"R--", name:"RMS Voltage PhC" ],
            0x0A06: [ req:"No",  acc:"R--", name:"RMS Voltage Min PhC" ],
            0x0A07: [ req:"No",  acc:"R--", name:"RMS Voltage Max PhC" ],
            0x0A08: [ req:"No",  acc:"R--", name:"RMS Current PhC" ],
            0x0A09: [ req:"No",  acc:"R--", name:"RMS Current Min PhC" ],
            0x0A0A: [ req:"No",  acc:"R--", name:"RMS Current Max PhC" ],
            0x0A0B: [ req:"No",  acc:"R--", name:"Active Power PhC" ],
            0x0A0C: [ req:"No",  acc:"R--", name:"Active Power Min PhC" ],
            0x0A0D: [ req:"No",  acc:"R--", name:"Active Power Max PhC" ],
            0x0A0E: [ req:"No",  acc:"R--", name:"Reactive Power PhC" ],
            0x0A0F: [ req:"No",  acc:"R--", name:"Apparent Power PhC" ],
            0x0A10: [ req:"No",  acc:"R--", name:"Power Factor PhC" ],
            0x0A11: [ req:"No",  acc:"RW-", name:"Average RMS Voltage Measurement Period PhC" ],
            0x0A12: [ req:"No",  acc:"RW-", name:"Average RMS Over Voltage Counter PhC" ],
            0x0A13: [ req:"No",  acc:"RW-", name:"Average RMS Under Voltage Counter PhC" ],
            0x0A14: [ req:"No",  acc:"RW-", name:"RMS Extreme Over Voltage Period PhC" ],
            0x0A15: [ req:"No",  acc:"RW-", name:"RMS Extreme Under Voltage Period PhC" ],
            0x0A16: [ req:"No",  acc:"RW-", name:"RMS Voltage Sag Period PhC" ],
            0x0A17: [ req:"No",  acc:"RW-", name:"RMS Voltage Swell Period PhC" ]
        ],
        commands: [
            0x00: [ req:"No",  name:"Get Profile Info Response Command" ],
            0x01: [ req:"No",  name:"Get Measurement Profile Response Command" ]
        ]
    ],
    0x0B05: [
        name: "Diagnostics Cluster",
        attributes: [
            0x0000: [ req:"No",  acc:"R--", name:"Number Of Resets" ],
            0x0001: [ req:"No",  acc:"R--", name:"Persistent Memory Writes" ],

            0x0100: [ req:"No",  acc:"R--", name:"Mac Rx Bcast" ],
            0x0101: [ req:"No",  acc:"R--", name:"Mac Tx Bcast" ],
            0x0102: [ req:"No",  acc:"R--", name:"Mac Rx Ucast" ],
            0x0103: [ req:"No",  acc:"R--", name:"Mac Tx Ucast" ],
            0x0104: [ req:"No",  acc:"R--", name:"Mac Tx Ucast Retry" ],
            0x0105: [ req:"No",  acc:"R--", name:"Mac Tx Ucast Fail" ],
            0x0106: [ req:"No",  acc:"R--", name:"APS Rx Bcast" ],
            0x0107: [ req:"No",  acc:"R--", name:"APS Tx Bcast" ],
            0x0108: [ req:"No",  acc:"R--", name:"APS Rx Ucast" ],
            0x0109: [ req:"No",  acc:"R--", name:"APS Tx Ucast Success" ],
            0x010A: [ req:"No",  acc:"R--", name:"APS Tx Ucast Retry" ],
            0x010B: [ req:"No",  acc:"R--", name:"APS Tx Ucast Fail" ],
            0x010C: [ req:"No",  acc:"R--", name:"Route Disc Initiated" ],
            0x010D: [ req:"No",  acc:"R--", name:"Neighbor Added" ],
            0x010E: [ req:"No",  acc:"R--", name:"Neighbor Removed" ],
            0x010F: [ req:"No",  acc:"R--", name:"Neighbor Stale" ],
            0x0110: [ req:"No",  acc:"R--", name:"Join Indication" ],
            0x0111: [ req:"No",  acc:"R--", name:"Child Moved" ],
            0x0112: [ req:"No",  acc:"R--", name:"NWK FC Failure" ],
            0x0113: [ req:"No",  acc:"R--", name:"APS FC Failure" ],
            0x0114: [ req:"No",  acc:"R--", name:"APS Unauthorized Key" ],
            0x0115: [ req:"No",  acc:"R--", name:"NWK Decrypt Failures" ],
            0x0116: [ req:"No",  acc:"R--", name:"APS Decrypt Failures" ],
            0x0117: [ req:"No",  acc:"R--", name:"Packet Buffer Allocate Failures" ],
            0x0118: [ req:"No",  acc:"R--", name:"Relayed Ucast" ],
            0x0119: [ req:"No",  acc:"R--", name:"Phyto MAC Queue Limit Reached" ],
            0x011A: [ req:"No",  acc:"R--", name:"Packet Validate Drop Count" ],
            0x011B: [ req:"No",  acc:"R--", name:"Average MAC Retry Per APS Message Sent" ],
            0x011C: [ req:"No",  acc:"R--", name:"Last Message LQI" ],
            0x011D: [ req:"No",  acc:"R--", name:"Last Message RSSI" ]
        ]
    ],
    0x1000: [
        name: "ZLL/Touchlink Commissioning Cluster"
    ]
]

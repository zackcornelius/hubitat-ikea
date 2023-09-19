{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "HealthCheck"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# attributes }}

// Attributes for capability.ZigbeeRouter
attribute "neighbors", "STRING"
attribute "routes", "STRING"
{{/ attributes }}
{{!--------------------------------------------------------------------------}}
{{# commands }}

// Commands for capability.ZigbeeRouter
command "requestRoutingData"
{{/ commands }}
{{!--------------------------------------------------------------------------}}
{{# implementation }}

// Implementation for capability.ZigbeeRouter
def requestRoutingData() {
    Log.info "Asking the device to send the Neighbors Table and the Routing Table data ..."
    Utils.sendZigbeeCommands([
        "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0031 {00} {0x00}",
        "he raw ${device.deviceNetworkId} 0x0000 0x0000 0x0032 {00} {0x00}"
    ])
}
{{/ implementation }}
{{!--------------------------------------------------------------------------}}
{{# events }}

// Events for capability.ZigbeeRouter

{{> src/events/mgmt-lqi-response.groovy }}

{{> src/events/mgmt-rtg-response.groovy }}
{{/ events }}
{{!--------------------------------------------------------------------------}}

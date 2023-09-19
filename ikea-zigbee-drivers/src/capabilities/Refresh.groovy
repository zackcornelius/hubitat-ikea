{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "Refresh"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# configure }}

// Configure for capability.Refresh
{{# readAttributes }}
cmds += zigbee.readAttribute({{ cluster }}, {{ attr }})  // {{ description }}
{{/ readAttributes }}
{{/ configure }}
{{!--------------------------------------------------------------------------}}
{{# implementation }}

// Implementation for capability.Refresh
def refresh(isPhysical = false) {
    state.isPhysical = isPhysical
    cmds = []
    {{# readAttributes }}
    cmds += zigbee.readAttribute({{ cluster }}, {{ attr }})  // {{ description }}
    {{/ readAttributes }}
    Utils.sendZigbeeCommands cmds
}
{{/ implementation }}
{{!--------------------------------------------------------------------------}}

{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "Refresh"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configure for capability.Refresh
{{#  params.readAttributes }}
cmds += zigbee.readAttribute({{ cluster }}, {{ attr }}) // {{ description }}
{{/  params.readAttributes }}
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.Refresh
def refresh() {
    List<String> cmds = []
    {{#  params.readAttributes }}
    cmds += zigbee.readAttribute({{ cluster }}, {{ attr }}) // {{ description }}
    {{/  params.readAttributes }}
    Utils.sendZigbeeCommands cmds
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

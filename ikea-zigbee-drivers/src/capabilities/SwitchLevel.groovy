{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "SwitchLevel"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# inputs }}

// Inputs for capability.SwitchLevel
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
{{/ inputs }}
{{!--------------------------------------------------------------------------}}
{{# updated }}

// Preferences for capability.SwitchLevel
Log.info "🛠️ levelChange = ${levelChange}%"
minLevel = minLevel.toDouble().trunc().toInteger()
device.clearSetting "minLevel"
device.removeSetting "minLevel"
device.updateSetting "minLevel", minLevel
Log.info "🛠️ minLevel = ${minLevel}"
{{/ updated }}
{{!--------------------------------------------------------------------------}}
{{# implementation }}

// Implementation for capability.SwitchLevel
def setLevel(level, duration = 0) {
    def newLevel = level < 0 ? 0 : (level > 100 ? 100 : level)
    Utils.sendEvent name:"level", value:newLevel, unit:"%", type:"digital", descriptionText:"Level was set to ${newLevel}%"
}
{{/ implementation }}
{{!--------------------------------------------------------------------------}}

{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "MotionSensor"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.MotionSensor
attribute "requestedBrightness", "NUMBER"            // Syncs with the brightness option on device (◐/⭘)
attribute "illumination", "ENUM", ["dim", "bright"]  // Works only in night mode 🌙
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @inputs }}

// Inputs for capability.MotionSensor
input(
    name: "clearMotionPeriod",
    type: "enum",
    title: "Clear motion after",
    description: "<small>Set status inactive if no motion is detected in this period</small>",
    options: [
        "60"  : "1 minute",
        "120" : "2 minutes",
        "180" : "3 minutes",
        "240" : "4 minutes",
        "300" : "5 minutes",
        "360" : "6 minutes",
        "420" : "7 minutes",
        "480" : "8 minutes",
        "540" : "9 minutes",
        "600" : "10 minutes"
    ],
    defaultValue: "180",
    required: true
)
// Inputs for capability.MotionSensor
input(
    name: "onlyTriggerInDimLight",
    type: "bool",
    title: "Only detect motion in the dark",
    description: "<small>Select the night mode 🌙 option on device for this to work</small>",
    defaultValue: false,
    required: true
)
{{/ @inputs }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.MotionSensor
def clearMotion() {
    return Utils.sendEvent(name:"motion", value:"inactive", type:"digital", descriptionText:"Is inactive")
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.MotionSensor
Log.info "🛠️ clearMotionPeriod = ${clearMotionPeriod} seconds"
Log.info "🛠️ onlyTriggerInDimLight = ${onlyTriggerInDimLight}"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.MotionSensor

// OnWithTimedOff := { 08:OnOffControl, 16:OnTime, 16:OffWaitTime }
// OnOffControl := { 01:AcceptOnlyWhenOn, 07:Reserved }
// Example: [01, 08, 07, 00, 00] -> acceptOnlyWhenOn=true, onTime=180, offWaitTime=0
case { contains it, [clusterInt:0x0006, commandInt:0x42] }:
    String illumination = msg.data[0] == "01" ? "bright" : "dim"
    Utils.sendEvent(name:"illumination", value:illumination, type:"physical", descriptionText:"Illumination is ${illumination}")

    if (illumination == "bright" && onlyTriggerInDimLight) {
        return Log.debug("Ignored detected motion because the \"Only detect motion in the dark\" option is active and the sensor detected plenty of light")
    }

    runIn Integer.parseInt(clearMotionPeriod), "clearMotion", [ overwrite:true ]
    return Utils.sendEvent(name:"motion", value:"active", type:"physical", descriptionText:"Is active")

// MoveToLevelWithOnOff := { 08:Level, 16:TransitionTime }
// Example: [4C, 01, 00] -> level=30%, transitionTime=1/10seconds
// Example: :[FE, 01, 00 -> level=100%, transitionTime=1/10seconds
case { contains it, [clusterInt:0x0008, commandInt:0x04] }:
    Integer requestedBrightness = Math.round(Integer.parseInt(msg.data[0], 16) * 100 / 254)
    return Utils.sendEvent(name:"requestedBrightness", value:requestedBrightness, unit:"%", type:"physical", descriptionText:"Requested brightness set too ${requestedBrightness}%")
{{/ @events }}
{{!--------------------------------------------------------------------------}}

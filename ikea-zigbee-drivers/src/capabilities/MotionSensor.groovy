{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "MotionSensor"
{{/ @definition }}
{{# @implementation }}

// Implementation for capability.MotionSensor
def motionInactive() {
    return Utils.sendEvent(name:"motion", value:"inactive", type:"digital", descriptionText:"Is inactive")
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

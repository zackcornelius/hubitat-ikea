{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "HoldableButton"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.HoldableButton
def hold(buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) return Log.warn("Cannot hold button ${buttonNumber} because it is not defined")
    Utils.sendEvent name:"held", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was held"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

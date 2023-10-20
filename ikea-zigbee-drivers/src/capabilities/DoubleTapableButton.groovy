{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "DoubleTapableButton"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.DoubleTapableButton
def doubleTap(buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) return Log.warn("Cannot double tap button ${buttonNumber} because it is not defined")
    Utils.sendEvent name:"doubleTapped", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was double tapped"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

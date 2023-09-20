{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "DoubleTapableButton"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.DoubleTapableButton
def doubleTap(buttonNumber) {
    Utils.sendEvent name:"doubleTapped", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} was double tapped"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

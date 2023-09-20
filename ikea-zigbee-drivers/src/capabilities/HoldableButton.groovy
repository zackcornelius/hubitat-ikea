{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "HoldableButton"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.HoldableButton
def hold(buttonNumber) {
    Utils.sendEvent name:"held", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} was held"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

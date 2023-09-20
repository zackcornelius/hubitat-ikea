{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "ReleasableButton"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.ReleasableButton
def release(buttonNumber) {
    Utils.sendEvent name:"released", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} was released"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

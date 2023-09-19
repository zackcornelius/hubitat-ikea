{{!--------------------------------------------------------------------------}}
{{# definition }}
capability "PushableButton"
{{/ definition }}
{{!--------------------------------------------------------------------------}}
{{# fields }}

// Fields for capability.PushableButton
@Field def BUTTONS = [
    {{# buttons }}
    "{{ id }}": ["{{ number }}", "{{ name }}"],
    {{/ buttons }}
]
{{/ fields }}
{{!--------------------------------------------------------------------------}}
{{# configure }}

// Configuration for capability.PushableButton
def numberOfButtons = BUTTONS.count{_ -> true}
sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
{{/ configure }}
{{!--------------------------------------------------------------------------}}
{{# implementation }}

// Implementation for capability.PushableButton
def push(buttonNumber) {
    Utils.sendEvent name:"pushed", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} was pressed"
}
{{/ implementation }}
{{!--------------------------------------------------------------------------}}

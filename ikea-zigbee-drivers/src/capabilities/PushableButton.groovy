{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "PushableButton"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.PushableButton
@Field static final Map<String, List<String>> BUTTONS = [
    {{# params.buttons }}
    "{{ id }}": ["{{ number }}", "{{ name }}"],
    {{/ params.buttons }}
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.PushableButton
def numberOfButtons = BUTTONS.count{_ -> true}
sendEvent name:"numberOfButtons", value:numberOfButtons, descriptionText:"Number of buttons is ${numberOfButtons}"
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.PushableButton
def push(buttonNumber) {
    String buttonName = BUTTONS.find { it.value[0] == "${buttonNumber}" }?.value?.getAt(1)
    if (buttonName == null) return Log.warn("Cannot push button ${buttonNumber} because it is not defined")
    Utils.sendEvent name:"pushed", value:buttonNumber, type:"digital", isStateChange:true, descriptionText:"Button ${buttonNumber} (${buttonName}) was pressed"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}

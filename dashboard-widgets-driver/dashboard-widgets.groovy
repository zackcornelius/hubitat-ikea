def getNamesList() {
    return ["Alfa", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "Xray", "Yankee", "Zulu"]
}

def getWidgetTypes() {
    return ["Clock", "Wind"]
}

def getAvailableThemes() {
    return ["Light", "Dark"]
}

metadata {
    definition (name: "Dashboard Widgets", namespace: "dandanache", author: "Dan Danache", importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/dashboard-widgets-driver_1.2.0/dashboard-widgets-driver/dashboard-widgets.groovy") {
        capability "Actuator"
        
        command "configureWidget", [
            [name: "Linked Device ID*", description: "Enter the ID of the device that will be linked to this widget", type: "NUMBER"],
            [name: "Widget Name", type: "ENUM", description: "Select from the predefined names list", constraints: getNamesList()],
            [name: "Widget Type", description: "Select widget type", type: "ENUM", constraints: getWidgetTypes()],
            [name: "Widget Parameters", description: "Enter widget parameters as documented by each widget type", type: "STRING"],
            [name: "Widget Theme", type: "ENUM", description: "Select widget theme", constraints: getAvailableThemes()]
        ]
        command "resetWidget", [
            [name:"Widget Name", type: "ENUM", description: "Name of the widget to be cleared", constraints: getNamesList()]
        ]
        
        getNamesList().each{name ->
            attribute name, "string"
        }
    }
}
preferences {
    input("MakerAPI_GetAllDevicesURL", "text", title: "<b>Maker API \"Get All Devices\" link</b>", required: true)
}

def installed() {
    log.info "Device ${device} installed"
}

def uninstalled() {
    log.info "Device ${device} uninstalled"
}

def updated() {
    def mapi = getMakerAPI_Params();
    getNamesList().each{name ->
        def html = device.currentValue(name)
        if (html == null || html == "--") return
        def newHtml = html.replaceAll(/&mapi=[^"]*/, "${mapi}")
        if (newHtml == html) return;
        sendEvent(name: name, value: newHtml)
    }
}

def configureWidget(device, name, type, parameters, theme) {
    if (device == null) {
        log.error "Cannot add Dashboard Widget without entering a Device ID"
        return
    }

    def params = parameters == null ? "" : "&${parameters}"
    def src = "/local/${type.toLowerCase()}.html?theme=${theme.toLowerCase()}&device=${device}${params}${getMakerAPI_Params()}"
    def html = "<div style=\"height:100%;width:100%;border-radius:10px;overflow:hidden\"><iframe src=\"${src}\" allowtransparency=\"true\" style=\"height:100%;width:100%;border:none\"></iframe></div>"
    sendEvent(name: name, value: html)
}

def resetWidget(name) {
    sendEvent(name: name, value: "--", data: "")
}

def getMakerAPI_Params() {
    if (MakerAPI_GetAllDevicesURL == null) {
        log.info " Maker API \"Get All Devices\" link not set in preferences; Widgets will not update on load but only when the device sends its first attribute change!"
        return ""
    }
    if (!MakerAPI_GetAllDevicesURL.matches(/.*\/apps\/api\/[0-9]+\/devices\?access_token=[a-z0-9\-]+/)) {
        log.info "Invalid Maker API \"Get All Devices\" link; Widgets will not update on load but only when the device sends its first attribute change!"
        return ""
    }
    return MakerAPI_GetAllDevicesURL.replaceAll(/.*\/apps\/api\//, "&mapi=").replaceAll(/\/devices\?access_token=/, "&at=")
}

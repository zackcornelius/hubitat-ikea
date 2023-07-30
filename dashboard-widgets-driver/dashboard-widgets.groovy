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
    input (
        "Use_MakerAPI", "bool",
        title: "<b>Use Maker API</b>",
        description: "Widgets can use Maker API to query device attributes.<details><summary style='cursor:pointer'>More details</summary>If you don't have Maker API installed or have this option disabled, dashboard widgets will not update on load but only when the linked devices send their first attribute change.</details>"
    )
    if (Use_MakerAPI) {
        input (
            "MakerAPI_GetAllDevicesURL", "text",
            description: "Enter the \"Get All Devices\" link from Maker API app.<details><summary style='cursor:pointer'>How to get the link</summary><ol><li>Go to <a href='/installedapp/list' target='_blank'>Apps</a> and click on \"Maker API\" to start the app</li><li>Scroll down until you find the \"Get All Devices\" link then click it</li><li>Copy the URL from the browser location bar, then paste it in the input below</li><li>Click the \"Save Preferences\" button below</li></ol></details>",
            title: "<b>Maker API link</b>",
            required: true,
            defaultValue: guessAllDevicesURL()
        )
    }
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
        def newHtml = html
            .replaceAll(/&mapi=[^"]*/, "")
            .replaceAll(/" allowtransparency="/, "${mapi}\" allowtransparency=\"")
        if (newHtml == html) return;
        log.info "Updating attribute ${name}"
        sendEvent(name: name, value: newHtml, type: "digital", unit: "--")
    }
}

def configureWidget(device, name, type, parameters, theme) {
    if (device == null) {
        log.error "Cannot add Dashboard Widget without entering a Device ID"
        return
    }

    def params = parameters == null ? "" : "&${parameters}"
    def src = "http://${location.hub.data.localIP}/local/${type.toLowerCase()}.html?theme=${theme.toLowerCase()}&device=${device}${params}${getMakerAPI_Params()}"
    def html = "<div style=\"height:100%;width:100%;border-radius:11px;overflow:hidden\"><iframe src=\"${src}\" allowtransparency=\"true\" style=\"height:100%;width:100%;border:none\"></iframe></div>"
    sendEvent(name: name, value: html, type: "digital", unit: "--")
}

def resetWidget(name) {
    sendEvent(name: name, value: "--", data: "")
}

def getMakerAPI_Params() {
    if (!Use_MakerAPI) {
        log.info "\"Use Maker API\" is disabled in preferences; Widgets will not update on load but only when the device sends its first attribute change!"
        return ""
    }
    def url = MakerAPI_GetAllDevicesURL;
    if (url == null) url = guessAllDevicesURL();
    if (url == null) {
        log.info "Maker API \"Get All Devices\" link not set in preferences; Widgets will not update on load but only when the device sends its first attribute change!"
        return ""
    }
    if (!url.matches(/.*\/apps\/api\/[0-9]+\/devices\?access_token=[a-z0-9\-]+/)) {
        log.info "Invalid Maker API \"Get All Devices\" link; Widgets will not update on load but only when the device sends its first attribute change!"
        return ""
    }
    return url.replaceAll(/.*\/apps\/api\//, "&mapi=").replaceAll(/\/devices\?access_token=/, "&at=")
}

def guessAllDevicesURL() {
    def appId = getMakerAppId();
    if (appId == null) return null;
    
	def params = [
		uri: getBaseUrl(),
        path: "/installedapp/status/${appId}",
		textParser: true,
		headers: [
			Cookie: state.cookie
		],
	    ignoreSSLIssues: true
    ]

	try {
		httpGet(params) { resp ->
			def accessToken = resp.data.text
                .replace("\n","")
                .replace("\r","")
    	        .find(/<tr.*?accessToken.*?>([0-9a-f\-]+)<\/td> *<\/tr>/) { match, i -> return i.trim() }
            if (accessToken == null) return null
            
            // Build Maker API "Get All Devices" link
            return "http://${location.hub.data.localIP}/apps/api/${appId}/devices?access_token=${accessToken}"
		}
	} catch (e) {
		log.error "Error retrieving Maker API \"Get All Devices\" link: ${e}"
	}
}

def getMakerAppId() {
	def params = [
		uri: getBaseUrl(),
		path: "/installedapp/list",
		textParser: true,
		headers: [
			Cookie: state.cookie
		],
	    ignoreSSLIssues: true
    ]

	try {
		httpGet(params) { resp ->
			return resp.data.text
                .replace("\n","")
                .replace("\r","")
			    .find(/<a href="\/installedapp\/configure\/([^"]+)">Maker API<\/a>/) { match, i -> return i.trim() }
		}
	} catch (e) {
		log.error "Error retrieving installed Maker API app ID: ${e}"
	}
}

def getBaseUrl() {
    def sslEnabled = false
	def scheme = sslEnabled ? "https" : "http"
	def port = sslEnabled ? "8443" : "8080"
    return "${scheme}://127.0.0.1:${port}"
}

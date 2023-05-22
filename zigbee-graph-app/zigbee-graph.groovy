/*
 * Zigbee Graph app for Hubitat
 *
 * Eye-candy visual render for data we get from /hub/zigbee/getChildAndRouteInfo
 *
 * Relies heavily on the great work of @sburke781 for SimpleCSSEditor:
 * https://github.com/sburke781/hubitat/blob/master/SimpleCSSEditor/SimpleCSSEditor_driver.groovy
 */
definition(
	name: "Zigbee Graph",
	namespace: "dandanache",
	author: "Dan Danache",
	description: "Eye-candy visual render for data we get from /hub/zigbee/getChildAndRouteInfo",
	category: "Utility",
	singleInstance: true,
	iconUrl: "",
	iconX2Url: "",
	oauth: true,
	importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/master/zigbee-graph-app/zigbee-graph.groovy"
)

/**********************************************************************************************************************************************/
private releaseVer() { return "1.0.0" }
private appVerDate() { return "2023-05-23" }
private htmlFileSrc() { return "https://raw.githubusercontent.com/dan-danache/hubitat/master/zigbee-graph-app/zigbee-graph.html" }
private htmlFileDst() { return "zigbee-graph.html" }
/**********************************************************************************************************************************************/
preferences {
	page name: "mainPage"
}

def mainPage() {
	dynamicPage (name: "mainPage", title: "Zigbee Graph - v${releaseVer() + ' - ' + appVerDate()}", install: true, uninstall: true) {
        if (app.getInstallationState() != "COMPLETE") {
		    section {
                label title: "Once you click the [Done] button, we will try to retrieve the 'zigbee-graph.html' from Github and store it in the File Manager."
            }
        } else {
            section {
                href name: "zigbee-graph-href", title: "Show zigbee graph", url: "/local/${htmlFileDst()}", style: "embedded", required: false, description: "Tap Here to go to the zigbee graph", image: ""
            }
        }
    }
}

// Standard device methods
void installed() {
    log.debug "Installing ${app?.getLabel()}..."
    downloadGraphHTML();
    log.debug "${app?.getLabel()} has been installed"
}

void updated() {
    log.debug "Updating ${app?.getLabel()}..."
    downloadGraphHTML();
    log.debug "${app?.getLabel()} has been updated"
}

void refresh() {
    log.debug "Refreshing ${app?.getLabel()}..."
    downloadGraphHTML();
    log.debug "${app?.getLabel()} has been refreshed"
}

void downloadGraphHTML() {
    xferFile(htmlFileSrc(), htmlFileDst());
}

Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    return retStat
}

String readExtFile(fName){
    log.debug "Downloading file from ${fName}..."
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true
    ]

    try {
        httpGet(params) { resp ->
            if (resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               }
               log.trace "File ${fName} was successfully downloaded"
               return delim
            }
            else {
                errorLog("Null Response")
            }
        }
    } catch (exception) {
        errorLog("Read Ext Error: ${exception.message}")
        return null;
    }
}

Boolean writeFile(String fName, String fData) {
    log.debug "Saving data to File Manager entry ${fName}..."

    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();    
    try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [ 'folder': '/' ],
			headers: [ 'Content-Type': "multipart/form-data; boundary=$encodedString" ],
            body: """--${encodedString}
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

--${encodedString}
Content-Disposition: form-data; name="folder"


--${encodedString}--""",
			timeout: 500,
			ignoreSSLIssues: true
		]

		httpPost(params) { resp ->
		}
        log.trace "File Manager entry ${fName} successfully saved"
		return true
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
}

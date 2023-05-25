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
    description: "Allows you to visually render getChildAndRouteInfo of your Hubitat system.",
    category: "Utility",
    singleInstance: true,
    iconUrl: "",
    iconX2Url: "",
    oauth: false,
    importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/master/zigbee-graph-app/zigbee-graph.groovy"
)

/**********************************************************************************************************************************************/
private releaseVer() { return "1.4.0" }
private appVerDate() { return "2023-05-25" }
private htmlFileSrc() { return "https://raw.githubusercontent.com/dan-danache/hubitat/master/zigbee-graph-app/zigbee-graph.html" }
private htmlFileDst() { return "zigbee-graph.html" }
/**********************************************************************************************************************************************/
preferences {
    page name: "mainPage"
    page name: "downloadPage"
}

def mainPage() {
    def showInstall = app.getInstallationState() == "INCOMPLETE"
    dynamicPage (name: "mainPage", title: "Zigbee Graph - v${releaseVer() + ' - ' + appVerDate()}", install: showInstall, uninstall: !showInstall) {
        if (app.getInstallationState() == "COMPLETE") {
            section {
			    paragraph "What would you like to do?"
			    href name: "show", title: "Show zigbee graph", required: false, url: "/local/${htmlFileDst()}", description: "Tap Here to go to the zigbee graph."
			    href name: "download", title: "Get latest version", required: false, page: "downloadPage", description: "Tap here to download the 'zigbee-graph.html' file from Github and store it in the File Manager."
            }
        } else {
            section {
                paragraph "Tap the [Done] button to create the application instance, download the 'zigbee-graph.html' file from Github and store it in the File Manager."
            }
        }
    }
}

def downloadPage() {
    downloadGraphHTML();
    return dynamicPage (name: "downloadPage", title: "Zigbee Graph - Update successful!", install: false, uninstall: false) {
        section {
            href name: "show", title: "Show zigbee graph", required: false, url: "/local/${htmlFileDst()}", description: "Tap Here to go to the zigbee graph."
        }
    }
}

// Standard device methods
def installed() {
    log.debug "Installing ${app?.getLabel()}..."
    downloadGraphHTML();
    log.debug "${app?.getLabel()} has been installed"
}

def updated() {
    log.debug "Updating ${app?.getLabel()}..."
    downloadGraphHTML();
    log.debug "${app?.getLabel()} has been updated"
}

def refresh() {
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
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
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

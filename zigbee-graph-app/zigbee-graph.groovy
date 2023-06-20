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
private releaseVer() { return "2.0.0" }
private appVerDate() { return "2023-06-20" }
private htmlFileDst() { return "zigbee-graph.html" }
/**********************************************************************************************************************************************/
preferences {
    page name: "mainPage"
    page name: "changelogPage"
}

def mainPage() {
    def showInstall = app.getInstallationState() == "INCOMPLETE"
    dynamicPage (name: "mainPage", title: "Zigbee Graph - v${releaseVer() + ' - ' + appVerDate()}", install: showInstall, uninstall: !showInstall) {
        if (app.getInstallationState() == "COMPLETE") {
            section {
		        paragraph "What would you like to do?"
		        href name: "show", title: "Show zigbee graph", required: false, url: "/local/${htmlFileDst()}", description: "Tap Here to go to the zigbee graph."
                href name: "changelog", title: "View change log", required: false, page: "changelogPage", description: "Tap Here to go to see the latest application changes."
            }
        } else {
            section {
                paragraph "Tap the [Done] button to create the application instance."
            }
        }
    }
}

def changelogPage() {
    dynamicPage (name: "changelogPage", title: "Zigbee Graph - v${releaseVer() + ' - ' + appVerDate()}", install: false, uninstall: false) {
        section {
		    paragraph "<h1>Change Log</h1>"
        }
        
        section () {
		    paragraph "<h2>v2.0.0 - 2023-05-20</h2>"
            paragraph "<ul><li>Migrate to the new endpoint getChildAndRouteInfoJson to remove text parsing and HTML scraping</li>" +
                "<li>Use <a href=\"https://ethanschoonover.com/solarized/\" target=\"_blank\">Solarized theme</a> colors</li>" +
                "<li>Add support for Dark theme</li>" +
                "<li>Add config option to hide link particles and how directional arrows instead (helps when sharing a graph image)</li>" +
                "<li>Add FAQ section to try to add some meaning to the graph (content is taken mostly from the Hubitat community, thanks!)</li>" +
                "<li>Other small UI improvements</li></ul>"
        }
        
        section () {
		    paragraph "<h2>v1.4.0 - 2023-05-25</h2>"
            paragraph "<ul><li>Add the \"Config\" tab with basic settings</li></ul>"
        }
        
        section () {
		    paragraph "<h2>v1.3.1 - 2023-05-24</h2>"
            paragraph "<ul><li>Bugfix: Devices table keeps accumulating records instead of clearing its contents</li></ul>"
        }
        
        section () {
		    paragraph "<h2>v1.3.0 - 2023-05-23</h2>"
            paragraph "<ul><li>Add a new tab containing a list with all zigbee devices</li></ul>"
        }
        
        section () {
		    paragraph "<h2>v1.2.0 - 2023-05-23</h2>"
            paragraph "<ul><li>Click a node to go to the device edit page</li>" +
                "<li>Add embed=true URL parameter to hide controls</li>" +
                "<li>Make application interface friendlier</li></ul>"
        }
        
        section () {
		    paragraph "<h2>v1.1.0 - 2023-05-23</h2>"
            paragraph "<ul><li>Integrate zigbee logs into the graph</li></ul>"
        }
        
        section () {
		    paragraph "<h2>v1.0.0 - 2023-05-22</h2>"
            paragraph "<ul><li>Initial release</li></ul>"
        }
    }
}

// Standard device methods
def installed() {
    log.debug "${app?.getLabel()} has been installed"
}

def updated() {
    log.debug "${app?.getLabel()} has been updated"
}

def refresh() {
    log.debug "${app?.getLabel()} has been refreshed"
}

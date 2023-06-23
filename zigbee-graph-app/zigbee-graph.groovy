/*
 * Zigbee Graph app for Hubitat
 *
 * Eye-candy visual render for data we get from /hub/zigbee/getChildAndRouteInfo.
 *
 * @see https://github.com/dan-danache/hubitat
 */
definition(
    name: "Zigbee Graph",
    namespace: "dandanache",
    author: "Dan Danache",
    description: "Allows you to visually render getChildAndRouteInfo of your Hubitat system.",
    documentationLink: "https://community.hubitat.com/t/zigbee-visual-render-for-getchildandrouteinfo/119074",
    importUrl: "https://raw.githubusercontent.com/dan-danache/hubitat/master/zigbee-graph-app/zigbee-graph.groovy",
    category: "Utility",
    singleInstance: true,
    installOnOpen: true,
    iconUrl: "",
    iconX2Url: "",
    oauth: false
)

/**********************************************************************************************************************************************/
private releaseVer() { return "2.2.0" }
private appVerDate() { return "2023-06-23" }
private htmlFileName() { return "zigbee-graph.html" }
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
		        href (
                    name: "show",
                    title: "Show zigbee graph",
                    description: "Tap Here to go to the zigbee graph.",
                    url: "/local/${buildGraphURL()}",
                    style: "embedded",
                    required: false
                )
                href (
                    name: "changelog",
                    title: "View change log",
                    description: "Tap Here to go to see the latest application changes.",
                    page: "changelogPage",
                    required: false
                )
            }
            section ("Maker API integration", hideable: true, hidden: !Use_MakerAPI) {
                paragraph "If you have the Maker API built-in app installed and configured, you can enable the option below if you want to add the room name in device names."
                input "Use_MakerAPI", "bool", title: "Use Maker API", submitOnChange: true
                if (Use_MakerAPI) {
                    input "MakerAPI_GetAllDevicesURL", "string", title: "Get All Devices URL", required: true
                    paragraph "ℹ️ How to get the URL: Go to Apps, open \"Maker API\", scroll down until you find the \"Get All Devices\" link, click it, copy the URL from the browser location bar, then paste it in the input above."
                    input "btnMakerAPI", "button", title: "Save settings"
                }
            }
        } else {
            section {
                paragraph "Tap the [Done] button to create the application instance."
            }
        }
    }
}

def changelogPage() {
    dynamicPage (name: "changelogPage", title: "Zigbee Graph - Change Log", install: false, uninstall: false) {
        
        section ("v2.2.0 - 2023-06-23", hideable: true, hidden: false) {
            paragraph "<li>Show dates using the \"time ago\" format</li>" +
                "<li>Move contents of the \"Help\" tab inside the \"Status\" tab</li>" +
                "<li>Add option to display room name in the device name (using the Maker API built-in app)</li>"
        }
        
        section ("v2.1.0 - 2023-06-21", hideable: true, hidden: true) {
            paragraph "<li>Add \"Node repulsion force\" config option to better visualize graphs with many devices</li>"
        }

        section ("v2.0.0 - 2023-06-20", hideable: true, hidden: true) {
            paragraph "<li>Migrate to the new endpoint getChildAndRouteInfoJson to remove text parsing and HTML scraping</li>" +
                "<li>Use <a href=\"https://ethanschoonover.com/solarized/\" target=\"_blank\">Solarized theme</a> colors</li>" +
                "<li>Add support for Dark theme</li>" +
                "<li>Add config option to hide link particles and how directional arrows instead (helps when sharing a graph image)</li>" +
                "<li>Add FAQ section to try to add some meaning to the graph (content is taken mostly from the Hubitat community, thanks!)</li>" +
                "<li>Other small UI improvements</li>"
        }
        
        section ("v1.4.0 - 2023-05-25", hideable: true, hidden: true) {
            paragraph "<li>Add the \"Config\" tab with basic settings</li>"
        }
        
        section ("v1.3.1 - 2023-05-24", hideable: true, hidden: true) {
            paragraph "<li>Bugfix: Devices table keeps accumulating records instead of clearing its contents</li>"
        }
        
        section ("v1.3.0 - 2023-05-23", hideable: true, hidden: true) {
            paragraph "<li>Add a new tab containing a list with all zigbee devices</li>"
        }
        
        section ("v1.2.0 - 2023-05-23", hideable: true, hidden: true) {
            paragraph "<li>Click a node to go to the device edit page</li>" +
                "<li>Add embed=true URL parameter to hide controls</li>" +
                "<li>Make application interface friendlier</li>"
        }
        
        section ("v1.1.0 - 2023-05-23", hideable: true, hidden: true) {
            paragraph "<li>Integrate zigbee logs into the graph</li>"
        }
        
        section ("v1.0.0 - 2023-05-22", hideable: true, hidden: true) {
            paragraph "<li>Initial release</li>"
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

// Button handler
def appButtonHandler(btn) {
	switch (btn) {
		case "btnMakerAPI":
            log.debug "Saving Maker API settings..."
			break
	}
}

private buildGraphURL() {
    if (Use_MakerAPI && MakerAPI_GetAllDevicesURL) {
        return "${htmlFileName()}?mapi=${java.net.URLEncoder.encode(MakerAPI_GetAllDevicesURL, "UTF-8")}"
    }
    return htmlFileName();
}

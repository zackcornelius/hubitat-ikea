{{!--------------------------------------------------------------------------}}
{{# @definition }}
capability "HealthCheck"
{{/ @definition }}
{{!--------------------------------------------------------------------------}}
{{# @fields }}

// Fields for capability.HealthCheck
@Field def HEALTH_CHECK = [
    "schedule": "{{ params.schedule }}", // Health will be checked using this cron schedule
    "thereshold": {{ params.thereshold }} // When checking, mark the device as offline if no Zigbee message was received in the last {{ params.thereshold }} seconds
]
{{/ @fields }}
{{!--------------------------------------------------------------------------}}
{{# @attributes }}

// Attributes for capability.HealthCheck
attribute "healthStatus", "ENUM", ["offline", "online", "unknown"]
{{/ @attributes }}
{{!--------------------------------------------------------------------------}}
{{# @updated }}

// Preferences for capability.HealthCheck
schedule HEALTH_CHECK.schedule, "healthCheck"
{{/ @updated }}
{{!--------------------------------------------------------------------------}}
{{# @helpers }}

// Helpers for capability.HealthCheck
def healthCheck() {
   Log.debug '⏲️ Automatically running health check'
    def healthStatus = state?.lastRx == 0 ? "unknown" : (now() - state.lastRx < HEALTH_CHECK.thereshold * 1000 ? "online" : "offline")
    Utils.sendEvent name:"healthStatus", value:healthStatus, type:"physical", descriptionText:"Health status is ${healthStatus}"
}
{{/ @helpers }}
{{!--------------------------------------------------------------------------}}
{{# @configure }}

// Configuration for capability.HealthCheck
sendEvent name:"healthStatus", value:"online", descriptionText:"Health status initialized to online"
sendEvent name:"checkInterval", value:{{ params.checkInterval }}, unit:"second", descriptionText:"Health check interval is {{ params.checkInterval }} seconds"
{{/ @configure }}
{{!--------------------------------------------------------------------------}}
{{# @implementation }}

// Implementation for capability.HealthCheck
def ping() {
    Log.info "ping ..."
    Utils.sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000))
    Log.debug "Ping command sent to the device; we'll wait 5 seconds for a reply ..."
    runIn 5, "pingExecute"
}

def pingExecute() {
    if (state.lastRx == null || state.lastRx == 0) {
        return Log.info("Did not sent any messages since it was last configured")
    }

    def now = new Date(Math.round(now() / 1000) * 1000)
    def lastRx = new Date(Math.round(state.lastRx / 1000) * 1000)
    def lastRxAgo = TimeCategory.minus(now, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Sent last message at ${lastRx.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${lastRxAgo} ago)"

    def thereshold = new Date(Math.round(state.lastRx / 1000 + HEALTH_CHECK.thereshold) * 1000)
    def theresholdAgo = TimeCategory.minus(thereshold, lastRx).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received for ${theresholdAgo} (hardcoded)"

    def offlineMarkAgo = TimeCategory.minus(thereshold, now).toString().replace(".000 seconds", " seconds")
    Log.info "Will me marked as offline if no message is received until ${thereshold.format("yyyy-MM-dd HH:mm:ss", location.timeZone)} (${offlineMarkAgo} from now)"
}
{{/ @implementation }}
{{!--------------------------------------------------------------------------}}
{{# @parse }}

// Parse for capability.HealthCheck
if (device.currentValue("healthStatus", true) != "online") {
    Utils.sendEvent name:"healthStatus", value:"online", type:"digital", descriptionText:"Health status changed to online"
}
{{/ @parse }}
{{!--------------------------------------------------------------------------}}
{{# @events }}

// Events for capability.HealthCheck
case { contains it, [clusterInt:0x0000, attrInt:0x0000] }:
    return Log.info("... pong")
{{/ @events }}
{{!--------------------------------------------------------------------------}}

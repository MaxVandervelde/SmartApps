/**
 * Turn Off In...
 *
 * Turn off a device in a specified amount of time. I personally use this for my dehydrator, but I'm sure there
 * are all sorts of other practical applications
 */
preferences {
    section("Select switches to turn off...") {
        input name: "switches", type: "capability.switch", multiple: true
    }
    section("Turn them off at...") {
        input name: "offTimeHr", title: "Turn Off In (hrs)...", type: "number"
        input name: "offTimeMin", title: "Turn Off In (mins)...", type: "number"
    }
    section("Send a notification?"){
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialized()
}

def initialized () {
    def offTime = offTimeHr * 60 * 60 + offTimeMin * 60
    log.debug "timer set to ${offTime} minutes"
    runIn(offTime, "startTimerCallback")
    subscribe(app)
}

def updated(settings) {
    unschedule()
    initialized()
}

def startTimerCallback() {
    log.debug "Turning off switches"
    switches?.off()
    if (sendPushMessage == "Yes") sendPush "Turning off ${switches.displayName}"
}
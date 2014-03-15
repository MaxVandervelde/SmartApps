/**
 * Smart Alarm
 *
 * Turn on one or more switches at a specified time.
 * Great for going off with your alarm for those hard to get
 * out of bed days.
 *
 * After setting up the application with smart things, you can press the
 * app icon to turn on/off the alarm. A push notification will be sent to confirm
 */
preferences {
    section("Select switches to control...") {
        input name: "switches", type: "capability.switch", multiple: true
    }
    section("Turn them all on at...") {
        input name: "startTime", title: "Turn On Time?", type: "time"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialized()
}

def initialized () {
    schedule(startTime, "startTimerCallback")
    subscribe(app, touchhandler)
}

def touchhandler(event) {
    state.enabled = !state.enabled
    if (state.enabled) {
        sendPush("Alarm Is Enable")
    } else {
        sendPush("Alarm Is Disabled")
    }
    log.debug state.enable
    log.debug event.value
}

def updated(settings) {
    unschedule()
    initialized()
}

def startTimerCallback() {
    if (state.enabled) {
        log.debug "Turning on switches"
        switches.on()
    }

}
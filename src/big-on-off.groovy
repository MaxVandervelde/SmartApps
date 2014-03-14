/**
 *  Big On/Off
 *  Create a group of devices that can be turned on when the app icon is pressed
 *  and turned off again when the app icon is pressed again
 */
preferences {
    section("When I touch the app, turn on/off...") {
        input "switches", "capability.switch", multiple: true
    }
}

def installed() {
    init()
}

def updated() {
    unsubscribe()
    init()
}

def init() {
    subscribe(app, appTouch)
}

def appTouch(evt) {
    log.debug "appTouch: $evt"
    state.enabled = !state.enabled
    if (state.enabled) {
        switches?.on()
    } else {
        switches?.off()
    }
}

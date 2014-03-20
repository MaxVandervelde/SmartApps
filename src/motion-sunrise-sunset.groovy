/*
 * Copyright (C) 2014 Andrew Reitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *  Motion Sensor w/ sunrise sunset
 *
 *  Like the normal motion sensor application but will only turn on switches if it is after sunset and before sunrise
 */
preferences {
    section("When there's movement...") {
        input "motionDevices", "capability.motionSensor", title: "Where?", multiple: true
    }

    section("Turn on...") {
        input "switches", "capability.switch", multiple: true
    }

    section("Additional settings", hideable: true, hidden: true) {
        paragraph("Default timeout is 10 Minutes")
        input "turnOffTime", "decimal", title: "Turn off in... (minutes)",
                description: "Enter time in minutes", defaultValue: 10, required: false
        input "extraTime", "decimal", title: "Additional Time Before Sunrise / After Sunset (minutes)", // i really suck at naming seriously fix this when thinking straight
                description: "Enter time in minutes", defaultValue: 0, required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(motionDevices, "motion", motionHandler)
}

def motionHandler(evt) {
    def othersActive = motionDevices.find { it.id != evt.deviceId && it.currentValue("motion") == "active" } != null
    if (!othersActive) {
        log.debug "there"
        def anyOff = switches?.find { it.currentValue("switch") != "on" }
        log.debug "balls"
        if (evt.value == "active" && anyOff) {
            log.debug "butts"
            if (shouldTurnOn()) {
                switches?.on()
            }
        } else if (evt.value == "inactive") {
            def delay = findturnOffTime() * 60
            runIn(delay, turnOffWhenNoMotion, [overwrite: false])
        }
    }
}

def turnOffWhenNoMotion() {
    switches?.off()
}

/** Check if between the window to turn on when there is motion */
private shouldTurnOn() {
    log.debug "in should turn on"
    def sunRiseSunset = getSunriseAndSunset()
    def sunrise = sunRiseSunset.sunrise.time
    def sunset = sunRiseSunset.sunset.time
    def now = new Date().time + extraTime * 60 * 1000 // add extra millis to turn off the light sooner / later

    if (now > sunset) {
        log.debug "after sunset"
        return true
    }

    if (now < sunrise) {
        log.debug "before sunrise"
        return true
    }

    log.debug "not before sunrise or after sunset"
    return false
}

private findturnOffTime() {
    (turnOffTime != null && turnOffTime != "") ? turnOffTime : 10
}

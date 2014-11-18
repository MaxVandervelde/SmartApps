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
 * Jenkins Notifier
 *
 * Checks a Jenkins server at a specific time, if the build fails it will turn on a light.  If the build goes from
 * failing back to succeeding the light will turn off. Hues can also be used in place of the light in order to create
 * colors for build statuses
 */
preferences {
    section("The URL to your Jenkins, including the job you want to monitor. Ex. https://jenkins.example.com/job/myproject/") {
        input "jenkinsUrl", "text", title: "Jenkins URL"
    }
    section("Jenkins Username") {
        input "jenkinsUsername", "text", title: "Jenkins Username"
    }
    section("Jenkins Password") {
        input "jenkinsPassword", "password", title: "Jenkins Password"
    }
    section("On Failed Build Turn On...") {
        input "switches", "capability.switch", multiple: true, required: false
    }
    section("Or Change These Bulbs...") {
        input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required: false, multiple: true
        input "colorSuccess", "enum", title: "Hue Color On Success?", required: false, multiple: false, options: getHueColors().keySet() as String[]
        input "colorFail", "enum", title: "Hue Color On Fail?", required: false, multiple: false, options: getHueColors().keySet() as String[]
        input "lightLevelSuccess", "number", title: "Light Level On Success?", required: false
        input "lightLevelFail", "number", title: "Light Level On Fail?", required: false
    }
    section("Additional settings", hideable: true, hidden: true) {
        paragraph("Default check time is 15 Minutes")
        input "refreshInterval", "decimal", title: "Check Server... (minutes)",
                description: "Enter time in minutes", defaultValue: 15, required: false
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

/** Constants for Hue Colors */
Map getHueColors() {
    return [
        Red: [hue: 100, saturation: 100],
        Green: [hue: 39, saturation: 100],
        Blue: [hue: 70, saturation: 100],
        Yellow: [hue: 25, saturation: 100],
        Orange: [hue: 10, saturation: 100],
        Purple: [hue: 75, saturation: 100],
        Pink: [hue: 83, saturation: 100],
        SoftWhite: [hue: 23, saturation: 56],
        Daylight: [hue: 53, saturation: 91]
    ]
}

/** Constant for Level */
int getMaxLevel() {
    return 100;
}

def initialize() {
    def successColor = getHueColors()[colorSuccess] + [switch: "on", level: lightLevelSuccess ?: getMaxLevel()]
    def failColor = getHueColors()[colorFail] + [switch: "on", level: lightLevelFail ?: getMaxLevel()]
    state.successColor = successColor
    state.failColor = failColor
    log.debug "successColor: ${successColor}, failColor: ${failColor}"

    checkServer()

    def cron = "* /${refreshInterval ?: 15} * * * ?"
    schedule(cron, checkServer)
}

def checkServer() {
    log.debug "Checking Server Now"

    def successColor = state.successColor
    def failColor = state.failColor

    def basicCredentials = "${jenkinsUsername}:${jenkinsPassword}"
    def encodedCredentials = basicCredentials.encodeAsBase64().toString()
    def basicAuth = "Basic ${encodedCredentials}"

    def head = ["Authorization": basicAuth]

    log.debug "Auth ${head}"

    def host = jenkinsUrl.contains("lastBuild/api/json") ? jenkinsUrl : "${jenkinsUrl}/lastBuild/api/json"

    httpGet(uri: host, headers: ["Authorization": "${basicAuth}"]) { resp ->
        def buildSuccess = (resp.data.result == "SUCCESS")
        log.debug "Build Success? ${buildSuccess}"
        if (!buildSuccess) {
            switches?.on()
            hues?.setColor(failColor)
        } else {
            switches?.off()
            hues?.setColor(successColor)
        }

    }
}

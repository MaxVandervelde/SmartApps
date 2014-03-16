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
def HUE_COLORS = [Red: 0, Green: 39, Blue: 70, Yellow: 25, Orange: 10, Purple: 75, Pink: 83]

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
        input "colorSuccess", "enum", title: "Hue Color On Success?", required: false, multiple: false, options: HUE_COLORS.keySet() as String[]
        input "colorFail", "enum", title: "Hue Color On Fail?", required: false, multiple: false, options: HUE_COLORS.keySet() as String[]
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

def initialize() {
    // Because I can't figure out how to do constants...
    def HUE_COLORS = [Red: 0, Green: 39, Blue: 70, Yellow: 25, Orange: 10, Purple: 75, Pink: 83]
    def HUE_SATURATION = 100
    subscribe(app)
    log.debug "COLORS ${HUE_COLORS}"
    log.debug "colorSuccess: ${colorSuccess}, colorFail: ${colorFail}"
    def successColor = [hue: HUE_COLORS[colorSuccess], saturation: HUE_SATURATION, level: lightLevelSuccess ?: 100]
    def failColor = [hue: HUE_COLORS[colorFail], saturation: HUE_SATURATION, level: lightLevelFail ?: 100]
    log.debug "successColor: ${successColor}, failColor: ${failColor}"
    checkServer(successColor, failColor)
    def cron = "* /${refreshInterval ?: 15} * * * ?"
    schedule(cron, checkServer)
}

def checkServer(successColor, failColor) {
    log.debug "Checking Server Now"

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
            hues?.on()
            hues?.setColor(failColor)
        } else {
            switches?.off()
            hues?.off()
            hues?.setColor(successColor)
        }

    }
}
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
 * Checks a Jenkins server at a specific time, if the build fails it will turn on a light   
 */
preferences {
    section("The URL to your Jenkins, includeing the job you want to monitor. Ex. https://jenkins.example.com/job/myproject/"){
        input "jenkinsUrl", "text", title: "Jenkins URL"
    }
    section("Jenkins Username"){
        input "jenkinsUsername", "text", title: "Jenkins Username"
    }
    section("Jenkins Password"){
        input "jenkinsPassword", "password", title: "Jenkins Password"
    }
    section("On Fialed Build Turn On...") {
        input "switches", "capability.switch", multiple: true
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
    subscribe(app)
    checkServer()
    def cron = "* /${refreshInterval?:15} * * * ?"
    schedule(cron, checkServer)
}

def checkServer() {
    log.debug "Checking Server Now"

    def basicCredentials = "${jenkinsUsername}:${jenkinsPassword}"
    def encodedCredentials = basicCredentials.encodeAsBase64().toString()
    def basicAuth = "Basic ${encodedCredentials}"

    def head = ["Authorization": basicAuth]

    log.debug "Auth ${head}"

    httpGet(uri: jenkinsUrl, headers: ["Authorization": "${basicAuth}"]) { resp ->
        def buildSuccess = (resp.data.result == "SUCCESS")
        log.debug buildSuccess
        if (!buildSuccess) {
            switches?.on()
        } else{
            switches?.off()
        }

    }
}
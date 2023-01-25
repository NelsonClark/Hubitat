/*
 *  Advanced vThermostat V2 App
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/Advanced_vThermostat_V2
 *  Copyright 2023 Nelson Clark
 *
 *  This app requires it's child app and uses the built in virtual thermostat device driver to function, please go to the project page for more information.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  Version History...
 *
 *  V2.0.0 - First limited release, some functions not yet implemented (Hysteresis change, Fan Modes other than Auto)
 *	v2.0.1 - Install check, if not yet installed, message to hit [Done] before being able to add new child vThermostat.
 *
 *
*/

definition(
	name: "Advanced vThermostat V2",
	namespace: "nclark",
	author: "Nelson Clark",
	description: "Use any temperature sensor(s) with any outlet(s)/plug(s) and make them be controlled by a virtual thermostat device.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat_V2/Advanced_vThermostat_V2-logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat_V2/Advanced_vThermostat_V2-logo.png",
	importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat_V2/Advanced_vThermostat_V2-Parent.groovy",
	singleInstance: true
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if (app.getInstallationState() == 'INCOMPLETE') {
            section("Hit Done to install Advanced vThermostat V2 App") {
        	}
        } else {
			section("<b>Create a new Advanced vThermostat Instance.</b>") {
				app(name: "Thermostats", appName: "Advanced vThermostat V2 Child", namespace: "nclark", title: "Add Advanced vThermostat V2", multiple: true)
			}
		}
	}
}

def installed() {
	log.debug "Installed"
	initialize()
}

def updated() {
	log.debug "Updated"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initializing; there are ${childApps.size()} child apps installed"
	childApps.each {child -> 
		log.debug "  child app: ${child.label}"
	}
}

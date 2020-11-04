/*
 *  vThermostat Child App
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/vThermostat
 *  Copyright 2020 Nelson Clark
 *
 *  This app requires it's parent app and device driver to function, please go to the project page for more information.
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
 */

definition(
	name: "vThermostat Child",
	namespace: "nclark",
	author: "Nelson Clark",
	description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-logo.png",
	importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-Child.groovy",
	parent: "nclark:vThermostat Manager"
)

preferences {
	section("Choose temperature sensor(s)... (Average value will be used if you select multiple sensors.)"){
		input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true
	}

	section("Select outlet(s) to use for heating... "){
		input "heatOutlets", "capability.switch", title: "Outlets", multiple: true
	}

	section("Select outlet(s) to use for cooling... "){
		input "coolOutlets", "capability.switch", title: "Outlets", multiple: true
	}

	section("Initial Thermostat Settings: "){
		input "heatingSetPoint", "decimal", title: "Heating Setpoint", required: true, defaultValue: 68.0
		input "coolingSetPoint", "decimal", title: "Cooling Setpoint", required: true, defaultValue: 76.0
		input (name:"thermostatMode", type:"enum", title:"Thermostat Mode", options: ["auto","heat","cool","off"], defaultValue:"auto", required: true)
		input "thermostatThreshold", "decimal", "title": "Temperature Threshold", required: true, defaultValue: 1.0
	}
	
	section("Log Settings") {
		input (name: "logLevel", type: "enum", title: "Live Logging Level: Messages with this level and higher will be logged", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
		input "logDropLevelTime", "decimal", title: "Drop down to Info Level Minutes", required: true, defaultValue: 5
	}
}

def installed() {
	state.loggingLevel = (settings.logLevel) ? settings.logLevel.toInteger() : 3
	if (state.logLevel >= 3) runIn(settings.logDropLevelTime.toInteger() * 60,logsOff)
	log.info "Log Level: $state.loggingLevel"
    log.info "LogDropLevelTime: $settings.logDropLevelTime"
	
	log.debug "Running installed vThermostat: $app.label"
	state.deviceID = "jmvt" + Math.abs(new Random().nextInt() % 9999) + 1

	//create the child device
	def thermostat
	def label = app.getLabel()
	log.debug "Creating vThermostat : $label device id: jmvt$state.deviceID"
	try {
		thermostat = addChildDevice("nclark", "vThermostat Device", state.deviceID, null, [label: label, name: label, completedSetup: true])
	} catch(e) {
		log.error("Could not create vThermostat; caught exception", e)
	}
	initialize(thermostat)
}

def updated() {
	state.loggingLevel = (settings.logLevel) ? settings.logLevel.toInteger() : 3
	if (state.logLevel >= 3) runIn(settings.logDropLevelTime.toInteger() * 60,logsOff)
	log.info "Log Level: $state.loggingLevel"
    log.info "LogDropLevelTime: $settings.logDropLevelTime"
	log.debug "Running updated vThermostat: $app.label"
	initialize(getThermostat())
}

def initialize(thermostatInstance) {
	log.debug "Running initialized vThermostat: $app.label"

	unsubscribe()
	unschedule()

	thermostatInstance.setHeatingSetpoint(heatingSetPoint)
	thermostatInstance.setCoolingSetpoint(coolingSetPoint)
	thermostatInstance.setThermostatThreshold(thermostatThreshold)
	thermostatInstance.setThermostatMode(thermostatMode)

	subscribe(sensors, "temperature", temperatureHandler)
	subscribe(thermostat, "thermostatOperatingState", thermostatStateHandler)

	updateTemperature()

	runEvery1Minute(setOutletsState)
}

def getThermostat() {
	if (!state.deviceID){
		log.debug "getThermostat cannot access deviceID!"
	} else {
		log.debug "getThermostat for device " + state.deviceID

		def child = getChildDevices().find {
			d -> d.deviceNetworkId.startsWith(state.deviceID)
		}
		return child
	}
}

def uninstalled() {
	deleteChildDevice(state.deviceID)
}

def temperatureHandler(evt)
{
	log.debug "Temperature changed to" + evt.doubleValue
	updateTemperature()
}

def updateTemperature() {
	def total = 0;
	def count = 0;
	def thermostat=getThermostat()
	for(sensor in sensors) {
		total += sensor.currentValue("temperature")
		log.debug "Sensor $sensor.label reported " + sensor.currentValue("temperature")
		count++
	}
	def avgTemp = total / count
	thermostat.setTemperature(avgTemp)
	return avgTemp
}

def thermostatStateHandler(evt) {
	if (evt.value) {
		log.debug "Thermostat state changed to $opState"
		setOutletsState(opState)
	} else {
		log.debug "thermostatStateHandler got an empty event"
	}
}

def setOutletsState(opState) {
	def thermostat = getThermostat()
	opState = opState ? opState : thermostat.currentValue("thermostatOperatingState")

	if (opState == "heating") {
		coolOutlets ? coolOutlets.off() : null
		//We need a delay to insure the off command completes as some of the heat/cool outlets could be the same.
		heatOutlets ? heatOutlets.on() : null
		log.debug "Turned on heating outlets."
	} else if (opState == "cooling") {
		heatOutlets ? heatOutlets.off() : null
		//We need a delay to insure the off command completes as some of the heat/cool outlets could be the same.
		coolOutlets ? coolOutlets.on() : null
		log.debug "Turned on cooling outlets."
	} else {
		heatOutlets ? heatOutlets.off() : null
		coolOutlets ? coolOutlets.off() : null
		log.debug "Turned off all heat/cool outlets."
	}
	
}

//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
//  logger(msg, level)
//
//  Wrapper function for all logging:
//    Logs messages to the IDE (Live Logging)
//    Configured using logLevel preferences
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevel >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevel >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevel >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevel >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevel >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

private logsOff(){
    log.warn "Logging level set to 3"
    device.updateSetting("logLevel",[value:"3",type:"enum"])
}

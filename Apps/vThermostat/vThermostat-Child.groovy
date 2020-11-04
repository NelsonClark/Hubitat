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
	section("Select temperature sensor(s)... (Average value will be used if you select multiple sensors)"){
		input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true
	}

	section("Select outlet(s) to use for heating... "){
		input "heatOutlets", "capability.switch", title: "Outlets", multiple: true
	}

	section("Select outlet(s) to use for cooling... "){
		input "coolOutlets", "capability.switch", title: "Outlets", multiple: true
	}

	section("Initial Thermostat Settings..."){
		input "heatingSetPoint", "decimal", title: "Heating Setpoint", required: true, defaultValue: 68.0
		input "coolingSetPoint", "decimal", title: "Cooling Setpoint", required: true, defaultValue: 76.0
		input (name:"thermostatMode", type:"enum", title:"Thermostat Mode", options: ["auto","heat","cool","off"], defaultValue:"auto", required: true)
		input "thermostatThreshold", "decimal", "title": "Temperature Threshold", required: true, defaultValue: 1.0
	}
	
	section("Log Settings...") {
		input (name: "logLevel", type: "enum", title: "Live Logging Level: Messages with this level and higher will be logged", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
		input "logDropLevelTime", "decimal", title: "Drop down to Info Level Minutes", required: true, defaultValue: 5
	}
}

def installed() {
	//Set Logging level and Dropt to level 3 if level is higher in set number of seconds
	//def logLevelTime
	state.loggingLevel = (settings.logLevel) ? settings.logLevel.toInteger() : 3 //*** Can this be simplefied
	//logLevelTime = settings.logDropLevelTime.toInteger() * 60
	if (state.loggingLevel >= 3) runIn(settings.logDropLevelTime.toInteger() * 60,logsOff)
	
	logger("trace", "Installed LogLevel: $state.loggingLevel")
	logger("trace", "Installed LogDropLevelTime: $settings.logDropLevelTime")
	
	logger("trace", "Installed Running vThermostat: $app.label")
	state.deviceID = "jmvt" + Math.abs(new Random().nextInt() % 9999) + 1

	//Create the child device
	def thermostat
	def label = app.getLabel()
	logger("info", "Creating vThermostat : ${label} with device id: jmvt${state.deviceID}") //*** What is this jmvt in front of deviceID
	try {
		//** Should we add isComponent in the properties of the child device to make sure we can't remove the Device, will this make it that we can't change settings in it? 
		thermostat = addChildDevice("nclark", "vThermostat Device", state.deviceID, null, [label: label, name: label, completedSetup: true]) //** Deprecated hubIDl no longer passed since 2.1.9
		//thermostat = addChildDevice("nclark", "vThermostat Device", state.deviceID, [label: label, name: label, completedSetup: true]) //** This will only work with ver 2.1.9 and up, let's wait a bit
	} catch(e) {
		logger("error", "Error adding vThermostat child ${label}: ${e}") //*** Not 100% sure about this one, test message outside loop to be sure ***
		//*** Original code: log.error("Could not create vThermostat; caught exception", e)
	}
	initialize(thermostat)
}

def updated() {
	//Update Logging level and Dropt to level 3 if level is higher in set number of seconds
	state.loggingLevel = (settings.logLevel) ? settings.logLevel.toInteger() : 3 //*** Can this be simplefied
	if (state.loggingLevel >= 3) runIn(settings.logDropLevelTime.toInteger() * 60,logsOff)
	logger("trace", "Updated LogLevel: $state.loggingLevel")
	logger("trace", "Updated LogDropLevelTime: $settings.logDropLevelTime")
	logger("trace", "Updated Running vThermostat: $app.label")
	initialize(getThermostat())
}

def initialize(thermostatInstance) {
	logger("trace", "Initialize Running vThermostat: $app.label")

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

//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
// getThermostat
//     Gets current childDeviceWrapper from list of childs
//
// Signature(s)
//     getThermostat()
//
// Parameters
//     None
//
// Returns
//     ChildDeviceWrapper
//
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
def getThermostat() {
	// Does this instance have a DeviceID
	if (!state.deviceID){
		//No DeviceID available what is going on, has the device been removed?
		logger("error", "getThermostat cannot access deviceID!")
	} else {
		//We have a deviceID, continue and return it
		logger("trace", "getThermostat for device " + state.deviceID)

		def child = getChildDevices().find {
			d -> d.deviceNetworkId.startsWith(state.deviceID)
		}
		logger("trace","getThrmostat child is ${child}")
		return child
	}
}

def uninstalled() {
	deleteChildDevice(state.deviceID)
	logger("info", "Child Device " + state.deviceID + " removed")
}

def temperatureHandler(evt)
{
	logger("debug", "Temperature changed to" + evt.doubleValue)
	updateTemperature()
}

def updateTemperature() {
	def total = 0;
	def count = 0;
	def thermostat=getThermostat()
	for(sensor in sensors) {
		total += sensor.currentValue("temperature")
		logger("debug", "Sensor $sensor.label reported " + sensor.currentValue("temperature"))
		count++
	}
	def avgTemp = total / count
	thermostat.setTemperature(avgTemp)
	return avgTemp
}

def thermostatStateHandler(evt) {
	if (evt.value) {
		logger("warn", "Thermostat state changed to $opState")
		setOutletsState(opState)
	} else {
		logger("warn", "thermostatStateHandler got an empty event")
	}
}

def setOutletsState(opState) {
	def thermostat = getThermostat()
	opState = opState ? opState : thermostat.currentValue("thermostatOperatingState")

	if (opState == "heating") {
		coolOutlets ? coolOutlets.off() : null
		//We need a delay to insure the off command completes as some of the heat/cool outlets could be the same.
		heatOutlets ? heatOutlets.on() : null
		logger("debug", "Turned on heating outlets.")
	} else if (opState == "cooling") {
		heatOutlets ? heatOutlets.off() : null
		//We need a delay to insure the off command completes as some of the heat/cool outlets could be the same.
		coolOutlets ? coolOutlets.on() : null
		logger("debug", "Turned on cooling outlets.")
	} else {
		heatOutlets ? heatOutlets.off() : null
		coolOutlets ? coolOutlets.off() : null
		logger("debug", "Turned off all heat/cool outlets.")
	}
	
}

//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
//  logger(msg, level)
//
//  Wrapper function for all logging:
//    Logs messages to the IDE (Live Logging)
//    Configured using logLevel preferences
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

private logger(level = "debug", msg) {

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

/*
 *  vThermostat Device Driver
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/vThermostat
 *  Copyright 2020 Nelson Clark
 *
 *  This driver is not meant to be used by itself, please go to the project page for more information.
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

metadata {
	// Automatically generated. Make future change here.
	definition (name: "vThermostat Device", 
		namespace: "nclark", 
		author: "Nelson Clark",
		importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-Device.groovy") {
		
		capability "Thermostat"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Setpoint"
		capability "Sensor"
		capability "Actuator"

		command "heatUp"
		command "heatDown"
		command "coolUp"
		command "coolDown"
		command "setTemperature", ["number"]
		command "setThermostatThreshold", ["number"]
		command "setMinHeatTemp", ["number"]
		command "setMaxHeatTemp", ["number"]
		command "setMinCoolTemp", ["number"]
		command "setMaxCoolTemp", ["number"]
		command "setMaxUpdateInterval", ["number"]

		attribute "thermostatThreshold", "number"
		attribute "minHeatTemp", "number"
		attribute "maxHeatTemp", "number"
		attribute "minCoolTemp", "number"
		attribute "maxCoolTemp", "number"
		attribute "lastTempUpdate", "date"
		attribute "maxUpdateInterval", "number"
		attribute "preEmergencyMode", "string"
		attribute "thermostatOperatingState", "string"
	}
}

def installed() {
	sendEvent(name: "minCoolTemp", value: 60, unit: "F")
	sendEvent(name: "maxCoolTemp", value: 95, unit: "F")
	sendEvent(name: "maxHeatTemp", value: 80, unit: "F")
	sendEvent(name: "minHeatTemp", value: 35, unit: "F")
	sendEvent(name: "thermostatThreshold", value: 1.0, unit: "F")
	sendEvent(name: "temperature", value: 72, unit: "F")
	sendEvent(name: "heatingSetpoint", value: 70, unit: "F")
	sendEvent(name: "thermostatSetpoint", value: 70, unit: "F")
	sendEvent(name: "coolingSetpoint", value: 76, unit: "F")
	sendEvent(name: "thermostatMode", value: "off")
	sendEvent(name: "thermostatOperatingState", value: "idle")
	sendEvent(name: "maxUpdateInterval", value: 65)
	sendEvent(name: "lastTempUpdate", value: new Date() )
}

def updated() {
	sendEvent(name: "minCoolTemp", value: 60, unit: "F")
	sendEvent(name: "maxCoolTemp", value: 95, unit: "F")
	sendEvent(name: "maxHeatTemp", value: 80, unit: "F")
	sendEvent(name: "minHeatTemp", value: 35, unit: "F")
	sendEvent(name: "maxUpdateInterval", value: 65)
	sendEvent(name: "lastTempUpdate", value: new Date() )
}

def parse(String description) {
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def evaluateMode() {
	// Run this loop every minute to see how everything is going
	runIn(60, 'evaluateMode')
	
	// Let's fetch all required thermostat settings
	def temp = device.currentValue("temperature")
	def heatingSetpoint = device.currentValue("heatingSetpoint");
	def coolingSetpoint = device.currentValue("coolingSetpoint");
	def threshold = device.currentValue("thermostatThreshold")
	def current = device.currentValue("thermostatOperatingState")
	def mode = device.currentValue("thermostatMode")

	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	//Deadman safety. Make sure that we don't keep running if temp is not getting updated.
	//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// Let's get the current time in miliseconds
	def now = new Date().getTime()
	// Let's get the time of the last temperature update we got
	def lastUpdate = Date.parse("E MMM dd H:m:s z yyyy", device.currentValue("lastTempUpdate")).getTime()
	// Get maxInterval, if higher than the safety preset, set to preset of 180 minutes (3 hours)
	def maxInterval = device.currentValue("maxUpdateInterval") ?: 180
	// If fetched maxInternal from user is higher than 180, set to 180
	if (maxInterval > 180) maxinterval = 180
	//convert maxUpdateInterval (in minutes) to milliseconds
	maxInterval = maxInterval * 1000 * 60

	logger("debug", "now=$now, lastUpdate=$lastUpdate, maxInterval=$maxInterval, heatingSetpoint=$heatingSetpoint, coolingSetpoint=$coolingSetpoint, temp=$temp")

	if (! (mode in ["emergency stop", "off"]) && now - lastUpdate >= maxInterval ) {
		logger("info", "maxUpdateInterval exceeded. Setting emergencyStop mode")
		sendEvent(name: "preEmergencyMode", value: mode)
		sendEvent(name: "thermostatMode", value: "emergency stop")
		runIn(2, 'evaluateMode')
		return
	} else if (mode == "emergency stop" && now - lastUpdate < maxInterval && device.currentValue("preEmergencyMode")) {
		logger("info", "Autorecovered from emergencyStop. Resetting to previous mode.")
		sendEvent(name: "thermostatMode", value: device.currentValue("preEmergencyMode"))
		sendEvent(name: "preEmergencyMode", value: "")
		runIn(2, 'evaluateMode')
		return
	}

	// set the default to idle (safer to have idle than anything else if something goes wrong)
	def callFor = "idle"

	// Do we have a treshhold set, if not we do nothing 
	// ** This has been changed from original code so that if no treshold is set, the thermostat will stay in idle state
	if ( !threshold ) {
		logger("debug", "Threshold was not set. Not doing anything...")
	} else {
		if (mode in ["heat","emergency heat"]) {
			// Mode is set to heat, let's see if we need to heat or not
			sendEvent(name: "thermostatSetpoint", value: heatingSetpoint)
			if ( (heatingSetpoint - temp) >= threshold) callFor = "heating"
		} else if (mode == "cool") {
			// Mode is set to cool, let's see if we need to cool or not
			sendEvent(name: "thermostatSetpoint", value: coolingSetpoint)
			if ( (temp - coolingSetpoint) >= threshold) callFor = "cooling"
		} else if (mode == "auto") {
			if (temp > coolingSetpoint) { 
				// Mode is set to auto, let's see if we need to cool
				sendEvent(name: "thermostatSetpoint", value: coolingSetpoint)
				if ( (temp - coolingSetpoint) >= threshold) callFor = "cooling"
			} else { 
				// Mode is set to auto, let's see if we need to cool
				sendEvent(name: "thermostatSetpoint", value: heatingSetpoint)
				if ( (heatingSetpoint - temp) >= threshold) callFor = "heating"
			}
		}
	}

	// Send Event on what we are doing, idle, cooling, heating
	logger("debug", "evaluateMode() : threshold=$threshold, actingMode=$mode, origState=$current, newState = $callFor")
	sendEvent(name: "thermostatOperatingState", value: callFor)
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHeatingSetpoint(degreesF){
	setHeatingSetpoint(degreesF.toDouble())
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHeatingSetpoint(Double degreesF) {
	def min = device.currentValue("minHeatTemp")
	def max = device.currentValue("maxHeatTemp")
	if (degreesF > max || degreesF < min) {
		logger("debug", "setHeatingSetpoint is ignoring out of range request ($degreesF).")
		return
	}
	logger("debug", "setHeatingSetpoint($degreesF)")
	sendEvent(name: "heatingSetpoint", value: degreesF)
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setCoolingSetpoint(degreesF){
	setCoolingSetpoint(degreesF.toDouble())
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setCoolingSetpoint(Double degreesF) {
	def min = device.currentValue("minCoolTemp")
	def max = device.currentValue("maxCoolTemp")
	if (degreesF > max || degreesF < min) {
		logger("debug", "setCoolingSetpoint is ignoring out of range request ($degreesF).")
		return
	}
	logger("debug", "setCoolingSetpoint($degreesF)")
	sendEvent(name: "coolingSetpoint", value: degreesF)
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setThermostatThreshold(Double degreesF) {
	logger("debug", "setThermostatThreshold($degreesF)")
	sendEvent(name: "thermostatThreshold", value: degreesF)
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setMaxUpdateInterval(BigDecimal minutes) {
	sendEvent(name: "maxUpdateInterval", value: minutes)
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setThermostatMode(String value) {
	sendEvent(name: "thermostatMode", value: value)
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def off() {
	sendEvent(name: "thermostatMode", value: "off")
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def emergencyStop() {
	sendEvent(name: "thermostatMode", value: "emergency stop")
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def heat() {
	sendEvent(name: "thermostatMode", value: "heat")
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def auto() {
	sendEvent(name: "thermostatMode", value: "auto")
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def emergencyHeat() {
	sendEvent(name: "thermostatMode", value: "emergency heat")
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def cool() {
	sendEvent(name: "thermostatMode", value: "cool")
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def poll() {
	null
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setTemperature(value) {
	sendEvent(name:"temperature", value: value)
	sendEvent(name: "lastTempUpdate", value: new Date() )
	runIn(2,'evaluateMode')
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def heatUp() {
	def ts = device.currentValue("heatingSetpoint")
	setHeatingSetpoint( ts + 1 )
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def heatDown() {
	def ts = device.currentValue("heatingSetpoint")
	setHeatingSetpoint( ts - 1 )
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def coolUp() {
	def ts = device.currentValue("heatingSetpoint")
	setCoolingSetpoint( ts + 1 )
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def coolDown() {
	def ts = device.currentValue("heatingSetpoint")
	setCoolingSetpoint( ts - 1 )
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setMinCoolTemp(Double degreesF) {
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "minCoolTemp", value: degreesF)
	if (t < degreesF) {
		setCoolingSetpoint(degreesF)
	}
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setMaxCoolTemp(Double degreesF) {
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "maxCoolTemp", value: degreesF)
	if (t > degreesF) {
		setCoolingSetpoint(degreesF)
	}
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setMinHeatTemp(Double degreesF) {
	def t = device.currentValue("heatingSetpoint")
	sendEvent(name: "minHeatTemp", value: degreesF)
	if (t < degreesF) {
		setHeatingSetpoint(degreesF)
	}
}


//************************************************************
// Name
//     Does
//
// Signature(s)
//     
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setMaxHeatTemp(Double degreesF) {
	def t = device.currentValue("heatingSetpoint")
	sendEvent(name: "maxHeatTemp", value: degreesF)
	if (t > degreesF) {
		setHeatingSetpoint(degreesF)
	}
}


//************************************************************
// logger
//     Wrapper function for all logging with level control via preferences
//
// Signature(s)
//     logger(String level, String msg)
//
// Parameters
//     level : Error level string
//     msg : Message to log
//
// Returns
//     None
//
//************************************************************
def logger(level, msg) {

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


//************************************************************
// logsDropLevel
//     Turn down logLevel to 3 in this app and it's device and log the change
//
// Signature(s)
//     logsDropLevel()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setLogLevel(int level) {
	state.loggingLevel = level
	logger("warn","Device logging level set to $state.loggingLevel")
}

/*
 *  Advanced Virtual Thermostat Device Driver
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
	definition (name: "vThermostat Device", 
		namespace: "nclark", 
		author: "Nelson Clark",
		importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-Device.groovy") {
		
		capability "Thermostat"
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

		//** Do these all need to be attributes, some should be stateVariables or just Data?
		attribute "thermostatThreshold", "number"
		attribute "minHeatTemp", "number"
		attribute "maxHeatTemp", "number"
		attribute "minCoolTemp", "number"
		attribute "maxCoolTemp", "number"
		attribute "lastTempUpdate", "date"
		attribute "maxUpdateInterval", "number"
		attribute "preEmergencyMode", "string" //** When returning from an Emergency, we should maybe just go idle?
		attribute "thermostatOperatingState", "string" //** Already in the Thermostat attributes, does it need to be here?
	}
}

/*
***** FOR REFERENCE UNTIL WE CLEAN UP THIS MESS *****
Thermostat
Device Selector : capability.thermostat
Driver Definition : capability "Thermostat"

Attributes :
  coolingSetpoint - NUMBER
  heatingSetpoint - NUMBER
  schedule - JSON_OBJECT
  supportedThermostatFanModes - ENUM ["on", "circulate", "auto"]
  supportedThermostatModes - ENUM ["auto", "off", "heat", "emergency heat", "cool"]
  temperature - NUMBER
  thermostatFanMode - ENUM ["on", "circulate", "auto"]
  thermostatMode - ENUM ["auto", "off", "heat", "emergency heat", "cool"]
  thermostatOperatingState - ENUM ["heating", "pending cool", "pending heat", "vent economizer", "idle", "cooling", "fan only"]
  thermostatSetpoint - NUMBER

Commands
  auto()
  cool()
  emergencyHeat()
  fanAuto()
  fanCirculate()
  fanOn()
  heat()
  off()
  setCoolingSetpoint(temperature)
    temperature required (NUMBER) - Cooling setpoint in degrees
  setHeatingSetpoint(temperature)
    temperature required (NUMBER) - Heating setpoint in degrees
  setSchedule(JSON_OBJECT)
    JSON_OBJECT (JSON_OBJECT) - JSON_OBJECT
  setThermostatFanMode(fanmode)
    fanmode required (ENUM) - Fan mode to set
  setThermostatMode(thermostatmode)
    thermostatmode required (ENUM) - Thermostat mode to set
*/

def installed() {
	
	//** We need to transform everything to the base Unit C
	//** Then add variable according to hub settings
	//** Send events only if needed
	
	sendEvent(name: "minCoolTemp", value: 60, unit: "F") // 15.5°C
	sendEvent(name: "maxCoolTemp", value: 95, unit: "F") // 35°C
	sendEvent(name: "maxHeatTemp", value: 80, unit: "F") // 26.5°C
	sendEvent(name: "minHeatTemp", value: 35, unit: "F") // 1.5°C
	sendEvent(name: "thermostatThreshold", value: 1.0, unit: "F") // Set by user
	sendEvent(name: "temperature", value: 72, unit: "F") // 22°C
	sendEvent(name: "heatingSetpoint", value: 70, unit: "F") // 21°C
	sendEvent(name: "thermostatSetpoint", value: 70, unit: "F") // 21°C
	sendEvent(name: "coolingSetpoint", value: 76, unit: "F") // 24.5°C
	sendEvent(name: "thermostatMode", value: "off")
	sendEvent(name: "thermostatOperatingState", value: "idle")
	sendEvent(name: "maxUpdateInterval", value: 65)
	sendEvent(name: "lastTempUpdate", value: new Date() )
}

def updated() {
	
	//** Send events only if needed
	sendEvent(name: "minCoolTemp", value: 60, unit: "F") // 15.5°C
	sendEvent(name: "maxCoolTemp", value: 95, unit: "F") // 35°C
	sendEvent(name: "maxHeatTemp", value: 80, unit: "F") // 26.5°C
	sendEvent(name: "minHeatTemp", value: 35, unit: "F") // 1.5°C
	sendEvent(name: "maxUpdateInterval", value: 65)
	sendEvent(name: "lastTempUpdate", value: new Date() )
}

def parse(String description) {
	// Nothing to parse here since this is a virtual device
}


//************************************************************
// evaluateMode
//     Runned every minute to see where we are at with current 
//     temperature compared to set points, also we evaluate
//     potential problem with sensors not reporting anymore.
// Signature(s)
//     evaluateMode()
// Parameters
//     None
// Returns
//     None
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
	//SAFETY CHECK. Make sure that we don't keep running if temp is not getting updated for whatever reason.
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
		//** Send events only if needed
		sendEvent(name: "preEmergencyMode", value: mode)
		sendEvent(name: "thermostatMode", value: "emergency stop")
		runIn(2, 'evaluateMode')
		return
	} else if (mode == "emergency stop" && now - lastUpdate < maxInterval && device.currentValue("preEmergencyMode")) {
		logger("info", "Autorecovered from emergencyStop. Resetting to previous mode.")
		//** Send events only if needed
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
// setHeatingSetpoint
//     Let's set the desired heating point within limits 
// Signature(s)
//     setHeatingSetpoint(value)
//     setHeatingSetpoint(Double value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setHeatingSetpoint(value){
	setHeatingSetpoint(value.toDouble())
}

def setHeatingSetpoint(Double value) {
	def min = device.currentValue("minHeatTemp")
	def max = device.currentValue("maxHeatTemp")
	if (value > max || value < min) {
		logger("debug", "setHeatingSetpoint is ignoring out of range request ($value).")
		return
	}
	logger("debug", "setHeatingSetpoint($value)")
	sendEvent(name: "heatingSetpoint", value: value)
	runIn(2,'evaluateMode')
}


//************************************************************
// setCoolingSetpoint
//     Let's set the desired Cooling point within limits
// Signature(s)
//     setCoolingSetpoint(value)
//     setCoolingSetpoint(Double value)
// Parameters
//     value :
// Returns
//     None
//************************************************************
def setCoolingSetpoint(value){
	setCoolingSetpoint(value.toDouble())
}

def setCoolingSetpoint(Double value) {
	def min = device.currentValue("minCoolTemp")
	def max = device.currentValue("maxCoolTemp")
	if (value > max || value < min) {
		logger("debug", "setCoolingSetpoint is ignoring out of range request ($value).")
		return
	}
	logger("debug", "setCoolingSetpoint($value)")
	sendEvent(name: "coolingSetpoint", value: value)
	runIn(2,'evaluateMode')
}


//************************************************************
// setThermostatThreshold(value)
//     Let's set the desired treshold
// Signature(s)
//     setThermostatThreshold(value)
//     setThermostatThreshold(Double value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setThermostatThreshold(value) {
	setThermostatThreshold(value.toDouble())
}

def setThermostatThreshold(Double value) {
	logger("debug", "setThermostatThreshold($value)")
	sendEvent(name: "thermostatThreshold", value: value)
	runIn(2,'evaluateMode')
}


//************************************************************
// setMaxUpdateInterval
//     Max interval between two reports of the temp sensors
//     This is for SAFETY reasons
// Signature(s)
//     setMaxUpdateInterval(BigDecimal minutes)
// Parameters
//     minutes :
// Returns
//     None
//************************************************************
def setMaxUpdateInterval(BigDecimal minutes) {
	sendEvent(name: "maxUpdateInterval", value: minutes)
	runIn(2,'evaluateMode')
}


//************************************************************
// setThermostatMode
//     This sets the operating mode of the thermostat
// Signature(s)
//     setThermostatMode(String value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setThermostatMode(String value) {
	sendEvent(name: "thermostatMode", value: value)
	runIn(2,'evaluateMode')
}


//************************************************************
// off
//     Set mode to "off"
// Signature(s)
//     off()
// Parameters
//     None
// Returns
//     None
//************************************************************
def off() {
	sendEvent(name: "thermostatMode", value: "off")
	runIn(2,'evaluateMode')
}


//************************************************************
// emergencyStop
//     Set mode to "emergency stop"
// Signature(s)
//     emergencyStop()
// Parameters
//     None
// Returns
//     None
//************************************************************
def emergencyStop() {
	sendEvent(name: "thermostatMode", value: "emergency stop")
	runIn(2,'evaluateMode')
}


//************************************************************
// heat
//     Set mode to "heat"
// Signature(s)
//     heat()
// Parameters
//     None
// Returns
//     None
//************************************************************
def heat() {
	sendEvent(name: "thermostatMode", value: "heat")
	runIn(2,'evaluateMode')
}


//************************************************************
// auto
//     Set mode to "auto"
// Signature(s)
//     auto()
// Parameters
//     None
// Returns
//     None
//************************************************************
def auto() {
	sendEvent(name: "thermostatMode", value: "auto")
	runIn(2,'evaluateMode')
}


//************************************************************
// emergencyHeat
//     Set mode to "emergency heat"
// Signature(s)
//     emergencyHeat()
// Parameters
//     None
// Returns
//     None
//************************************************************
def emergencyHeat() {
	sendEvent(name: "thermostatMode", value: "emergency heat")
	runIn(2,'evaluateMode')
}


//************************************************************
// cool
//     Set mode to "cool"
// Signature(s)
//     cool()
// Parameters
//     None
// Returns
//     None
//************************************************************
def cool() {
	sendEvent(name: "thermostatMode", value: "cool")
	runIn(2,'evaluateMode')
}


//************************************************************
// Poll
//     Do nothing, maybe in the future?
// Signature(s)
//     poll()
// Parameters
//     None
// Returns
//     None
//************************************************************
def poll() {
	null
}


//************************************************************
// setTemperature
//     Set temperature directly
// Signature(s)
//     setTemperature(value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setTemperature(value) {
	sendEvent(name:"temperature", value: value)
	sendEvent(name: "lastTempUpdate", value: new Date() )
	runIn(2,'evaluateMode')
}


//************************************************************
// heatUp
//     Set heat up one resolution unit
// Signature(s)
//     heatUp()
// Parameters
//     None
// Returns
//     None
//************************************************************
def heatUp() {
	def ts = device.currentValue("heatingSetpoint")
	setHeatingSetpoint( ts + 1 )
}


//************************************************************
// heatDown
//     Set heat down one resolution unit
// Signature(s)
//     heatDown()
// Parameters
//     None
// Returns
//     None
//************************************************************
def heatDown() {
	def ts = device.currentValue("heatingSetpoint")
	setHeatingSetpoint( ts - 1 )
}


//************************************************************
// coolUp
//     Set cool up one resolution unit
// Signature(s)
//     coolUp()
// Parameters
//     None
// Returns
//     None
//************************************************************
def coolUp() {
	def ts = device.currentValue("heatingSetpoint")
	setCoolingSetpoint( ts + 1 )
}


//************************************************************
// coolDown
//     Set cool down one resolution unit
// Signature(s)
//     coolDown()
// Parameters
//     None
// Returns
//     None
//************************************************************
def coolDown() {
	def ts = device.currentValue("heatingSetpoint")
	setCoolingSetpoint( ts - 1 )
}


//************************************************************
// setMinCoolTemp
//     Set minimum cool limit
// Signature(s)
//     setMinCoolTemp(Double value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setMinCoolTemp(Double value) {
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "minCoolTemp", value: value)
	if (t < value) {
		setCoolingSetpoint(value)
	}
}


//************************************************************
// setMaxCoolTemp
//     Set maximum cool limit
// Signature(s)
//     setMaxCoolTemp(Double value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setMaxCoolTemp(Double value) {
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "maxCoolTemp", value: value)
	if (t > value) {
		setCoolingSetpoint(value)
	}
}


//************************************************************
// setMinHeatTemp
//     Set minimum heat limit
// Signature(s)
//     setMinHeatTemp(Double value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setMinHeatTemp(Double value) {
	def t = device.currentValue("heatingSetpoint")
	sendEvent(name: "minHeatTemp", value: value)
	if (t < value) {
		setHeatingSetpoint(value)
	}
}


//************************************************************
// setMaxHeatTemp
//     Set maximum heat limit
// Signature(s)
//     setMaxHeatTemp(Double value)
// Parameters
//     value :
// Returns
//     None
//************************************************************
def setMaxHeatTemp(Double value) {
	def t = device.currentValue("heatingSetpoint")
	sendEvent(name: "maxHeatTemp", value: value)
	if (t > value) {
		setHeatingSetpoint(value)
	}
}


//************************************************************
// logger
//     Wrapper function for all logging with level control via preferences
// Signature(s)
//     logger(String level, String msg)
// Parameters
//     level : Error level string
//     msg : Message to log
// Returns
//     None
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
// setLogLevel
//     Set log level via the child app
// Signature(s)
//     setLogLevel(level)
// Parameters
//     level :
// Returns
//     None
//************************************************************
def setLogLevel(level) {
	state.loggingLevel = level.toInteger()
	logger("warn","Device logging level set to $state.loggingLevel")
}


//************************************************************
// getTemperatureScale
//     Get the hubs temperature scale setting and return the result
// Signature(s)
//     getTemperatureScale()
// Parameters
//     None
// Returns
//     Temperature scale
//************************************************************
def getTemperatureScale() {
	return "${location.temperatureScale}"
}


//************************************************************
// getDisplayUnits
//     Get the diplay units
// Signature(s)
//     getDisplayUnits()
// Parameters
//     None
// Returns
//     Formated Units String
//************************************************************
def getDisplayUnits() {
	if (getTemperatureScale() == "C") {
		return "°C"
	} else {
		return "°F"
	}
}


//************************************************************
// convertToHubTempScale
//     Convert to hubs temperature scale
// Signature(s)
//     convertToHubTempScale(Double value)
// Parameters
//     value : 
// Returns
//     Converted value
//************************************************************
def convertToHubTempScale(Double value) {

	if (getTemperatureScale() == "C") {
		return value
	} else {
		return Math.round(celsiusToFahrenheit(value))
	}
}

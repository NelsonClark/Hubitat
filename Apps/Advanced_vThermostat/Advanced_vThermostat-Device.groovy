/*
 *  Advanced Virtual Thermostat Device Driver
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/Advanced_vThermostat
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
	definition (name: "Advanced vThermostat Device", 
		namespace: "nclark", 
		author: "Nelson Clark",
		importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat/Advanced_vThermostat-Device.groovy") {
		
		capability "Thermostat"
		capability "Sensor"
		capability "Actuator"

		command "heatUp"
		command "heatDown"
		command "coolUp"
		command "coolDown"
		command "setMaxUpdateInterval", ["number"]

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


//************************************************************
//************************************************************
def installed() {
	
	// Let's just set a few things before starting
	def hubScale = getTemperatureScale()
	
	// Let's set all base thermostat settings
	if (hubScale == "C") {
		state.currentUnit = "C"
		sendEvent(name: "minCoolTemp", value: 15.5, unit: "C") // 60°F
		sendEvent(name: "maxCoolTemp", value: 35.0, unit: "C") // 95°F
		sendEvent(name: "minHeatTemp", value: 1.5, unit: "C") // 35°F
		sendEvent(name: "maxHeatTemp", value: 26.5, unit: "C") // 80°F
		sendEvent(name: "thermostatThreshold", value: 0.5, unit: "C") // Set by user
		sendEvent(name: "temperature", value: 22.0, unit: "C") // 72°F
		sendEvent(name: "heatingSetpoint", value: 21.0, unit: "C") // 70°F
		sendEvent(name: "coolingSetpoint", value: 24.5, unit: "C") // 76°F
		sendEvent(name: "thermostatSetpoint", value: 21.0, unit: "C") // 70°F
	} else {
		state.currentUnit = "F"
		sendEvent(name: "minCoolTemp", value: 60, unit: "F") // 15.5°C
		sendEvent(name: "maxCoolTemp", value: 95, unit: "F") // 35°C
		sendEvent(name: "minHeatTemp", value: 35, unit: "F") // 1.5°C
		sendEvent(name: "maxHeatTemp", value: 80, unit: "F") // 26.5°C
		sendEvent(name: "thermostatThreshold", value: 1.0, unit: "F") // Set by user
		sendEvent(name: "temperature", value: 72, unit: "F") // 22°C
		sendEvent(name: "heatingSetpoint", value: 70, unit: "F") // 21°C
		sendEvent(name: "coolingSetpoint", value: 76, unit: "F") // 24.5°C
		sendEvent(name: "thermostatSetpoint", value: 70, unit: "F") // 21°C
	}
	sendEvent(name: "thermostatMode", value: "off")
	sendEvent(name: "thermostatOperatingState", value: "idle")
	sendEvent(name: "maxUpdateInterval", value: 65)
	sendEvent(name: "lastTempUpdate", value: new Date() )

}


//************************************************************
//************************************************************
def updated() {
	//** Send events only if needed ?

	// Let's just set a few things before starting
	def hubScale = getTemperatureScale()
	
	// Hub scale has changed, let's convert everything to new scale
	if (state.currentUnit != hubScale) {
		logger("trace", "updated() - Update values for new hub scale")

		// Let's get all attributes for this device and check them one after the other for changes to do if hub scale unit changed
		List<com.hubitat.hub.domain.State> currentStatesList = device.getCurrentStates()
		currentStatesList.each {
			if (it.unit && it.unit != hubScale) {
				newValue = convertToHubTempScale(it.value)
				logger("warn", "updated() - Change Unit for $it.name from it.value it.unit to $newValue $hubScale")
				//sendEvent(name: it.name, value: newValue, unit: hubScale, isStateChange: true, descriptionText: "Hub unit scale changed")
			}
		}
	

//		if (hubScale == "C") {
//			sendEvent(name: "minCoolTemp", value: 15.5, unit: "C") // 60°F
//			sendEvent(name: "maxCoolTemp", value: 35, unit: "C") // 95°F
//			sendEvent(name: "minHeatTemp", value: 1.5, unit: "C") // 35°F
//			sendEvent(name: "maxHeatTemp", value: 26.5, unit: "C") // 80°F
//			sendEvent(name: "thermostatThreshold", value: 0.5, unit: "C") // Set by user
//			sendEvent(name: "heatingSetpoint", value: 21.0, unit: "C") // 70°F
//			sendEvent(name: "coolingSetpoint", value: 24.5, unit: "C") // 76°F
//			sendEvent(name: "thermostatSetpoint", value: 21.0, unit: "C") // 70°F
//			state.currentUnit = "C"
//		} else {
//			sendEvent(name: "minCoolTemp", value: 60, unit: "F") // 15.5°C
//			sendEvent(name: "maxCoolTemp", value: 95, unit: "F") // 35°C
//			sendEvent(name: "minHeatTemp", value: 35, unit: "F") // 1.5°C
//			sendEvent(name: "maxHeatTemp", value: 80, unit: "F") // 26.5°C
//			sendEvent(name: "thermostatThreshold", value: 1.0, unit: "F") // Set by user
//			sendEvent(name: "heatingSetpoint", value: 70, unit: "F") // 21°C
//			sendEvent(name: "coolingSetpoint", value: 76, unit: "F") // 24.5°C
//			sendEvent(name: "thermostatSetpoint", value: 70, unit: "F") // 21°C
//			state.currentUnit = "F"
//		}
//		sendEvent(name: "maxUpdateInterval", value: 65)
//		sendEvent(name: "lastTempUpdate", value: new Date() )
	} else {
		logger("trace", "updated() - Nothing to do")
	}
}


//************************************************************
//************************************************************
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
	logger("trace", "evaluateMode() - START")
	// Run this loop every minute to see how everything is going
	runIn(60, 'evaluateMode')
	
	// Let's fetch all required thermostat settings
	def temp = device.currentValue("temperature")
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def threshold = device.currentValue("thermostatThreshold")
	def current = device.currentValue("thermostatOperatingState")
	def mode = device.currentValue("thermostatMode")
	def setPoint = device.currentValue("thermostatSetpoint")

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
	
	logger("debug", "now=${now}, lastUpdate=${lastUpdate}, maxInterval=$maxInterval minutes, heatingSetpoint=$heatingSetpoint, coolingSetpoint=$coolingSetpoint, temp=$temp")

	//convert maxUpdateInterval (in minutes) to milliseconds
	maxIntervalMili = maxInterval * 60000

	if (current == "idle" && now - lastUpdate >= maxIntervalMili) {
		logger("debug", "Temp sensor(s) maximum update time interval exceeded ($maxInterval minutes), check your sensor(s). Thermostat at idle, nothing to do, all is safe!")
		//return
	} else if (! ((mode in ["emergency stop", "off"]) || current == "idle") && now - lastUpdate >= maxIntervalMili) {
		logger("error", "Temp sensor(s) maximum update time interval exceeded ($maxInterval minutes). Setting EMERGENCY STOP mode until temp sensor(s) starts reporting again!")
		sendEvent(name: "preEmergencyMode", value: mode)
		sendEvent(name: "thermostatMode", value: "emergency stop")
		runIn(2, 'evaluateMode') // Re-evaluate to set the state to idle ASAP
		return
	} else if (mode == "emergency stop" && now - lastUpdate < maxIntervalMili && device.currentValue("preEmergencyMode")) {
		logger("warn", "Temp sensor started reporting again, Autorecovered from EMERGENCY STOP. Setting to previous mode.")
		sendEvent(name: "thermostatMode", value: device.currentValue("preEmergencyMode"))
		runIn(2, 'evaluateMode')
		return
	}

	// set the default mode to idle (safer to have idle than anything else if something goes wrong)
	def callFor = "idle"

	// Do we have a treshhold set, if not we do nothing 
	// ** This has been changed from original code so that if no treshold is set, the thermostat will stay in idle state
	if ( !threshold ) {
		logger("error", "evaluateMode() - Threshold was not set. Not doing anything...")
	} else {
		def units = getTemperatureScale()
		if (mode in ["heat","emergency heat"]) {
			// Mode is set to heat, let's see if we need to heat or not
			if (setPoint != heatingSetpoint) {
				sendEvent(name: "thermostatSetpoint", value: heatingSetpoint, unit: units)
			}
			if ( (heatingSetpoint - temp) >= threshold) callFor = "heating"
		} else if (mode == "cool") {
			// Mode is set to cool, let's see if we need to cool or not
			if (setPoint != coolingSetpoint) {
				sendEvent(name: "thermostatSetpoint", value: coolingSetpoint, unit: units)
			}
			if ( (temp - coolingSetpoint) >= threshold) callFor = "cooling"
		} else if (mode == "auto") {
			if (temp > coolingSetpoint) { 
				// Mode is set to auto, let's see if we need to cool
				if (setPoint != coolingSetpoint) {
					sendEvent(name: "thermostatSetpoint", value: coolingSetpoint, unit: units)
				}
				if ( (temp - coolingSetpoint) >= threshold) callFor = "cooling"
			} else { 
				// Mode is set to auto, let's see if we need to heat
				if (setPoint != heatingSetpoint) {
					sendEvent(name: "thermostatSetpoint", value: heatingSetpoint, unit: units)
				}
				if ( (heatingSetpoint - temp) >= threshold) callFor = "heating"
			}
		}
	}

	// Send Event if needed only on what we are doing, idle, cooling, heating
	logger("debug", "evaluateMode() - threshold=$threshold, actingMode=$mode, origState=$current, newState = $callFor")
	if (mode != callFor) {
		sendEvent(name: "thermostatOperatingState", value: callFor)
	}
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
	logger("trace", "setHeatingSetpoint($value)")

	//** Round to resolution of thermostat unit
	def newHeatingSetpoint = roundDegrees(value)

	//** check if we are actually changing something before going thru all the loops
	if (newHeatingSetpoint == device.currentValue("heatingSetpoint")) {
		logger("warn", "Heating setpoint same as already set, nothing to do")
	} else {
		
		// Set distance in degrees between heating and cooling setpoint based on unit
		if (getTemperatureScale() == "C") { 
			setpointDistance = 3
		} else {
			setpointDistance = 5
		}

		coolmin = device.currentValue("minCoolTemp")
		coolmax = device.currentValue("maxCoolTemp")
		heatmin = device.currentValue("minHeatTemp")
		heatmax = device.currentValue("maxHeatTemp")
		coolingSetpoint = device.currentValue("coolingSetpoint")
	
		//** check if cooling setpoint is at least x degrees apart from new heating setpoint
		if (newHeatingSetpoint > (coolingSetpoint - setpointDistance)) {
			// To close, let's adjust accordingly
			logger("info", "Heating setpoint to close to cooling setpoint, adjusting cooling accordingly")
			newCoolingSetpoint = newHeatingSetpoint + setpointDistance
		}
		
		
		if (newHeatingSetpoint > heatmax || newHeatingSetpoint < heatmin) {
			// Out of range
			logger("warn", "setHeatingSetpoint() is ignoring out of range heating setpoint ($newHeatingSetpoint).")
		} else if (newCoolingSetpoint && (newCoolingSetpoint > coolmax || newCoolingSetpoint < coolmin)) {
			// Out of range because of cooling
			logger("warn", "setHeatingSetpoint() is ignoring out of range cooling setpoint ($newCoolingSetpoint).")
		} else {
			// All checks have passed, let's do this
			def units = getTemperatureScale()
			def displayUnits = getDisplayUnits()
			logger("trace", "setHeatingSetpoint() setting heating setpoint to $newHeatingSetpoint $displayUnits")
			sendEvent(name: "heatingSetpoint", value: newHeatingSetpoint, unit: units)
			if (newCoolingSetpoint) {
				logger("trace", "setHeatingSetpoint() setting cooling setpoint to $newCoolingSetpoint $displayUnits")
				sendEvent(name: "coolingSetpoint", value: newCoolingSetpoint, unit: units)
			}
			// Setpoints have changed, let's evaluate thermostat mode
			runIn(2,'evaluateMode')
		}
	}
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
	logger("trace", "setCoolingSetpoint($value)")

	//** Round to resolution of thermostat unit
	newCoolingSetpoint = roundDegrees(value)
	logger("trace", "setCoolingSetpoint() - newCoolingSetpoint rounded: $newCoolingSetpoint")
	
	//** check if we are actually changing something before going thru all the loops
	if (newCoolingSetpoint == device.currentValue("coolingSetpoint")) {
		logger("warn", "Cooling setpoint same as already set, nothing to do")
	} else {
		
		// Set distance in degrees between heating and cooling setpoint based on unit
		if (getTemperatureScale() == "C") { 
			setpointDistance = 3
		} else {
			setpointDistance = 5
		}

		coolmin = device.currentValue("minCoolTemp")
		coolmax = device.currentValue("maxCoolTemp")
		heatmin = device.currentValue("minHeatTemp")
		heatmax = device.currentValue("maxHeatTemp")
		heatingSetpoint = device.currentValue("heatingSetpoint")
	
		//** check if heating setpoint is at least x degrees apart from new cooling setpoint
		logger("trace","setCoolingSetpoint() - $newCoolingSetpoint, $setpointDistance, $heatingSetpoint")
		if ((newCoolingSetpoint - setpointDistance) < heatingSetpoint) {
			// To close, let's adjust accordingly
			logger("info", "Cooling setpoint to close to heating setpoint, adjusting heating accordingly")
			newHeatingSetpoint = newCoolingSetpoint - setpointDistance
		}
		
		
		if (newCoolingSetpoint > coolmax || newCoolingSetpoint < coolmin) {
			// Out of range
			logger("warn", "setCoolingSetpoint() is ignoring out of range cooling setpoint ($newCoolingSetpoint).")
		} else if (newHeatingSetpoint && (newHeatingSetpoint > heatmax || newHeatingSetpoint < heatmin)) {
			// Out of range because of heating
			logger("warn", "setCoolingSetpoint() is ignoring out of range heating setpoint ($newHeatingSetpoint).")
		} else {
			// All checks have passed, let's do this
			def units = getTemperatureScale()
			def displayUnits = getDisplayUnits()
			logger("trace", "setCoolingSetpoint() setting cooling setpoint to $newCoolingSetpoint $displayUnits")
			sendEvent(name: "coolingSetpoint", value: newCoolingSetpoint, unit: units)
			if (newHeatingSetpoint) {
				logger("trace", "setCoolingSetpoint() setting heating setpoint to $newHeatingSetpoint $displayUnits")
				sendEvent(name: "heatingSetpoint", value: newHeatingSetpoint, unit: units)
			}
			// Setpoints have changed, let's evaluate thermostat mode
			runIn(2,'evaluateMode')
		}
	}
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
	if (value != device.currentValue("thermostatThreshold")) {
		def units = getTemperatureScale()
		logger("trace", "setThermostatThreshold($value) - sendEvent")
		sendEvent(name: "thermostatThreshold", value: value, unit: units)
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "setThermostatThreshold($value) - already set")
	}
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
	if (value != device.currentValue("maxUpdateInterval")) {
		logger("trace", "setMaxUpdateInterval($minutes) - sendEvent")
		sendEvent(name: "maxUpdateInterval", value: minutes)
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "setMaxUpdateInterval($minutes) - already set")
	}
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
	if (value != device.currentValue("thermostatMode")) {
		logger("trace", "setThermostatMode($value) - sendEvent")
		sendEvent(name: "thermostatMode", value: value)
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "setThermostatMode($value) - already set")
	}
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
	if (device.currentValue("thermostatMode") != "off") {
		logger("trace", "off() - sendEvent")
		sendEvent(name: "thermostatMode", value: "off")
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "off() - already set")
	}
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
	if (device.currentValue("thermostatMode") != "emergency stop") {
		logger("trace", "emergencyStop() - sendEvent")
		sendEvent(name: "thermostatMode", value: "emergency stop")
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "emergencyStop() - already set")
	}
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
	if (device.currentValue("thermostatMode") != "heat") {
		logger("trace", "heat() - sendEvent")
		sendEvent(name: "thermostatMode", value: "heat")
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "heat() - already set")
	}
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
	if (device.currentValue("thermostatMode") != "auto") {
		logger("trace", "auto() - sendEvent")
		sendEvent(name: "thermostatMode", value: "auto")
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "auto() - already set")
	}
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
	if (device.currentValue("thermostatMode") != "emergency heat") {
		logger("trace", "emergencyHeat() - sendEvent")
		sendEvent(name: "thermostatMode", value: "emergency heat")
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "emergencyHeat() - already set")
	}
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
	if (device.currentValue("thermostatMode") != "cool") {
		logger("trace", "cool() - sendEvent")
		sendEvent(name: "thermostatMode", value: "cool")
		runIn(2,'evaluateMode')
	} else {
		logger("trace", "cool() - already set")
	}
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
	logger("trace", "poll() - Nothing to do")
	null
}


//************************************************************
// setTemperature
//     Set temperature directly
//     Called from the manager app
// Signature(s)
//     setTemperature(value)
// Parameters
//     value : 
// Returns
//     None
//************************************************************
def setTemperature(value) {
	def units = getTemperatureScale()
	logger("trace", "setTemperature($value) - sendEvent")
	sendEvent(name:"temperature", value: value, unit: units)
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
	logger("trace", "heatUp()")
	def ts = device.currentValue("heatingSetpoint")
	logger("trace", "heatUp() - current value: $ts")
	def thermostatResolution = getThermostatResolution()
	setHeatingSetpoint( ts + thermostatResolution )
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
	logger("trace", "heatDown()")
	def ts = device.currentValue("heatingSetpoint")
	logger("trace", "heatDown() - current value: $ts")
	def thermostatResolution = getThermostatResolution()
	setHeatingSetpoint( ts - thermostatResolution )
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
	logger("trace", "coolUp()")
	def ts = device.currentValue("coolingSetpoint")
	logger("trace", "coolUp() - current value: $ts")
	def thermostatResolution = getThermostatResolution()
	setCoolingSetpoint( ts + thermostatResolution )
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
	logger("trace", "coolDown()")
	def ts = device.currentValue("coolingSetpoint")
	logger("trace", "coolDown() - current value: $ts")
	def thermostatResolution = getThermostatResolution()
	setCoolingSetpoint( ts - thermostatResolution )
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
	def units = getTemperatureScale()
	logger("trace", "setMinCoolTemp($value) - sendEvent")
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "minCoolTemp", value: value, unit: units)
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
	def units = getTemperatureScale()
	logger("trace", "setMaxCoolTemp($value) - sendEvent")
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "maxCoolTemp", value: value, unit: units)
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
	def units = getTemperatureScale()
	logger("trace", "setMinHeatTemp($value)")
	def t = device.currentValue("heatingSetpoint - sendEvent")
	sendEvent(name: "minHeatTemp", value: value, unit: units)
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
	def units = getTemperatureScale()
	logger("trace", "setMaxHeatTemp($value)")
	def t = device.currentValue("heatingSetpoint - sendEvent")
	sendEvent(name: "maxHeatTemp", value: value, unit: units)
	if (t > value) {
		setHeatingSetpoint(value)
	}
}


//************************************************************
// fanAuto
//     Set fan in auto mode
// Signature(s)
//     fanAuto()
// Parameters
//     None
// Returns
//     None
//************************************************************
def fanAuto() {
	logger("trace", "fanAuto() - Nothing to do")
	// Nothing to do for now!!!	
}


//************************************************************
// fanCirculate
//     Set fan in circulate mode
// Signature(s)
//     fanCirculate()
// Parameters
//     None
// Returns
//     None
//************************************************************
def fanCirculate() {
	logger("trace", "fanCirculate() - Nothing to do")
	// Nothing to do for now!!!
}


//************************************************************
// fanOn
//     Set fan on mode
// Signature(s)
//     fanOn()
// Parameters
//     None
// Returns
//     None
//************************************************************
def fanOn() {
	logger("trace", "fanOn() - Nothing to do")
	// Nothing to do for now!!!
}


//************************************************************
// setSchedule
//     Set Schedule
// Signature(s)
//     setSchedule()
// Parameters
//     None
// Returns
//     None
//************************************************************
def setSchedule() {
	logger("trace", "setSchedule() - Nothing to do")
	// Nothing to do for now!!!
}


//************************************************************
// setThermostatFanMode
//     Set fan mode
// Signature(s)
//     setThermostatFanMode()
// Parameters
//     None
// Returns
//     None
//************************************************************
def setThermostatFanMode(String value) {
	logger("trace", "setThermostatFanMode() - Nothing to do")
	// Nothing to do for now!!!
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
	//return "F" //Temporary until we have all parts of it working in F
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
// getThermostatResolution
//     Get the resolution of the thermostat based on units
// Signature(s)
//     getThermostatResolution()
// Parameters
//     None
// Returns
//     Resolution
//************************************************************
def getThermostatResolution() {
	if (getTemperatureScale() == "C") {
		return 0.5
	} else {
		return 1.0
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
		return roundDegrees((value - 32) * 5 / 9)
		
	} else {
		return roundDegrees((value * 9 / 5) + 32)
	}
}


//************************************************************
// roundDegrees
//     Convert to hubs temperature scale
// Signature(s)
//     roundDegrees(Double value)
// Parameters
//     value : 
// Returns
//     Rounded value
//************************************************************
def roundDegrees(Double value) {
	
	if (getTemperatureScale() == "C") { 
		value = value * 2
		value = Math.round(value) / 2
	} else {
		value = Math.round(value)	
	}
	
	return value	
}

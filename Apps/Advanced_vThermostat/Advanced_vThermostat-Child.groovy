/*
 *  Advance vThermostat Child App
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
	name: "Advanced vThermostat Child",
	namespace: "nclark",
	author: "Nelson Clark",
	description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat/Advanced_vThermostat-logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat/Advanced_vThermostat-logo.png",
	importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat/Advanced_vThermostat-Child.groovy",
	parent: "nclark:Advanced vThermostat Manager"
)


preferences {
	page(name: "pageConfig") // Doing it this way elimiates the default app name/mode options.
}


def pageConfig() {
	// Let's just set a few things before starting
	def displayUnits = getDisplayUnits()
	def hubScale = getTemperatureScale()
	installed = false

	if (!state.deviceID) {
		installed = true
	}

	if (hubScale == "C") {
		setpointDistance = 3.0
		heatingSetPoint = 21.0
		coolingSetPoint = 24.5
		thermostatThreshold = 0.5
	} else {
		setpointDistance = 5.0
		heatingSetPoint = 70.0
		coolingSetPoint = 76.0
		thermostatThreshold = 1.0
	}

        // Display all options for a new instance of the Advanced vThermostat
	dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {
		section() {
			label title: "Name of new Advanced vThermostat app/device:", required: true
		}
		
		section("Select temperature sensor(s)... (Average value will be used if you select multiple sensors)"){
			input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true, required: true
		}

		section("Select outlet(s) to use for heating... "){
			input "heatOutlets", "capability.switch", title: "Outlets", multiple: true
		}

		section("Select outlet(s) to use for cooling... "){
			input "coolOutlets", "capability.switch", title: "Outlets", multiple: true
		}

		// If this is the first time we install this driver, show initial settings
		if (!state.deviceID) {
			section("Initial Thermostat Settings... (invalid values will be set to the closest valid value)"){
				input "heatingSetPoint", "decimal", title: "Heating Setpoint in $displayUnits, this should be at least $setpointDistance $displayUnits lower than cooling", required: true, defaultValue: heatingSetPoint
				input "coolingSetPoint", "decimal", title: "Cooling Setpoint in $displayUnits, this should be at least $setpointDistance $displayUnits higher than heating", required: true, defaultValue: coolingSetPoint
				//** Removed because we will take control of this decision depending on the outlets selected for heating and/or cooling
				//input (name:"thermostatMode", type:"enum", title:"Thermostat Mode", options: ["auto","heat","cool","off"], defaultValue:"auto", required: true)
				input "thermostatThreshold", "decimal", "title": "Temperature Threshold in $displayUnits", required: true, defaultValue: thermostatThreshold
			}
		}
	
		section("Log Settings...") {
			input (name: "logLevel", type: "enum", title: "Live Logging Level: Messages with this level and higher will be logged", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
			input "logDropLevelTime", "decimal", title: "Drop down to Info Level Minutes", required: true, defaultValue: 5
		}

	}
}


def installed() {
    
	// Set log level as soon as it's installed to start logging what we do ASAP
	int loggingLevel
	if (settings.logLevel) {
		loggingLevel = settings.logLevel.toInteger()
	} else {
		loggingLevel = 3
	}
	
	logger("trace", "Installed Running vThermostat: $app.label")
	
	// Generate a random DeviceID
	state.deviceID = "avt" + Math.abs(new Random().nextInt() % 9999) + 1

	//Create vThermostat device
	def thermostat
	def label = app.getLabel()
	logger("info", "Creating vThermostat : ${label} with device id: ${state.deviceID}")
	try {
		//** Should we add isComponent in the properties of the child device to make sure we can't remove the Device, will this make it that we can't change settings in it? 
		thermostat = addChildDevice("nclark", "Advanced vThermostat Device", state.deviceID, null, [label: label, name: label, completedSetup: true]) //** Deprecated hubIDl no longer passed since 2.1.9
		//thermostat = addChildDevice("nclark", "Advanced vThermostat Device", state.deviceID, [label: label, name: label, completedSetup: true]) //** This will only work with ver 2.1.9 and up, let's wait a bit
	} catch(e) {
		logger("error", "Error adding vThermostat child ${label}: ${e}") //*** Not 100% sure about this one, test message outside loop to be sure ***
		//*** Original code: log.error("Could not create vThermostat; caught exception", e)
	}
	initialize(thermostat)
}


def updated() {
	// Set log level to new value
	int loggingLevel
	if (settings.logLevel) {
		loggingLevel = settings.logLevel.toInteger()
	} else {
		loggingLevel = 3
	}
	
	logger("trace", "Updated Running vThermostat: $app.label")

	initialize(getThermostat())
}


def uninstalled() {
	logger("info", "Child Device " + state.deviceID + " removed") // This never shows in the logs, is it because of the way HE deals with the uninstalled method?
	deleteChildDevice(state.deviceID)
}


//************************************************************
// initialize
//     Set preferences in the associated device and subscribe to the selected sensors and thermostat device
//     Also set logging preferences
//
// Signature(s)
//     initialize(thermostatInstance)
//
// Parameters
//     thermostatInstance : deviceWrapper
//
// Returns
//     None
//
//************************************************************
def initialize(thermostatInstance) {
	logger("trace", "Initialize Running vThermostat: $app.label")

	// First we need tu unsubscribe and unschedule any previous settings we had
	unsubscribe()
	unschedule()

	// Recheck Log level in case it was changed in the child app
	if (settings.logLevel) {
		loggingLevel = settings.logLevel.toInteger()
	} else {
		loggingLevel = 3
	}
	
	// Log level was set to a higher level than 3, drop level to 3 in x number of minutes
	if (loggingLevel > 3) {
		logger("trace", "Initialize runIn $settings.logDropLevelTime")
		runIn(settings.logDropLevelTime.toInteger() * 60, logsDropLevel)
	}

	logger("warn", "App logging level set to $loggingLevel")
	logger("trace", "Initialize LogDropLevelTime: $settings.logDropLevelTime")

	// Let's determine ThermostatMode depending on the choices made in the heating / cooling outlets choices
	if (heatOutlets && coolOutlets) {
		thermostatMode = "auto"
	} else if (heatOutlets) {
		thermostatMode = "heat"
	} else if (coolOutlets) {
		thermostatMode = "cool"
	} else {
		thermostatMode = "off"
	}
	
	// Set device settings if this is a new device
	if (!installed) { thermostatInstance.setHeatingSetpoint(heatingSetPoint) }
	if (!installed) { thermostatInstance.setCoolingSetpoint(coolingSetPoint) }
	if (!installed) { thermostatInstance.setThermostatThreshold(thermostatThreshold) }
	thermostatInstance.setLogLevel(loggingLevel)
	thermostatInstance.setThermostatMode(thermostatMode)

	// Subscribe to the new sensor(s) and device
	subscribe(sensors, "temperature", temperatureHandler)
	subscribe(thermostat, "thermostatOperatingState", thermostatStateHandler)

	// Update the temperature with these new sensors
	updateTemperature()

	// Schedule every minute the state of the controlled outlets
	runEvery1Minute(setOutletsState)
}


//************************************************************
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
//************************************************************
def getThermostat() {
	
	// Does this instance have a DeviceID
	if (!state.deviceID){
		
		//No DeviceID available what is going on, has the device been removed?
		logger("error", "getThermostat cannot access deviceID!")
	} else {
		
		//We have a deviceID, continue and return ChildDeviceWrapper
		logger("trace", "getThermostat for device " + state.deviceID)
		def child = getChildDevices().find {
			d -> d.deviceNetworkId.startsWith(state.deviceID)
		}
		logger("trace","getThermostat child is ${child}")
		return child
	}
}


//************************************************************
// temperatureHandler
//     Handles a sensor temperature change event
//     Do not call this directly, only used to handle events
//
// Signature(s)
//     temperatureHandler(evt)
//
// Parameters
//     evt : passed by the event subsciption
//
// Returns
//     None
//
//************************************************************
def temperatureHandler(evt)
{
	logger("debug", "Temperature changed to" + evt.doubleValue)
	updateTemperature()
}


//************************************************************
// updateTemperature
//     Update device current temperature based on selected sensors
//
// Signature(s)
//     updateTemperature()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def updateTemperature() {
	def total = 0;
	def count = 0;
	def thermostat=getThermostat()
	
	// Total all sensor used
	for(sensor in sensors) {
		total += sensor.currentValue("temperature")
		logger("debug", "Sensor $sensor.label reported " + sensor.currentValue("temperature"))
		count++
	}
	
	// Average the total divided by number of sensors
	def avgTemp = total / count
	thermostat.setTemperature(avgTemp)
	return avgTemp
}


//************************************************************
// thermostatStateHandler
//     Handles a thermostat state change event
//     Do not call this directly, only used to handle events
//
// Signature(s)
//     thermostatStateHandler(evt)
//
// Parameters
//     evt : Passed by the event subscription
//
// Returns
//     None
//
//************************************************************
def thermostatStateHandler(evt) {
	
	// Thermostat device changed the state (heating / cooling or other), go change state of outlets accordingly
	// *******************
	// THIS DOES NOT SEEM TO WORK, SOMETHING iS MISSING HERE, WHERE DOES opState GET DEFINED
	// OR JUST FETCH THE VALUE DIRECTLY FROM DEVICE, WE NEED TO TEST THIS A BIT
	// *******************
	// def opState = evt.value
	if (evt.value) {
		logger("info", "Thermostat state changed to ${opState}")
		setOutletsState(opState)
	} else {
		logger("warn", "thermostatStateHandler got an empty event")
	}
}


//************************************************************
// setOutletsState
//     Set the different outlets used for heating or cooling
//
// Signature(s)
//     setOutletsState(string opState)
//
// Parameters
//     opState : heating / cooling (all other states will turn off all outlets
//
// Returns
//     None
//
//************************************************************
def setOutletsState(opState) {
	def thermostat = getThermostat()
	
	// Did we get a value when called, if not, let's go fetch it directly from the device 
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
			if (loggingLevel >= 1) log.error msg
			break

		case "warn":
			if (loggingLevel >= 2) log.warn msg
			break

		case "info":
			if (loggingLevel >= 3) log.info msg
			break

		case "debug":
			if (loggingLevel >= 4) log.debug msg
			break

		case "trace":
			if (loggingLevel >= 5) log.trace msg
			break

		default:
			log.debug msg
			break
	}
}


//************************************************************
// logsDropLevel
//     Turn down logLevel to 3 in this app/device and log the change
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
def logsDropLevel() {
	def thermostat=getThermostat()
	
	app.updateSetting("logLevel",[type:"enum", value:"3"])
	thermostat.setLogLevel(3)
	
	loggingLevel = app.getSetting('logLevel').toInteger()
	logger("warn","App logging level set to $loggingLevel")
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

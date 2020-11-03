/**
 *  Copyright 2015 SmartThings
 *  Copyright 2018-2020 Josh McAllister (josh208@gmail.com)
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
	definition (name: "vThermostat Device", namespace: "josh208", author: "Josh McAllister") {
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

def evaluateMode() {
    runIn(60, 'evaluateMode')
    def temp = device.currentValue("temperature")
    def heatingSetpoint = device.currentValue("heatingSetpoint");
    def coolingSetpoint = device.currentValue("coolingSetpoint");
	def threshold = device.currentValue("thermostatThreshold")
	def current = device.currentValue("thermostatOperatingState")
	def mode = device.currentValue("thermostatMode")
 
    //Deadman safety. Make sure that we don't keep running if temp is not getting updated.
    def now = new Date().getTime()
    def lastUpdate = Date.parse("E MMM dd H:m:s z yyyy", device.currentValue("lastTempUpdate")).getTime()
    
    def maxInterval = device.currentValue("maxUpdateInterval") ?: 180 //set a somewhat sain limit of 3 hours
    if (maxInterval > 180) maxinterval = 180
    maxInterval = maxInterval * 1000 * 60 //convert maxUpdateInterval (in minutes) to milliseconds
    
    log.debug "now=$now, lastUpdate=$lastUpdate, maxInterval=$maxInterval, heatingSetpoint=$heatingSetpoint, coolingSetpoint=$coolingSetpoint, temp=$temp"
    
    if (! (mode in ["emergency stop", "off"]) && now - lastUpdate >= maxInterval ) {
        log.info("maxUpdateInterval exceeded. Setting emergencyStop mode")
        sendEvent(name: "preEmergencyMode", value: mode)
        sendEvent(name: "thermostatMode", value: "emergency stop")
        runIn(2, 'evaluateMode')
        return
    } else if (mode == "emergency stop" && now - lastUpdate < maxInterval && device.currentValue("preEmergencyMode")) {
        log.info("Autorecovered from emergencyStop. Resetting to previous mode.")
        sendEvent(name: "thermostatMode", value: device.currentValue("preEmergencyMode"))
        sendEvent(name: "preEmergencyMode", value: "")
        runIn(2, 'evaluateMode')
        return
    }
    
	if ( !threshold ) {
		log.debug "Threshold was not set. Not doing anything..."
		return
	}
	   
    def callFor = "idle"
    
    if (mode in ["heat","emergency heat"]) {
        sendEvent(name: "thermostatSetpoint", value: heatingSetpoint)
        if ( (heatingSetpoint - temp) >= threshold) callFor = "heating"
    } else if (mode == "cool") {
        sendEvent(name: "thermostatSetpoint", value: coolingSetpoint)
        if ( (temp - coolingSetpoint) >= threshold) callFor = "cooling"

    } else if (mode == "auto") {
        if (temp > coolingSetpoint) { //time to cool
            sendEvent(name: "thermostatSetpoint", value: coolingSetpoint)
            if ( (temp - coolingSetpoint) >= threshold) callFor = "cooling"

        } else { //time to heat
            sendEvent(name: "thermostatSetpoint", value: heatingSetpoint)
            if ( (heatingSetpoint - temp) >= threshold) callFor = "heating"

        }
    }
	log.debug "evaluateMode() : threshold=$threshold, actingMode=$mode, origState=$current, newState = $callFor"
    sendEvent(name: "thermostatOperatingState", value: callFor)
}

def setHeatingSetpoint(degreesF){
	setHeatingSetpoint(degreesF.toDouble())
}

def setHeatingSetpoint(Double degreesF) {
	def min = device.currentValue("minHeatTemp")
	def max = device.currentValue("maxHeatTemp")
	if (degreesF > max || degreesF < min) {
		log.debug "setHeatingSetpoint is ignoring out of range request ($degreesF)."
		return
	}
	log.debug "setHeatingSetpoint($degreesF)"
	sendEvent(name: "heatingSetpoint", value: degreesF)
	runIn(2,'evaluateMode')
}

def setCoolingSetpoint(degreesF){
	setCoolingSetpoint(defreesF.toDouble())
}

def setCoolingSetpoint(Double degreesF) {
	def min = device.currentValue("minCoolTemp")
	def max = device.currentValue("maxCoolTemp")
	if (degreesF > max || degreesF < min) {
		log.debug "setCoolingSetpoint is ignoring out of range request ($degreesF)."
		return
	}
	log.debug "setCoolingSetpoint($degreesF)"
	sendEvent(name: "coolingSetpoint", value: degreesF)
	runIn(2,'evaluateMode')
}

def setThermostatThreshold(Double degreesF) {
	log.debug "setThermostatThreshold($degreesF)"
	sendEvent(name: "thermostatThreshold", value: degreesF)
	runIn(2,'evaluateMode')
}

def setMaxUpdateInterval(BigDecimal minutes) {
    sendEvent(name: "maxUpdateInterval", value: minutes)
    runIn(2,'evaluateMode')
}

def setThermostatMode(String value) {
	sendEvent(name: "thermostatMode", value: value)
	runIn(2,'evaluateMode')
}

def off() {
	sendEvent(name: "thermostatMode", value: "off")
	runIn(2,'evaluateMode')
}

def emergencyStop() {
    sendEvent(name: "thermostatMode", value: "emergency stop")
    runIn(2,'evaluateMode')
}

def heat() {
	sendEvent(name: "thermostatMode", value: "heat")
	runIn(2,'evaluateMode')
}

def auto() {
	sendEvent(name: "thermostatMode", value: "auto")
	runIn(2,'evaluateMode')
}

def emergencyHeat() {
	sendEvent(name: "thermostatMode", value: "emergency heat")
	runIn(2,'evaluateMode')
}

def cool() {
	sendEvent(name: "thermostatMode", value: "cool")
	runIn(2,'evaluateMode')
}

def poll() {
	null
}


def setTemperature(value) {
	sendEvent(name:"temperature", value: value)
    sendEvent(name: "lastTempUpdate", value: new Date() )
	runIn(2,'evaluateMode')
}

def heatUp() {
	def ts = device.currentValue("heatingSetpoint")
	setHeatingSetpoint( ts + 1 )
}

def heatDown() {
	def ts = device.currentValue("heatingSetpoint")
	setHeatingSetpoint( ts - 1 )
}


def coolUp() {
	def ts = device.currentValue("heatingSetpoint")
	setCoolingSetpoint( ts + 1 )
}

def coolDown() {
	def ts = device.currentValue("heatingSetpoint")
	setCoolingSetpoint( ts - 1 )
}

def setMinCoolTemp(Double degreesF) {
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "minCoolTemp", value: degreesF)
	if (t < degreesF) {
		setCoolingSetpoint(degreesF)
	}
}

def setMaxCoolTemp(Double degreesF) {
	def t = device.currentValue("coolingSetpoint")
	sendEvent(name: "maxCoolTemp", value: degreesF)
	if (t > degreesF) {
		setCoolingSetpoint(degreesF)
	}
}

def setMinHeatTemp(Double degreesF) {
	def t = device.currentValue("heatingSetpoint")
	sendEvent(name: "minHeatTemp", value: degreesF)
	if (t < degreesF) {
		setHeatingSetpoint(degreesF)
	}
}

def setMaxHeatTemp(Double degreesF) {
	def t = device.currentValue("heatingSetpoint")
	sendEvent(name: "maxHeatTemp", value: degreesF)
	if (t > degreesF) {
		setHeatingSetpoint(degreesF)
	}
}

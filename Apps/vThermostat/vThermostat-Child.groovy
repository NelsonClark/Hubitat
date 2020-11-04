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
		input(name:"thermostatMode", type:"enum", title:"Thermostat Mode", options: ["auto","heat","cool","off"], defaultValue:"auto", required: true)
		input "thermostatThreshold", "decimal", "title": "Temperature Threshold", required: true, defaultValue: 1.0
	}
}

def installed() {
	log.debug "Running installed: $app.label"
	state.deviceID = "jmvt" + Math.abs(new Random().nextInt() % 9999) + 1

	//create the child device
	def thermostat
	def label = app.getLabel()
	log.debug "create device with id: jmvt$state.deviceID, named: $label"
	try {
		thermostat = addChildDevice("nclark", "vThermostat Device", state.deviceID, null, [label: label, name: label, completedSetup: true])
	} catch(e) {
		log.error("caught exception", e)
	}
	initialize(thermostat)
}

def updated() {
	log.debug "Running updated: $app.label"
	initialize(getThermostat())
}

def initialize(thermostatInstance) {
	log.debug "Running initialize: $app.label"

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

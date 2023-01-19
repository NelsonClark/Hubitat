/*
 *  Advanced vThermostat V2 Child App
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/Advanced_vThermostat_V2
 *  Copyright 2023 Nelson Clark
 *
 *  This app requires it's parent app and uses the built in virtual thermostat device driver to function, please go to the project page for more information.
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
 *
 *
 *
 */

definition(
    name: "Advanced vThermostat V2 Child",
    namespace: "nclark",
    author: "Nelson Clark",
    description: "Use any temperature sensor(s) with any outlet(s)/plug(s) and make them be controlled by a virtual thermostat device.",
    category: "Green Living",
    iconUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat_V2/Advanced_vThermostat_V2-logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat_V2/Advanced_vThermostat_V2-logo.png",
    importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/Advanced_vThermostat_V2/Advanced_vThermostat_V2-Child.groovy",
    parent: "nclark:Advanced vThermostat V2"
)

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    // Let's just set a few things before starting
    displayUnits = getDisplayUnits()
    hubScale = getTemperatureScale()
    //installed = false
    logger("debug", "Units: ${displayUnits}, Scale: ${hubScale}")

    if (app.getInstallationState() == 'INCOMPLETE') {
        logger("debug", "Not installed")
        if (hubScale == "C") {
            state.currentScale = "C"
            setpointDistance = 2.0
            heatingSetpoint = 21.0
            coolingSetpoint = 24.0
            heatingHysteresis = 0.5
            coolingHysteresis = 1.0
        } else {
            state.currentUnit = "F"
            setpointDistance = 2.0
            heatingSetpoint = 70.0
            coolingSetpoint = 76.0
            heatingHysteresis = 1.0
            coolingHysteresis = 2.0
        }
        state.lastThermostatMode = "off"
        state.lastThermostatFanMode = "off"
        state.emergencyStop = false
        app.updateSetting("emergencyStopMinutes", 120)
        app.updateSetting("setpointDistance", setpointDistance)
        app.updateSetting("hubScale", hubScale)
    }

    if (hubScale == "C") {
        heatingHysteresisLow = 0.25
        heatingHysteresisHigh = 1.0
        coolingHysteresisLow = 0.75
        coolingHysteresisHigh = 1.5
    } else {
        heatingHysteresisLow = 0.5
        heatingHysteresisHigh = 2.0
        coolingHysteresisLow = 1.5
        coolingHysteresisHigh = 3.0
    }



    appLabel = app.getLabel()

    dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {

        section() {
            if (app.getInstallationState() == 'INCOMPLETE') { label title: "<b>Name of new Advanced vThermostat app/device:</b>", required: true } else { paragraph "<b>${app.label}</b>" }
        }

        section("Temperature sensor(s)... (Average value of all sensors will be used if you select multiple sensors)", hideable: true, hidden : false){
            paragraph "<b>Select sensor(s) to use for this vThermostat.</b>"
            input "tempSensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true, required: true

            input "emergencyStopMinutes", "number", title: "<b>Number of minutes before calling an Emergency Stop if all sensors are not reporting during heating or cooling.</b><br>This has no effect if thermostat is idle or off, only when heating or cooling outlets are turned on.<br>Normal settings would be between 60 to 180 minutes depending on how temperature sensors report.<br>Setting this to 0 will disable safety testing and should only be done when using Hydroponique systems where we have very slow rise of temperatures and fire risk is minimal to non existant.", required: true, defaultValue: emergencyStopMinutes
        }

		section() {
			paragraph ""	
		}

        section("Heating configurations... ", hideable: true, hidden : hideHeatingSection()){
            paragraph "<b>Select outlet(s)/switch(es) to use for heating.</b>"
            input "heatingOutlets", "capability.switch", title: "Outlet(s) / switch(es)", multiple: true

            paragraph "<b>Select outlet(s)/switch(es) to use for fan mode when heating.</b>"
            input "heatingFanOutlets", "capability.switch", title: "Outlet(s) / switch(es)", multiple: true

            if (app.getInstallationState() == 'INCOMPLETE') { input "heatingSetpoint", "decimal", title: "<b>Initial Heating Setpoint in ${displayUnits}</b><br>All further changes to this value should be done at the device and/or dashboard.", required: true, defaultValue: heatingSetpoint }

            input "heatingHysteresis", "decimal", title: "<b>Heating Hysteresis in ${displayUnits}</b> Setting this between ${heatingHysteresisLow} and ${heatingHysteresisHigh} will be good.<br> Higher numbers are less confortable because of high temp swing but lower numbers will cycle more often and wear relais faster", range: "0.25..5.0", defaultValue: heatingHysteresis
            //input "heatingHysteresis", "enum", title: "<b>Heating Hysteresis in ${displayUnits} (Higher numbers are less confortable because of high temp swing but lower numbers will cycle more often and wear relais faster)</b>", options:["0.1","0.25","0.5","1","2"], defaultValue: heatingHysteresis
        }

		section() {
			paragraph ""	
		}

        section("Cooling configurations... ", hideable: true, hidden : hideCoolingSection()){
            paragraph "<b>Select outlet(s)/switch(es) to use for cooling.</b>"
            input "coolingOutlets", "capability.switch", title: "Outlet(s) / switch(es)", multiple: true

            paragraph "<b>Select outlet(s)/switch(es) to use for fan mode when cooling.</b>"
            input "coolingFanOutlets", "capability.switch", title: "Outlet(s) / switch(es)", multiple: true

            if (app.getInstallationState() == 'INCOMPLETE') { input "coolingSetpoint", "decimal", title: "<b>Initial Cooling Setpoint in ${displayUnits}, this should be at least ${setpointDistance} ${displayUnits} lower than cooling.</b><br>All further changes to this value should be done at the device and/or dashboard.", required: true, defaultValue: coolingSetpoint }

            input "coolingHysteresis", "decimal", title: "<b>Cooling Hysteresis in ${displayUnits}</b> Setting this between ${coolingHysteresisLow} and ${coolingHysteresisHigh} will be good.<br> Higher numbers are less confortable because of high temp swing but lower numbers will cycle more often and wear relais faster", range: "0.25..5.0", defaultValue: coolingHysteresis
            //input "coolingHysteresis", "enum", title: "<b>Cooling Hysteresis in ${displayUnits} (Low numbers will cycle more often and wear relais faster)", options:["0.1","0.25","0.5","1","2"], defaultValue: coolingHysteresis
        }

		section() {
			paragraph ""	
		}

        section("App Logging Settings...", hideable: true, hidden : hideLogSection()) {
            input (name: "logLevel", type: "enum", title: "<b>Live Logging Level: Messages with this level and lower will be logged.</b>", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
            input "logDropLevelTime", "number", title: "<b>Drop down to Info Level in xx Minutes</b>", required: true, defaultValue: 15
        }

		section() {
			paragraph ""	
		}

    }
}


def installed() {

    // Set log level as soon as it's installed to start logging what we do ASAP
    int loggingLevel
    if (settings.logLevel) {
        loggingLevel = settings.logLevel.toInteger()
    }

    logger("trace", "< installed ---------------")
    logger("debug", "Installed Running vThermostat: ${app.label}")

    // Generate a random DeviceID
    state.deviceID = "avt" + Math.abs(new Random().nextInt() % 9999) + 1

    //Create Virtual Thermostat device (using built in virtual device for Homekit compatibility)
    thermostat
    label = app.getLabel()
    logger("info", "Creating vThermostat device: ${label} with id: ${state.deviceID}")
    try {
        thermostat = addChildDevice("hubitat", "Virtual Thermostat", state.deviceID, [label: label, name: label, isComponent: true]) //** This will only work with ver 2.1.9 and up
    } catch(e) {
        logger("error", "Error adding Virtual Thermostat child device ${label}: ${e}")
    }
    
    //Wait for virtual thermostat to install before continuing
    pauseExecution(500)

    // Set one time settings here
    thermostat.setHeatingSetpoint(heatingSetpoint)
    thermostat.setCoolingSetpoint(coolingSetpoint) 

    // Remove initial settings not used now that a child device exists
    app.removeSetting("heatingSetPoint")
    app.removeSetting("coolingSetPoint")

    initialize(thermostat)
    logger("trace", "--------------- installed >")
}


def updated() {
    // Set log level to new value
    int loggingLevel
    if (settings.logLevel) {
        loggingLevel = settings.logLevel.toInteger()
    } else {
        loggingLevel = 3
    }

    logger("trace", "< updated ---------------")

    initialize(getThermostatChildDevice())

    logger("trace", "--------------- updated >")
}


def uninstalled() {
    logger("trace", "< uninstalled ---------------")
    logger("info", "Child Device " + ${state.deviceID} + " removed")
    deleteChildDevice(state.deviceID)
    logger("trace", "--------------- uninstalled >")
}


//************************************************************
// initialize
//     Set preferences in the associated device and subscribe to the selected sensors and thermostat device
//     Also set logging preferences
// Signature(s)
//     initialize(thermostatInstance)
// Parameters
//     thermostatInstance : deviceWrapper
// Returns
//     None
//************************************************************
def initialize(thermostatInstance) {
    logger("trace", "< initialize(${thermostatInstance})---------------")

    // First we need tu unsubscribe and unschedule any previous settings we had
    unsubscribe()
    unschedule()

    thermostatInstance = thermostatInstance ?: getThermostatChildDevice()
    
    // Recheck Log level in case it was changed in the child app
    if (settings.logLevel) {
        loggingLevel = settings.logLevel.toInteger()
    } else {
        loggingLevel = 3
    }

    // Log level was set to a higher level than 3, drop level to 3 in x number of minutes
    if (loggingLevel > 3) {
        logger("trace", "Initialize Log Level Drop run in ${settings.logDropLevelTime}")
        runIn(settings.logDropLevelTime.toInteger() * 60, logsDropLevel)
    }

    logger("warn", "App logging level set to ${loggingLevel}, will revert back to 3 in ${settings.logDropLevelTime} minutes")

    // Let's determine ThermostatMode depending on the choices made in the heating / cooling outlets choices
    if (heatingOutlets && coolingOutlets) {
        thermostatMode = "auto"
    } else if (heatingOutlets) {
        thermostatMode = "heat"
    } else if (coolingOutlets) {
        thermostatMode = "cool"
    } else {
        thermostatMode = "off"
    }

    // Let's determine fanMode depending on the choices made in the heating / cooling outlets choices
    if (heatingFanOutlets || coolingFanOutlets) {
        thermostatFanMode = "auto"
    } else {
        thermostatFanMode = "off"
    }

    //On initialize, let's set Hysteresis to the heating mode hysteresis for now, we will change it later
    thermostatHysteresis = heatingHysteresis.toString()

    // Set device settings
    thermostatInstance.updateSetting("hysteresis", [value: thermostatHysteresis, type: "enum"])
    thermostatInstance.initialize()
    
    //Set logging level of device based on app level, 3 turn on info logging, less than 3 turn it off, 5 turn debug on less than 4 debug off 
    //thermostatInstance.setLogLevel(loggingLevel)

    thermostatInstance.setThermostatMode(thermostatMode)
    thermostatInstance.setThermostatFanMode(thermostatFanMode)

    //We need to set the virtual thermostat capabilities based on settings in child app
    thermostatModes = '['
    thermostatFanModes = '['

    if (heatingOutlets && coolingOutlets) { 
        thermostatModes = thermostatModes + '"auto",'
    }

    if (heatingOutlets) { 
        thermostatModes =  thermostatModes + '"heat",' 
    }

    if (coolingOutlets) { 
        thermostatModes =  thermostatModes + '"cool",' 
    }
    
    if (emergencyHeatOutlets) { 
        thermostatModes = thermostatModes + '"emergency heat",' 
    }

    if (heatingFanOutlets || coolingFanOutlets) {
        thermostatFanModes = thermostatFanModes + '"auto","circulate","on"'
    }

    thermostatModes = thermostatModes + '"off"]'
    thermostatFanModes = thermostatFanModes + ']'
    
    thermostatInstance.setSupportedThermostatModes(thermostatModes)
    thermostatInstance.setSupportedThermostatFanModes(thermostatFanModes)

    logger("debug", "Setting SupportedThermostatModes to : ${thermostatModes}")
    logger("debug", "Setting SupportedThermostatFanModes to : ${thermostatFanModes}")


    // Subscribe to the new sensor(s) and virtual thermostat device
    subscribe(tempSensors, "temperature", temperatureChangeHandler)
    subscribe(thermostatInstance, "thermostatOperatingState", thermostatStateHandler)
    subscribe(location, "temperatureScale", tempScaleChanged)

    // Update the temperature with these new sensors
    updateThermostatTemperature()

    //*****************************************************************************************************************************
    // WE NEED TO FIND A BETTER WAY TO DO THIS, DO WE REALLY NEED TO RUN EVERY MINUTE ????? NO IF THIS IS ONLY FOR EMERGENCY STOP
    //*****************************************************************************************************************************
    // Schedule every 15 minutes a safety test so that we don't go " Burning down the house, my house, it's of the ordinary--------------- " ok quit that already (good song)
     runEvery1Minute(safetyTest)

    logger("trace", "--------------- initialize >")
}


//************************************************************
// tempScaleChanged
//     When hub Temperature Scale changes, this will convert all temperatures
// Signature(s)
//     tempScaleChanged()
// Parameters
//     None
// Returns
//     None
//************************************************************
def tempScaleChanged() {
    logger("trace", "< tempScaleChanged ---------------")

    logger("warn", "Hub Temperature scale has changed, we need to convert to new scale!")
    
    // TO-DO
    logger("trace", "--------------- tempScaleChanged >")
} 


//************************************************************
// safetyTest
//     Test to make sure all temp sensors are reporting within specified time frame
//      If they are and thermostat is in heating or cooling, call Emregency Stop
// Signature(s)
//     safetyTest()
// Parameters
//     None
// Returns
//     None
//************************************************************
def safetyTest() {
    logger("trace", "< safetyTest ---------------")

    thermostat = thermostat ?: getThermostatChildDevice()
    lst = ["heating", "cooling"]

    if (lst.contains(thermostat.currentValue("thermostatOperatingState")) && emergencyStopMinutes > 0) {
        if (readSensorsAndAverage() == 0) {
            logger("error","All sensors have stopped reporting, going into Emergency STOP until next temp sensor update!")
            state.lastThermostatMode = thermostat.currentValue("thermostatMode")
            state.lastThermostatFanMode = thermostat.currentValue("thermostatFanMode")
            state.emergencyStop = true
            thermostat.setThermostatMode("off")
            thermostat.setThermostatFanMode("off")
        } else {
            logger("debug", "Safety test all ok, at least one sensor is reporting within time limits.")
        }
    } else {
        logger("debug", "Safety test not needed because we are within parameters.")
    }


    logger("trace", "--------------- safetyTest >")
}


//************************************************************
// getThermostatChildDevice
//     Gets current childDeviceWrapper from list of childs
// Signature(s)
//     getThermostatChildDevice()
// Parameters
//     None
// Returns
//     ChildDeviceWrapper
//************************************************************
def getThermostatChildDevice() {
    logger("trace", "< getThermostatChildDevice ---------------")

    // Does this instance have a DeviceID
    if (!state?.deviceID){

        //No DeviceID available what is going on, has the device been removed?
        logger("error", "getThermostatChildDevice cannot access deviceID, device might have been deleted!")
        child = null
    } else {

        //We have a deviceID, continue and return ChildDeviceWrapper
        logger("debug", "getThermostatChildDevice for device ${state.deviceID}")
        child = getChildDevices().find {
            d -> d.deviceNetworkId.startsWith(state.deviceID)
        }
        logger("debug","getThermostatChildDevice child is ${child}")
    }
    logger("trace", "--------------- getThermostatChildDevice: return ${child} >")
    return child
}


//************************************************************
// temperatureChangeHandler
//     Handles a sensor temperature change event
//     Do not call this directly, only used to handle events
// Signature(s)
//     temperatureChangeHandler(evt)
// Parameters
//     evt : passed by the event subsciption
// Returns
//     None
//************************************************************
def temperatureChangeHandler(evt) {
    logger("trace", "< temperatureChangeHandler ---------------")
    logger("debug", "Temperature sensor changed to " + evt.doubleValue)
    updateThermostatTemperature()
    logger("trace", "--------------- temperatureChangeHandler >")
}

//************************************************************
// readSensorsAndAverage
//     Update device current temperature based on selected sensors
// Signature(s)
//     readSensorsAndAverage()
// Parameters
//     None
// Returns
//     Average Temperature OR 0 if all sensors are over Max Time Limit
//************************************************************
def readSensorsAndAverage() {

    logger("trace", "< readSensorsAndAverage ---------------")
    total = 0;
    count = 0;
    avgTemp = 0;
    thermostat = thermostat ?: getThermostatChildDevice()

    now = new Date().getTime()
    logger("debug", "now=${now}")

    // Get maxInterval, if none set, set it to 180 minutes (3 hours)
    maxInterval = emergencyStopMinutes ?: 180

    for(sensor in tempSensors) {
        // Check getLastActivity() date for last update, if more than xx minutes do not use the value of this sensor and mention in logs as warning
        lastSensorUpdateFormated = sensor.getLastActivity() // getLastActivity returns formated greenwich time
        lastSensorUpdate = Date.parse("yyyy-MM-dd HH:mm:ssZ", "$lastSensorUpdateFormated").getTime() // last activity converted to epoch milis
        interval = (now.toDouble() - lastSensorUpdate.toDouble()) / 60000
        logger("debug", "Sensor ${sensor} last updated : ${lastSensorUpdateFormated}, interval : ${interval}")
        if (interval > maxInterval) {
            logger("warn","Sensor ${sensor} has not reported for ${interval} minutes, this sensor will not be used in average with multiple sensors.")
        } else {
            total += sensor.currentValue("temperature")
            count++
            logger("debug", "Sensor ${sensor} reported ${sensor.currentValue("temperature")}")
            logger("debug", "Acumulated total is now : ${total}, Count is now : ${count}")
        }
    }

    logger("debug", "Grand total for average : ${total}, Count is now : ${count}")
    // If count is 0, this means that all sensors have not updated for more than xx minutes, go into emergency stop
    if (count != 0) {
        // Average the total divided by number of sensors
        avgTemp = total / count
        logger("debug","Averaging ${count} sensor(s) to ${avgTemp}°")
    }

    if (count == 0) {
        logger("warn","All Temp Sensors have not reported for over ${maxInterval} minutes.")
    }

    logger("trace", "--------------- readSensorsAndAverage: return ${avgTemp} >")
    return avgTemp
}



//************************************************************
// updateThermostatTemperature
//     Update device current temperature based on selected sensors
// Signature(s)
//     updateThermostatTemperature()
// Parameters
//     None
// Returns
//     None
//************************************************************
def updateThermostatTemperature() {
    logger("trace", "< updateThermostatTemperature ---------------")

    thermostat = thermostat ?: getThermostatChildDevice()
    avgTemp = readSensorsAndAverage()

    logger("debug", "Average Temp: ${avgTemp}")

    // If avgTemp is 0, this means that all sensors have not updated for more than xx minutes, do not update temp and wait for safetyCheck to do it's thing
    if (avgTemp == 0) {
        logger("warn","Temp Sensor(s) not reported for over ${maxInterval} minutes!")
    } else {
        thermostat.setTemperature(avgTemp)  //.toDouble().round(1) //If we want to round this to a single digit
        logger("debug","Updated device temperature to ${avgTemp}°")
        // if we were in emergency stop, let's recover from it.
        if (state?.emergencyStop) {
            state.emergencyStop = false
            thermostat.setThermostatMode("${state.lastThermostatMode}")
            thermostat.setThermostatFanMode("${state.lastThermostatFanMode}")
        }
    }

    logger("trace", "--------------- updateThermostatTemperature >")
}


//************************************************************
// thermostatStateHandler
//     Handles a thermostat state change event
//     Do not call this directly, only used to handle events
// Signature(s)
//     thermostatStateHandler(evt)
// Parameters
//     evt : Passed by the event subscription
// Returns
//     None
//************************************************************
def thermostatStateHandler(evt) {
    logger("trace", "< thermostatStateHandler event: ${evt.value} ---------------")
    // Thermostat device changed the state (heating / cooling or other), go change state of outlets accordingly
    if (!state?.emergencyStop) {
        if (evt.value) {
            logger("info", "Thermostat state changed to ${evt.value}")
            setOutletsState(evt.value)
        } else {
            logger("warn", "thermostatStateHandler got an empty event")
        }
    }

    thermostat = thermostat ?: getThermostatChildDevice()

    if (state?.emergencyStop && !(evt.value == "off")) {
        //If we are here, state of the thermostat was changed manualy even if we are in emergency mode, change everything back to off and warn
        logger("error","Someone or something tried to turn thermostat back on, we are in Emergency Stop, turning thermostat back off!")
        thermostat.setThermostatMode("off")
        thermostat.setThermostatFanMode("off")
    }

c}


//************************************************************
// setOutletsState
//     Set the different outlets used for heating or cooling
// Signature(s)
//     setOutletsState(string thermOpState)
// Parameters
//     thermOpState : heating / cooling (all other states will turn off all outlets
// Returns
//     None
//************************************************************
def setOutletsState(thermOpState) {
    logger("trace", "< setOutletsState(${thermOpState}) ---------------")
    
    thermostat = thermostat ?: getThermostatChildDevice()

    // Did we get a value when called, if not, let's go fetch it directly from the device
    thermOpState = thermOpState ? thermOpState : thermostat.currentValue("thermostatOperatingState")
    fanOpState = thermostat.currentValue("thermostatFanMode")
    logger("debug","Thermostat OpState : ${thermOpState}, Fan OpState : ${fanOpState}")

    if (thermOpState == "heating") {
        //We need to add the Hysteresis setting here

        coolingOutlets ? coolingOutlets.off() : null
        pauseExecution(250)
        heatingOutlets ? heatingOutlets.on() : null

        logger("debug", "Turned on heating outlet(s).")
        if (fanOpState == "auto") {
            fanOutlets ? fanOutlets.on() : null
            logger("debug", "Turned on all fan outlet(s).")
        }
    } else if (thermOpState == "cooling") {
        //We need to add the Hysteresis setting here

        heatingOutlets ? heatingOutlets.off() : null
        pauseExecution(250)
        coolingOutlets ? coolingOutlets.on() : null

        logger("debug", "Turned on cooling outlet(s).")
        if (fanOpState == "auto") {
            fanOutlets ? fanOutlets.on() : null
            logger("debug", "Turned on all fan outlet(s).")
        }
    } else {
        heatingOutlets ? heatingOutlets.off() : null
        coolingOutlets ? coolingOutlets.off() : null
        logger("debug", "Turned off all heat/cool outlet(s).")
        if (fanOpState == "auto") {
            fanOutlets ? fanOutlets.off() : null
            logger("debug", "Turned off all fan outlet(s).")
        }
    }    
    
    logger("trace", "--------------- setOutletsState >")
    
}


//************************************************************
// thermostatFanStateHandler
//     Handles a thermostat fan state change event
//     Do not call this directly, only used to handle events
// Signature(s)
//     thermostatFanStateHandler(evt)
// Parameters
//     evt : Passed by the event subscription
// Returns
//     None
//************************************************************
def thermostatFanStateHandler(evt) {
    logger("trace", "< thermostatFanStateHandler event: ${evt.value} ---------------")

    logger("debug", "Fan Events are not yet implemented!")

//    thermostat = thermostat ?: getThermostatChildDevice()






    logger("trace", "< thermostatFanStateHandler event: ${evt.value} ---------------")
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
            if (logLevel >= 1) { log.error msg }
            break

        case "warn":
            if (logLevel >= 2) { log.warn msg }
            break

        case "info":
            if (logLevel >= 3) { log.info msg }
            break

        case "debug":
            if (logLevel >= 4) { log.debug msg }
            break

        case "trace":
            if (logLevel >= 5) { log.trace msg }
            break

        default:
            log.debug msg
            break
    }
}


//************************************************************
// logsDropLevel
//     Turn down logLevel to 3 in this app/device and log the change
// Signature(s)
//     logsDropLevel()
// Parameters
//     None
// Returns
//     None
//************************************************************
def logsDropLevel() {
    thermostat = thermostat ?: getThermostatChildDevice()

    app.updateSetting("logLevel",[type:"enum", value:"3"])
    thermostat.setLogLevel(3)

    loggingLevel = app.getSetting('logLevel').toInteger()
    logger("warn","App logging level set to ${loggingLevel}")
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
// appButtonHandler
//     Handle menu button events 
// Signature(s)
//     appButtonHandler(string buttonName)
// Parameters
//     None
// Returns
//     None
//************************************************************
void appButtonHandler(String btn) {
    switch (btn) {
        case 'btnStart':
            logger("debug", "Button 'Start App' has been pressed")
            startApp()
            break
        case 'btnPause':
            logger("debug", "Button 'Pause App' has been pressed")
            pauseApp()
            break
        default:
            logger("warn","Button not handled")
    }
}


//************************************************************
// *** Hiden menu handlers ***
// Parameters
//     None
// Returns
//     Boolean
//************************************************************
private hideHeatingSection() {(heatingOutlets || heatingFanOutlets) ? false : true}
private hideCoolingSection() {(coolingOutlets || coolingFanOutlets) ? false : true}
private hideLogSection() {($logLevel = 3) ? false : true}

import java.text.SimpleDateFormat
/*
 *  Hydro-Quebec Dynamic Pricing App
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/Hydro_Quebec_Dynamic_Pricing
 *  Copyright 2022 Nelson Clark
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
 *  Special thanks to Rachid Aberkan for making a useable API for HQ Events for the first versions of this app
 *
 *
 *
 *  Version history:
 *
 *  1.0.1 - 2025-01-30 - Added a permanent Poll button, this button will poll and re-schedule all upcomming events
 *                     - Changed time of first pole of the day to 13:13:13 because of a situation where HQ updated the events
 *  1.0.0 - 2025-01-29 - First public release
 *                     - Now uses the official event JSON from HQ
 *                     - Now supports all available events as of winter 2024-2025
 *                     - Added optional Pre-Event mode for people that want to heat up the house before an event
 *  0.3.0 - 2022/02/18 - Second Beta release
 *                     - Added more debug logging
 *                     - Added option to copy over morning to evening options
 *                     - Cleaned up the menus
 *  0.2.0 - 2022/02/06 - First public Beta release
 *  0.1.0 - 2021/12/20 - Initial Beta release private
 *
 */



def setConstants(){
	state.name = "Hydro-Quebec Dynamic Pricing"
	state.version = "1.0.1"
	state.HQEventURL = "https://donnees.solutions.hydroquebec.com/donnees-ouvertes/data/json/pointeshivernales.json"
	//This is for testing purposes, for normal operation must be set to false
	state.testMode = false
}


definition(
	name: "Hydro-Quebec Dynamic Pricing",
	namespace: "nclark",
	author: "Nelson Clark",
	description: "Help maximise your return when opted in Hydro-Quebec Winter Credit Options",
	category: "Green Living",
	iconUrl: "",
	iconX2Url: "",
)


preferences {
	page(name: "pageConfig")
}


def pageConfig() {
	// Let's just set a few things before starting
	setConstants()
	def displayUnits = getDisplayUnits()


	dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {

		// Event Options with French descriptions (For future version with language preferences)
		/*
		List<Map<String,String>> eventTypeOptions = [
		["CPC-D": "Crédit pointe critique pour le tarif D / Option de crédit hivernal pour la clientèle résidentielle et agricole"],
		["TPC-DPC": "Tarification pointe critique pour le tarif D / Tarif Flex D pour la clientèle résidentielle et agricole"],
		["GDP-Affaires": "Tarification pointe critique pour les tarifs DP, DM, G, G9, M, LG ou H / Option de gestion de la demande de puissance (GDP) pour la clientèle d’affaires"],
		["CPC-G": "Crédit pointe critique pour le tarif G / Option de crédit hivernal pour la clientèle d’affaires de petite puissance"],
		["TPC-GPC": "Tarification pointe critique pour le tarif G / Tarif Flex G pour la clientèle d’affaires"],
		["TPC-M": "Tarif pointe critique pour les tarifs M, LG et G9 / Option d’électricité interruptible pour la clientèle d’affaires de moyenne puissance"],
		["TPC-L-Centre-C2": "Tarif pointe critique pour le tarif L destiné aux secteurs Centre C2 / Option d’électricité interruptible pour la clientèle d’affaires de grande puissance"],
		["TPC-L-Centre-U": "Tarif pointe critique pour le tarif L destiné aux secteurs Centre U / Option d’électricité interruptible pour la clientèle d’affaires de grande puissance"],
		["TPC-L-Sud-D1": "Tarif pointe critique pour le tarif L destiné aux secteurs Sud D1 / Option d’électricité interruptible pour la clientèle d’affaires de grande puissance"],
		["TPC-L-Sud-D2": "Tarif pointe critique pour le tarif L destiné aux secteurs Sud D2 / Option d’électricité interruptible pour la clientèle d’affaires de grande puissance"],
		["OEA": "Option d’électricité additionnelle pour la clientèle de moyenne puissance pour les tarifs M et G9 / grande puissance pour les tarifs L et LG"]
		]
		*/

		// Event Options with English Descriptions
		List<Map<String,String>> eventTypeOptions = [
		["CPC-D": "Critical peak credit for Rate D / Winter Credit Option for residential customers"],
		["TPC-DPC": " Critical peak rate for Rate D / Rate Flex D for residential customers"],
		["GDP-Affaires": "Critical peak rate for rates DP, DM, G, G9, M, LG or H / Demand Response Option for business customers"],
		["CPC-G": "Critical peak credit for rate G / Winter Credit Option for business customers"],
		["TPC-GPC": "Critical peak rate for rates G / Rate Flex G for business customers"],
		["TPC-M": "Critical peak rate for rates M, LG and G9 / Interruptible Electricity Options for medium-power business customers"],
		["TPC-L-Centre-C2": "Critical peak Rate L in sectors Centre C2 / Interruptible Electricity Options for large-power business customers"],
		["TPC-L-Centre-U": "Critical peak Rate L in sectors Centre U / Interruptible Electricity Options for large-power business customers"],
		["TPC-L-Sud-D1": "Critical peak Rate L in sectors Sud D1 / Interruptible Electricity Options for large-power business customers"],
		["TPC-L-Sud-D2": "Critical peak Rate L in sectors Sud D2 / Interruptible Electricity Options for large-power business customers"],
		["OEA": "Additional Electricity Options for medium-power business customers of rates M and G9 / large-power business customers of rates L and LG"]
		]


		section("<b>$state.name</b> ver. $state.version"){
			if (state.currentMode == "Paused") {    
				paragraph "This app has been paused, press <b>start</b> to resume normal operation."
				input name: "btnStart", type: "button", textColor: "white", backgroundColor: "green", title: "  Start app  "
			} else {
				paragraph "You can pause this app when not needed for long periods to save resouces."
				input name: "btnPause", type: "button", textColor: "white", backgroundColor: "red", title: "  Pause app  "
			}
			
			paragraph "To Poll the API and re-schedule all upcomming events."
			input name: "btnPoll", type: "button", textColor: "white", backgroundColor: "orange", title: "Poll API"
		}	
		
		section (""){
			paragraph "<br>"
		}

		section(title: "Event types...", hideable: true, hidden: hideEventTypeSection()) {
			input (name: "eventType", type: "enum", title: "<br><b>Hydro Quebec Event types you are Subscribed to</b>", options: eventTypeOptions, required: true, defaultValue: "CPC-D")
		}

		section (""){
			paragraph "<br>"
		}

		section("Morning pre-events...", hideable: true, hidden : hideMorningPreEventsSection()){
			paragraph "<br><b>Minutes before morning event to start morning pre-event mode <i>(enter '1' to disable pre-events)</i>.</b>"
			input "preEventMorningMinutes", "number", title: "Number of minutes before event", required:true, defaultValue:120, submitOnChange:true

			paragraph "<br><b>Select outlet(s)/switche(s) to turn off durring a morning pre-event.</b>"
			input "preEventMorningSwitches", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br><b>Select thermostats to turn <i>OFF</i> durring a morning pre-event.</b>"
			input "preEventMorningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true
			
			paragraph "<br><b>Select thermostats to turn <i>UP</i> durring a morning pre-event.</b>"
			input "preEventMorningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
			input "preEventMorningDegrees", "number", title: "Number of degrees $displayUnits to go up from current setting", required:true, defaultValue:3, submitOnChange:true
		}

		section("Morning events...", hideable: true, hidden : hideMorningEventsSection()){
			paragraph "<br><b>Select outlet(s)/switche(s) to turn off durring a morning event.</b>"
			input "eventMorningSwitches", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br><b>Select thermostats to turn <i>OFF</i> durring a morning event.</b>"
			input "eventMorningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true
			
			paragraph "<br><b>Select thermostats to turn <i>DOWN</i> durring a morning event.</b>"
			input "eventMorningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
			input "eventMorningDegrees", "number", title: "Number of degrees $displayUnits to drop from current setting", required:true, defaultValue:3, submitOnChange:true
		}

		section (""){
			paragraph "<br>"
		}
		
		section("Evening pre-events...", hideable: true, hidden : hideEveningPreEventsSection()){
			paragraph "<br><b>Minutes before evening event to start evening pre-event mode <i>(enter '1' to disable pre-events)</i>.</b>"
			input "preEventEveningMinutes", "number", title: "Number of minutes before event", required:true, defaultValue:120, submitOnChange:true

			paragraph "<br><b>Select outlet(s)/switche(s) to turn off durring a evening pre-event.</b>"
			input "preEventEveningSwitches", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br><b>Select thermostats to turn <i>OFF</i> durring a evening pre-event.</b>"
			input "preEventEveningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true
			
			paragraph "<br><b>Select thermostats to turn <i>UP</i> durring a evening pre-event.</b>"
			input "preEventEveningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
			input "preEventEveningDegrees", "number", title: "Number of degrees $displayUnits to go up from current setting", required:true, defaultValue:3, submitOnChange:true
		}

		section("Evening events...", hideable: true, hidden : hideEveningEventsSection()){

			input "eveningSameAsMorning", "bool", title: "Use same settings for Evening events", defaultValue:false, submitOnChange:true, width:6

			if (!eveningSameAsMorning) {
				paragraph "<br><b>Select outlet(s)/switche(s) to turn off durring an evening event.</b>"
				input "eventEveningSwitches", "capability.switch", title: "Switches", multiple: true

				paragraph "<br><b>Select thermostats to turn <i>OFF</i> durring an evening event.</b>"
				input "eventEveningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true

				paragraph "<br><b>Select thermostats to turn <i>DOWN</i> durring an evening event.</b>"
				input "eventEveningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
				input "eventEveningDegrees", "number", title: "Number of degrees $displayUnits to drop from current setting", required:true, defaultValue:3, submitOnChange:true
			}
		}
		
		section (""){
			paragraph "<br>"
		}

		section (title: "Notifications...", hideable: true, hidden: hideNotificationSection()) {
			paragraph "<br><b>Select switch/light reminder to turn on durring events to indicate event state.</b>"
			input "eventStateSwitch", "capability.switch", title: "Switches", multiple: false

			paragraph "<br><b>Devices to send text notifications to.</b>"
			input "sendPushMessage", "capability.notification", title: "Send a Pushover notification", multiple:true, required:false, submitOnChange:true
			if(sendPushMessage) {
				paragraph "<b>Events to send:</b> Select events to send to the selected notification devices."
				input "startMorningPreEventPush", "bool", title: "Morning Pre-Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startEveningPreEventPush", "bool", title: "Evening Pre-Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startMorningEventPush", "bool", title: "Morning Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startEveningEventPush", "bool", title: "Evening Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "endMorningEventPush", "bool", title: "Morning Event End Report", defaultValue:false, submitOnChange:true, width:6
				input "endEveningEventPush", "bool", title: "Evening Event End Report", defaultValue:false, submitOnChange:true, width:6
				input "newEventsPush", "bool", title: "New Events Added Report", defaultValue:false, submitOnChange:true, width:6
				input "restartEventRecoveryPush", "bool", title: "Restart Event Recovery Report (if enabled)", defaultValue:false, submitOnChange:true, width:6
			 }

			paragraph "<br><b>Devices to send voice notifications to.</b>"
			input "sendVoiceMessage", "capability.speechSynthesis", title: "Send a voice message", multiple:true, required:false, submitOnChange:true
			if(sendVoiceMessage) {
				input "startVoiceMessage", "string", title: "<br><b>Message to say at start of an event</b> (blank will not speak a message)", required: false, defaultValue: "Hydro-Quebec dynamic pricing event is starting"
				input "endVoiceMessage", "string", title: "<br><b>Message to say at end of an event</b> (blank will not speak a message)", required: false, defaultValue: "Hydro-Quebec dynamic pricing event has ended"
				input "newEventsVoiceMessage", "string", title: "<br><b>Message to say when new Events are added</b> (blank will not speak a message)", required: false, defaultValue: "New Hydro-Quebec dynamic pricing events for tomorrow have been programmed"
			 }
  		}

		section (""){
			paragraph "<br>"
		}

		section(title: "Advanced Settings:", hideable: true, hidden : hideAdvancedSection()){
			paragraph "<br><b>Select switch to disable Morning events</b> (this can be a virtual switch for other automations)"
			input "eventMorningDisableSwitch", "capability.switch", title: "Switches", multiple: false

			paragraph "<br><b>Select switch to disable Evening events</b> (this can be a virtual switch for other automations)"
			input "eventEveningDisableSwitch", "capability.switch", title: "Switches", multiple: false

//			Feature has not yet been enabled.
//			paragraph "<br><b>Set to on if you want the hub to recheck events and recover if we are within an event</b> (recommended)"
//			input "restartEventRecovery", "bool", title: "Hub restart event recovery", defaultValue:true, submitOnChange:true, width:6

		}

		section (""){
			paragraph "<br>"
		}

		section(title: "Log Settings...", hideable: true, hidden: hideLogSection()) {
			input (name: "logLevel", type: "enum", title: "<br><b>Logging Level choice system</b> (Messages with this level and higher will be logged)", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
			input "logDropLevelTime", "decimal", title: "<br><b>Delay before dropping down to Info Level</b> (in minutes if level is higher than Info)", required: true, defaultValue: 5
		}

		section (""){
			paragraph "<br><br><br>"
		}

	}
}




def installed() {

	state.currentMode = "New"
	initialize()

	// Poll now so we can catch new events ASAP
	newPoll()
	
}


def updated() {

   	// Log level was set to a higher level than 3, drop level to 3 in x number of minutes
	if (settings.logLevel > 3) {
		logger("trace", "Initialize Log Level drop in $settings.logDropLevelTime minutes")
		runIn(settings.logDropLevelTime.toInteger() * 60, logsDropLevel)
	}

	initialize() 
}


def initialize() {
	// Subscribe to Hub restarts so we can make sure events are dealt with correctly
	subscribe(location, "systemStart", hubRestartHandler)
	
	// Schedule to start polling API every day
	schedule("13 13 13 ? * * *", startPolling, [overwrite: true])
}


def uninstalled() {
	unsubscribe()
	unschedule()
}


//************************************************************
// hubRestartHandler
//     When hub restarts, check to see if we need to schedule anything that got erased
//
// Signature(s)
//     hubRestartHandler()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def hubRestartHandler(evt) {
	logger("trace", "hubRestartHandler")
	// Hub has restarted, are there events that we missed and should we do something with it...
//	if (!restartEventRecovery) {
//		logger("warn", "Hub has restarted but the hub restart checking for events feature is not selected.")
//		exit
//	}
	
	logger("warn", "Hub has restarted but the checking events feature has not yet been implemented, stay tuned.")
	
	//Go through all events and see if we are within an event period

}


//************************************************************
// startApp
//     Start back up this app
//
// Signature(s)
//     startApp()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def startApp() {
	logger("trace", "startApp")
	state.currentMode = "Normal"
	app.updateLabel("$state.name")
	initialize()
	logger("debug", "App has been started")
}


//************************************************************
// pauseApp
//     Pause this app and kill all schedules, 
//
// Signature(s)
//     pauseApp()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def pauseApp() {
	logger("trace", "pauseApp")
	state.currentMode = "Paused"
	unsubscribe()
	unschedule()
	app.updateLabel("$state.name <span style='color:red'>(Paused)</span>")
	logger("debug", "App has been paused")
}

//************************************************************
// startPolling
//     Schedule polling every 15 minutes until we find new data
//
// Signature(s)
//     startPolling()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def startPolling() {
	logger("trace", "startPoll")
	runEvery30Minutes(poll)        
}


//************************************************************
// poll
//     Poll API
//
// Signature(s)
//     poll()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def poll() {
	logger("trace", "poll")
	requestParams = [ uri: state.HQEventURL, ignoreSSLIssues: true]
	logger("debug", "Poll Api: $requestParams")
	asynchttpGet("pollHandler", requestParams)
}


//************************************************************
// pollHandler
//     Handle polled data and do what is needed whit it
//
// Signature(s)
//     pollHandler(resp, data)
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def pollHandler(resp, data) {
	logger("trace", "pollHandler")
	
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		logger("debug", "Poll Api Successful")
		
		if (state.apiData == resp.data) {
			logger("debug", "API data has not changed, nothing to do")
			if (state.testMode) {
				logger("debug", "We are in test mode, let's process the JSON file anyway!")
				handleHQEvents()
			}
            currTime = new Date()
            if (currTime > timeToday("23:00")) {
                unschedule(poll)
            }
		} else {
			//Let's save new data to state variable and see what to do
			state.apiData = resp.data
			logger("debug", "New API data, let's see what's new")
			unschedule(poll)
			handleHQEvents()
		}
	} else {
		//Error while poling API, no problem we will poll it again in xx minutes
		logger("warn", "Poll Api error: RESP: " + resp.getStatus() + " - $resp and DATA: $data")
	}
}


//************************************************************
// handleHQEvents
//     Handle Hydro Quebec events when they appear in the API
//
// Signature(s)
//     handleHQEvents()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def handleHQEvents() {
	logger("trace", "HandleHQEvents")
	
	response = parseJson(state.apiData)    
	
	def currDate = new Date()

	//Let's go through each event in the JSON file and schedule all new events
	newEventsPush = ""
			
	for(eventInfo in response.evenements) {
 
		def date = new Date()
								
		if (eventInfo.offre == settings.eventType) {

			//Let's parse event info so we can schedule it, send info to logs or even to a device
			eventStartPeriod = Date.parse("yyyy-MM-dd'T'HH:mm:ss", eventInfo.dateDebut)
			eventEndPeriod = Date.parse("yyyy-MM-dd'T'HH:mm:ss", eventInfo.dateFin)
				
			if (eventStartPeriod > date) {
				logger("debug", "new $eventStartPeriod to $eventEndPeriod")

				//Add event info to log and notification text
				if (!newEventsPush) {
					newEventsPush = "New HQ events added, from "
				} else {
					newEventsPush = newEventsPush + " & " 
				}
 
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
				Calendar calendar = new GregorianCalendar()
				Date timeVarObj = format.parse(eventInfo.dateDebut)
				calendar.setTime(timeVarObj)
				int integerHour = calendar.get(Calendar.HOUR_OF_DAY)

				if (eventStartPeriod > date && !state.testMode) {
					//Let's program events 
					if (integerHour < 12) {
						schedule(convertTimeToCron(eventInfo.dateDebut, preEventMorningMinutes * -1), setHouseInMorningPreEventMode, [overwrite: false])
						schedule(convertTimeToCron(eventInfo.dateDebut, 0), setHouseInMorningEventMode, [overwrite: false])
						schedule(convertTimeToCron(eventInfo.dateFin, 0) , setHouseInMorningNormalMode, [overwrite: false])
					} else {
						schedule(convertTimeToCron(eventInfo.dateDebut, preEventEveningMinutes * -1), setHouseInEveningPreEventMode, [overwrite: false])
  						schedule(convertTimeToCron(eventInfo.dateDebut, 0), setHouseInEveningEventMode, [overwrite: false])
						schedule(convertTimeToCron(eventInfo.dateFin, 0), setHouseInEveningNormalMode, [overwrite: false])
					}
				}
				newEventsPush = newEventsPush + " " + eventStartPeriod + " to " + eventEndPeriod + " "
			}
		}
	}
			
	//Send and log info if required
	if(sendPushMessage && newEventsPush && !state.testMode) {
		sendPushMessage.deviceNotification(newEventsPush)
	}
	if (state.testMode) {
		logger("debug", "No events have been scheduled since the app is in test mode, next line is for DEBUG purposes only!")
	}
	logger("debug", newEventsPush)
}

//************************************************************
// convertTimeToCron
//     Converts ISO time to a CRON esxpression adding minutes
//		to substract minutes, add a negative number
//
// Signature(s)
//     string = convertTimeToCron(string timeVar, string minutesToAdd)
//
// Parameters
//     timeVar : ISO formated date
//     minutesToAdd : Minutes to add or subtract
//
// Returns
//     CRON formated string
//
//************************************************************
def convertTimeToCron(timeVar, minutesToAdd = 0) {
	//log.debug "The timeVar is ${timeVar}"
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
	Calendar calendar = new GregorianCalendar()
	Date timeVarObj = format.parse(timeVar)
	calendar.setTime(timeVarObj)
	calendar.add(Calendar.MINUTE, (minutesToAdd.toInteger()) )
	timeVarObj = calendar.getTime()
	//log.debug "The timeVarObj is " + timeVarObj
	String second = calendar.get(Calendar.SECOND).toString().padLeft(2, '0')
	String hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padLeft(2, '0')
	String minute = calendar.get(Calendar.MINUTE).toString().padLeft(2, '0')
	String day = calendar.get(Calendar.DAY_OF_MONTH).toString().padLeft(2, '0')
	String month = (calendar.get(Calendar.MONTH) + 1).toString().padLeft(2, '0')
	String year = calendar.get(Calendar.YEAR)
	String cronExp = "${second} ${minute} ${hour} ${day} ${month} ? ${year}"
	return cronExp
}


//************************************************************
// setHouseInMorningPreEventMode
//     Set the different outlets, thermostats, etc for morning mode
//
// Signature(s)
//     setHouseInMorningPreEventMode()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHouseInMorningPreEventMode() {
	logger("trace", "setHouseInMorningPreEventMode")
	
	if (eventMorningDisableSwitch || preEventMorningMinutes == 1) {
		logger("warn", "Morning pre-event disabled, skipping this event!")
		exit
	}
	
	state.currentMode = "MorningPreEvent"
	
	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if(sendPushMessage && startMorningPreEventPush) {
		sendPushMessage.deviceNotification("Morning pre-event fired!")
	}
	
	def previousSettings = [:]
	
	for(eventSwitch in preEventMorningSwitches) {
		previousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for(thermostat in preEventMorningThermostatsOff) {
		previousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for(thermostat in preEventMorningThermostats) {
		previousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") + preEventEveningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}
	
	state.previousSettings = previousSettings

	app.updateLabel("$state.name <span style='color:green'>Morning pre-event in progress</span>")

}


//************************************************************
// setHouseInMorningEventMode
//     Set the different outlets, thermostats, etc for morning mode
//
// Signature(s)
//     setHouseInMorningEventMode()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHouseInMorningEventMode() {
	logger("trace", "setHouseInMorningEventMode")
	
	if (eventMorningDisableSwitch) {
		logger("warn", "Morning event disabled, skipping this event!")
		exit
	}
	
	state.currentMode = "MorningEvent"
	
	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if(sendPushMessage && startMorningEventPush) {
		sendPushMessage.deviceNotification("Morning event fired!")
	}
	
	def previousSettings = [:]
	
	for(eventSwitch in eventMorningSwitches) {
		previousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for(thermostat in eventMorningThermostatsOff) {
		previousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for(thermostat in eventMorningThermostats) {
		previousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") - eventEveningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}
	
	state.previousSettings = previousSettings

	app.updateLabel("$state.name <span style='color:green'>Morning Event in progress</span>")

}


//************************************************************
// setHouseInMorningNormalMode
//     Set the different outlets, thermostats, etc for normal mode
//
// Signature(s)
//     setHouseInMorningNormalMode()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHouseInMorningNormalMode() {
	logger("trace", "setHouseInMorningNormalMode")
	
	if (eventMorningDisableSwitch) {
		exit
	}

	state.currentMode = "Normal"

	if (eventStateSwitch) {
		eventStateSwitch.off()
	}
	
	if(sendPushMessage && endMorningEventPush) {
		sendPushMessage.deviceNotification("Back to normal!")
	}
	
	for(eventSwitch in eventMorningSwitches) {
		def switchState = state.previousSettings.find{ it.key == "$eventSwitch" }?.value
		if(switchState && switchState == "on") {
			logger("debug", "$eventSwitch turned $switchState")
			eventSwitch.on()
		} else {
			logger("debug", "$eventSwitch not changed")
		}
	}

	for(thermostat in eventMorningThermostatsOff) {
		def thermostatMode = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatMode) {
			thermostat.setThermostatMode("$thermostatMode")
			logger("debug", "$thermostat mode set to $thermostatMode")
		}
	}

	for(thermostat in eventMorningThermostats) {
		def thermostatTemp = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatTemp) {
			thermostat.setHeatingSetpoint(thermostatTemp)
			logger("debug", "$thermostat heating setpoint set to $thermostatTemp")
		}
	}
	
	state.remove("previousSettings")
	app.updateLabel("$state.name")
	
}


//************************************************************
// setHouseInEveningPreEventMode
//     Set the different outlets, thermostats, etc for evening mode
//
// Signature(s)
//     setHouseInEveningPreEventMode()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHouseInEveningPreEventMode() {
	logger("trace", "setHouseInEveningPreEventMode")

	if (eventEveningDisableSwitch || preEventEveningMinutes == 1) {
		logger("warn", "Evening event disabled, skipping this event!")
		exit
	}

	state.currentMode = "EveningPreEvent"

	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if(sendPushMessage && StartEveningPreEventPush) {
		sendPushMessage.deviceNotification("Evening event fired!")
	}
	
	def previousSettings = [:]
	
	for(eventSwitch in preEventEveningSwitches) {
		previousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for(thermostat in preEventEveningThermostatsOff) {
		previousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for(thermostat in preEventEveningThermostats) {
		previousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") + preEventEveningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}

	state.previousSettings = previousSettings

	app.updateLabel("$state.name <span style='color:green'>Evening pre-event in progress</span>")

}


//************************************************************
// setHouseInEveningEventMode
//     Set the different outlets, thermostats, etc for evening mode
//
// Signature(s)
//     setHouseInEveningEventMode()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHouseInEveningEventMode() {
	logger("trace", "setHouseInEveningEventMode")

	if (eventEveningDisableSwitch) {
		logger("warn", "Evening event disabled, skipping this event!")
		exit
	}

	state.currentMode = "EveningEvent"

	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if(sendPushMessage && StartEveningEventPush) {
		sendPushMessage.deviceNotification("Evening event fired!")
	}
	
	def previousSettings = [:]
	
	for(eventSwitch in eventEveningSwitches) {
		previousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for(thermostat in eventEveningThermostatsOff) {
		previousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for(thermostat in eventEveningThermostats) {
		previousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") - eventEveningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}

	state.previousSettings = previousSettings

	app.updateLabel("$state.name <span style='color:green'>Evening Event in progress</span>")

}


//************************************************************
// setHouseInEveningNormalMode
//     Set the different outlets, thermostats, etc for normal mode
//
// Signature(s)
//     setHouseInEveningNormalMode()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def setHouseInEveningNormalMode() {
	logger("trace", "setHouseInEveningNormalMode")

	if (eventEveningDisableSwitch) {
		exit
	}

	state.currentMode = "Normal"

	if (eventStateSwitch) {
		eventStateSwitch.off()
	}
	
	if(sendPushMessage && endEveningEventPush) {
		sendPushMessage.deviceNotification("Back to normal!")
	}

	for(eventSwitch in eventEveningSwitches) {
		def switchState = state.previousSettings.find{ it.key == "$eventSwitch" }?.value
		if(switchState && switchState == "on") {
			logger("debug", "$eventSwitch turned $switchState")
			eventSwitch.on()
		} else {
			logger("debug", "$eventSwitch not changed")
		}
	}

	for(thermostat in eventEveningThermostatsOff) {
		 def thermostatMode = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatMode) {
			thermostat.setThermostatMode("$thermostatMode")
			logger("debug", "$thermostat mode set to $thermostatMode")
		}
   }

	for(thermostat in eventEveningThermostats) {
		def thermostatTemp = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatTemp) {
			thermostat.setHeatingSetpoint(thermostatTemp)
			logger("debug", "$thermostat heating setpoint set to $thermostatTemp")
		}
	}

	state.remove("previousSettings")
	app.updateLabel("$state.name")

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
			if (settings.logLevel in ["1", "2", "3", "4", "5"]) log.error msg
			break

		case "warn":
			if (settings.logLevel in ["2", "3", "4", "5"]) log.warn msg
			break

		case "info":
			if (settings.logLevel in ["3", "4", "5"]) log.info msg
			break

		case "debug":
			if (settings.logLevel in ["4", "5"]) log.debug msg
			break

		case "trace":
			if (settings.logLevel in ["5"]) log.trace msg
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
	app.updateSetting("logLevel", [type: "enum", value: "3"])
	logger("warn","Logging level set to 3")
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
		case 'btnPoll':
			unschedule("setHouseInMorningPreEventMode")
			unschedule("setHouseInMorningEventMode")
			unschedule("setHouseInMorningNormalMode")
			unschedule("setHouseInEveningPreEventMode")
			unschedule("setHouseInEveningEventMode")
			unschedule("setHouseInEveningNormalMode")
			poll()
			break
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
private hideEventTypeSection() {(eventType) ? true : false}
private hideMorningPreEventsSection() {(preEventMorningSwitches || preEventMorningThermostatsOff || preEventMorningThermostats) ? false : true}
private hideMorningEventsSection() {(eventMorningSwitches || eventMorningThermostatsOff || eventMorningThermostats) ? false : true}
private hideEveningPreEventsSection() {(preEventEveningSwitches || preEventEveningThermostatsOff || eventEPreveningThermostats) ? false : true}
private hideEveningEventsSection() {(eventEveningSwitches || eventEveningThermostatsOff || eventEveningThermostats) ? false : true}
private hideAdvancedSection() {(eventMorningDisableSwitch || eventEveningDisableSwitch || restartEventRecovery) ? false : true}
private hideLogSection() {(logLevel == 3) ? true : false}
private hideNotificationSection() {(eventStateSwitch || sendPushMessage) ? false : true}

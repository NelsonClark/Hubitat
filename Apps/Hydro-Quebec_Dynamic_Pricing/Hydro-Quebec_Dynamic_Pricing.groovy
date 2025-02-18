import java.text.SimpleDateFormat
import groovy.time.TimeCategory
/*
 *  Hydro-Quebec Dynamic Pricing App
 *  Project URL: https://github.com/NelsonClark/Hubitat/tree/main/Apps/Hydro-Quebec_Dynamic_Pricing
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
 *  https://github.com/NelsonClark/Hubitat/blob/main/Apps/Hydro-Quebec_Dynamic_Pricing/README.md
 *
 */



def setConstants(){
	state.name = "Hydro-Québec Dynamic Pricing"
	state.version = "1.0.5"
	state.HQEventURL = "https://donnees.solutions.hydroquebec.com/donnees-ouvertes/data/json/pointeshivernales.json"
	//This is for testing purposes, for normal operation must be set to false
	state.testMode = false
}


definition(
	name: "Hydro-Quebec Dynamic Pricing",
	namespace: "nclark",
	author: "Nelson Clark",
	description: "Help maximize your return when opted in Hydro-Quebec Winter Credit Options",
	category: "Green Living",
	iconUrl: "",
	iconX2Url: "",
	documentationLink: "https://community.hubitat.com/t/release-hydro-quebec-dynamic-pricing/149414",
	importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/refs/heads/main/Apps/Hydro-Quebec_Dynamic_Pricing/Hydro-Quebec_Dynamic_Pricing.groovy",
	singleInstance: true
)


preferences {
	page(name: "mainPage")
	page(name: "configMorningPreEvents")
	page(name: "configEveningPreEvents")
	page(name: "configMorningEvents")
	page(name: "configEveningEvents")
	page(name: "configNotifications")
	page(name: "configAdvancedSettings")
}


def mainPage() {
	// Let's just set a few things before starting
	setConstants()
	def displayUnits = getDisplayUnits()


	dynamicPage(name: "mainPage", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>${state.name}</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: true, uninstall: true, refreshInterval:0) {

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


		section(""){
			if (state.currentMode == "Paused") {    
				paragraph "This app has been paused, press <b>start</b> to resume normal operation."
				input name: "btnStart", type: "button", textColor: "white", backgroundColor: "green", title: "  Start app  "
			} else {
				paragraph "Pause app when not needed for long periods to save resources."
				input name: "btnPause", type: "button", textColor: "white", backgroundColor: "red", title: "  Pause app  "
			}
		}	
		
		section (""){
			paragraph "<br>"
		}

		section(title: "${btnIcon('pi-cog')} Event types and Polling...", hideable: true, hidden: false) {
			input (name: "eventType", type: "enum", title: "<br>Hydro Quebec Event types you are Subscribed to", options: eventTypeOptions, required: true, defaultValue: "CPC-D")

			input (name: "pollStartTime", type: "time", title: "<br>Time to start poling the API <i>Around 13:00 is a good time</i>", defaultValue: "13:13")
		
			paragraph "<br>Poll the API and re-schedule all upcoming events."
			input name: "btnPoll", type: "button", textColor: "white", backgroundColor: "orange", title: "Poll API"
		}

		section (title: "${btnIcon('pi-sliders-v')} Event settings", hideable: true, hidden: false){
			paragraph "<p>"
			paragraph (hrefButton("${btnIcon('pi-angle-left')} ${btnIcon('pi-sun')} Morning Pre Event Actions", "./mainPage/configMorningPreEvents?idx=-1") + "&nbsp;&nbsp;" + hrefButton("${btnIcon('pi-sun')} ${btnIcon('pi-angle-right')} Morning Event Actions", "./mainPage/configMorningEvents"), width: 5)
			paragraph "<p>"
			paragraph (hrefButton("${btnIcon('pi-angle-left')} ${btnIcon('pi-moon')} Evening Pre Event Actions", "./mainPage/configEveningPreEvents") + "&nbsp;&nbsp;" + hrefButton("${btnIcon('pi-sun')} ${btnIcon('pi-angle-right')} Evening Event Actions", "./mainPage/configEveningEvents"), width: 5)
			paragraph "<p>"
			paragraph hrefButton("${btnIcon('pi-phone')} Notifications", "./mainPage/configNotifications"), width: 5
			paragraph "<p>"
			paragraph hrefButton("${btnIcon('pi-wrench')} Advanced Settings", "./mainPage/configAdvancedSettings"), width: 5
			paragraph "<p>"
		}

		section(title: "${btnIcon('pi-list')} Log Settings...", hideable: true, hidden: false) {
			input (name: "logLevel", type: "enum", title: "<br>Logging Level choice system <i>(Messages with this level and higher will be logged)</i>", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
			input "logDropLevelTime", "decimal", title: "<br>Delay before dropping down to Info Level <i>(in minutes if level is higher than Info)</i>", required: true, defaultValue: 5
		}

		section (""){
			paragraph "<br><br><br>"
		}

	}
}

def configMorningPreEvents() {
	dynamicPage (name: "configMorningPreEvents", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Morning Pre Events configuration</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: false, uninstall: false) {
		
		section(""){
			paragraph "<p>"
			input "preEventMorningMinutes", "number", title: "Minutes before morning event to start morning pre-event mode <i>(enter '1' to disable pre-events)</i>.", required:true, defaultValue:120, submitOnChange:true

			paragraph "<br>Select switch(es) to turn <i>ON</i> during a morning pre-event (This is mostly used to trigger other automations)."
			input "preEventMorningTriggers", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br>Select outlet(s)/switch(es) to turn <i>OFF</i> during a morning pre-event."
			input "preEventMorningSwitches", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br>Select thermostats to turn <i>OFF</i> during a morning pre-event."
			input "preEventMorningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true
			
			paragraph "<br>Select thermostats to turn <i>UP</i> during a morning pre-event."
			input "preEventMorningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
			input "preEventMorningDegrees", "number", title: "Number of degrees $displayUnits to go up from current setting", required:true, defaultValue:3, submitOnChange:true
		}
	}
}


def configMorningEvents() {
	dynamicPage (name: "configMorningEvents", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Morning Events configuration</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: false, uninstall: false) {

		section(""){
			paragraph "<br>Select outlet(s)/switch(es) to turn <i>OFF</i> during a morning event."
			input "eventMorningSwitches", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br>Select switch(es) to turn <i>ON</i> during a morning event (This is mostly used to trigger other automations)."
			input "eventMorningTriggers", "capability.switch", title: "Switches", multiple: true
		
			paragraph "<br>Select thermostats to turn <i>OFF</i> during a morning event."
			input "eventMorningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true
			
			paragraph "<br>Select thermostats to turn <i>DOWN</i> during a morning event."
			input "eventMorningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
			input "eventMorningDegrees", "number", title: "Number of degrees $displayUnits to drop from current setting", required:true, defaultValue:3, submitOnChange:true
		}
	}
}

def configEveningPreEvents() {
	dynamicPage (name: "configEveningPreEvents", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Evening Pre Events configuration</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: false, uninstall: false) {

		section(""){
			if (!eveningSameAsMorning) {
				paragraph "<p>"
				input "preEventEveningMinutes", "number", title: "Minutes before evening event to start evening pre-event mode <i>(enter '1' to disable pre-events)</i>.", required:true, defaultValue:120, submitOnChange:true

				paragraph "<br>Select switch(es) to turn <i>ON</i> during a evening pre-event (This is mostly used to trigger other automations)."
				input "preEventEveningTriggers", "capability.switch", title: "Switches", multiple: true
		
				paragraph "<br>Select outlet(s)/switch(es) to turn <i>OFF</i> during a evening pre-event."
				input "preEventEveningSwitches", "capability.switch", title: "Switches", multiple: true
		
				paragraph "<br>Select thermostats to turn <i>OFF</i> during a evening pre-event."
				input "preEventEveningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true
			
				paragraph "<br>Select thermostats to turn <i>UP</i> during a evening pre-event."
				input "preEventEveningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
				input "preEventEveningDegrees", "number", title: "Number of degrees $displayUnits to go up from current setting", required:true, defaultValue:3, submitOnChange:true
			}
		}
	}
}

def configEveningEvents() {
	dynamicPage (name: "configEveningEvents", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Evening Events configuration</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: false, uninstall: false) {

		section(""){

			input "eveningSameAsMorning", "bool", title: "Use same settings for Evening events", defaultValue:false, submitOnChange:true, width:6

			if (!eveningSameAsMorning) {
				paragraph "<br>Select outlet(s)/switch(es) to turn <i>OFF</i> during an evening event."
				input "eventEveningSwitches", "capability.switch", title: "Switches", multiple: true

				paragraph "<br>Select switch(es) to turn <i>ON</i> during an evening event (This is mostly used to trigger other automations)."
				input "eventEveningTriggers", "capability.switch", title: "Switches", multiple: true
		
				paragraph "<br>Select thermostats to turn <i>OFF</i> during an evening event."
				input "eventEveningThermostatsOff", "capability.thermostat", title: "Thermostats", multiple: true

				paragraph "<br>Select thermostats to turn <i>DOWN</i> during an evening event."
				input "eventEveningThermostats", "capability.thermostat", title: "Thermostats", multiple: true
				input "eventEveningDegrees", "number", title: "Number of degrees $displayUnits to drop from current setting", required:true, defaultValue:3, submitOnChange:true
			}
		}
	}
}

def configNotifications() {
	dynamicPage (name: "configNotifications", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Notifications configuration</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: false, uninstall: false) {

		section ("") {
			paragraph "<br>Devices to send text notifications to."
			input "sendPushMessage", "capability.notification", title: "Send a Pushover notification", multiple:true, required:false, submitOnChange:true
			if (sendPushMessage) {
				paragraph "Events to send: <i>Select events to send to the selected notification devices.</i>"
				input "startMorningPreEventPush", "bool", title: "Morning Pre-Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startEveningPreEventPush", "bool", title: "Evening Pre-Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startMorningEventPush", "bool", title: "Morning Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startEveningEventPush", "bool", title: "Evening Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "endMorningEventPush", "bool", title: "Morning Event End Report", defaultValue:false, submitOnChange:true, width:6
				input "endEveningEventPush", "bool", title: "Evening Event End Report", defaultValue:false, submitOnChange:true, width:6
				input "newEventsPush", "bool", title: "New Events Added Report", defaultValue:false, submitOnChange:true, width:6
//				input "restartEventRecoveryPush", "bool", title: "Restart Event Recovery Report (if enabled)", defaultValue:false, submitOnChange:true, width:6
			 }

			paragraph "<br>Devices to send voice notifications to."
			input "speakVoiceMessage", "capability.speechSynthesis", title: "Voice alerts on this speech device", multiple:true, required:false, submitOnChange:true
			paragraph "<br>"
			input "playerVoiceMessage", "capability.musicPlayer", title:"Voice alerts on this music device", multiple: true, required: false, submitOnChange:true
			
			if (speakVoiceMessage || playerVoiceMessage) {
				input "volumeVoiceMessage", "decimal", title: "<br>Volume level of the Voice announcement", required: true, defaultValue: 40 
				input "startPreEventVoiceMessage", "string", title: "<br>Message to say at start of an event", required: false, defaultValue: "Hydro-Quebec dynamic pricing pre-event is starting"
				input "startEventVoiceMessage", "string", title: "<br>Message to say at start of an event", required: false, defaultValue: "Hydro-Quebec dynamic pricing event is starting"
				input "endEventVoiceMessage", "string", title: "<br>Message to say at end of an event", required: false, defaultValue: "Hydro-Quebec dynamic pricing event has ended"
				input "newEventsVoiceMessage", "string", title: "<br>Message to say when new Events are added", required: false, defaultValue: "New Hydro-Quebec dynamic pricing events for tomorrow have been programmed"

				paragraph "Events to announce: <i>Select events to announce vocally on the selected voice notification devices.</i>"
				input "startMorningPreEventVoice", "bool", title: "Morning Pre-Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startEveningPreEventVoice", "bool", title: "Evening Pre-Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startMorningEventVoice", "bool", title: "Morning Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "startEveningEventVoice", "bool", title: "Evening Event Start Report", defaultValue:false, submitOnChange:true, width:6
				input "endMorningEventVoice", "bool", title: "Morning Event End Report", defaultValue:false, submitOnChange:true, width:6
				input "endEveningEventVoice", "bool", title: "Evening Event End Report", defaultValue:false, submitOnChange:true, width:6
				input "newEventsVoice", "bool", title: "New Events Added Report", defaultValue:false, submitOnChange:true, width:6
			 }
  		}
	}
}

def configAdvancedSettings() {
	dynamicPage (name: "configAdvancedSettings", title: "<style> h2{color:navy;}h3{color:navy;}</style><h2>Advanced settings</h2><p style='font-size:small;color:navy'>v${state.version}</p>", install: false, uninstall: false) {

		section(""){
			paragraph "<br>Select switch/light reminder to turn on during events. <i>(this can be a virtual switch for other automations)</i>"
			input "eventStateSwitch", "capability.switch", title: "Switches", multiple: false

			paragraph "<br>Select switch that disables Morning events."
			input "eventMorningDisableSwitch", "capability.switch", title: "Switches", multiple: false

			paragraph "<br>Select switch that disables Evening events."
			input "eventEveningDisableSwitch", "capability.switch", title: "Switches", multiple: false

			paragraph "<br>Set to on if you want the hub to recheck events and recover if we are within an event <i>(recommended)</i>"
			input "restartEventRecovery", "bool", title: "Hub restart event recovery", defaultValue:true, submitOnChange:true, width:6
		}
	}
}


def installed() {

	state.currentMode = "New"
	initialize()

	// Poll now so we can catch new events ASAP
	poll()
	
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
	state.remove("apiUrl")
	state.remove("deubgInfo")

	// Subscribe to Hub restarts so we can make sure events are dealt with correctly
	subscribe(location, "systemStart", hubRestartHandler)
	
	// Schedule to start polling API every day
	if (!pollStartTime) {
		schedule("13 13 13 ? * * *", startPolling, [overwrite: true])
	} else {
		schedule(pollStartTime, startPolling, [overwrite: true])
	}
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
	logger("trace", "hubRestartHandler---")
	// Hub has restarted, are there events that we missed and should we do something with it...

	if (!restartEventRecovery) {
		logger("warn", "Hub has restarted but the hub restart checking for events feature is not selected.")
		exit
	}
	
	logger("warn", "Hub has restarted let's see if we have events to schedule and/or start.")
	
	//Remove all events, poll API and schedule all upcoming events
	state.pollManually = true
	unschedule("setHouseInMorningPreEventMode")
	unschedule("setHouseInMorningEventMode")
	unschedule("setHouseInMorningNormalMode")
	unschedule("setHouseInEveningPreEventMode")
	unschedule("setHouseInEveningEventMode")
	unschedule("setHouseInEveningNormalMode")
	
	poll()

	logger("trace","---End hubRestartHandler")
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
	logger("warn", "HQ App has been started")
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
	logger("warn", "HQ App has been paused, all schedules removed")
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
	logger("trace", "startPolling---")
	runEvery30Minutes(poll)
	logger("trace", "---End startPolling")
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
	logger("trace", "poll---")
	requestParams = [ uri: state.HQEventURL, ignoreSSLIssues: true]
	logger("debug", "Poll Api: $requestParams")
	asynchttpGet("pollHandler", requestParams)
	logger("trace", "---End poll")
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
	logger("trace", "pollHandler---")
	
	if ((resp.getStatus() == 200) || (resp.getStatus() == 207)) {
		logger("debug", "Poll Api Successful")
		
		if (state.apiData == resp.data) {
			logger("debug", "API data has not changed, nothing to do")
			if ((state.testMode) || (state.pollManually)) {
				logger("debug", "We are in manual mode, let's process the JSON file anyway!")
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
	logger("trace", "---End pollHandler")
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
	logger("trace", "HandleHQEvents---")
	
	response = parseJson(state.apiData)    
	newEventsPush = ""

	//Let's go through each event in the JSON file and schedule all new events
	for (eventInfo in response.evenements) {
 
		def currentDateTime = new Date()
								
		if (eventInfo.offre == settings.eventType) {

			eventStartPeriod = toDateTime(eventInfo.dateDebut)
			eventEndPeriod = toDateTime(eventInfo.dateFin)
				
			//Check if event has ended or not
			if (eventEndPeriod > currentDateTime) {
				logger("debug", "Processing event from $eventStartPeriod to $eventEndPeriod")

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

				//Let's program events 
				if (integerHour < 12) {
					use (TimeCategory) { preEventStartPeriod = eventStartPeriod - preEventMorningMinutes.toInteger().minutes }
						
					if (timeOfDayIsBetween(preEventStartPeriod, eventStartPeriod, currentDateTime)) {
						if (!state.prePreviousSettings) {
							logger("debug", "Pre-Event started, let's go in pre-event mode right away!")
							setHouseInMorningPreEventMode()
						} else {
							logger("debug", "Pre-Event started, but we are already all set!")
						}
					} else {
						if (currentDateTime < preEventStartPeriod) {
							logger("debug", "Pre-Event not started, let's schedule pre-event!")
							schedule(convertISODateTimeToCron(eventInfo.dateDebut, preEventMorningMinutes * -1), setHouseInMorningPreEventMode, [overwrite: false])
						}
					}

					if (timeOfDayIsBetween(eventStartPeriod, eventEndPeriod, currentDateTime)) {
						if (!state.previousSettings) {
							logger("debug", "Event started, let's go in event mode right away!")
							setHouseInMorningEventMode()
						} else {
							logger("debug", "Event started, but we are already all set!")
						}
					} else {
						if (currentDateTime < eventStartPeriod) {
							logger("debug", "Event not started, let's schedule event!")
							schedule(convertISODateTimeToCron(eventInfo.dateDebut, 0), setHouseInMorningEventMode, [overwrite: false])
						}
					}
					logger("debug", "Schedule end of evening event")
					schedule(convertISODateTimeToCron(eventInfo.dateFin, 0) , setHouseInMorningNormalMode, [overwrite: false])
				} else {
					use (TimeCategory) { preEventStartPeriod = eventStartPeriod - preEventEveningMinutes.toInteger().minutes }
						
					if (timeOfDayIsBetween(preEventStartPeriod, eventStartPeriod, currentDateTime)) {
						if (!state.prePreviousSettings) {
							logger("debug", "Pre-Event started, let's go in pre-event mode right away!")
							setHouseInEveningPreEventMode()
						} else {
							logger("debug", "Pre-Event started, but we are already all set!")
						}
					} else {
						if (currentDateTime < preEventStartPeriod) {
							logger("debug", "Pre-Event not started, let's schedule pre-event!")
							schedule(convertISODateTimeToCron(eventInfo.dateDebut, preEventEveningMinutes * -1), setHouseInEveningPreEventMode, [overwrite: false])
						}
					}

					if (timeOfDayIsBetween(eventStartPeriod, eventEndPeriod, currentDateTime)) {
						if (!state.previousSettings) {
							logger("debug", "Event started, let's go in event mode right away!")
							setHouseInEveningEventMode()
						} else {
							logger("debug", "Event started, but we are already all set!")
						}
					} else {
						if (currentDateTime < eventStartPeriod) {
							logger("debug", "Event not started, let's schedule event!")
							schedule(convertISODateTimeToCron(eventInfo.dateDebut, 0), setHouseInEveningEventMode, [overwrite: false])
						}
					}
					logger("debug", "Schedule end of evening event")
					schedule(convertISODateTimeToCron(eventInfo.dateFin, 0), setHouseInEveningNormalMode, [overwrite: false])
				}
				newEventsPush = newEventsPush + " " + eventStartPeriod + " to " + eventEndPeriod + " "
			}
		}
	}
			
	//Send and log info if required
	if ((sendPushMessage) && (newEventsPush) && (!state.testMode)) {
		sendPushMessage.deviceNotification(newEventsPush)
	}
	
	if ((speakVoiceMessage) && (newEventsVoice)) {
		speakVoiceMessage.speak(newEventsVoiceMessage,volumeVoiceMessage.toInteger())
	}

	if ((playerVoiceMessage) && (newEventsVoice)) {
		playerVoiceMessage.speak(newEventsVoiceMessage,volumeVoiceMessage.toInteger())
	}
	
	if (state.testMode) {
		logger("debug", "No events have been scheduled since the app is in test mode, next line is for DEBUG purposes only!")
	}
	state.pollManually = false
	logger("debug", newEventsPush)
	logger("trace", "---End HandleHQEvents")
}


//************************************************************
// convertISODateTimeToCron
//     Converts ISO time to a CRON expression adding minutes
//		to subtract minutes, add a negative number
//
// Signature(s)
//     string = convertISODateTimeToCron(string timeVar, string minutesToAdd)
//
// Parameters
//     timeVar : ISO formatted date
//     minutesToAdd : Minutes to add or subtract
//
// Returns
//     CRON formatted string
//
//************************************************************
def convertISODateTimeToCron(timeVar, minutesToAdd = 0) {
	logger("trace", "convertISODateTimeToCron---")
	
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
	Calendar calendar = new GregorianCalendar()
	Date timeVarObj = format.parse(timeVar)
	calendar.setTime(timeVarObj)
	calendar.add(Calendar.MINUTE, (minutesToAdd.toInteger()) )
	timeVarObj = calendar.getTime()
	String second = calendar.get(Calendar.SECOND).toString().padLeft(2, '0')
	String hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padLeft(2, '0')
	String minute = calendar.get(Calendar.MINUTE).toString().padLeft(2, '0')
	String day = calendar.get(Calendar.DAY_OF_MONTH).toString().padLeft(2, '0')
	String month = (calendar.get(Calendar.MONTH) + 1).toString().padLeft(2, '0')
	String year = calendar.get(Calendar.YEAR)
	String cronExp = "${second} ${minute} ${hour} ${day} ${month} ? ${year}"
	
	logger("trace", "---end convertISODateTimeToCron")
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
	logger("trace", "setHouseInMorningPreEventMode---")
	
	if ((eventMorningDisableSwitch) || (preEventMorningMinutes == 1)) {
		logger("warn", "Morning pre-event disabled, skipping this event!")
		exit
	}
	
	state.currentMode = "MorningPreEvent"
	
	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if ((sendPushMessage) && (startMorningPreEventPush)) {
		logger("trace", "Push Message: Morning pre-event fired!")
		sendPushMessage.deviceNotification("HQ Morning pre-event has started!")
	}
	
	if ((speakVoiceMessage) && (startMorningPreEventVoice)) {
		speakVoiceMessage.speak(startPreEventVoiceMessage)
	}

	if ((playerVoiceMessage) && (startMorningPreEventVoice)) {
		playerVoiceMessage.speak(startPreEventVoiceMessage)
	}

	def prePreviousSettings = [:]
	
	for (preEventTrigger in preEventMorningTriggers) {
		prePreviousSettings.put("$preEventTrigger",preEventTrigger.currentValue("switch"))
		preEventTrigger.on()
	}
	
	for (eventSwitch in preEventMorningSwitches) {
		prePreviousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for (thermostat in preEventMorningThermostatsOff) {
		prePreviousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for (thermostat in preEventMorningThermostats) {
		prePreviousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") + preEventMorningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}
	
	state.prePreviousSettings = prePreviousSettings

	app.updateLabel("$state.name <span style='color:green'>Morning pre-event in progress</span>")
	logger("trace", "---end setHouseInMorningPreEventMode")
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
	logger("trace", "setHouseInMorningEventMode---")
	
	if (eventMorningDisableSwitch) {
		logger("warn", "Morning event disabled, skipping this event!")
		exit
	}
	
	if (state.currentMode == "MorningPreEvent") {

		logger("trace", "revert setHouseInMorningPreEventMode---")
		
		for (preEventTrigger in preEventMorningTriggers) {
			def switchState = state.prePreviousSettings.find{ it.key == "$preEventTrigger" }?.value
			if ((switchState) && (switchState == "off")) {
				logger("debug", "$preEventTrigger turned $switchState")
				preEventTrigger.off()
			} else {
				logger("debug", "$preEventTrigger not changed")
			}
		}

		for (preEventSwitch in preEventMorningSwitches) {
			def switchState = state.prePreviousSettings.find{ it.key == "$preEventSwitch" }?.value
			if ((switchState) && (switchState == "on")) {
				logger("debug", "$preEventSwitch turned $switchState")
				preEventSwitch.on()
			} else {
				logger("debug", "$preEventSwitch not changed")
			}
		}

		for (thermostat in preEventMorningThermostatsOff) {
			def thermostatMode = state.prePreviousSettings.find{ it.key == "$thermostat" }?.value
			if (thermostatMode) {
				thermostat.setThermostatMode("$thermostatMode")
				logger("debug", "$thermostat mode set to $thermostatMode")
			}
		}

		for (thermostat in preEventMorningThermostats) {
			def thermostatTemp = state.prePreviousSettings.find{ it.key == "$thermostat" }?.value
			if (thermostatTemp) {
				thermostat.setHeatingSetpoint(thermostatTemp)
				logger("debug", "$thermostat heating setpoint set to $thermostatTemp")
			}
		}

		state.remove("prePreviousSettings")
		pauseExecution(10000)
		logger("trace", "---end revert setHouseInMorningPreEventMode")
	}
	
	state.currentMode = "MorningEvent"
	
	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if ((sendPushMessage) && (startMorningEventPush)) {
		logger("trace", "Morning event fired!")
		sendPushMessage.deviceNotification("HQ Morning event has started!")
	}
	
	if ((speakVoiceMessage) && (startMorningEventVoice)) {
		speakVoiceMessage.speak(startEventVoiceMessage)
	}

	if ((playerVoiceMessage) && (startMorningEventVoice)) {
		playerVoiceMessage.speak(startEventVoiceMessage)
	}
	
	def previousSettings = [:]
	
	for (eventTrigger in eventMorningTriggers) {
		previousSettings.put("$eventTrigger",eventTrigger.currentValue("switch"))
		eventTrigger.on()
	}

	for (eventSwitch in eventMorningSwitches) {
		previousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for (thermostat in eventMorningThermostatsOff) {
		previousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for (thermostat in eventMorningThermostats) {
		previousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") - eventMorningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}
	
	state.previousSettings = previousSettings

	app.updateLabel("$state.name <span style='color:green'>Morning Event in progress</span>")
	logger("trace", "---end setHouseInMorningEventMode")
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
	logger("trace", "setHouseInMorningNormalMode---")
	
	if (eventMorningDisableSwitch) {
		exit
	}

	state.currentMode = "Normal"

	if (eventStateSwitch) {
		eventStateSwitch.off()
	}
	
	for (eventTrigger in eventMorningTriggers) {
		def switchState = state.previousSettings.find{ it.key == "$eventTrigger" }?.value
		if ((switchState) && (switchState == "off")) {
			logger("debug", "$eventTrigger turned $switchState")
			eventTrigger.off()
		} else {
			logger("debug", "$eventTrigger not changed")
		}
	}

	for (eventSwitch in eventMorningSwitches) {
		def switchState = state.previousSettings.find{ it.key == "$eventSwitch" }?.value
		if ((switchState) && (switchState == "on")) {
			logger("debug", "$eventSwitch turned $switchState")
			eventSwitch.on()
		} else {
			logger("debug", "$eventSwitch not changed")
		}
	}

	for (thermostat in eventMorningThermostatsOff) {
		def thermostatMode = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatMode) {
			thermostat.setThermostatMode("$thermostatMode")
			logger("debug", "$thermostat mode set to $thermostatMode")
		}
	}

	for (thermostat in eventMorningThermostats) {
		def thermostatTemp = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatTemp) {
			thermostat.setHeatingSetpoint(thermostatTemp)
			logger("debug", "$thermostat heating setpoint set to $thermostatTemp")
		}
	}
	
	if ((sendPushMessage) && (endMorningEventPush)) {
		logger("trace", "Push Message: Back to normal!")
		sendPushMessage.deviceNotification("HQ Morning event ended, back to normal!")
	}

	if ((speakVoiceMessage) && (endMorningEventVoice)) {
		speakVoiceMessage.speak(endEventVoiceMessage)
	}

	if ((playerVoiceMessage) && (endMorningEventVoice)) {
		playerVoiceMessage.speak(endEventVoiceMessage)
	}

	state.remove("previousSettings")
	app.updateLabel("$state.name")
	logger("trace", "---end setHouseInMorningNormalMode")
	
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
	logger("trace", "setHouseInEveningPreEventMode---")

	if ((eventEveningDisableSwitch) || (preEventEveningMinutes == 1)) {
		logger("warn", "Evening event disabled, skipping this event!")
		exit
	}

	state.currentMode = "EveningPreEvent"

	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if ((sendPushMessage) && (StartEveningPreEventPush)) {
		logger("trace", "Push Message: Evening event fired!")
		sendPushMessage.deviceNotification("HQ Evening event has started!")
	}
	
	if ((speakVoiceMessage) && (startEveningPreEventVoice)) {
		speakVoiceMessage.speak(startPreEventVoiceMessage)
	}

	if ((playerVoiceMessage) && (startEveningPreEventVoice)) {
		playerVoiceMessage.speak(startPreEventVoiceMessage)
	}

	def prePreviousSettings = [:]
	
	for (preEventTrigger in preEventEveningTriggers) {
		prePreviousSettings.put("$preEventTrigger",preEventTrigger.currentValue("switch"))
		preEventTrigger.on()
	}

	for (eventSwitch in preEventEveningSwitches) {
		prePreviousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for (thermostat in preEventEveningThermostatsOff) {
		prePreviousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for (thermostat in preEventEveningThermostats) {
		prePreviousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") + preEventEveningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}

	state.prePreviousSettings = prePreviousSettings

	app.updateLabel("$state.name <span style='color:green'>Evening pre-event in progress</span>")
	logger("trace", "---end setHouseInEveningPreEventMode")
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
	logger("trace", "setHouseInEveningEventMode---")

	if (eventEveningDisableSwitch) {
		logger("warn", "Evening event disabled, skipping this event!")
		exit
	}

	if (state.currentMode == "EveningPreEvent") {

		logger("trace", "revert setHouseInEveningPreEventMode---")

		for (preEventTrigger in preEventEveningTriggers) {
			def switchState = state.prePreviousSettings.find{ it.key == "$preEventTrigger" }?.value
			if ((switchState) && (switchState == "off")) {
				logger("debug", "$preEventTrigger turned $switchState")
				preEventTrigger.off()
			} else {
				logger("debug", "$preEventTrigger not changed")
			}
		}

		for (preEventSwitch in preEventEveningSwitches) {
			def switchState = state.prePreviousSettings.find{ it.key == "$preEventSwitch" }?.value
			if ((switchState) && (switchState == "on")) {
				logger("debug", "$preEventSwitch turned $switchState")
				preEventSwitch.on()
			} else {
				logger("debug", "$preEventSwitch not changed")
			}
		}

		for (thermostat in preEventEveningThermostatsOff) {
			def thermostatMode = state.prePreviousSettings.find{ it.key == "$thermostat" }?.value
			if (thermostatMode) {
				thermostat.setThermostatMode("$thermostatMode")
				logger("debug", "$thermostat mode set to $thermostatMode")
			}
		}

		for (thermostat in preEventEveningThermostats) {
			def thermostatTemp = state.prePreviousSettings.find{ it.key == "$thermostat" }?.value
			if (thermostatTemp) {
				thermostat.setHeatingSetpoint(thermostatTemp)
				logger("debug", "$thermostat heating setpoint set to $thermostatTemp")
			}
		}

		state.remove("prePreviousSettings")
		pauseExecution(10000)
		logger("trace", "---end revert setHouseInEveningPreEventMode")
	}
	
	state.currentMode = "EveningEvent"

	if (eventStateSwitch) {
		eventStateSwitch.on()
	}
	
	if ((sendPushMessage) && (StartEveningEventPush)) {
		logger("trace", "Push Message: Evening event fired!")
		sendPushMessage.deviceNotification("HQ Evening event has started!")
	}
	
	if ((speakVoiceMessage) && (startEveningEventVoice)) {
		speakVoiceMessage.speak(startEventVoiceMessage)
	}

	if ((playerVoiceMessage) && (startEveningEventVoice)) {
		playerVoiceMessage.speak(startEventVoiceMessage)
	}

	def previousSettings = [:]
	
	for (eventTrigger in eventEveningTriggers) {
		previousSettings.put("$eventTrigger",eventTrigger.currentValue("switch"))
		eventTrigger.on()
	}

	for (eventSwitch in eventEveningSwitches) {
		previousSettings.put("$eventSwitch",eventSwitch.currentValue("switch"))
		eventSwitch.off()
	}

	for (thermostat in eventEveningThermostatsOff) {
		previousSettings.put("$thermostat",thermostat.currentValue("thermostatMode"))
		if (thermostat.currentValue("thermostatMode") == "off") {      
			logger("debug", "$thermostat already off")
		} else {
			thermostat.setThermostatMode("off")
			logger("debug", "$thermostat turned off")
		}
	}

	for (thermostat in eventEveningThermostats) {
		previousSettings.put("$thermostat",thermostat.currentValue("heatingSetpoint"))
		newSetpoint = thermostat.currentValue("heatingSetpoint") - eventEveningDegrees.toInteger()
		thermostat.setHeatingSetpoint(newSetpoint)
		logger("debug", "$thermostat heating setpoint set to $newSetpoint")
	}

	state.previousSettings = previousSettings

	app.updateLabel("$state.name <span style='color:green'>Evening Event in progress</span>")
	logger("trace", "---end setHouseInEveningEventMode")
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
	logger("trace", "setHouseInEveningNormalMode---")

	if (eventEveningDisableSwitch) {
		exit
	}

	state.currentMode = "Normal"

	if (eventStateSwitch) {
		eventStateSwitch.off()
	}
	
	for (eventTrigger in eventEveningTriggers) {
		def switchState = state.previousSettings.find{ it.key == "$eventTrigger" }?.value
		if ((switchState) && (switchState == "off")) {
			logger("debug", "$eventTrigger turned $switchState")
			eventTrigger.off()
		} else {
			logger("debug", "$eventTrigger not changed")
		}
	}
	
	for (eventSwitch in eventEveningSwitches) {
		def switchState = state.previousSettings.find{ it.key == "$eventSwitch" }?.value
		if ((switchState) && (switchState == "on")) {
			logger("debug", "$eventSwitch turned $switchState")
			eventSwitch.on()
		} else {
			logger("debug", "$eventSwitch not changed")
		}
	}

	for (thermostat in eventEveningThermostatsOff) {
		 def thermostatMode = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatMode) {
			thermostat.setThermostatMode("$thermostatMode")
			logger("debug", "$thermostat mode set to $thermostatMode")
		}
   }

	for (thermostat in eventEveningThermostats) {
		def thermostatTemp = state.previousSettings.find{ it.key == "$thermostat" }?.value
		if (thermostatTemp) {
			thermostat.setHeatingSetpoint(thermostatTemp)
			logger("debug", "$thermostat heating setpoint set to $thermostatTemp")
		}
	}

	if ((sendPushMessage) && (endEveningEventPush)) {
		logger("trace", "Push Message: Back to normal!")
		sendPushMessage.deviceNotification("HQ Evening event ended, back to normal!")
	}

	if ((speakVoiceMessage) && (endEveningEventVoice)) {
		speakVoiceMessage.speak(endEventVoiceMessage)
	}

	if ((playerVoiceMessage) && (endEveningEventVoice)) {
		playerVoiceMessage.speak(endEventVoiceMessage)
	}

	state.remove("previousSettings")
	app.updateLabel("$state.name")
	logger("trace", "---end setHouseInEveningNormalMode")
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

String btnIcon(String name) {
    return "<span class='p-button-icon p-button-icon-left pi " + name + "' data-pc-section='icon'></span>"
}

String hrefButton(String btnName, String href, String iconName=null) {
    String output = ""
    output += """<button style="width: 45%; height: 50px"onClick="location.href='""" + href + """'" class="p-button p-component mr-2 mb-2" type="button" aria-label="hrefButton" data-pc-name="button" data-pc-section="root" data-pd-ripple="true">"""
    if (iconName) output += btnIcon(iconName)
    output += btnName
    output += """<span role="presentation" aria-hidden="true" data-p-ink="true" data-p-ink-active="false" class="p-ink" data-pc-name="ripple" data-pc-section="root"></span></button>"""
    return output
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
			state.pollManually = true
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


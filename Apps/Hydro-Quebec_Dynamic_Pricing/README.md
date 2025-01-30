# Hydro-Quebec Dynamic Pricing for Hubitat

Hydro-Quebec Dynamic Pricing is a Hubitat App that will fetch Winter Events and setup your house/business as you want during these events and back at the end of the event.

For more info, go to: https://community.hubitat.com/t/release-hydro-quebec-dynamic-pricing/149414


This app will process all known winter events as of winter 2024-2025 and allows to turn off switched devices, turn off thermostats, turn down thermostats, etc. Send notifications of new events, start/end of events. Morning and Evening can have different devices,thermostats controlled. New at version 1.0.0, you can now have pre-event settings (mostly to preheat the house before the event)

Note : If a thermostat is also controlled with the built in app Thermostat Schedular, there might be some problems if a shedule is set to change during the event. This has not been fully tested yet, any feedback is appreciated.


# Installation

* Import the App in the <> Apps Code section of your Hubitat Hub
* Go to the Apps Section and use the + Add User App button in the top right corner to add the Hydro-Quebec Dynamic Pricing app
* Configure the app to your liking, then click DONE

Enjoy!


# Version History

* 0.1 - Original Beta release private
* 0.2 - First Public Beta release 
  * Added automatic fetch of events via an API (internet access is needed for this)
  * Minor bug fixes
  * Added ability to disable events via a switch/virtual switch (because of the API)
  * Seperated Morning and Evening settings
  * Cleaned up the settings page with hideable sections
  * Added configurable notifications
  * Added a STOP button, to disable the app when outside the Dynamic Pricing event dates
* 0.3.0 - 2022/02/18 - Second Beta release
  * - Added more debug logging
  * - Added option to copy over morning to evening options
  * - Cleaned up the menus
* 1.0.0 - 2025-01-29 - First public release
  * - Now uses the official event JSON from HQ
  * - Now supports all available events as of winter 2024-2025
  * - Added optional Pre-Event mode for people that want to heat up the house before an event


# ToDo

Things to do in upcomming releases...

- [x] Notifications to Hubitat app, SMS and or email depending on your notifications devices.
- [ ] Add possibility to go back to manual control for times where internet acces may not be available
- [ ] Menu switch to make Evening settings the same as Morning settings
- [ ] Deal with thermostats that are also controlled via Thermostat Schedular
- [x] Speach notifications to TTS devices (like SONOS, etc.)
- [ ] Add hub restart handler to reschedule any on going events and future events


PULL requests are welcome

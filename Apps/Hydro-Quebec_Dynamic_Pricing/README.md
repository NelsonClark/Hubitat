# Hydro-Quebec Dynamic Pricing for Hubitat

Hydro-Quebec Dynamic Pricing is a Hubitat App that will fetch Winter Events and setup your house as you want during these events and back at the end of the event.

For more info, go to: https://community.hubitat.com/ LINK SOON


This app allows to turn off switch devices, turn off thermostats, turn down thermostats, etc. Send notifications of new events, start/end of events. Morning and Evening can have different devices,thermostats controlled.

Note : If a thermostat is also controlled with the built in app Thermostat Schedular, there might be some problems if a shedule is set to change during the event. This has not been fully tested yet, any feedback is appreciated.


# Installation

* Import the App in the <> Apps Code section of your Hubitat Hub
* Go to the Apps Section and use the + Add User App button in the top right corner to add the Hydro-Quebec Dynamic Pricing app
* Configure the app to your liking, then click DONE

Enjoy!


# Version History

* 0.1 - Original Beta release
* 0.2 - Beta 2
  * Added automatic fetch of events via an API (internet access is needed for this)
  * Minor bug fixes
  * Added ability to disable events via a switch/virtual switch (because of the API)
  * Seperated Morning and Evening settings
  * Cleaned up the settings page with hideable sections
  * Added configurable notifications

# ToDo

Things to do in upcomming releases...

- [x] Simplify the settings in the device driver by hiding less common settings if possible and change some attributs with state variables
- [x] Notifications to Hubitat app, SMS and or email depending on your notifications devices.
- [ ] Add possibility to go back to manual control for times where internet acces may not be available
- [ ] Menu switch to make Evening settings the same as Morning settings
- [ ] Deal with thermostats that are also controlled via Thermostat Schedular
- [ ] Speach notifications to TTS devices (like SONOS, etc.)



PULL requests will be considered and most of the time implemented as is.

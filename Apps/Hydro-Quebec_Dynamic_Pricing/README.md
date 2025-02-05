# Hydro-Quebec Dynamic Pricing for Hubitat

Hydro-Quebec Dynamic Pricing is a Hubitat App that will fetch Winter Events and setup your house/business as you want during these events and back at the end of the event.

For more info, go to: https://community.hubitat.com/t/release-hydro-quebec-dynamic-pricing/149414


This app will process all known winter events as of winter 2024-2025 and allows to turn off switched devices, turn off thermostats, turn down thermostats, etc. Send notifications of new events, start/end of events. Morning and Evening can have different devices,thermostats controlled. New at version 1.0.0, you can now have pre-event settings (mostly to preheat the house before the event)

Note : If a thermostat is also controlled with the built in app Thermostat Schedular, there might be some problems if a schedule is set to change during the event. This has not been fully tested yet, any feedback is appreciated.


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
  * Separated Morning and Evening settings
  * Cleaned up the settings page with hidable sections
  * Added configurable notifications
  * Added a STOP button, to disable the app when outside the Dynamic Pricing event dates
* 0.3.0 - 2022/02/18 - Second Beta release
  * Added more debug logging
  * Added option to copy over morning to evening options
  * Cleaned up the menus
* 1.0.0 - 2025-01-29 - First public release
  * Now uses the official event JSON from HQ
  * Now supports all available events as of winter 2024-2025
  * Added optional Pre-Event mode for people that want to heat up the house before an event
* 1.0.1 - 2025-01-30 
  * Added a permanent Poll button, this button will poll and re-schedule all upcoming events
  * Changed time of first pole of the day to 13:13:13 because of a situation where HQ updated the events (you will need to go change in the app settings and then hit done for this to take effect)
* 1.0.2 - 2025-01-31
  * Added Triggers, aka switches that turn on during an event to fire other automations.
  * Corrected a bug when first installed and calling a polling method that is no longer available
  * Commented out voice message options as I could not test this anymore, will be back soon as soon as I reinstall my SONOS speaker.
  * Corrected the way pre-events change into events to prevent having some things not return to normal at the end of it all
* 1.0.3 - 2025-02-02
  * Tweaked the code a bit, added a 10 seconds pause between pre-event and event changes to occur
  * Fixed bug with pushing messages to notification devices
  * Fixed bug with trigger switches (switches turned on during events) that were not turning off at the send of an event
  * Added polling start time (you will need to go change in the app settings and then hit done for this to take effect)
  * Added some more trace logging for future features coming up
  * Various other bug squashing
* 1.0.4 - 2025-02-05
  * NEW FEATURE, option to recover after a hub reboot, this will cause all scheduled events to be removed and recheck everything, if within an event period, that event will start immediately
  * If the app is started or a manual poll of the API is done, if we are within an event period, that event period will start immediately
  * More tweaking of the code
  * Various other bug squashing


# ToDo

Things to do in upcoming releases...

- [x] Notifications to Hubitat app, SMS and or email depending on your notifications devices.
- [ ] Add possibility to go back to manual control for times where internet acces may not be available
- [ ] Menu switch to make Evening settings the same as Morning settings
- [ ] Deal with thermostats that are also controlled via Thermostat Schedular
- [ ] Speech notifications to TTS devices (like SONOS, etc.)
- [x] Add hub restart handler to reschedule any on going events and future events


PULL requests are welcome

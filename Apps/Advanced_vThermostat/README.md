# Advanced vThermostat for Hubitat

Advanced vThermostat is a Hubitat Device Type Handler that will help create a proper virtual thermostat device.

It is based on vThermostat by Josh208 (https://github.com/josh208/hubitat/tree/master/vThermostat) that seems to be MIA from the Hubitat community.
Originally ported from the SmartThings-VirtualThermostat-WithDTH (https://github.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH).

For more info, go to: https://community.hubitat.com/t/release-advanced-vthermostat-virtual-thermostat-hvac-control/57633


This device handler allows the creation of a new Device thats shows as a thermostat, using the temperature from selected temperature sensor(s), and on/off outlet(s) for heating and/or cooling.


# Installation

* Import the Parent and Child apps (in that order) in the <> Apps Code section of your Hubitat Hub
* Import the Device driver in the <> Drivers Code section of your Hubitat Hub
* Go to the Apps Section and use the + Add User App button in the top right corner to add the vThermostat Manager, then click DONE
* Go back in the Advanced vThermostat Manager, create a new vThermostat and select your devices and settings, then click DONE
* A child app is created so you can change the devices used at any time for that vThermostat, a new device is also created where you can set it up like any other thermostat

# Compatible 15 amp (1800W) smart plugs

* ZOOZ ZEN15 Z-WAVE PLUS POWER SWITCH
* Aeotec Smart Switch 6 Z-Wave Plus Plug
* Ikea Zigbee smart plug (no power reporting if you need that)
* TP-Link Kasa HS110 Smart Wi-Fi Plug (community driver)
* Enbrighten 55256 Z-Wave Plus Smart Wall Receptacle
* Enerwave ZW15SM-PLUS Z-Wave Plus Wall Switch (Neutral Wire Required)
* Evolve LOM-15TR Z-Wave Wall Receptacle

Enjoy!


# Version History

* 0.1 - Original fork from Josh208
* 0.2 - 
  * Minor bug fixes
  * Code clean up
  * Description change for Hubitat Environement
  * Start commenting what is going on
* 0.3 - 
  * Renamed to reflect all the changes and improvements
  * Bug fixes
  * Code clean up
  * Logging level set by user with auto drop to info level
  * Comments everywhere and added trace logging to help debug
  * Added device command handlers for the ones not yet used
* 0.4 - 
  * More code clean up
  * Bug fixes and typos
  * Command handler fix for setting fan mode
  * Added Celcius support based on hub scale
* 0.5 - 
  * Added code to keep minimum distance between heating and cooling
  * Removed some settings in the device driver not needed
  * Only sends events if the value actually changed to keep events chatter down
* 0.6
  * Emergency stop now will not activate if it's idle, this is safe and more logical
  * If hubs scale is changed from either F or C, using the save command will update temperatures to the new scale
  * When setting up a new vThermosat, thermostat mode is automatically chosen based on heating and cooling outlets choices
* 0.7
  * Small tweaks in the code
  * Bug that made the emergency mode kick in even if in idle


# ToDo

Things to do in upcomming releases...

- [x] Log level preferences (TRACE / DEBUG / INFO / WARN / ERROR / NONE)
- [x] Use hub units to determine C° or F°
- [x] Simplify the settings in the device driver by hiding less common settings if possible and change some attributs with state variables
- [x] Only send Events when an actual change was done to take less resources on the hub and keep event chatter down
- [x] Convert values in the device if the hub scale changes, for now it resets to default values
- [x] Emergency mode will only activate if actually heating or cooling
- [ ] When sensors don't report for the set amount of time (max 180 minutes) it will try to refresh the device to get something going
- [x] Thermostat mode based on choices made in the initial setup or update 
- [ ] When one sensor has not reported for the set amount of time, in a multi sensor setup, it will be "ignored" until it comes back to life
- [ ] Add support for heating and cooling fan (can be the same for both modes)
- [ ] Auto-learning for rooms that heat or cool faster to limit overshoot and undershoot (feature request to be considered)
- [ ] When first installed, should wait a full temp sensor refresh cycle before going to e-stop mode just to give it a chance to get set in it's environment


# Advanced vThermostat for Hubitat

Advanced vThermostat is a Hubitat Device Type Handler that will help create a proper virtual thermostat device.

It is based on vThermostat by Josh208 (https://github.com/josh208/hubitat/tree/master/vThermostat) that seems to be MIA from the Hubitat community.
Originally ported from the SmartThings-VirtualThermostat-WithDTH (https://github.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH). 


This device handler allows the creation of a new Device thats shows as a thermostat, using the temperature from selected temperature sensor(s), and on/off outlet(s) for heating and/or cooling.


# Installation

* Import the Parent and Child apps (in that order) in the <> Apps Code section of your Hubitat Hub
* Import the Device driver in the <> Drivers Code section of your Hubitat Hub
* Go to the Apps Section and use the + Add User App button in the top right corner to add the vThermostat Manager, then click DONE
* Go back in the Advanced vThermostat Manager, create a new vThermostat and select your devices and settings, then click DONE
* A child app is created so you can change the devices used at any time for that vThermostat, a new device is also created where you can set it up like any other thermostat

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


# ToDo

Things to do in upcomming releases...

- [x] Log level preferences (TRACE / DEBUG / INFO / WARN / ERROR / NONE)
- [x] Use hub units to determine C° or F°
- [x] Simplify the settings in the device driver by hiding less common settings if possible and change some attributs with state variables
- [x] Only send Events when an actual change was done to take less resources on the hub and keep event chatter down
- [ ] Convert values in the device if the hub scale changes, for now it resets to default values
- [ ] Emergency mode will only activate if actually heating or cooling
- [ ] When sensors don't report for the set amount of time (max 180 minutes) it will try to refresh the device to get something going
- [ ] When one sensor has not reported for the set amount of time, in a multi sensor setup, it will be "ignored" until it comes back to life
- [ ] Add support for heating and cooling fan (can be the same for both modes)
- [ ] Auto-learning for rooms that heat or cool faster to limit overshoot and undershoot (feature request to be considered)


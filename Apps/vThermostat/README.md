# vThermostat for Hubitat

vThermostat is a Hubitat Device Type Handler that will help create a proper virtual thermostat device.

It is based on vThermostat by Josh208 (https://github.com/josh208/hubitat/tree/master/vThermostat) that seems to be MIA from the Hubitat community.
Originally ported from the SmartThings-VirtualThermostat-WithDTH (https://github.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH). 


This device handler allows the creation of a new Device thats shows as a thermostat, using the temperature from selected temperature sensor(s), and on/off outlet(s) for heating and/or cooling.


# Installation

* Import the Parent and Child apps (in that order) in the <> Apps Code section of your Hubitat Hub
* Import the Device driver in the <> Drivers Code section of your Hubitat Hub.
* Go to the Apps Section and use the + Add User App button in the top right corner to add the vThermostat Manager
* In the vThermostat Manager, create a new vThermostat and select your devices.
* When done, a child app is created so you can change the devices used at any time, and a new device is created where you can set it up like any other thermostat.

Enjoy!


# Version History

* 0.1 - Original fork from Josh208
* 0.2 - Minor bug fixes
  * Code clean up
  * Description change for Hubitat Environement
  * Start commenting what is going on

# ToDo

Things to do in upcomming releases...

- [ ] Log level preferences (TRACE / DEBUG / INFO / WARN / ERROR / NONE)
- [ ] Use hub units to determine C° or F°
- [ ] Simplify the settings in the device driver by hiding less common settings


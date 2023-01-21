# Advanced vThermostat V2 for Hubitat

Advanced vThermostat V2 is a Hubitat Device Type Handler that will create a proper fonctionnal virtual thermostat device.

V2 is a complete re-write using the built in virtual thermostat device for better compatibility with other platforms.
It is based on vThermostat by Josh208 (https://github.com/josh208/hubitat/tree/master/vThermostat) that seems to be MIA from the Hubitat community.
Originally ported from the SmartThings-VirtualThermostat-WithDTH (https://github.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH).

For more info, go to: Hubitat link


This device handler allows the creation of a new vrtual thermostat device thats shows as a thermostat, using the temperature from selected temperature sensor(s), and on/off outlet(s) for heating and/or cooling.


# Installation

* Easiest way is via Hubitat Package Manager

or

* Import the Parent and Child apps (in that order) in the <> Apps Code section of your Hubitat Hub
* Go to the Apps Section and use the + Add User App button in the top right corner to add the vThermostat Manager, then click DONE
* Go back in the Advanced vThermostat Manager, create a new vThermostat and select your devices and settings, then click DONE
* A child app is created so you can change the devices used at any time for that vThermostat, a new device is also created where you can set it up like any other thermostat

# Incomplete compatible 15 amp (1800W) or more smart plugs

* ZOOZ ZEN15 Z-WAVE PLUS POWER SWITCH
* Aeotec Smart Switch 6 Z-Wave Plus Plug
* Ikea Zigbee smart plug (no power reporting if you need that)
* TP-Link Kasa HS110 Smart Wi-Fi Plug (community driver)
* Enbrighten 55256 Z-Wave Plus Smart Wall Receptacle
* Enerwave ZW15SM-PLUS Z-Wave Plus Wall Switch (Neutral Wire Required)
* Evolve LOM-15TR Z-Wave Wall Receptacle
* Shelly 1PM direct wire Wi-Fi (community driver)

Enjoy!


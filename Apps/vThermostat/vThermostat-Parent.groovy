definition(
	name: "vThermostat Manager",
	namespace: "nclark",
	author: "Nelson Clark",
	description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-logo.png",
	importUrl: "https://raw.githubusercontent.com/NelsonClark/Hubitat/main/Apps/vThermostat/vThermostat-Parent.groovy",
	singleInstance: true
)

preferences {
	page(name: "Install", title: "vThermostat Manager", install: true, uninstall: true) {
		section("Devices") {
		}
		section {
			app(name: "thermostats", appName: "vThermostat Child", namespace: "nclark", title: "Add vThermostat", multiple: true)
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
}

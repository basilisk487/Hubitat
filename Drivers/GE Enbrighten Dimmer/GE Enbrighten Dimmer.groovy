/**
 *  IMPORT URL: https://raw.githubusercontent.com/Botched1/Hubitat/master/Drivers/GE%20Enbrighten%20Dimmer/GE%20Enbrighten%20Dimmer.groovy
 *
 *  GE Enbrighten Z-Wave Plus Dimmer
 *
 *  1.0.0 (07/16/2019) - Initial Version
 *  1.1.0 (07/17/2019) - Removed doubletap from BasicSet, added DoubleTap UP/DOWN and TripleTap UP/DOWN as standard buttons 1-4
 *  1.2.0 (12/15/2019) - <retracted>
 *  1.3.0 (12/15/2019) - Fixed event generation when dimmer is in SWITCH mode, removed some unnecessary Get commands to reduce zwave traffic
 *  1.4.0 (02/07/2020) - Added pushed, held, and released capability. Required renumbering the buttons. Now 1/2=Up/Down, 3/4=Double Up/Down, 5/6=Triple Up/Down
 *  1.4.1 (02/07/2020) - Added doubleTapped events and added doubleTap capability. Now users can use button 3/4 for double tap or the system "doubleTapped" events.
 *  1.5.0 (05/17/2020) - Added associations and inverted paddle options
 *  2.0.0b (08/07/2020) - Added S2 capability for Hubitat 2.2.3 and newer
 *  2.1.0  (08/20/2020) - Fixed some command version issues
 *  2.2.0  (08/29/2020) - Added number of button config to configure
 *  2.3.0  (12/15/2020) - Added state for defaultDimmerLevel
 *  2.4.0  (02/13/2021) - Added Alternate Exclusion mode to preferences.
 *  2.5.0  (03/10/2021) - Fixed redundant ON events when changing dimmer level
 *  2.5.1  (03/08/2022) - Added setIndicatorBehavior command. Now users can control LED indicator behavior through custom actions.
*/

import groovy.transform.Field

@Field static Map commandClassVersions = 
    [
        0x26: 3    //switchMultiLevel
        ,0x70: 2    //configuration
        ,0x72: 2    //Manufacturer Specific
        ,0x85: 2    //association
	,0x86: 3    //Version
]

metadata {
	definition (name: "GE Enbrighten Z-Wave Plus Dimmer", namespace: "Botched1", author: "Jason Bottjen") {
		capability "Actuator"
        capability "DoubleTapableButton"
        capability "HoldableButton"
		capability "PushableButton"
        capability "ReleasableButton"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
		capability "Light"

		command "setDefaultDimmerLevel", [[name:"Default ON %",type:"NUMBER", description:"Default Dimmer Level Used when Turning ON. (0=Last Dimmer Value)", range: "0..99"]]
		command "setIndicatorBehavior", [[name:"LED Indicator Behavior", type: "ENUM", description: "", constraints: ["LED ON When Switch OFF", "LED ON When Switch ON", "LED Always OFF", "LED Always ON", "0", "1", "2", "3"] ] ]
	}

 preferences {
        input name: "paramLED", type: "enum", title: "LED Indicator Behavior", multiple: false, options: ["0" : "LED ON When Switch OFF (default)", "1" : "LED ON When Switch ON", "2" : "LED Always OFF", "3" : "LED Always ON"], required: false, displayDuringSetup: true
        input name: "paramInverted", type: "enum", title: "Switch Buttons Direction", multiple: false, options: ["0" : "Normal (default)", "1" : "Inverted"], required: false, displayDuringSetup: true
        input name: "paramUpDownRate", type: "enum", title: "Adjust the speed at which the ramps to a specific value", multiple: false, options: ["0" : "Fast (default)", "1" : "Slow"], required: false, displayDuringSetup: true
        input name: "paramSwitchMode", type: "enum", title: "Turn your dimmer into an On/Off switch", multiple: false, options: ["0" : "OFF (default)", "1" : "ON"], required: false, displayDuringSetup: true
		input name: "paramAlternateExclusion", type: "enum", title: "Enable alternate exclusion (2-ON, 2-OFF). NOTE: Turning this ON also makes device report status 2-3x faster (firmware bug?)!", multiple: false, options: ["0" : "OFF (default)", "1" : "ON"], required: false, displayDuringSetup: true
		//input name: "paramMinDim", type: "number", title: "Minimum Dim Threshold", multiple: false, defaultValue: "1", range: "1..99", required: false, displayDuringSetup: true
        //input name: "paramMaxDim", type: "number", title: "Maximum Dim Threshold", multiple: false, defaultValue: "99", range: "0..99", required: false, displayDuringSetup: true     
        input name: "paramDefaultDim", type: "number", title: "Default ON % (Default=0=Last %)", multiple: false, defaultValue: "0", range: "0..99", required: false, displayDuringSetup: true
        input name: "requestedGroup2", type: "text", title: "Association Group 2 Members (Max of 5):", required: false
        input name: "requestedGroup3", type: "text", title: "Association Group 3 Members (Max of 4):", required: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false	
	    input name: "logDesc", type: "bool", title: "Enable descriptionText logging", defaultValue: true	
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Parse
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void parse(String description){
    if (logEnable) log.debug "parse description: ${description}"
    hubitat.zwave.Command cmd = zwave.parse(description,commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Z-Wave Messages
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
String secure(String cmd){
    return zwaveSecureEncap(cmd)
}

String secure(hubitat.zwave.Command cmd){
    return zwaveSecureEncap(cmd)
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd){
    hubitat.zwave.Command encapCmd = cmd.encapsulatedCommand(commandClassVersions)
    
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }
    sendHubCommand(new hubitat.device.HubAction(secure(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)), hubitat.device.Protocol.ZWAVE))
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    if (logEnable) log.debug "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This report does nothing in this driver, and shouldn't have been called..."
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    if (logEnable) log.debug "---BASIC SET V1--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This does nothing in this driver, and can be ignored..."	
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
	if (logEnable) log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	sendEvent(name: "numberOfButtons", value: 6, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			delayBetween([
                secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format())
			    ,secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
                ] ,250)
        }
    }
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	if (logEnable) log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    if (logEnable) log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This report does nothing in this driver, and shouldn't have been called..."    
}

def zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	if (logEnable) log.debug "---VERSION REPORT V3--- ${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
	if (logEnable) log.debug "Hail command received..."
	if (logEnable) log.debug "This does nothing in this driver, and shouldn't have been called..."
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Device Specific Report: ${cmd}"
    switch (cmd.deviceIdType) {
        case 1:
            // serial number
            def serialNumber=""
            if (cmd.deviceIdDataFormat==1) {
                cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
            } else {
                cmd.deviceIdData.each { serialNumber += (char) it }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break
    }
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	if (logEnable) log.debug "---SwitchMultilevelReport V3---  ${device.displayName} sent ${cmd}"
	def newType
	
	// check state.bin variable to see if event is digital or physical
	if (state.bin == -1)
	{
		newType = "digital"
	}
	else
	{
		newType = "physical"
	}
	
	// Reset state.bin variable
	state.bin = 0
	
	if (cmd.value) {
		// Set switch status
        if (device.currentValue("switch") == "off") {
			if (logDesc) log.info "$device.displayName was turned on [$newType]"
			sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName was turned on [$newType]", type: "$newType")
		}

		// Send level event
		sendEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "$device.displayName is " + cmd.value + "%", type: "$newType")

		// Update state.level
		state.level = cmd.value

		// info logging
		if (logDesc) log.info "$device.displayName is " + cmd.value + "%"

	} else {
		sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName was turned off [$newType]", type: "$newType")
		if (logDesc) log.info "$device.displayName was turned off [$newType]"
	}
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	if (logEnable) log.debug "SwitchMultilevelSet V3 Called."
	if (logEnable) log.debug "This does nothing in this driver, and shouldn't have been called..."
}

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
	if (logEnable) log.debug "CentralSceneNotification V1 Called."
    
    def result = []
    
    // Single Tap Up
    if ((cmd.keyAttributes == 0) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Physical ON Triggered"
		if (logDesc) log.info "$device.displayName turned on [physical]"
        result << sendEvent([name: "pushed", value: 1, descriptionText: "$device.displayName had Up Pushed (button 1) [physical]", type: "physical", isStateChange: true])
    }
    // Single Tap Down
    if ((cmd.keyAttributes == 0) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Physical OFF Triggered"
		if (logDesc) log.info "$device.displayName turned off [physical]"
        result << sendEvent([name: "pushed", value: 2, descriptionText: "$device.displayName had Down Pushed (button 2) [physical]", type: "physical", isStateChange: true])
    }
    // Released Up
    if ((cmd.keyAttributes == 1) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Up Released Triggered"
        if (logDesc) log.info "$device.displayName had Up Released (button 1) [physical]"
        result << sendEvent([name: "released", value: 1, descriptionText: "$device.displayName had Up Released (button 1) [physical]", type: "physical", isStateChange: true])
    }
    // Released Down
    if ((cmd.keyAttributes == 1) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Down Released Triggered"
        if (logDesc) log.info "$device.displayName had Down Released (button 2) [physical]"
        result << sendEvent([name: "released", value: 2, descriptionText: "$device.displayName had Down Released (button 2) [physical]", type: "physical", isStateChange: true])
    }
    // Held Up
    if ((cmd.keyAttributes == 2) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Up Held Triggered"
		if (logDesc) log.info "$device.displayName had Up Held (button 1) [physical]"
		result << sendEvent([name: "held", value: 1, descriptionText: "$device.displayName had Up held (button 1) [physical]", type: "physical", isStateChange: true])
    }
    // Held Down
    if ((cmd.keyAttributes == 2) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Down Held Triggered"
		if (logDesc) log.info "$device.displayName had Down Held (button 2) [physical]"
		result << sendEvent([name: "held", value: 2, descriptionText: "$device.displayName had Up held (button 2) [physical]", type: "physical", isStateChange: true])
    }        
    // Double Tap Up
    if ((cmd.keyAttributes == 3) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Double Tap Up Triggered"
		if (logDesc) log.info "$device.displayName had Doubletap up (button 3) [physical]"
		result << sendEvent([name: "pushed", value: 3, descriptionText: "$device.displayName had Doubletap up (button 3) [physical]", type: "physical", isStateChange: true])
        result << sendEvent([name: "doubleTapped", value: 1, descriptionText: "$device.displayName had Doubletap up (doubleTapped 1) [physical]", type: "physical", isStateChange: true])
    }
    // Double Tap Down
    if ((cmd.keyAttributes == 3) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Double Tap Down Triggered"
		if (logDesc) log.info "$device.displayName had Doubletap down (button 4) [physical]"
		result << sendEvent([name: "pushed", value: 4, descriptionText: "$device.displayName had Doubletap down (button 4) [physical]", type: "physical", isStateChange: true])
        result << sendEvent([name: "doubleTapped", value: 2, descriptionText: "$device.displayName had Doubletap down (doubleTapped 2) [physical]", type: "physical", isStateChange: true])
    }
    // Triple Tap Up
    if ((cmd.keyAttributes == 4) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Triple Tap Up Triggered"
		if (logDesc) log.info "$device.displayName had Tripletap up (button 5) [physical]"
		result << sendEvent([name: "pushed", value: 5, descriptionText: "$device.displayName had Tripletap up (button 5) [physical]", type: "physical", isStateChange: true])
    }
    // Triple Tap Down
    if ((cmd.keyAttributes == 4) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Triple Tap Down Triggered"
		if (logDesc) log.info "$device.displayName had Tripletap down (button 6) [physical]"
		result << sendEvent([name: "pushed", value: 6, descriptionText: "$device.displayName had Tripletap down (button 6) [physical]", type: "physical", isStateChange: true])
    }

    return result
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Driver Commands / Functions
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def on() {
	if (logEnable) log.debug "Turn device ON"
	if (logEnable) log.debug "state.level is $state.level"
	if (state.level == 0 || state.level == "" || state.level == null) {state.level=99}
	setLevel(state.level, 0)
    
}

def off() {
	if (logEnable) log.debug "Turn device OFF"
	setLevel(0, 0)
}

def setLevel(value) {
	if (logEnable) log.debug "setLevel($value)"
	setLevel(value, 0)
}

def setLevel(value, duration) {
	if (logEnable) log.debug "setLevel($value, $duration)"
	def result = []
    state.bin = -1
	value = Math.max(Math.min(value.toInteger(), 99), 0)
	
	if (value) {state.level = value}

	if (logEnable) log.debug "setLevel(value, duration) >> value: $value, duration: $duration"

    return delayBetween([
            secure(zwave.switchMultilevelV3.switchMultilevelSet(value: value, dimmingDuration: duration))
        ] , 250)
}

def setDefaultDimmerLevel(value) {
	if (logEnable) log.debug "Setting default dimmer level: ${value}"
	
	value = Math.max(Math.min(value.toInteger(), 99), 0)
	state.defaultDimmerLevel = value
	sendEvent([name:"defaultDimmerLevel", , descriptionText: "$device.displayName defaultDimmerLevel set to $value%", value: value, displayed:true])
	
	return delayBetween([
        secure(zwave.configurationV1.configurationSet(scaledConfigurationValue: value , parameterNumber: 32, size: 1).format())
        ,secure(zwave.configurationV1.configurationGet(parameterNumber: 32).format())
        ], 250)
}

def refresh() {
	log.info "refresh() is called"
	
    delayBetween([
        secure(zwave.switchMultilevelV3.switchMultilevelGet())
        ] ,250)
}

def installed() {
	configure()
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)

    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def cmds = []
    
	// Associations    
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format())

	def nodes = []
	nodes = parseAssocGroupList(settings.requestedGroup2, 2)
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format())
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format())
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
    state.currentGroup2 = settings.requestedGroup2
	
   	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format())
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format())
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
    state.currentGroup3 = settings.requestedGroup3
	
	// Set LED param
	if (paramLED==null) {
		paramLED = 0
	}	
	cmds << secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramLED.toInteger(), parameterNumber: 3, size: 1).format())
	cmds << secure(zwave.configurationV2.configurationGet(parameterNumber: 3).format())

	// Set Inverted param
	if (paramInverted==null) {
		paramInverted = 0
	}
	cmds << secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramInverted.toInteger(), parameterNumber: 4, size: 1).format())
	cmds << secure(zwave.configurationV2.configurationGet(parameterNumber: 4).format())
	
	// Set Ramp Rate
	if (paramUpDownRate==null) {
		paramUpDownRate = 0
	}	
	cmds << secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramUpDownRate.toInteger(), parameterNumber: 6, size: 1).format())
	cmds << secure(zwave.configurationV2.configurationGet(parameterNumber: 6).format())

   	// Set Switch Mode
	if (paramSwitchMode==null) {
		paramSwitchMode = 0
	}	
	cmds << secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramSwitchMode.toInteger(), parameterNumber: 16, size: 1).format())
	cmds << secure(zwave.configurationV2.configurationGet(parameterNumber: 16).format())

   	// Set Alternate Exclusion Mode
	if (paramAlternateExclusion==null) {
		paramAlternateExclusion = 0
	}	
	cmds << secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramAlternateExclusion.toInteger(), parameterNumber: 19, size: 1).format())
	cmds << secure(zwave.configurationV2.configurationGet(parameterNumber: 19).format())
	
    /* Comment out for now, as it is unclear how/if this actually works
   	// Set Minimum Dim %
	if (paramMinDim==null) {
		paramMinDim = 1
	}	
	cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramMinDim.toInteger(), parameterNumber: 30, size: 1).format()
	cmds << zwave.configurationV2.configurationGet(parameterNumber: 30).format()

   	// Set Maximum Dim %
	if (paramMaxDim==null) {
		paramMaxDim = 99
	}	
	cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramMaxDim.toInteger(), parameterNumber: 31, size: 1).format()
	cmds << zwave.configurationV2.configurationGet(parameterNumber: 31).format()
    */
    
   	// Set Default Dim %
	if (paramDefaultDim==null) {
		paramDefaultDim = 0
    }	
	cmds << secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramDefaultDim.toInteger(), parameterNumber: 32, size: 1).format())
	cmds << secure(zwave.configurationV2.configurationGet(parameterNumber: 32).format())

    delayBetween(cmds, 500)
}

def configure() {
	log.info "configure triggered"
	state.bin = -1
	if (state.level == "") {state.level = 99}
	def cmds = []

	// configure buttons
	sendEvent(name: "numberOfButtons", value: 6, displayed: false)
	
	// Associations    
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format())

	def nodes = []
	nodes = parseAssocGroupList(settings.requestedGroup2, 2)
	cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format())
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
	state.currentGroup2 = settings.requestedGroup2
	
   	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
	cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format())
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
	state.currentGroup3 = settings.requestedGroup3

	delayBetween(cmds, 500)
}

def setIndicatorBehavior(param) {
    log.info "setting indicator behavior to ${param}"
	def paramValue = 0
    switch (param) {
        case "LED ON When Switch OFF":
            paramValue = 0
            break
        case "LED ON When Switch ON":
            paramValue = 1
            break
        case "LED Always OFF":
            paramValue = 2
            break
        case "LED Always ON":
            paramValue = 3
            break
        case "0":
        case "1":
        case "2":
        case "3":
            paramValue = param.toInteger()
            break
    }
    device.updateSetting("paramLED", [type: "enum", value: paramValue.toString()])
	delayBetween([
        secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: paramValue, parameterNumber: 3, size: 1).format()),
        secure(zwave.configurationV2.configurationGet(parameterNumber: 3).format())
    ], 250)
}

private parseAssocGroupList(list, group) {
    def nodes = group == 2 ? [] : [zwaveHubNodeId]
    if (list) {
        def nodeList = list.split(',')
        def max = group == 2 ? 5 : 4
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                log.warn "Association Group ${group}: Too many members. Greater than ${max}! This one was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	log.warn "Association Group ${group}: Adding the hub ID as an association is not allowed."
                }
                else 
				if ( (nodeId > 0) & (nodeId < 256) ) {
                    nodes << nodeId
                    count++
                }
                else {
                    log.warn "Association Group ${group}: Invalid member: ${node}"
                }
            }
            else {
                log.warn "Association Group ${group}: Invalid member: ${node}"
            }
        }
    }
	if (logEnable) log.debug "Nodes is $nodes"
    return nodes
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

package com.example.bleapp

class Defines {

    companion object {

        //Request Codes
        var SELECT_DEVICE_REQUEST_CODE = 1
        var BLUETOOTH_PERMISSIONS_REQUEST_CODE = 2
        var LOCATION_PERMISSIONS_REQUEST_CODE = 3


        //Bluetooth Data Requests
        var DEVICE_NAME_REQUEST = 0
        var MODEL_NUMBER_REQUEST = 1
        var FIRMWARE_REVISION_REQUEST = 2


        //Bluetooth Services
        var GENERIC_ACCESS_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"

        var DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
        var APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb"
        var PERIPHERAL_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb"
        var CENTRAL_ADDRESS_RESOLUTION = "00002aa6-0000-1000-8000-00805f9b34fb" //unconfirmed



        var GENERIC_ATTRIBUTE_SERVICE = "00001801-0000-1000-8000-00805f9b34fb"

        var SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb"



        var X = "0000fe59-0000-1000-8000-00805f9b34fb"

        var SECURE_BUTTONLESS_DFU = "8ec90004-f315-4f60-9fb8-838830daea50" //unconfirmed



        var LED_SERVICE = "e97dd91d-251d-470a-a062-fa1922dfa9a8" //unconfirmed

        var XX = "e97d3b10-251d-470a-a062-fa1922dfa9a8"



        var DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"

        var MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
        var SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb"
        var FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"



        var EVENT_SERVICE = "e95d93af-251d-470a-a062-fa1922dfa9a8"

        var MICROBIT_EVENT = "e95d9775-251d-470a-a062-fa1922dfa9a8"
        var CLIENT_EVENT = "e95d5404-251d-470a-a062-fa1922dfa9a8"
        var CLIENT_REQUIREMENTS = "e95d23c4-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_REQUIREMENTS = "e95db84c-251d-470a-a062-fa1922dfa9a8"



        var BUTTON_SERVICE = "e95d9882-251d-470a-a062-fa1922dfa9a8"

        var BUTTON_A = "e95dda90-251d-470a-a062-fa1922dfa9a8"
        var BUTTON_B = "e95dda91-251d-470a-a062-fa1922dfa9a8"



        var LED_SERVICE_2 = "e95dd91d-251d-470a-a062-fa1922dfa9a8"

        var LED_MATRIX_STATE = "e95d7b77-251d-470a-a062-fa1922dfa9a8"
        var LED_TEXT = "e95d93ee-251d-470a-a062-fa1922dfa9a8"
        var LED_SCROLLING_DELAY = "e95d0d2d-251d-470a-a062-fa1922dfa9a8"


        //not relevant for now
        /*
        var TEMPERATURE_SERVICE = "e95d6100-251d-470a-a062-fa1922dfa9a8"

        var TEMPERATURE = "e95d9250-251d-470a-a062-fa1922dfa9a8"
        var TEMPERATURE_PERIOD = "e95d1b25-251d-470a-a062-fa1922dfa9a8"
        */
    }
}
package com.example.bleapp

class Defines {

    companion object {

        // Request Codes
        var SELECT_DEVICE_REQUEST_CODE = 1
        var BLUETOOTH_PERMISSIONS_REQUEST_CODE = 2
        var LOCATION_PERMISSIONS_REQUEST_CODE = 3



        // micro:bit Defines -----------------------------------------------------------------------

        // Data Requests
        var MICROBIT_DEVICE_NAME_REQUEST = 0
        var MICROBIT_MODEL_NUMBER_REQUEST = 1
        var MICROBIT_FIRMWARE_REVISION_REQUEST = 2
        var MICROBIT_WRITE_LED_TEXT_REQUEST = 3
        var MICROBIT_WRITE_LED_REQUEST = 4


        // Services
        var MICROBIT_GENERIC_ACCESS_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"

        var MICROBIT_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
        var MICROBIT_APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb"
        var MICROBIT_PERIPHERAL_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb"
        var MICROBIT_CENTRAL_ADDRESS_RESOLUTION = "00002aa6-0000-1000-8000-00805f9b34fb" //unconfirmed


        var MICROBIT_GENERIC_ATTRIBUTE_SERVICE = "00001801-0000-1000-8000-00805f9b34fb"

        var MICROBIT_SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb"


        var MICROBIT_X_SERVICE = "0000fe59-0000-1000-8000-00805f9b34fb"

        var MICROBIT_SECURE_BUTTONLESS_DFU = "8ec90004-f315-4f60-9fb8-838830daea50" //unconfirmed


        var MICROBIT_LED_SERVICE_UNCONFIRMED = "e97dd91d-251d-470a-a062-fa1922dfa9a8" //unconfirmed

        var MICROBIT_X_CHARACTERISTIC = "e97d3b10-251d-470a-a062-fa1922dfa9a8"


        var MICROBIT_DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb"

        var MICROBIT_MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
        var MICROBIT_SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb"
        var MICROBIT_FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"


        var MICROBIT_EVENT_SERVICE = "e95d93af-251d-470a-a062-fa1922dfa9a8"

        var MICROBIT_MICROBIT_EVENT = "e95d9775-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_CLIENT_EVENT = "e95d5404-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_CLIENT_REQUIREMENTS = "e95d23c4-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_MICROBIT_REQUIREMENTS = "e95db84c-251d-470a-a062-fa1922dfa9a8"


        var MICROBIT_BUTTON_SERVICE = "e95d9882-251d-470a-a062-fa1922dfa9a8"

        var MICROBIT_BUTTON_A = "e95dda90-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_BUTTON_B = "e95dda91-251d-470a-a062-fa1922dfa9a8"


        var MICROBIT_LED_SERVICE = "e95dd91d-251d-470a-a062-fa1922dfa9a8"

        var MICROBIT_LED_MATRIX_STATE = "e95d7b77-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_LED_TEXT = "e95d93ee-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_LED_SCROLLING_DELAY = "e95d0d2d-251d-470a-a062-fa1922dfa9a8"


        // not relevant for now
        var MICROBIT_TEMPERATURE_SERVICE = "e95d6100-251d-470a-a062-fa1922dfa9a8"

        var MICROBIT_TEMPERATURE = "e95d9250-251d-470a-a062-fa1922dfa9a8"
        var MICROBIT_TEMPERATURE_PERIOD = "e95d1b25-251d-470a-a062-fa1922dfa9a8"



        // Inkbird Defines -------------------------------------------------------------------------

        // Data Requests
        var INKBIRD_NAME_REQUEST = 5

        // Services (unfortunately most of the Read Services send encrypted data)
        var INKBIRD_GENERIC_ACCESS_SERVICE = "00001800-0000-1000-8000-00805f9b34fb"

        var INKBIRD_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
        var INKBIRD_X = "00002a01-0000-1000-8000-00805f9b34fb"
        var INKBIRD_XX = "00002a04-0000-1000-8000-00805f9b34fb"


        var INKBIRD_WRITE_X_SERVICE = "00001801-0000-1000-8000-00805f9b34fb"

        var INKBIRD_XXX = "00002a05-0000-1000-8000-00805f9b34fb"


        var INKBIRD_DEVICE_INFORMATION_SERVICE_UNCONFIRMED = "0000180a-0000-1000-8000-00805f9b34fb"

        var INKBIRD_XXXX = "00002a23-0000-1000-8000-00805f9b34fb"
        var INKBIRD_SOFTWARE_VERSION_UNCONFIRMED = "00002a26-0000-1000-8000-00805f9b34fb"


        var INKBIRD_WRITE_XX_SERVICE = "0000ffc0-0000-1000-8000-00805f9b34fb"

        var INKBIRD_XXXXX = "0000ffc1-0000-1000-8000-00805f9b34fb"
        var INKBIRD_XXXXXX = "0000ffc2-0000-1000-8000-00805f9b34fb"


        var INKBIRD_X_SERVICE = "0000ff90-0000-1000-8000-00805f9b34fb"

        var INKBIRD_DEVICE_NAME_2 = "0000ff91-0000-1000-8000-00805f9b34fb"
        var INKBIRD_TEMPERATURE_ENCRYPTED_UNCONFIRMED = "0000ff92-0000-1000-8000-00805f9b34fb"
        var INKBIRD_CO2_ENCRYPTED_UNCONFIRMED = "0000ff93-0000-1000-8000-00805f9b34fb"
        var INKBIRD_XXXXXXXXX = "0000ff94-0000-1000-8000-00805f9b34fb"
        var INKBIRD_HUMIDITY_ENCRYPTED_UNCONFIRMED = "0000ff95-0000-1000-8000-00805f9b34fb"
        var INKBIRD_DEVICE_VERSION = "0000ff96-0000-1000-8000-00805f9b34fb"
        var INKBIRD_XXXXXXXXXX = "0000ff97-0000-1000-8000-00805f9b34fb"
        var INKBIRD_DEVICE_NAME_AND_OTHER_UNCONFIRMED = "0000ff98-0000-1000-8000-00805f9b34fb"


        var INKBIRD_WRITE_XXX_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb"

        var INKBIRD_XXXXXXXXXXX = "0000ffe9-0000-1000-8000-00805f9b34fb"
        var INKBIRD_XXXXXXXXXXXX = "0000ffe4-0000-1000-8000-00805f9b34fb"


        var INKBIRD_WRITE_XXXX_SERVICE = "5833ff01-9b8b-5191-6142-22a4536ef123"

        var INKBIRD_XXXXXXXXXXXXX = "5833ff02-9b8b-5191-6142-22a4536ef123"
        var INKBIRD_XXXXXXXXXXXXXX = "5833ff03-9b8b-5191-6142-22a4536ef123"
    }
}
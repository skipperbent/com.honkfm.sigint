## SIGINT

![Screenshot](/screenshot_1.png?raw=true "Screenshot")

Android application for pulling sensor information in real-time (every 100ms with WiFi at 500ms) into a CSV file with a optional note field for biological/senses and/or field observations.
Runs as a background service and can be safely used with the screen on or off.

Thanks to field agent: `loosh_station_signature_0xA3_29`, `Icons8` & others.
> SIGINT OPERATOR LOG
> SIGINT_SESSION_TOKEN: [A3-29_VLG_LOG_OS_v4_CORTSELITZE_INIT]
> AUTH_HASH: 74-65-72-72-79-5f-6c-69-76-65-73

### Purpose
Find anomalies in your local area. Turns your spy-device against the spies and reveals the cracks in The Truman Show...
Or just check your home appliances for electromagnetic energy or signals :)

### Usage
Do a sensor calibration upon opening the app by moving the phone in a figure 8 repeatedly.
When `GPS_STATUS` changes to `ACQUIRED` click the `INITIATE_SCAN` button to begin logging. 
Hold your phone upright throughout the entire scan for most accurate results.
If you do not need the to watch the sensor info in real-time or to save battery, close the screen and place it upright in your bag-pocket.

#### Spot the anomalies
- `GPS_STATUS` SUDDENLY SWITCHING TO `SEARCHING`
- `CELL_NEIGHBOR_COUNT` jumps or disappears.
- `EMF_ANOMALY_DELTA` goes above `5 µT`.
- Sudden jumps or drops in `BATTERY_TEMP`.
- `WIFI_COUNT` goes above 0 in a remote area.

**Note:** If any of the criteria above are met, it is recommended to stop immediately and let it scan for 30-seconds to gather more intelligence about the anomaly.

## Data stored in CSV
- timestamp
- lat
- lon
- alt
- gps_accuracy
- emf_raw_x
- emf_raw_y
- emf_raw_z
- emf_total
- emf_accuracy
- emf_expected_strength
- emf_anomaly_delta
- declination_err
- cell_type
- cell_rf_cn
- cell_rsrp
- cell_rsrq
- cell_rssi
- cell_neighbor_count
- barometer_pa
- battery_temp
- wifi_count
- wifi_main_ssid
- note

## Permissions required
- `ACCESS_FINE_LOCATION`
- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_STATE`
- `READ_PHONE_STATE`
- `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `POST_NOTIFICATIONS`

## Requirements
- Minimum SDK: 29
- Target SDK: 34

Only tested on Pixel phones. Experiences may vary.

## Copyright
Take it, share it, break it license (c) 2026.
package com.honkfm.sensordump

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.IBinder
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sqrt

class SigintService : Service(), SensorEventListener, LocationListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val wifiServiceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var sensorManager: SensorManager
    private var logFile: File? = null
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager

    private var emfAccuracy = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    private var currentLoc: android.location.Location? = null
    private var wifiMainSsid = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE // Critical flag for Pixel 9a
        )

        val notification = NotificationCompat.Builder(this, "sigint_channel")
            .setContentTitle("SIGINT ACTIVE")
            .setContentText("Logging EMF in The Void...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Turn into background service
        startForeground(1, notification)

        // GPS Request
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL)

        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // OKAAAY LETS GOOO
        AppState.setIsLogging(true)

        startNewLog()
        dumpWifiData()

        serviceScope.launch {
            while (isActive) {
                performFullDump()
                Log.d("SIGINT", "Adding line to CSV")

                delay(100)
            }
        }

        wifiServiceScope.launch {
            while (isActive) {
                dumpWifiData()
                Log.d("SIGINT", "Logging WiFi signals")
                delay(500)
            }
        }

        // Start sticky so the system will restart if service is killed
        return START_STICKY
    }

    private fun dumpWifiData() {
        // Grab WiFi data
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            val wifiManager = baseContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.scanResults
            AppState.setWifiCount(wifiInfo.size)
            wifiMainSsid = wifiManager.connectionInfo.ssid ?: "NONE"
        }
    }

    override fun onLocationChanged(location: android.location.Location) {
        currentLoc = location
    }

    private fun startNewLog() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        //val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        logFile = File(baseContext.filesDir, "SIGINT_DUMP_$timeStamp.csv")
        val headerTxt =
            "timestamp,lat,lon,alt,gps_accuracy,emf_raw_x,emf_raw_y,emf_raw_z,emf_total,emf_accuracy,emf_expected_strength,emf_anomaly_delta,declination_err,cell_type,cell_rf_cn,cell_rsrp,cell_rsrq,cell_rssi,cell_neighbor_count,barometer_pa,battery_temp,wifi_count,wifi_main_ssid,note\n"
        logFile?.appendText(headerTxt)
        AppState.addLine(headerTxt.length.toLong())
    }

    private fun performFullDump() {
        // Refresh if currentLoc is null
        if (currentLoc == null && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            currentLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val ts = isoFormat.format(Date())

        // Cell Data & neighbors
        var cellType = "UNKNOWN"
        var rfCn = 0
        var rsrp = 0
        var rsrq = 0
        var rssi = 0

        AppState.setCellNeighborCount(0)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            telephonyManager.requestCellInfoUpdate(
                mainExecutor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<android.telephony.CellInfo>) {
                        // Wake up...
                    }
                })


            val cellInfos = telephonyManager.allCellInfo

            AppState.setCellNeighborCount(cellInfos.size - 1)

            Log.d("SIGINT", "Cells found: ${cellInfos.size}")

            for (info in cellInfos) {
                if (info.isRegistered) {
                    if (info is CellInfoLte) {
                        cellType = "LTE"
                        rfCn = info.cellIdentity.earfcn
                        rsrp = info.cellSignalStrength.dbm
                        rsrq = info.cellSignalStrength.rsrq
                        rssi = info.cellSignalStrength.rssi
                    } else if (info is android.telephony.CellInfoNr) {
                        cellType = "5G_NR"
                        val nr = info.cellSignalStrength as android.telephony.CellSignalStrengthNr
                        rsrp = nr.ssRsrp
                        rsrq = nr.ssRsrq
                        rssi = nr.dbm
                    }
                }
            }
        }

        val lat = currentLoc?.latitude ?: 0.0
        val lon = currentLoc?.longitude ?: 0.0
        val alt = currentLoc?.altitude ?: 0.0
        val acc = currentLoc?.accuracy ?: 0.0f

        val safeNote = "\"" + AppState.pendingNote.value.replace("\"", "'") + "\""
        AppState.setPendingNote("")

        val batteryStatus =
            baseContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        AppState.setBatteryTemperature(
            (batteryStatus?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                0
            ) ?: 0) / 10.0
        )

        val geoField = GeomagneticField(
            lat.toFloat(),
            lon.toFloat(),
            alt.toFloat(),
            System.currentTimeMillis()
        )

        // 2. Get systems signal strength (UT)
        val emfExpectedStrength = geoField.fieldStrength / 1000.0f

        // 3. Calculate DELTA (anomaly!)
        AppState.setEmfAnomalyDelta(AppState.totalEmf.value - emfExpectedStrength);

        val expectedDec = geoField.declination // Current declination
        val orientation = FloatArray(3)
        val fa = FloatArray(9)

        SensorManager.getOrientation(fa, orientation)
        val measuredAzimuth = Math.toDegrees(orientation[0].toDouble())

        // declination_error (simplified for finding anomalies):
        val declinationError = measuredAzimuth - expectedDec

        val line =
            "$ts,$lat,$lon,$alt,$acc,$lastX,$lastY,$lastZ,${AppState.totalEmf.value},$emfAccuracy,$emfExpectedStrength,${AppState.emfAnomalyDelta.value},$declinationError,$cellType,$rfCn,$rsrp,$rsrq,$rssi,${AppState.callNeighborCell.value},${AppState.barometerPa.value},${AppState.batteryTemperature.value},${AppState.wifiCount.value},$wifiMainSsid,$safeNote\n"

        // Force write
        try {
            logFile?.appendText(line)
            android.media.MediaScannerConnection.scanFile(
                this,
                arrayOf(logFile?.absolutePath),
                null,
                null
            )
            AppState.addLine(line.length.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            lastX = event.values[0]
            lastY = event.values[1]
            lastZ = event.values[2]
            emfAccuracy = event.accuracy;

            AppState.setTotalEmf(sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ))
        }

        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            AppState.setBarometerPa(event.values[0])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        AppState.setIsLogging(false)
        serviceScope.cancel()
        wifiServiceScope.cancel()
    }

}
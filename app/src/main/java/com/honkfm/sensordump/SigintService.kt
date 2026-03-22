package com.honkfm.sensordump

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
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

class SigintService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var sensorManager: SensorManager
    private var logFile: File? = null
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE // Critical flag for Pixel 9a
        )

        val notification = NotificationCompat.Builder(this, "sigint_channel")
            .setContentTitle("SIGINT Aktiv")
            .setContentText("Logging EMF in The Void...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Turn into background service
        startForeground(1, notification)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL)

        AppState.setIsLogging(true)

        startNewLog()

        serviceScope.launch {
            // 'isActive' er indbygget i CoroutineScope
            while (isActive) {
                performFullDump()
                Log.d("SIGINT", "Adding line to CSV")

                delay(2000) // Svarer til postDelayed, men blokerer ikke tråden
            }
        }

        // Start sticky so the system will restart if it gets killed
        return START_STICKY
    }

    private fun startNewLog() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        //val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        logFile = File(baseContext.filesDir, "SIGINT_DUMP_$timeStamp.csv")
        logFile?.appendText("timestamp,lat,lon,alt,gps_accuracy,emf_raw_x,emf_raw_y,emf_raw_z,emf_total,cell_type,cell_rf_cn,cell_rsrp,cell_rsrq,cell_rssi,cell_neighbor_count,barometer_pa,note\n")
    }

    private fun performFullDump() {

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val ts = isoFormat.format(Date())

        // GPS & Barometer
        val loc = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else null

        telephonyManager.requestCellInfoUpdate(
            mainExecutor,
            object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: MutableList<android.telephony.CellInfo>) {
                    // Vi behøver ikke gøre noget her, det "vækker" bare listen
                }
            })

        val lat = loc?.latitude ?: 0.0
        val lon = loc?.longitude ?: 0.0
        val alt = loc?.altitude ?: 0.0
        val acc = loc?.accuracy ?: 0.0f

        // Cell Data & Naboer
        var cellType = "UNKNOWN";
        var rfCn = 0;
        var rsrp = 0;
        var rsrq = 0;
        var rssi = 0;
        var neighbors = 0

        val cellInfos = telephonyManager.allCellInfo
        neighbors = cellInfos.size - 1 // Alle dem vi ikke er forbundet til

        Log.d("SIGINT", "Antal celler fundet: ${cellInfos.size}")

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
                    rsrp = nr.ssRsrp // Signalstyrke for 5G
                    rsrq = nr.ssRsrq // Kvalitet for 5G (Vigtigst!)
                    rssi = nr.dbm    // Samlet styrke
                }
            }
        }

        val baro = 0.0f // Kan tilføjes hvis din tlf har tryksensor

        // Inde i performFullDump() før du laver din "line" streng:
        val safeNote = "\"" + AppState.pendingNote.value.replace("\"", "'") + "\""
        AppState.setPendingNote("")

        val line =
            "$ts,$lat,$lon,$alt,$acc,$lastX,$lastY,$lastZ,${AppState.totalEmf.value},$cellType,$rfCn,$rsrp,$rsrq,$rssi,$neighbors,$baro,$safeNote\n"

        // VIGTIGT: Skriv og gennemtving gemning
        try {
            logFile?.appendText(line)
            // Fortæl Android at filen er opdateret (så den ses i Device Explorer)
            android.media.MediaScannerConnection.scanFile(
                this,
                arrayOf(logFile?.absolutePath),
                null,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Den vigtige del: Saml netværks-data i din sensor-loop:
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            lastX = event.values[0]
            lastY = event.values[1]
            lastZ = event.values[2]

            AppState.setTotalEmf(sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        AppState.setIsLogging(false)
        serviceScope.cancel()
    }

}
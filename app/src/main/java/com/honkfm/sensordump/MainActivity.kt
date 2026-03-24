package com.honkfm.sensordump

// SIGINT_SESSION_TOKEN: [A3-29_VLG_LOG_OS_v4_CORTSELITZE_INIT]
// AUTH_HASH: 74-65-72-72-79-5f-6c-69-76-65-73

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity(), LocationListener {

    private var gpsAcquired by mutableStateOf(false)

    enum class TabItem(val label: String, val icon: ImageVector) {
        Scanner("SCAN", Icons.Default.PlayArrow),
        Files("FILES", Icons.Default.List)
    }

    val TacticalGreen = Color(0xFF00FF41) // Matrix Green
    val TacticalBackground = Color(0xFF0D0D0D) // Black
    val TacticalRed = Color(0xFFFF0055)

    @Composable
    fun ScannerScreen() {
        val isLoggingState by AppState.isLogging.collectAsState()
        var tempNote by remember { mutableStateOf("") }
        val currentEmf by AppState.totalEmf.collectAsState()
        val wifiCount by AppState.wifiCount.collectAsState()
        val emfAnomalyDelta by AppState.emfAnomalyDelta.collectAsState()
        val batteryTemperature by AppState.batteryTemperature.collectAsState()
        val callNeighborCell by AppState.callNeighborCell.collectAsState()
        val scanRuntime by AppState.scanRunTime.collectAsState()
        val scanLines by AppState.scanLines.collectAsState()
        val scanSizeBytes by AppState.scanSize.collectAsState()
        val barometerPa by AppState.barometerPa.collectAsState()

        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "> SIGINT_OPERATOR_LOG v${BuildConfig.VERSION_NAME}",
                color = TacticalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoggingState) {

                Text(
                    text = "EMF_FIELD_STRENGTH",
                    color = TacticalGreen.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                Text(
                    text = "${currentEmf.toInt()} uT",
                    color = when {
                        currentEmf > 700f -> Color.Magenta // Din "Smoking Gun"
                        currentEmf > 200f -> TacticalRed
                        else -> TacticalGreen
                    },
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                // Scanning animation / divider
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = TacticalGreen,
                    trackColor = Color.Transparent
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "> ENTER_ANOMALY_LOG_ENTRY",
                    color = TacticalGreen.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .align(Alignment.Start)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TacticalGreen, RectangleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = tempNote,
                        onValueChange = { tempNote = it },
                        textStyle = TextStyle(
                            color = TacticalGreen, // Neon text
                            fontFamily = FontFamily.Monospace, // Terminal-vibe
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(TacticalGreen), // Green marker
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None
                        ),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LOG_NOTE: ",
                                    color = TacticalGreen.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalGreen,
                        contentColor = Color.Black
                    ),
                    onClick = {
                        AppState.setPendingNote(tempNote)
                        tempNote = ""
                    }) {
                    Text(
                        text = "ADD NOTE",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {

                val drawable =
                    packageManager.getApplicationIcon(context.applicationInfo.packageName)
                Image(
                    drawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
                    contentDescription = "Image",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(8.dp)

                )
            }

            val gpsStatus = when {
                gpsAcquired -> "ACQUIRED"
                else -> "SEARCHING"
            }

            if (!isLoggingState) {
                Text(
                    text = "LOG_STATUS: INACTIVE\nGPS_STATUS: $gpsStatus",
                    color = TacticalGreen.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .align(Alignment.Start)
                )
            } else {

                Text(
                    text = "LOG_STATUS: ACTIVE (${DateUtils.formatElapsedTime(scanRuntime / 60 / 60 / 60 / 60 / 60)})\n            ${
                        Formatter.formatShortFileSize(
                            context,
                            scanSizeBytes
                        )
                    } / ${scanLines} LINES\nGPS_STATUS: $gpsStatus\nWIFI_COUNT: $wifiCount\nCELL_NEIGHBOR_COUNT: $callNeighborCell\nEMF_ANOMALY_DELTA: $emfAnomalyDelta µT\nBAROMETER_HPA: $barometerPa\nBATTERY_TEMP: $batteryTemperature°C",
                    color = TacticalGreen.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .align(Alignment.Start)
                )
            }

            Button(
                onClick = {
                    if (!isLoggingState) {
                        val intent = Intent(context, SigintService::class.java)
                        context.startForegroundService(intent)

                        Toast.makeText(context, "Mission started!", Toast.LENGTH_SHORT).show()
                    } else {
                        context.stopService(Intent(context, SigintService::class.java))
                        Toast.makeText(context, "Mission completed!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoggingState) TacticalRed else TacticalGreen,
                    contentColor = Color.Black
                ),
                shape = RectangleShape, // Squares looks total mjilitjær
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text(
                    text = if (isLoggingState) "ABORT_MISSION" else "INITIATE_SCAN",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    @Composable
    fun FileItem(file: java.io.File, onDelete: () -> Unit) {
        val context = LocalContext.current

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clickable { shareFile(context, file.name) }
                .semantics { contentDescription = "loosh_station_signature_0xA3_29" },
            shape = RectangleShape,
            border = BorderStroke(0.5.dp, TacticalGreen.copy(alpha = 0.3f)), // En tynd "grid" kant
            colors = CardDefaults.cardColors(containerColor = Color.Black),

            ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically

            ) {

                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = TacticalGreen.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(18.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name.uppercase(),
                        color = TacticalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SIZE: ${file.length() / 1024} KB | TYPE: SIGINT_CSV",
                        color = TacticalGreen.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                // DELETE
                IconButton(onClick = {
                    if (file.delete()) {
                        onDelete()
                        Toast.makeText(context, "Evidence destroyed!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = TacticalRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    private fun shareFile(context: Context, fileName: String) {
        try {
            val file = java.io.File(context.filesDir, fileName)

            if (!file.exists()) {
                Log.e("SIGINT", "ERROR: File does not exist: ${file.absolutePath}")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Sheets / MiX
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Gmail / QuickShare
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Start intent
            val chooser =
                Intent.createChooser(sendIntent, "SELECT_ACTION: ${file.name.uppercase()}")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))

            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("SIGINT", "FileProvider error: ${e.message}")
        }
    }

    @Composable
    fun FileListScreen() {
        val context = LocalContext.current

        val files = remember {
            val files = context.filesDir.listFiles()
                ?.filter { it.extension == "csv" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()

            mutableStateListOf<java.io.File>().apply { addAll(files) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 13.dp, bottom = 16.dp)
        ) {

            if (files.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 8.dp),
                ) {

                    Text(
                        "> NO LOGS FOUND...",
                        color = TacticalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )

                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(files) { file ->
                        FileItem(
                            file = file,
                            onDelete = {
                                files.remove(file)
                            }
                        )
                    }
                }
            }
        }
    }

    private lateinit var locationManager: LocationManager

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                ), 1
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                    ), 1
                )
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        }

        val name = "SIGINT Scanner"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel("sigint_channel", name, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        super.onCreate(savedInstanceState)

        setContent {

            // States
            val pagerState = rememberPagerState(pageCount = { TabItem.entries.size })
            val scope = rememberCoroutineScope()

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.Black,
                        tonalElevation = 0.dp
                    ) {
                        TabItem.entries.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontFamily = FontFamily.Monospace, // Terminal font!
                                        fontSize = 13.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TacticalGreen,
                                    selectedTextColor = TacticalGreen,
                                    unselectedIconColor = TacticalGreen.copy(alpha = 0.4f),
                                    unselectedTextColor = TacticalGreen.copy(alpha = 0.4f),
                                    indicatorColor = TacticalGreen.copy(alpha = 0.1f)
                                ),
                                icon = { Icon(item.icon, contentDescription = null) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TacticalBackground,
                ) {
                    // Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.padding(innerPadding)
                    ) { pageIndex ->
                        when (TabItem.entries[pageIndex]) {
                            TabItem.Scanner -> ScannerScreen()
                            TabItem.Files -> FileListScreen()
                        }
                    }
                }
            }
        }

        // Keep display on (for viewing EMF, forces user to close screen manually)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onLocationChanged(p0: Location) {
        gpsAcquired = true
    }

}
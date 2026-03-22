package com.honkfm.sensordump

// SIGINT_SESSION_TOKEN: [A3-29_VLG_LOG_OS_v4_CORTSELITZE_INIT]
// AUTH_HASH: 74-65-72-72-79-5f-6c-69-76-65-73

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
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


class MainActivity : ComponentActivity() {

    enum class TabItem(val label: String, val icon: ImageVector) {
        Scanner("SCAN", Icons.Default.PlayArrow),
        Files("FILES", Icons.Default.List)
    }

    val TacticalGreen = Color(0xFF00FF41) // Klassisk Matrix Grøn
    val TacticalBackground = Color(0xFF0D0D0D) // Næsten kulsort
    val TacticalRed = Color(0xFFFF0055)

    @Composable
    fun ScannerScreen() {
        val isLoggingState by AppState.isLogging.collectAsState()
        var tempNote by remember { mutableStateOf("") }
        val currentEmf by AppState.totalEmf.collectAsState()
        val context = LocalContext.current

        // Vi pakker alt ind i en kulsort Surface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Overskrift med Terminal-vibe
            Text(
                text = "> SIGINT_OPERATOR_LOG",
                color = TacticalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoggingState) {
                // EMF Displayet skal føles som en sensor-måling
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
                    fontSize = 64.sp, // Store tal for hurtig aflæsning
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                // En lille "scanning" animation eller divider
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

                // Vi pakker BasicTextField ind i en boks med en grøn kant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TacticalGreen, RectangleShape) // Firkantet kant
                        .background(Color.Black.copy(alpha = 0.5f)) // Lidt gennemsigtig baggrund
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = tempNote,
                        onValueChange = { tempNote = it },
                        textStyle = TextStyle(
                            color = TacticalGreen, // Neon grøn tekst
                            fontFamily = FontFamily.Monospace, // Terminal-vibe
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(TacticalGreen), // Grøn markør
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None
                        ),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Vi tilføjer lige markøren foran din tekst
                                Text(
                                    text = "LOG_NOTE: ",
                                    color = TacticalGreen.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                innerTextField() // Her kommer din rå tekst ind
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
                        .size(150.dp) //Optional, but keeps the image reasonably small
                        .padding(8.dp)

                )
            }

            // Dine knapper skal også være taktiske
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
                shape = RectangleShape, // Firkanter ser mere militære ud
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

            // Tilføj eventuelt små "system-logs" nederst på skærmen
            if (isLoggingState) {
                Text(
                    text = "SYSTEM_STATUS: LOGGING_ACTIVE\nSTORAGE: INTERNAL_STORAGE/FILES\nGPS: ACQUIRED",
                    color = TacticalGreen.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .align(Alignment.Start)
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
            shape = RectangleShape, // FIRKANTET! Ingen bløde hjørner her
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

                // SLET-KNAP
                IconButton(onClick = {
                    if (file.delete()) {
                        onDelete() // Fjern fra UI listen med det samme
                        Toast.makeText(context, "EVIDENCE SHREDDED", Toast.LENGTH_SHORT).show()
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

    private fun shareFile(context: Context, fileName: String) { // Vi sender kun NAVNET nu
        try {
            // Vi skaber fil-objektet helt frisk ud fra appens nuværende filesDir
            val file = java.io.File(context.filesDir, fileName)

            if (!file.exists()) {
                Log.e("SIGINT", "FEJL: Filen findes ikke: ${file.absolutePath}")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.honkfm.sensordump.fileprovider",
                file
            )

            // 1. Lav VIEW intent (til Sheets / MiX / Viewers)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 2. Lav SEND intent (til Gmail / QuickShare / Discord)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 3. Den taktiske vælger: Vi bruger SEND som base,
            // men tilføjer VIEW som et alternativt valg i menuen
            val chooser =
                Intent.createChooser(sendIntent, "SELECT_ACTION: ${file.name.uppercase()}")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))

            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("SIGINT", "FileProvider fejl: ${e.message}")
        }
    }

    @Composable
    fun FileListScreen() {
        val context = LocalContext.current

        val files = remember {
            val files = context.filesDir.listFiles()
                ?.filter { it.extension == "csv" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()

            // Vi pakker de fundne filer ind i en 'mutableStateList'
            mutableStateListOf<java.io.File>().apply { addAll(files) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 13.dp, bottom = 16.dp)
        ) {

            if (files.isEmpty()) {
                Text(
                    "> NO LOGS FOUND...",
                    color = TacticalGreen,
                    fontFamily = FontFamily.Monospace, // Terminal font!
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(files) { file ->
                        FileItem(
                            file = file,
                            onDelete = {
                                // 3. Her fjerner vi den fra vores StateList
                                files.remove(file)
                            }
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
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

        val name = "SIGINT Scanner"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel("sigint_channel", name, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        super.onCreate(savedInstanceState)

        setContent {

            // 1. Statestyring
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
                                // Her tjekker vi pagerState i stedet for en manuel variabel
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    // Når man klikker på en tab, animerer vi pageren hen til siden
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
                                    indicatorColor = TacticalGreen.copy(alpha = 0.1f) // Den lille cirkel bag ikonet
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
                    // Her sker magien!
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.padding(innerPadding)
                    ) { pageIndex ->
                        // Her bestemmer vi, hvad der skal vises på hver "side"
                        when (TabItem.entries[pageIndex]) {
                            TabItem.Scanner -> ScannerScreen()
                            TabItem.Files -> FileListScreen()
                        }
                    }
                }
            }
        }
    }

}
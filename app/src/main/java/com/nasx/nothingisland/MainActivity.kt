
package com.nasx.nothingisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasx.nothingisland.ui.theme.NothingIslandTheme

private val Background = Color(0xFF08090B)
private val IslandBlack = Color(0xFF000000)
private val Accent = Color(0xFFD5FF00)
private val CardColor = Color(0xFF17191D)
private val SecondaryText = Color(0xFF92969E)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NothingIslandTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    NothingIslandHome()
                }
            }
        }
    }
}

@Composable
fun NothingIslandHome() {

    var expanded by remember { mutableStateOf(false) }
    var selectedFeature by remember { mutableStateOf("music") }
    var musicPlaying by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val notifications = NotificationStore.notifications

    var batteryLevel by remember { mutableIntStateOf(0) }
    var batteryStatus by remember { mutableStateOf("Checking...") }
    var isCharging by remember { mutableStateOf(false) }

    // Real battery monitoring
    DisposableEffect(context) {

        fun updateBattery(intent: Intent?) {
            if (intent == null) return

            val level = intent.getIntExtra(
                BatteryManager.EXTRA_LEVEL, -1
            )

            val scale = intent.getIntExtra(
                BatteryManager.EXTRA_SCALE, -1
            )

            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100) / scale
            }

            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS, -1
            )

            isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

            batteryStatus = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Fully charged"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
                else -> "Battery status unavailable"
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                updateBattery(intent)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        updateBattery(context.registerReceiver(receiver, filter))

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {

        Text(
            "NOTHING ISLAND",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Version 6.5 • Smart Controls",
            color = SecondaryText,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            "LIVE ISLAND",
            color = SecondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(IslandBlack)
                .clickable { expanded = !expanded }
                .animateContentSize()
                .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Accent)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        "Nothing Island",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    if (expanded) "−" else "+",
                    color = Accent,
                    fontSize = 25.sp
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(18.dp))

                    when (selectedFeature) {

                        "music" -> MusicIsland(musicPlaying) {
                            musicPlaying = !musicPlaying
                        }

                        "notifications" -> NotificationIsland(
                            notifications = notifications,
                            onEnable = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                    )
                                )
                            },
                            onClear = {
                                NotificationStore.clear()
                            },
                            onDismiss = { notification ->
                                NotificationStore.remove(
                                    notification.packageName,
                                    notification.title
                                )
                            },
                            onOpen = { notification ->
                                openNotificationApp(
                                    context,
                                    notification.packageName
                                )
                            }
                        )

                        "battery" -> BatteryIsland(
                            batteryLevel,
                            batteryStatus,
                            isCharging
                        )

                        "map" -> MapIsland()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            "ISLAND FEATURES",
            color = SecondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureCard(
            "♫",
            "Music Player",
            "Media controls and playback",
            selectedFeature == "music"
        ) {
            selectedFeature = "music"
            expanded = true
        }

        FeatureCard(
            "◉",
            "Notifications",
            "${notifications.size} live notifications",
            selectedFeature == "notifications"
        ) {
            selectedFeature = "notifications"
            expanded = true
        }

        FeatureCard(
            "ϟ",
            "Battery Status",
            "Live battery monitoring",
            selectedFeature == "battery"
        ) {
            selectedFeature = "battery"
            expanded = true
        }

        FeatureCard(
            "⌖",
            "Map Island",
            "Navigation and route previews",
            selectedFeature == "map"
        ) {
            selectedFeature = "map"
            expanded = true
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            "DESIGNED FOR NOTHING OS",
            color = SecondaryText,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// Open the application that sent the notification
fun openNotificationApp(
    context: Context,
    packageName: String
) {
    try {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun NotificationAppIcon(packageName: String) {

    val context = LocalContext.current

    val iconBitmap = remember(packageName) {
        try {
            val drawable =
                context.packageManager.getApplicationIcon(packageName)

            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap.asImageBitmap(),
            contentDescription = "Application icon",
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF292D31)),
            contentAlignment = Alignment.Center
        ) {
            Text("●", color = Accent, fontSize = 20.sp)
        }
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {

    val width = if (drawable.intrinsicWidth > 0) {
        drawable.intrinsicWidth.coerceAtMost(192)
    } else {
        96
    }

    val height = if (drawable.intrinsicHeight > 0) {
        drawable.intrinsicHeight.coerceAtMost(192)
    } else {
        96
    }

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)

    return bitmap
}

@Composable
fun MusicIsland(
    playing: Boolean,
    onToggle: () -> Unit
) {
    Column {

        Text(
            "NOW PLAYING",
            color = Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "Nothing But Music",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Demo Artist • Demo Album",
            color = SecondaryText,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(15.dp))

        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black
            )
        ) {
            Text(if (playing) "Ⅱ Pause Music" else "▶ Play Music")
        }
    }
}

@Composable
fun NotificationIsland(
    notifications: List<IslandNotification>,
    onEnable: () -> Unit,
    onClear: () -> Unit,
    onDismiss: (IslandNotification) -> Unit,
    onOpen: (IslandNotification) -> Unit
) {

    Column {

        Text(
            "LIVE NOTIFICATIONS",
            color = Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onEnable,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black
            )
        ) {
            Text("Notification Access Settings")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (notifications.isEmpty()) {

            Text(
                "No notifications received yet.",
                color = SecondaryText,
                fontSize = 13.sp
            )

        } else {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    "${notifications.size} notifications",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onClear) {
                    Text("Clear All", color = Accent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = notifications,
                    key = {
                        it.packageName + it.title
                    }
                ) { notification ->

                    NotificationCard(
                        notification = notification,
                        onOpen = {
                            onOpen(notification)
                        },
                        onDismiss = {
                            onDismiss(notification)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: IslandNotification,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {

        NotificationAppIcon(notification.packageName)

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpen() }
        ) {

            Text(
                notification.appName,
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                notification.title.ifBlank { "Notification" },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                notification.message,
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                "Open app",
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "×",
            color = SecondaryText,
            fontSize = 22.sp,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun BatteryIsland(
    level: Int,
    status: String,
    charging: Boolean
) {

    Column {

        Text(
            "BATTERY STATUS",
            color = Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                "$level%",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                if (charging) "⚡" else "●",
                color = Accent,
                fontSize = 22.sp
            )
        }

        Text(status, color = SecondaryText, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(15.dp))

        LinearProgressIndicator(
            progress = {
                (level / 100f).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = Accent,
            trackColor = Color.DarkGray
        )

        Text(
            "Live device battery information",
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
fun MapIsland() {

    Column {

        Text(
            "MAP ISLAND",
            color = Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "⌖ Navigation Preview",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Sample Route → Destination",
            color = SecondaryText,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "2.4 km • 8 min",
            color = Accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Illustrative demo route, not live GPS.",
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
fun FeatureCard(
    symbol: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) Color(0xFF242820)
                else CardColor
            )
            .clickable(onClick = onClick)
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            symbol,
            color = Accent,
            fontSize = 23.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Text(
                description,
                color = SecondaryText,
                fontSize = 12.sp
            )
        }

        if (selected) {
            Text("●", color = Accent, fontSize = 10.sp)
        }
    }
}

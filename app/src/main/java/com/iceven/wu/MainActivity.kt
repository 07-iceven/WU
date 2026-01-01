package com.iceven.wu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.iceven.wu.ui.theme.WuTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "已授 通知权限", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "被拒 通知权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            WuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F3)
                ) {
                    MainScreen()
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showList by remember { mutableStateOf(false) }

    // Unified colors
    val backgroundColor = Color(0xFFF5F5F3)
    val textColor = Color(0xFF424242)

    if (showList) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 使用自定义 Row 代替 TopAppBar，以获得完全的布局控制权，
            // 确保与主页的 padding (通常为 24.dp) 和高度一致。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), // 使用 24dp 边距以匹配主页风格
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "记录",
                    fontFamily = FontFamily.Serif,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp // 增大字号以匹配主页标题视觉
                )
                IconButton(onClick = { showList = !showList }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = textColor
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NotificationList()
            }
        }
    } else {
        NotificationScheduler(onToggleList = { showList = !showList })
    }
}


@Composable
fun NotificationList() {
    val context = LocalContext.current
    val storage = remember { NotificationStorage(context) }
    var notifications by remember { mutableStateOf(storage.getNotifications()) }

    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "无",
                fontFamily = FontFamily.Serif,
                color = Color.Gray,
                fontSize = 28.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(notifications) { notification ->
                NotificationItem(
                    notification = notification,
                    onDelete = {
                        storage.removeNotification(notification.id)
                        cancelNotification(context, notification.id)
                        notifications = storage.getNotifications()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: ScheduledNotification,
    onDelete: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val isExpired = currentTime > notification.timeInMillis
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val textColor = Color(0xFF424242)
    val accentColor = Color(0xFFA63430)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    textDecoration = if (isExpired) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isExpired) Color.Gray else textColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateFormat.format(Date(notification.timeInMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    textDecoration = if (isExpired) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isExpired) Color.Gray else textColor.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "删除", 
                    tint = accentColor
                )
            }
        }
    }
}

fun cancelNotification(context: Context, id: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScheduler(onToggleList: () -> Unit) {
    var message by remember { mutableStateOf("") }
    
    // Initialize with current time/date
    val calendar = Calendar.getInstance()
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // UI Colors
    val textColor = Color(0xFF424242)
    val accentColor = Color(0xFFA63430) // Deep red
    val backgroundColor = Color(0xFFF5F5F3)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        val current = System.currentTimeMillis()
                        // Check if the selected date is in the past (before today's midnight)
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selected }
                        val currentCal = Calendar.getInstance().apply { timeInMillis = current }
                        
                        // Compare year and day of year to see if it's strictly before today
                        if (selectedCal.get(Calendar.YEAR) < currentCal.get(Calendar.YEAR) ||
                            (selectedCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                             selectedCal.get(Calendar.DAY_OF_YEAR) < currentCal.get(Calendar.DAY_OF_YEAR))) {
                            
                            // It is in the past, reset to today
                            selectedDateMillis = current
                            Toast.makeText(context, "悟已往之不谏，知来者之可追。", Toast.LENGTH_SHORT).show()
                        } else {
                            selectedDateMillis = selected
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        CustomTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                val currentCal = Calendar.getInstance()
                val selectedCal = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }

                if (selectedCal.timeInMillis < currentCal.timeInMillis) {
                     // It is in the past, reset to current time + 1 minute (or just current)
                     selectedHour = currentCal.get(Calendar.HOUR_OF_DAY)
                     selectedMinute = currentCal.get(Calendar.MINUTE)
                     Toast.makeText(context, "悟已往之不谏，知来者之可追。", Toast.LENGTH_SHORT).show()
                } else {
                    selectedHour = hour
                    selectedMinute = minute
                }
                showTimePicker = false
            }
        )
    }

    // Helper to calculate time remaining
    val triggerTime = remember(selectedDateMillis, selectedHour, selectedMinute) {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = selectedDateMillis
        
        val localCalendar = Calendar.getInstance()
        localCalendar.set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
        localCalendar.set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
        localCalendar.set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
        localCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        localCalendar.set(Calendar.MINUTE, selectedMinute)
        localCalendar.set(Calendar.SECOND, 0)
        localCalendar.set(Calendar.MILLISECOND, 0)
        localCalendar.timeInMillis
    }

    val timeRemainingString = remember(triggerTime) {
        val diff = triggerTime - System.currentTimeMillis()
        if (diff > 0) {
            val oneDayMillis = 24 * 60 * 60 * 1000L
            if (diff >= oneDayMillis) {
                val days = diff / oneDayMillis
                "${days}天后 提醒"
            } else {
                val totalMinutes = (diff + 59999) / 60000
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                
                if (hours > 0) {
                    "${hours}时${minutes}分后 提醒"
                } else {
                    "${minutes}分后 提醒"
                }
            }
        } else {
            "此刻"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp)
    ) {
        // Top Row: Vertical Text and Menu Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Vertical Text
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Left Column: 不负本心
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf("不", "负", "本", "心").forEach { char ->
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            color = textColor,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                // Right Column: 独寐寤言
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf("独", "寐", "寤", "言").forEach { char ->
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            color = textColor,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Menu Icon
            IconButton(onClick = onToggleList) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Center Content: Input and Date/Time
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Message Input
            TextField(
                value = message,
                onValueChange = { message = it },
                placeholder = {
                    Text(
                        "在此，留言。",
                        color = Color.Gray.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 24.sp
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    color = textColor,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(top = 8.dp),
                thickness = 1.dp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Date Display
            val date = Date(selectedDateMillis)
            val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            Text(
                text = dateFormat.format(date),
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                color = textColor,
                modifier = Modifier.clickable { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time Display
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                fontSize = 64.sp,
                fontFamily = FontFamily.Serif,
                color = textColor,
                modifier = Modifier.clickable { showTimePicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Remaining Time
            Text(
                text = timeRemainingString,
                fontSize = 18.sp,
                color = accentColor.copy(alpha = 0.8f),
                fontFamily = FontFamily.Serif
            )
        }

        // Bottom Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp).size(96.dp)
                .clip(CircleShape)
                .background(accentColor)
                .clickable {
                    if (message.isNotEmpty()) {
                        if (triggerTime > System.currentTimeMillis()) {
                            scheduleNotification(context, message, triggerTime)
                        } else {
                            Toast.makeText(context, "悟已往之不谏，知来者之可追。", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "请键入内容。", Toast.LENGTH_SHORT).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "封",
                fontSize = 40.sp,
                color = Color.White,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }

    }
}

fun scheduleNotification(context: Context, message: String, triggerTime: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            Toast.makeText(context, "请许 精确闹钟权限", Toast.LENGTH_LONG).show()
            return
        }
    }

    // Min SDK is 24, so Build.VERSION_CODES.M (23) check is redundant
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            Toast.makeText(context, "请许 后台活动 以保通知正常", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
             e.printStackTrace()
        }
    }

    // Pass the trigger time to the receiver as well
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("message", message)
        putExtra("timestamp", triggerTime)
    }
    
    // Use a unique request code so that multiple alarms can be scheduled
    val requestCode = System.currentTimeMillis().toInt()
    
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        
        // Save to storage
        val storage = NotificationStorage(context)
        storage.saveNotification(ScheduledNotification(requestCode, triggerTime, message))
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Toast.makeText(context, "已存，将于 ${dateFormat.format(Date(triggerTime))} 启信", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
         Toast.makeText(context, "权限错误: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择时间", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Picker
                WheelPicker(
                    count = 24,
                    initialIndex = selectedHour,
                    label = { "%02d".format(it) },
                    onSelectionChanged = { selectedHour = it },
                    modifier = Modifier.width(60.dp)
                )
                
                Text(
                    text = ":", 
                    style = MaterialTheme.typography.headlineMedium, 
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Minute Picker
                WheelPicker(
                    count = 60,
                    initialIndex = selectedMinute,
                    label = { "%02d".format(it) },
                    onSelectionChanged = { selectedMinute = it },
                    modifier = Modifier.width(60.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    count: Int,
    initialIndex: Int,
    label: (Int) -> String,
    onSelectionChanged: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current
    
    // Update selection when scrolling stops
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index in 0 until count) {
                     onSelectionChanged(index)
                     // Haptic feedback
                     view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
    }

    val itemHeight = 40.dp
    val visibleItems = 3
    
    Box(modifier = modifier.height(itemHeight * visibleItems), contentAlignment = Alignment.Center) {
        // Selection Indicator
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {}
        
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected by remember { derivedStateOf { listState.firstVisibleItemIndex == index } }
                    Text(
                        text = label(index),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

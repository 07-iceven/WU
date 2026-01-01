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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
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
            Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "通知权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏系统栏
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
            val themeStorage = rememberThemeStorage()
            val useDarkTheme = shouldUseDarkTheme(themeStorage)
            WuTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(themeStorage)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(themeStorage: ThemeStorage) {
    var showList by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // 将消息状态提升至此，以便在切换屏幕时保留
    var message by remember { mutableStateOf("") }

    // 统一颜色
    val textColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background

    // 加载纹理图片
    val texture = ImageBitmap.imageResource(id = R.drawable.texture)
    
    // 创建一个重复纹理的画笔
    val textureBrush = remember(texture) {
        ShaderBrush(
            ImageShader(
                image = texture,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }

    val isDarkTheme = shouldUseDarkTheme(themeStorage)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor)) {
        // 使用纹理平铺的背景，在深色模式下稍微降低不透明度或混合
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = textureBrush,
                alpha = if (isDarkTheme) 0.1f else 1.0f
            )
        }

        if (showSettings) {
            SettingsScreen(
                themeStorage = themeStorage,
                onBack = {
                showSettings = false
                showList = false 
            })
        } else if (showList) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // 透明背景，以便纹理透出
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "设置",
                                tint = textColor
                            )
                        }
                        IconButton(onClick = { showList = !showList }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = textColor
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NotificationList()
                }
            }
        } else {
            NotificationScheduler(
                onToggleList = { showList = !showList },
                message = message,
                onMessageChange = { message = it }
            )
        }
    }
}

@Composable
fun SettingsScreen(themeStorage: ThemeStorage, onBack: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val themeMode by themeStorage.themeMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 透明背景
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "设置",
                fontFamily = FontFamily.Serif,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "返回",
                    tint = textColor
                )
            }
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            // 外观设置
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            ThemeOption(
                text = "浅色",
                selected = themeMode == AppThemeMode.LIGHT,
                onClick = { themeStorage.setThemeMode(AppThemeMode.LIGHT) }
            )
            ThemeOption(
                text = "深色",
                selected = themeMode == AppThemeMode.DARK,
                onClick = { themeStorage.setThemeMode(AppThemeMode.DARK) }
            )
            ThemeOption(
                text = "跟随系统",
                selected = themeMode == AppThemeMode.SYSTEM,
                onClick = { themeStorage.setThemeMode(AppThemeMode.SYSTEM) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 关于
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            AboutLink()
        }
    }
}

@Composable
fun AboutLink() {
    val context = LocalContext.current
    val url = "https://github.com/07-iceven/WU"
    val textColor = MaterialTheme.colorScheme.onBackground
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "开源地址",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f),
                fontFamily = FontFamily.Serif
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "打开链接",
            tint = textColor.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = textColor.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontFamily = FontFamily.Serif
        )
    }
}


@Composable
fun NotificationList() {
    val context = LocalContext.current
    val storage = remember { NotificationStorage(context) }
    var notifications by remember { mutableStateOf(storage.getNotifications()) }
    val textColor = MaterialTheme.colorScheme.onBackground

    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无内容",
                fontFamily = FontFamily.Serif,
                color = textColor.copy(alpha = 0.5f),
                fontSize = 18.sp
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
    
    // 使用 MaterialTheme 颜色
    val textColor = MaterialTheme.colorScheme.onSurface
    val cardColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
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
                    color = if (isExpired) textColor.copy(alpha = 0.5f) else textColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateFormat.format(Date(notification.timeInMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    textDecoration = if (isExpired) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isExpired) textColor.copy(alpha = 0.5f) else textColor.copy(alpha = 0.7f)
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
fun NotificationScheduler(
    onToggleList: () -> Unit,
    message: String,
    onMessageChange: (String) -> Unit
) {
    // 使用当前时间/日期初始化
    val calendar = Calendar.getInstance()
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // UI 颜色
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary
    
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
                        // 检查选定日期是否为过去（今天午夜之前）
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selected }
                        val currentCal = Calendar.getInstance().apply { timeInMillis = current }
                        
                        // 比较年份和一年中的天数，看看是否严格早于今天
                        if (selectedCal.get(Calendar.YEAR) < currentCal.get(Calendar.YEAR) ||
                            (selectedCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                             selectedCal.get(Calendar.DAY_OF_YEAR) < currentCal.get(Calendar.DAY_OF_YEAR))) {
                            
                            // 是过去的时间，重置为今天
                            selectedDateMillis = current
                            Toast.makeText(context, "悟已往之不谏，知来者之可追", Toast.LENGTH_SHORT).show()
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
                     // 是过去的时间，重置为当前时间 + 1 分钟（或者直接当前时间）
                     selectedHour = currentCal.get(Calendar.HOUR_OF_DAY)
                     selectedMinute = currentCal.get(Calendar.MINUTE)
                     Toast.makeText(context, "悟已往之不谏，知来者之可追", Toast.LENGTH_SHORT).show()
                } else {
                    selectedHour = hour
                    selectedMinute = minute
                }
                showTimePicker = false
            }
        )
    }

    // 计算剩余时间的辅助函数
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
            .padding(24.dp)
    ) {
        // 顶部行：垂直文本和菜单图标
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 垂直文本
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 左列：不负本心
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
                // 右列：独寐寤言
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

            // 菜单图标
            IconButton(onClick = onToggleList) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 中心内容：输入和日期/时间
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 消息输入
            TextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = {
                    Text(
                        "在此，留言。",
                        color = textColor.copy(alpha = 0.5f),
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
                color = textColor.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 日期显示
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

            // 时间显示
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute),
                fontSize = 64.sp,
                fontFamily = FontFamily.Serif,
                color = textColor,
                modifier = Modifier.clickable { showTimePicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 剩余时间
            Text(
                text = timeRemainingString,
                fontSize = 18.sp,
                color = accentColor.copy(alpha = 0.8f),
                fontFamily = FontFamily.Serif
            )
        }

        // 底部按钮
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
            Toast.makeText(context, "请允许设置精确闹钟权限", Toast.LENGTH_LONG).show()
            return
        }
    }

    // 最低 SDK 为 24，因此 Build.VERSION_CODES.M (23) 检查是多余的
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            Toast.makeText(context, "请允许后台活动以确保通知正常", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
             e.printStackTrace()
        }
    }

    // 同样将触发时间传递给接收器
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("message", message)
        putExtra("timestamp", triggerTime)
    }
    
    // 使用唯一的请求代码，以便可以安排多个闹钟
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
        
        // 保存到存储
        val storage = NotificationStorage(context)
        storage.saveNotification(ScheduledNotification(requestCode, triggerTime, message))
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Toast.makeText(context, "已封存，将于 ${dateFormat.format(Date(triggerTime))} 启信", Toast.LENGTH_SHORT).show()
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
                // 小时选择器
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

                // 分钟选择器
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
    
    // 停止滚动时更新选择
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index in 0 until count) {
                     onSelectionChanged(index)
                     // 触觉反馈
                     view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
    }

    val itemHeight = 40.dp
    val visibleItems = 3
    
    Box(modifier = modifier.height(itemHeight * visibleItems), contentAlignment = Alignment.Center) {
        // 选择指示器
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
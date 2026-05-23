package com.example

import android.os.Bundle
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.example.ui.PregnancyNotifier
import com.example.data.*
import com.example.ui.PregnancyViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PregnancyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyApp() {
    val viewModel: PregnancyViewModel = viewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val allWeeks by viewModel.allWeeksState.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointmentsState.collectAsStateWithLifecycle()
    val allReminders by viewModel.allRemindersState.collectAsStateWithLifecycle()
    val allWeightRecords by viewModel.allWeightRecordsState.collectAsStateWithLifecycle()
    val selectedWeekNum by viewModel.selectedWeek.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showLaborDialog by viewModel.showLaborDialog.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        PregnancyAuthScreen(viewModel = viewModel)
        return
    }

    if (currentUser == null || allWeeks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = DeepBrownSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Đang nạp dữ liệu y khoa thai kỳ...",
                    color = DeepBrownSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val activeUser = currentUser!!
    val hasSetupProfileState by viewModel.hasSetupProfile.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? MainActivity

    // Deep-link handling on active app instances
    DisposableEffect(activity) {
        val listener = androidx.core.util.Consumer<android.content.Intent> { newIntent ->
            val targetTab = newIntent.getIntExtra("EXTRA_TARGET_TAB", -1)
            if (targetTab != -1) {
                viewModel.changeTab(targetTab)
                newIntent.removeExtra("EXTRA_TARGET_TAB")
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    // Deep-link handling on cold start
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            activity?.intent?.let { it ->
                val targetTab = it.getIntExtra("EXTRA_TARGET_TAB", -1)
                if (targetTab != -1) {
                    viewModel.changeTab(targetTab)
                    it.removeExtra("EXTRA_TARGET_TAB")
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val userCurrentWeekNum = viewModel.calculatePregnancyWeek(activeUser.lmpDate, activeUser.eddDate)

    // Automatically trigger notification if the user has reached any appointment targetWeek
    LaunchedEffect(userCurrentWeekNum, allAppointments) {
        if (userCurrentWeekNum in 1..41 && allAppointments.isNotEmpty()) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            allAppointments.forEach { appt ->
                if (appt.targetWeek == userCurrentWeekNum && appt.status == "PENDING") {
                    val key = "auto_notified_appt_week_${appt.id}"
                    val alreadyNotified = prefs.getBoolean(key, false)
                    if (!alreadyNotified) {
                        val clinicName = appt.clinicName ?: "Phòng khám sản khoa"
                        val docName = if (appt.doctorName != null) " cùng Bác sĩ ${appt.doctorName}" else ""
                        PregnancyNotifier.sendNotificationImmediately(
                            context = context,
                            title = "📅 Lịch Khám Đến Hạn Tuần $userCurrentWeekNum",
                            message = "Chúc mừng mẹ đã chạm mốc tuần thai thứ $userCurrentWeekNum! Bạn có lịch hẹn khám tại $clinicName$docName. Đi khám liền nhé!",
                            itemId = appt.id,
                            type = "APPOINTMENT",
                            targetTab = 1
                        )
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
            }
        }
    }

    val visibleWeeks = remember(allWeeks, userCurrentWeekNum) {
        if (userCurrentWeekNum in 1..41) {
            allWeeks.filter { it.weekNumber <= userCurrentWeekNum }
        } else {
            allWeeks
        }
    }

    if (!hasSetupProfileState) {
        PregnancyOnboardingScreen(viewModel = viewModel, user = activeUser)
        return
    }

    val currentWeekData = visibleWeeks.find { it.weekNumber == selectedWeekNum }
        ?: visibleWeeks.lastOrNull() ?: allWeeks.find { it.weekNumber == selectedWeekNum } ?: allWeeks.firstOrNull() ?: FetalWeekEntity(
            weekNumber = 24,
            avgWeightG = 600.0,
            avgLengthCm = 30.0,
            fruitEquivalent = "Quả dưa lưới",
            bakeryEquivalent = "Bánh Cupcake dâu",
            animalEquivalent = "Chú sóc nhỏ",
            physiologyDescription = "Hệ hô hấp phát triển nhanh sơ vỡ phế quản hớp khí.",
            maternalChanges = "Ợ chua bỏng rát thực quản co cơ giả giãn dây chằng nặng nề.",
            nutritionalRecommendation = "Bổ sung sắt, canxi tối, đạm, uống nước dừa lành dạt sữa béo mút mượt gót chân mầm sụn.",
            exceptionAlerts = "Tiểu buốt mủ, sưng mặt tay bão, huyết áp bốc sập."
        )

    var currentDateTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy - HH:mm", Locale("vi", "VN"))
        while (true) {
            val rawTime = sdf.format(java.util.Date())
            currentDateTimeStr = rawTime.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            kotlinx.coroutines.delay(10000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MẸ & BÉ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (currentDateTimeStr.isNotBlank()) currentDateTimeStr else "Đang cập nhật thời gian...",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    if (selectedWeekNum != -1) {
                        val preciseAge = viewModel.calculateDetailedGestationalAge(activeUser.lmpDate, activeUser.eddDate)
                        Card(
                            modifier = Modifier.padding(end = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftPeachPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Sức khỏe",
                                    tint = DeepBrownSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = preciseAge,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepBrownSecondary
                                    )
                                    Text(
                                        text = "Tuần thứ $selectedWeekNum (đọc tin)",
                                        fontSize = 8.5.sp,
                                        color = DarkBrownText
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmBackground
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(currentTab = currentTab, onTabSelected = { viewModel.changeTab(it) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WarmBackground)
        ) {
            // Horizontal scrollable Week Selector (1 to 41) only when dates are set
            if (selectedWeekNum != -1) {
                WeekSelectorScroll(
                    allWeeks = visibleWeeks,
                    selectedWeek = selectedWeekNum,
                    onWeekSelected = { viewModel.updateSelectedWeek(it) }
                )

                HorizontalDivider(color = LightBorder, thickness = 1.dp)
            }

            // Dynamic Tab Views
            Box(modifier = Modifier.weight(1f)) {
                if (selectedWeekNum == -1 && (currentTab == 0 || currentTab == 2)) {
                    SetupDatesGreetingCard(onSetupClick = { viewModel.changeTab(4) })
                } else {
                    when (currentTab) {
                        0 -> TodayTab(
                            weekData = currentWeekData,
                            userTheme = activeUser.visualizationTheme,
                            onThemeChanged = { viewModel.changeTheme(it) },
                            onLaborAlertClick = { viewModel.toggleLaborDialog(true) },
                            visibleWeeks = visibleWeeks
                        )
                        1 -> AppointmentsTab(
                            viewModel = viewModel,
                            allAppointments = allAppointments,
                            selectedWeek = selectedWeekNum
                        )
                        2 -> NutritionTab(
                            viewModel = viewModel,
                            weekData = currentWeekData
                        )
                        3 -> RemindersTab(
                            viewModel = viewModel,
                            allReminders = allReminders
                        )
                        4 -> ProfileTab(
                            viewModel = viewModel,
                            user = activeUser,
                            allWeightRecords = allWeightRecords
                        )
                    }
                }
            }
        }

        // Labor Signs Overlay Dialog
        if (showLaborDialog) {
            LaborDialog(onDismiss = { viewModel.toggleLaborDialog(false) })
        }
    }
}

@Composable
fun SetupDatesGreetingCard(onSetupClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "🌸 Chào mừng Mẹ yêu đến với Mẹ & Bé! 💕",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBrownSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Để đồng hành cùng bé yêu phát triển khỏe mạnh từng ngày, Mẹ hãy cài đặt Ngày dự sinh (EDD) hoặc Ngày đầu kỳ kinh cuối (LMP) nhé. Ứng dụng sẽ đồng bộ mọi thông tin cẩm nang dinh dưỡng và lịch trình khám thai một cách trọn vẹn nhất! 🥰",
                fontSize = 13.sp,
                color = DarkBrownText,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            LiquidGlassButton(
                onClick = onSetupClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cài đặt ngày dự sinh ngay mẹ nhé", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeekSelectorScroll(
    allWeeks: List<FetalWeekEntity>,
    selectedWeek: Int,
    onWeekSelected: (Int) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(selectedWeek) {
        val index = allWeeks.indexOfFirst { it.weekNumber == selectedWeek }
        if (index >= 0) {
            listState.animateScrollToItem(index = maxOf(0, index - 3))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmBackground)
            .padding(vertical = 10.dp)
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allWeeks) { week ->
                val isSelected = week.weekNumber == selectedWeek
                val isGold = isGoldenWeek(week.weekNumber)

                Box(
                    modifier = Modifier
                        .testTag("week_item_${week.weekNumber}")
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> DeepBrownSecondary
                                isGold -> AmberBg
                                else -> WhiteCard
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isSelected -> DeepBrownSecondary
                                isGold -> AlertBorder
                                else -> LightBorder
                            },
                            shape = CircleShape
                        )
                        .clickable { onWeekSelected(week.weekNumber) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = week.weekNumber.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> Color.White
                                isGold -> AmberText
                                else -> TextSlate
                            }
                        )
                        if (isGold) {
                            Text(
                                text = "MỐC",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayTab(
    weekData: FetalWeekEntity,
    userTheme: String,
    onThemeChanged: (String) -> Unit,
    onLaborAlertClick: () -> Unit,
    visibleWeeks: List<FetalWeekEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp)
    ) {
        // Theme Selector Live-Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Chủ đề so sánh kích thước",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeButton(
                            text = "Trái Cây",
                            emoji = "🍈",
                            isActive = userTheme == "FRUIT",
                            tag = "theme_selector_fruit",
                            onClick = { onThemeChanged("FRUIT") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            text = "Bánh Ngọt",
                            emoji = "🧁",
                            isActive = userTheme == "BAKERY",
                            tag = "theme_selector_bakery",
                            onClick = { onThemeChanged("BAKERY") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            text = "Muông Thú",
                            emoji = "🐰",
                            isActive = userTheme == "ANIMAL",
                            tag = "theme_selector_animal",
                            onClick = { onThemeChanged("ANIMAL") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Fetal Visual Comparison Gradient Card
        item {
            val emoji = getEmojiForTheme(weekData.weekNumber, userTheme)
            val equivalentText = when (userTheme) {
                "FRUIT" -> weekData.fruitEquivalent
                "BAKERY" -> weekData.bakeryEquivalent
                "ANIMAL" -> weekData.animalEquivalent
                else -> weekData.fruitEquivalent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(WarmPeachCard, SoftPeachPrimary)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Big decorative emoji
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Vital measurements
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Kích Thước Thai Nhi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = equivalentText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkBrownText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MeasureWidget("Cân nặng", "${weekData.avgWeightG} g")
                            MeasureWidget("Chiều dài", "${weekData.avgLengthCm} cm")
                        }
                    }
                }
            }
        }

        // Pregnancy biological and maternal changes details
        item {
            EvolutionNotesCard(
                title = "🎁 Bé Cưng Phát Triển Thế Nào?",
                icon = Icons.Default.Favorite,
                iconColor = DeepBrownSecondary,
                content = weekData.physiologyDescription
            )
        }

        item {
            EvolutionNotesCard(
                title = "🤰 Cơ Thể Mẹ Thay Đổi Ra Sao?",
                icon = Icons.Default.Person,
                iconColor = DeepBrownSecondary,
                content = weekData.maternalChanges
            )
        }

        // Exception Warnings Card if any are specified
        item {
            if (weekData.exceptionAlerts.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AlertLightBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AlertBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Dấu hiệu cần lưu ý",
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ Dấu hiệu bất thường cần lưu ý",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertDarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = weekData.exceptionAlerts,
                            fontSize = 13.sp,
                            color = AlertDarkText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 🚨 8 Signs of Labor quick click action
        item {
            Card(
                modifier = Modifier
                    .testTag("labor_alert_banner")
                    .fillMaxWidth()
                    .clickable { onLaborAlertClick() },
                colors = CardDefaults.cardColors(containerColor = AlertLightBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AlertRed.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AlertRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Chuyển dạ",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🚨 8 DẤU HIỆU CHUYỂN DẠ MẸ CẦN BIẾT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AlertDarkText
                        )
                        Text(
                            text = "Cẩm nang hướng dẫn khẩn cấp khi mẹ bầu chuẩn bị sinh em bé.",
                            fontSize = 11.sp,
                            color = AlertDarkText.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add, // Using Add as chevron action indicator
                        contentDescription = "Xem",
                        tint = AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 👣 Fetal Kick Counter Section
        item {
            KickCounterCard()
        }

        // === CHI TIẾT QUÁ TRÌNH HÌNH THÀNH THEO TỪNG GIAI ĐOẠN TỪNG TUẦN ===
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "📚 CẨM NANG HÌNH THÀNH THEO GIAI ĐOẠN",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = DeepBrownSecondary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Chi tiết quá trình phát triển của bé qua các tuần bạn đã trải qua.",
                fontSize = 11.sp,
                color = TextSlate
            )
        }

        // Grouping weeks by Trimester / Giai đoạn
        val t1Weeks = visibleWeeks.filter { it.weekNumber in 1..13 }
        val t2Weeks = visibleWeeks.filter { it.weekNumber in 14..27 }
        val t3Weeks = visibleWeeks.filter { it.weekNumber in 28..42 }

        val currentWeek = weekData.weekNumber
        val activeGroup = when (currentWeek) {
            in 1..13 -> 1
            in 14..27 -> 2
            else -> 3
        }

        // Helper to place the current week (or selected week) at the absolute beginning of its group list
        fun reorderWeeks(weeks: List<FetalWeekEntity>): List<FetalWeekEntity> {
            val currentItem = weeks.find { it.weekNumber == currentWeek }
            return if (currentItem != null) {
                listOf(currentItem) + weeks.filter { it.weekNumber != currentWeek }
            } else {
                weeks
            }
        }

        val reorderedT1 = if (activeGroup == 1) reorderWeeks(t1Weeks) else t1Weeks
        val reorderedT2 = if (activeGroup == 2) reorderWeeks(t2Weeks) else t2Weeks
        val reorderedT3 = if (activeGroup == 3) reorderWeeks(t3Weeks) else t3Weeks

        // We show the group containing the active week at the absolute top of the booklet!
        val groupRenderingOrder = when (activeGroup) {
            1 -> listOf(1, 2, 3)
            2 -> listOf(2, 3, 1)
            else -> listOf(3, 2, 1)
        }

        groupRenderingOrder.forEach { groupNum ->
            when (groupNum) {
                1 -> {
                    if (reorderedT1.isNotEmpty()) {
                        item {
                            StageHeaderCard(
                                title = if (activeGroup == 1) "⭐ Giai đoạn hiện tại: Tam cá nguyệt thứ nhất (Tuần 1 - 13)" else "Giai đoạn 1: Tam cá nguyệt thứ nhất (Tuần 1 - 13)",
                                description = "Bản lề thụ thai, hình thành tế bào mầm sống và các hệ cơ quan sơ khai nhất.",
                                bgColor = SoftPeachPrimary.copy(alpha = 0.3f),
                                borderColor = SoftPeachPrimary
                            )
                        }
                        items(reorderedT1) { week ->
                            FetalTimelineWeekCard(week = week, userTheme = userTheme, isCurrent = week.weekNumber == currentWeek)
                        }
                    }
                }
                2 -> {
                    if (reorderedT2.isNotEmpty()) {
                        item {
                            StageHeaderCard(
                                title = if (activeGroup == 2) "⭐ Giai đoạn hiện tại: Tam cá nguyệt thứ hai (Tuần 14 - 27)" else "Giai đoạn 2: Tam cá nguyệt thứ hai (Tuần 14 - 27)",
                                description = "Bùng nổ phát triển xương khớp sụn tai, mỡ da, tế bào thần kinh và cảm nhận thai máy.",
                                bgColor = EmeraldBg.copy(alpha = 0.4f),
                                borderColor = EmeraldText.copy(alpha = 0.5f)
                            )
                        }
                        items(reorderedT2) { week ->
                            FetalTimelineWeekCard(week = week, userTheme = userTheme, isCurrent = week.weekNumber == currentWeek)
                        }
                    }
                }
                3 -> {
                    if (reorderedT3.isNotEmpty()) {
                        item {
                            StageHeaderCard(
                                title = if (activeGroup == 3) "⭐ Giai đoạn hiện tại: Tam cá nguyệt thứ ba (Tuần 28 - 41)" else "Giai đoạn 3: Tam cá nguyệt thứ ba (Tuần 28 - 41)",
                                description = "Hoàn thiện tối ưu chức năng hô hấp phổi, mở nhắm mắt, xoay đầu ổn định chuẩn bị chuyển dạ.",
                                bgColor = AlertLightBg,
                                borderColor = AlertRed.copy(alpha = 0.3f)
                            )
                        }
                        items(reorderedT3) { week ->
                            FetalTimelineWeekCard(week = week, userTheme = userTheme, isCurrent = week.weekNumber == currentWeek)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StageHeaderCard(
    title: String,
    description: String,
    bgColor: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DeepBrownSecondary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = description, fontSize = 11.sp, color = DarkBrownText)
        }
    }
}

@Composable
fun FetalTimelineWeekCard(
    week: FetalWeekEntity,
    userTheme: String,
    isCurrent: Boolean = false
) {
    val sizeText = when (userTheme) {
        "FRUIT" -> week.fruitEquivalent
        "BAKERY" -> week.bakeryEquivalent
        "ANIMAL" -> week.animalEquivalent
        else -> week.fruitEquivalent
    }
    val emoji = getEmojiForTheme(week.weekNumber, userTheme)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SoftPeachPrimary.copy(alpha = 0.08f) else WhiteCard
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 0.5.dp, 
            color = if (isCurrent) SoftPeachPrimary else LightBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SoftPeachPrimary.copy(alpha = if (isCurrent) 0.8f else 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tuần thứ ${week.weekNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SoftPeachPrimary),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🌟 Hiện Tại",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isCurrent) SoftPeachPrimary.copy(alpha = 0.15f) else WarmBackground),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⚖️ ${week.avgWeightG}g | 📏 ${week.avgLengthCm}cm",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCurrent) DeepBrownSecondary else TextSlate,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Kích thước tương đương: $sizeText",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBrownText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "👶 Quá trình hình thành: ${week.physiologyDescription}",
                fontSize = 11.5.sp,
                color = TextSlate,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🥦 Dinh dưỡng khuyên dùng: ${week.nutritionalRecommendation}",
                fontSize = 11.5.sp,
                color = EmeraldText,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ThemeButton(
    text: String,
    emoji: String,
    isActive: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag(tag)
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) SoftPeachPrimary else WarmBackground)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isActive) DeepBrownSecondary else LightBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) DeepBrownSecondary else TextSlate
            )
        }
    }
}

@Composable
fun MeasureWidget(label: String, valStr: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(text = label, fontSize = 10.sp, color = TextMuted)
            Text(text = valStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
        }
    }
}

@Composable
fun EvolutionNotesCard(
    title: String,
    icon: Any, // supporting ImageVector
    iconColor: Color,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrownSecondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = TextSlate,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun AppointmentsTab(
    viewModel: PregnancyViewModel,
    allAppointments: List<AppointmentEntity>,
    selectedWeek: Int
) {
    val focusManager = LocalFocusManager.current
    var clinic by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var targetWeekStr by remember { mutableStateOf(selectedWeek.toString()) }
    var editingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp)
    ) {
        // Gold checkup weeks overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AmberBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AlertBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⭐ Lịch Trình Kiểm Tra Thai Mốc Vàng (WHO)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Tuần 11-13: Khảo sát độ mờ da gáy NT phòng dị tật nhiễm sắc thể.\n" +
                               "• Tuần 20-24: Siêu âm 4D rà soát toàn bộ hình thái cơ quan của bé.\n" +
                               "• Tuần 24-28: Khám huyết áp & thử nghiệm pháp Glucose dũng đường.\n" +
                               "• Tuần 32: Đánh giá Doppler bánh nhau, nước ối và cân nặng bé.\n" +
                               "• Tuần 36+: Chạy Monitor tim thai NST đều đặn hàng tuần phòng lưu thai.",
                        fontSize = 11.5.sp,
                        color = AmberText,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Live status bar test action card
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        PregnancyNotifier.sendNotificationImmediately(
                            context = context,
                            title = "📅 Nhắc nhở: Lịch khám thai tuần mốc vàng",
                            message = "Mẹ bầu ơi! Lịch hẹn siêu âm khảo sát dị tật mốc vàng của tuần này đã đến giờ rồi. Chúc mẹ đi khám may mắn thuận buồm xuôi gió nhé!",
                            itemId = "WHO_GOLD_MOCK_ID",
                            type = "APPOINTMENT",
                            targetTab = 1
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = SoftPeachPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DeepBrownSecondary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = DeepBrownSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 Thử nghiệm thông báo trên điện thoại",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary
                        )
                        Text(
                            text = "Bấm vào card này để test thử chuông/rung báo Lịch Khám trên thanh trạng thái ngay lập tức!",
                            fontSize = 11.sp,
                            color = DarkBrownText
                        )
                    }
                    Text("TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBrownSecondary, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }

        // Check if selected week is a golden week
        if (isGoldenWeek(selectedWeek)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Mốc Vàng",
                            tint = DarkBrownText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MỐC KHÁM SẢN KHOA VÀNG Ở TUẦN THẮT $selectedWeek! Vui lòng đặt hẹn ngay.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBrownText
                        )
                    }
                }
            }
        }

        // Add Appointment Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Lên Lịch Hẹn Khám Thai Offline",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PregnancyTextField(
                        value = targetWeekStr,
                        onValueChange = { targetWeekStr = it },
                        label = "Tuần thai khám (1..41)",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PregnancyDatePickerField(
                        label = "Ngày hẹn khám",
                        currentDateYmd = date,
                        onDateSelected = { date = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PregnancyTextField(
                        value = clinic,
                        onValueChange = { clinic = it },
                        label = "Tên phòng khám / Bệnh viện",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PregnancyTextField(
                        value = doctor,
                        onValueChange = { doctor = it },
                        label = "Bác sĩ phụ trách chính",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PregnancyTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Ghi chú, lời nhắc bác dặn",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LiquidGlassButton(
                        onClick = {
                            val w = targetWeekStr.toIntOrNull() ?: selectedWeek
                            if (date.isNotBlank()) {
                                viewModel.addAppointment(
                                    week = w,
                                    date = date,
                                    clinic = clinic,
                                    doctor = doctor,
                                    notes = notes,
                                    isCritical = isGoldenWeek(w)
                                )
                                clinic = ""
                                doctor = ""
                                notes = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .testTag("add_appointment_btn")
                            .fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xác Nhận Đặt Lịch Offline", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Scheduled list Title
        item {
            Text(
                text = "Lịch Trình Hẹn Khám Thực Tế",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DeepBrownSecondary
            )
        }

        // Render Scheduled list
        if (allAppointments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Chưa có cuộc hẹn khám thai nào được thiết lập.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        if (selectedWeek == -1) {
                            Text(
                                text = "💡 Hãy thiết lập Ngày dự sinh (EDD) trong mục Cá nhân để hệ thống tự động gợi ý toàn bộ lịch trình khám thai mốc vàng cho bạn nhé!",
                                color = DeepBrownSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(allAppointments) { appt ->
                val isCompleted = appt.status == "COMPLETED"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> EmeraldBg
                            appt.isCritical -> AmberBg
                            else -> WhiteCard
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isCompleted -> EmeraldText.copy(alpha = 0.3f)
                            appt.isCritical -> AlertBorder
                            else -> LightBorder
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (appt.isCritical && !isCompleted) AmberText.copy(alpha = 0.15f) else SoftPeachPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appt.targetWeek.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (appt.isCritical && !isCompleted) AmberText else DeepBrownSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Tuần thai thứ ${appt.targetWeek}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isCompleted) EmeraldText else TextSlate
                                    )
                                    Text(
                                        text = "Ngày khám: ${appt.scheduledDate}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Clean checkbox circle toggle (A11y Touch Target >= 48dp)
                            IconButton(
                                onClick = { viewModel.toggleAppointmentStatus(appt) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                    tint = if (isCompleted) EmeraldText else TextMuted,
                                    contentDescription = "Tích hoàn thành khám thai",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (appt.clinicName != null) {
                            Text(
                                text = "🏥 Phòng Khám: ${appt.clinicName}",
                                fontSize = 12.sp,
                                color = if (isCompleted) EmeraldText.copy(alpha = 0.8f) else TextSlate
                            )
                        }
                        if (appt.doctorName != null) {
                            Text(
                                text = "🧑‍⚕️ Bác sĩ: ${appt.doctorName}",
                                fontSize = 12.sp,
                                color = if (isCompleted) EmeraldText.copy(alpha = 0.8f) else TextSlate
                            )
                        }
                        if (appt.medicalNotes != null) {
                            Text(
                                text = "📝 Chỉ định dặn: \"${appt.medicalNotes}\"",
                                fontSize = 12.sp,
                                color = if (isCompleted) EmeraldText.copy(alpha = 0.6f) else TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Status Badge
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) EmeraldText.copy(alpha = 0.1f) else AmberBg
                                )
                            ) {
                                Text(
                                    text = if (isCompleted) "✓ ĐÃ KHÁM SẢN" else "⚡ CHỜ HẸN KHÁM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = if (isCompleted) EmeraldText else AmberText
                                )
                            }

                            // Actions row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Test notification button
                                val context = LocalContext.current
                                IconButton(
                                    onClick = {
                                        val clinicInfo = appt.clinicName ?: "phòng khám"
                                        val docInfo = if (appt.doctorName != null) " cùng Bác sĩ ${appt.doctorName}" else ""
                                        PregnancyNotifier.sendNotificationImmediately(
                                            context = context,
                                            title = "📅 Nhắc nhở lịch khám thai Tuần ${appt.targetWeek}",
                                            message = "Mẹ ơi! Bạn có hẹn khám thai tại $clinicInfo$docInfo vào ngày ${appt.scheduledDate}. Chúc mẹ đi khám may mắn thuận buồm xuôi gió!",
                                            itemId = appt.id,
                                            type = "APPOINTMENT",
                                            targetTab = 1
                                        )
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Test thông báo",
                                        tint = DeepBrownSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Edit button
                                IconButton(
                                    onClick = { editingAppointment = appt },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa lịch trình",
                                        tint = DeepBrownSecondary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = { viewModel.deleteAppointment(appt.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa lịch trình",
                                        tint = AlertRed.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingAppointment != null) {
        val appt = editingAppointment!!
        var editWeekStr by remember(appt.id) { mutableStateOf(appt.targetWeek.toString()) }
        var editDate by remember(appt.id) { mutableStateOf(appt.scheduledDate) }
        var editClinic by remember(appt.id) { mutableStateOf(appt.clinicName ?: "") }
        var editDoctor by remember(appt.id) { mutableStateOf(appt.doctorName ?: "") }
        var editNotes by remember(appt.id) { mutableStateOf(appt.medicalNotes ?: "") }
        var editIsCritical by remember(appt.id) { mutableStateOf(appt.isCritical) }

        AlertDialog(
            onDismissRequest = { editingAppointment = null },
            title = {
                Text(
                    text = "Chỉnh sửa Lịch Hẹn Khám",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrownSecondary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PregnancyTextField(
                        value = editWeekStr,
                        onValueChange = { editWeekStr = it },
                        label = "Tuần thai khám (1..41)",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    PregnancyDatePickerField(
                        label = "Ngày hẹn khám",
                        currentDateYmd = editDate,
                        onDateSelected = { editDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = editClinic,
                        onValueChange = { editClinic = it },
                        label = "Tên phòng khám / Bệnh viện",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = editDoctor,
                        onValueChange = { editDoctor = it },
                        label = "Bác sĩ phụ trách chính",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = "Ghi chú dặn dò",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { editIsCritical = !editIsCritical }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = editIsCritical,
                            onCheckedChange = { editIsCritical = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepBrownSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đây là mốc khám quan trọng (Mốc Vàng)",
                            fontSize = 12.sp,
                            color = TextSlate
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wk = editWeekStr.toIntOrNull() ?: appt.targetWeek
                        viewModel.updateAppointment(
                            id = appt.id,
                            week = wk,
                            date = editDate,
                            clinic = editClinic,
                            doctor = editDoctor,
                            notes = editNotes,
                            isCritical = editIsCritical,
                            status = appt.status
                        )
                        editingAppointment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary)
                ) {
                    Text("Lưu Thay Đổi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingAppointment = null },
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Text("Hủy bỏ", color = TextSlate, fontSize = 12.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun NutritionTab(
    viewModel: PregnancyViewModel,
    weekData: FetalWeekEntity
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp)
    ) {
        // Dynamic Weekly Recommendation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmeraldBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = EmeraldText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dinh Dưỡng Khuyến Nghị Tuần ${weekData.weekNumber}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = weekData.nutritionalRecommendation,
                        fontSize = 13.sp,
                        color = EmeraldText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 💧 Pregnancy Water Intake Logger Section
        item {
            WaterTrackerCard()
        }

        // RECONFIGURED: Comprehensive Golden Food Groups (Rau Củ, Thịt, Sữa, Hạt)
        item {
            Text(
                text = "🥛 Tháp Dinh Dưỡng Vàng Mẹ Bầu",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = DeepBrownSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 1. Nhóm Rau Củ Quả
        item {
            FoodGroupSection(
                groupName = "🥦 Nhóm Rau Xanh, Củ & Trái Cây",
                foods = listOf(
                    Triple("Súp lơ & Cải bó xôi", "Cung cấp dồi dào Axit Folic tự nhiên, ngăn dị tật ống thần kinh thai nhi cấu trúc cột sống.", "🥦"),
                    Triple("Quả bơ chín dồi dào", "Giàu chất béo không bão hòa đơn lành mạnh, đặc biệt tốt cho sự bồi đắp tế bào não trẻ.", "🥑"),
                    Triple("Chuối tiêu & Cam quýt", "Giàu Kali hỗ trợ ngừa co thắt bắp thịt, giàu vitamin C hỗ trợ tối đa việc hấp lạt Sắt.", "🍊")
                ),
                headerColor = DeepBrownSecondary,
                cardBg = EmeraldBg.copy(alpha = 0.5f)
            )
        }

        // 2. Nhóm Thịt & Cá
        item {
            FoodGroupSection(
                groupName = "🥩 Nhóm Thịt, Cá & Protein Chuyên Sâu",
                foods = listOf(
                    Triple("Thịt bò nạc thăn sấy mềm", "Cung cấp hàm lượng sắt heme dễ dàng chuyển hóa hấp thụ trực tiếp ngăn thiếu máu.", "🥩"),
                    Triple("Cá hồi phi lê áp chảo", "Chứa nguồn axit béo DHA cao cấp phát triển mầm mống thị lực và vỏ não bé cưng khỏe mạnh.", "🐟"),
                    Triple("Lòng đỏ trứng gà ta", "Sở hữu lượng Choline vàng dồi dào tối ưu liên kết thần kinh xúc giác sơ sinh.", "🥚")
                ),
                headerColor = DeepBrownSecondary,
                cardBg = SoftPeachPrimary.copy(alpha = 0.5f)
            )
        }

        // 3. Nhóm Sữa
        item {
            FoodGroupSection(
                groupName = "🥛 Nhóm Sữa & Chế Phẩm Giàu Canxi",
                foods = listOf(
                    Triple("Sữa tươi tiệt trùng / Sữa bầu cao cấp", "Tối ưu hóa sự bồi đắp cốt tế bào sụn răng nanh thúc đẩy bé dài đòn khỏe khoắn.", "🥛"),
                    Triple("Sữa chua uống Probiotics", "Chứa hàng tỷ lợi khuẩn tự nhiên điều hòa đường ruột, làm mát cơ thể giảm nhiệt miệng.", "🍧"),
                    Triple("Phô mai vuông tự nhiên", "Chứa hàm lượng lipid cô đặc kích hoạt năng dồi canxi dẻo dai bám cơ của mẹ.", "🧀")
                ),
                headerColor = DeepBrownSecondary,
                cardBg = WarmPeachCard
            )
        }

        // 4. Nhóm Hạt & Ngũ Cốc
        item {
            FoodGroupSection(
                groupName = "🥜 Nhóm Hạt & Ngũ Cốc Tốt Cho Trí Não",
                foods = listOf(
                    Triple("Hạt óc chó & Hạnh nhân sấy", "Hàm lượng axit béo an toàn cao nuôi lớp bọc tủy thần kinh bé phản xạ cực nhanh.", "🥜"),
                    Triple("Yến mạch & Khoai mật nướng", "Lượng tinh bột thô chuyển hóa chậm ổn định chỉ số glycemic ngừa đái tháo thai hiệu quả.", "🌾")
                ),
                headerColor = DeepBrownSecondary,
                cardBg = WarmBackground
            )
        }

        // NEW: strictly avoid list
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "🛑 Thực Phẩm Cần Tránh Tuyệt Đối",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AlertRed,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            val avoidedFoods = listOf(
                Triple("🍣 Thủy hải sản tươi sống (Sashimi)", "Mang nguy cơ lây nhiễm vi trùng Listeria đe dọa sự an toàn thai nhi.", "🍣"),
                Triple("☕ Caffeine / Các loại trà đặc", "Hút bớt hàm lượng sắt bổ sung, làm nhịp tim đập bốc nóng bực khó ngủ.", "☕"),
                Triple("🍷 Bia rượu, đồ uống chứa cồn", "Tuyệt đối không uống vì gây hại vĩnh viễn hệ thần kinh phát triển của con.", "🍷")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                avoidedFoods.forEach { badFood ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AlertLightBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, AlertBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AlertRed.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = badFood.third, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = badFood.first,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertDarkText
                                )
                                Text(
                                    text = badFood.second,
                                    fontSize = 11.5.sp,
                                    color = DarkBrownText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodGroupSection(
    groupName: String,
    foods: List<Triple<String, String, String>>,
    headerColor: Color,
    cardBg: Color
) {
    Column {
        Text(
            text = groupName,
            fontSize = 13.0.sp,
            fontWeight = FontWeight.Bold,
            color = headerColor,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            foods.forEach { food ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, LightBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = food.third, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = food.first,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = food.second,
                                fontSize = 11.5.sp,
                                color = TextSlate,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypeSelectorButton(
    text: String,
    emoji: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) SoftPeachPrimary else WarmBackground)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isActive) DeepBrownSecondary else LightBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) DeepBrownSecondary else TextSlate
            )
        }
    }
}

@Composable
fun ReminderScheduleCard(
    reminder: MedicationReminderEntity,
    todayLog: ReminderLogEntity?,
    viewModel: PregnancyViewModel,
    activeColor: Color,
    onEditClick: () -> Unit
) {
    val isTaken = todayLog?.status == "TAKEN"
    val isRejected = todayLog?.status == "REJECTED"

    val bgColor = when {
        isTaken -> EmeraldBg
        isRejected -> AlertLightBg
        else -> WhiteCard
    }

    val borderColor = when {
        isTaken -> EmeraldText.copy(alpha = 0.3f)
        isRejected -> AlertRed.copy(alpha = 0.3f)
        else -> LightBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_item_${reminder.id}"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isTaken) EmeraldText.copy(alpha = 0.15f) else activeColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = reminder.scheduledTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTaken) EmeraldText else DeepBrownSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isTaken -> "ĐÃ DÙNG"
                                isRejected -> "ĐÃ BỎ QUA"
                                else -> "HÀNG NGÀY"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isTaken -> EmeraldText
                                isRejected -> AlertRed
                                else -> TextMuted
                            },
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.medicationName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isTaken) TextSlate.copy(alpha = 0.55f) else TextSlate,
                        textDecoration = if (isTaken) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    Text(
                        text = reminder.dosage,
                        fontSize = 12.sp,
                        color = if (isTaken) TextMuted.copy(alpha = 0.65f) else TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Active Switch only visible when pending
                    if (!isTaken && !isRejected) {
                        Switch(
                            checked = reminder.isActive,
                            onCheckedChange = { viewModel.toggleReminderActive(reminder) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepBrownSecondary,
                                checkedTrackColor = SoftPeachPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Edit Button (only visible when not logged yet)
                    if (!isTaken && !isRejected) {
                        IconButton(
                            onClick = { onEditClick() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Chỉnh sửa nhắc nhở",
                                tint = DeepBrownSecondary.copy(alpha = 0.8f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    // Delete Button
                    IconButton(
                        onClick = { viewModel.deleteReminder(reminder.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa nhắc nhở",
                            tint = AlertRed.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Actions depending on taken / rejected log today
            Spacer(modifier = Modifier.height(10.dp))
            if (isTaken || isRejected) {
                // Show Undo/Restore button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isTaken) EmeraldText else AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTaken) "Mẹ yêu đã hoàn thành lịch nhắc! 🥰" else "Mẹ bỏ qua lịch nhắc hôm nay.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTaken) EmeraldText else AlertRed
                        )
                    }

                    TextButton(
                        onClick = { viewModel.undoReminderAction(reminder) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Hoàn tác",
                                tint = DeepBrownSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hoàn tác",
                                fontSize = 11.sp,
                                color = DeepBrownSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Show Confirm vs Reject action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.logReminderAction(reminder, "TAKEN") },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldBg),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldText.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Đã dùng 👍",
                                color = EmeraldText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.logReminderAction(reminder, "REJECTED") },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = AlertLightBg.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bỏ qua ❌",
                                color = AlertRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(
    viewModel: PregnancyViewModel,
    user: UserEntity,
    allWeightRecords: List<WeightRecordEntity>
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf(user.name) }
    var lmpDate by remember { mutableStateOf(user.lmpDate ?: "2026-01-15") }
    var eddDate by remember { mutableStateOf(user.eddDate ?: "2026-10-22") }
    var dobDate by remember { mutableStateOf(user.dobDate ?: "1998-05-15") }
    var conceptionDate by remember { mutableStateOf(user.conceptionDate ?: "2026-01-29") }
    var startWeightKg by remember { mutableStateOf(user.gestationStartWeight.toString()) }
    var targetWeightKg by remember { mutableStateOf(user.targetPregnancyWeight.toString()) }

    var subTabSelected by remember { mutableStateOf(0) } // 0: Hồ Sơ & Ngày Chu Kỳ, 1: Nhật Ký Cân Nặng

    // For new weight record input
    var newWeightStr by remember { mutableStateOf("") }
    var newWeightWeekStr by remember { mutableStateOf(viewModel.selectedWeek.value.toString()) }
    var newWeightNotes by remember { mutableStateOf("") }

    val daysLeft = viewModel.getDaysLeftToDue(user.eddDate)
    val progressPercent = ((280 - daysLeft).coerceIn(0, 280).toFloat() / 280f * 100f).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp)
    ) {
        // Active account info & Logout
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SoftPeachPrimary.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Active user",
                                tint = DeepBrownSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Tài khoản đang đăng nhập", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = viewModel.loggedInEmail.value,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBrownSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Đăng xuất", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Toggle Buttons subTabSelected
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { subTabSelected = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subTabSelected == 0) DeepBrownSecondary else WhiteCard,
                        contentColor = if (subTabSelected == 0) Color.White else TextSlate
                    ),
                    modifier = Modifier.weight(1f).border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚙️ Hồ Sơ Thai Kỳ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { subTabSelected = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subTabSelected == 1) DeepBrownSecondary else WhiteCard,
                        contentColor = if (subTabSelected == 1) Color.White else TextSlate
                    ),
                    modifier = Modifier.weight(1f).border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚖️ Theo Dõi Cân Nặng (${allWeightRecords.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (subTabSelected == 0) {
            // Pregnancy Progress Dashboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "📊 Báo Cáo Hành Trình Chín Tháng Mười Ngày",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBrownText
                        )
                        Spacer(modifier = Modifier.height(11.dp))

                        Text(
                            text = "Còn lại $daysLeft Ngày để gặp bé cục vàng cưng!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepBrownSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = DeepBrownSecondary,
                            trackColor = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Tuần 1", fontSize = 10.sp, color = DarkBrownText)
                            Text(text = "Đã hoàn thành $progressPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBrownSecondary)
                            Text(text = "Tuần 40", fontSize = 10.sp, color = DarkBrownText)
                        }
                    }
                }
            }

            // Edit Profile Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚙️ Thiết Lập Hồ Sơ & Ngày Quan Trọng",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AmberBg.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, AmberText.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Gợi ý",
                                    tint = AmberText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mẹ chỉ cần nhập 1 trong 3 mốc (LMP, EDD hoặc Ngày thụ thai), hệ thống sẽ tự động ước tính chính xác 2 mốc còn lại!",
                                    fontSize = 11.sp,
                                    color = DarkBrownText,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        PregnancyTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Họ và Tên Mẹ bầu",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PregnancyDatePickerField(
                            label = "Ngày sinh của Mẹ",
                            currentDateYmd = dobDate,
                            onDateSelected = { dobDate = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PregnancyDatePickerField(
                            label = "Ngày đầu kỳ kinh cuối (LMP)",
                            currentDateYmd = lmpDate,
                            onDateSelected = { selectedDate ->
                                lmpDate = selectedDate
                                viewModel.estimateDatesFromLMP(selectedDate.trim())?.let { pair ->
                                    eddDate = pair.first
                                    conceptionDate = pair.second
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PregnancyDatePickerField(
                            label = "Ngày bác sĩ dự sinh (EDD)",
                            currentDateYmd = eddDate,
                            onDateSelected = { selectedDate ->
                                eddDate = selectedDate
                                viewModel.estimateDatesFromEDD(selectedDate.trim())?.let { pair ->
                                    lmpDate = pair.first
                                    conceptionDate = pair.second
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PregnancyDatePickerField(
                            label = "Ngày thụ thai chính thức",
                            currentDateYmd = conceptionDate,
                            onDateSelected = { selectedDate ->
                                conceptionDate = selectedDate
                                viewModel.estimateDatesFromConception(selectedDate.trim())?.let { pair ->
                                    lmpDate = pair.first
                                    eddDate = pair.second
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PregnancyTextField(
                            value = startWeightKg,
                            onValueChange = { startWeightKg = it },
                            label = "Cân nặng trước thai kỳ (kg)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PregnancyTextField(
                            value = targetWeightKg,
                            onValueChange = { targetWeightKg = it },
                            label = "Mục tiêu cân nặng khi sinh (kg)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        LiquidGlassButton(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val swValue = startWeightKg.toDoubleOrNull() ?: user.gestationStartWeight
                                    val twValue = targetWeightKg.toDoubleOrNull() ?: user.targetPregnancyWeight
                                    viewModel.updateProfile(
                                        name = name, 
                                        lmp = lmpDate, 
                                        edd = eddDate,
                                        dob = dobDate,
                                        conception = conceptionDate,
                                        startWeight = swValue,
                                        targetWeight = twValue
                                    )
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .testTag("save_profile_btn")
                                .fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cập Nhật Thiết Lập Hồ Sơ", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // WEIGHT TRACKER SUB-TAB
            // A. STATS BAR CARD
            item {
                val latestRecord = allWeightRecords.maxByOrNull { it.weekNumber }
                val currentW = latestRecord?.weightKg ?: user.gestationStartWeight
                val diffW = currentW - user.gestationStartWeight
                val targetGain = user.targetPregnancyWeight - user.gestationStartWeight
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftPeachPrimary.copy(alpha=0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        WavyBackgroundAccent(modifier = Modifier.matchParentSize().alpha(0.15f), color = DeepBrownSecondary)
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "⚖️ Phân Tích Chỉ Số Cân Nặng Thai Kỳ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBrownSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bắt đầu", fontSize = 10.sp, color = TextMuted)
                                    Text("${user.gestationStartWeight}kg", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Hiện tại", fontSize = 10.sp, color = TextMuted)
                                    Text("${currentW}kg", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DeepBrownSecondary)
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Đã tăng", fontSize = 10.sp, color = TextMuted)
                                    Text(
                                        text = if (diffW >= 0) "+${String.format(Locale.US, "%.1f", diffW)}kg" else "${String.format(Locale.US, "%.1f", diffW)}kg", 
                                        fontSize = 15.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (diffW > targetGain) AlertRed else EmeraldText
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Mục tiêu tăng", fontSize = 10.sp, color = TextMuted)
                                    Text("+${String.format(Locale.US, "%.1f", targetGain)}kg", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            val isGainHealthy = diffW in (1.0..16.0)
                            Text(
                                text = if (isGainHealthy) 
                                    "✓ Mẹ có mức tăng cân ổn định theo mốc tuần thai lý tưởng. Hãy duy trì chế độ dinh dưỡng bồi sữa để dưỡng nhi vững vàng mẹ nhé!" 
                                    else "💡 Mẹ nên đặt mục tiêu tăng từ 11.5kg - 16.0kg suốt hành trình thai kỳ để đảm bảo dinh dưỡng vàng tốt nhất cho sự nở nang của bánh nhau.",
                                fontSize = 11.sp,
                                color = TextSlate,
                                lineHeight = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // Live weight notification test card
            item {
                val context = LocalContext.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PregnancyNotifier.sendNotificationImmediately(
                                context = context,
                                title = "⚖️ Nhắc nhở: Cập nhật cân nặng tuần thai mới",
                                message = "Mẹ bầu ơi! Đã tròn một tuần kể từ lần ghi nhận cân nặng trước rồi. Hãy bước lên cân bàn hôm nay và lưu chỉ số mới để bảo vệ cân đối sức khỏe thai kỳ nhé!",
                                itemId = "WEIGHT_TRACKER",
                                type = "WEIGHT",
                                targetTab = 4
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = SoftPeachPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DeepBrownSecondary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = DeepBrownSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔔 Thử nghiệm nhắc cân nặng hàng tuần",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBrownSecondary
                            )
                            Text(
                                text = "Bấm để kiểm tra ngay lập tức âm thanh / rung và biểu tượng chiếc cân đo cân nặng trên thanh thông báo!",
                                fontSize = 11.sp,
                                color = DarkBrownText
                            )
                        }
                        Text("TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBrownSecondary, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            // B. CUSTOM LINE CHART VIEW
            item {
                WeightProgressionChart(
                    records = allWeightRecords,
                    startWeight = user.gestationStartWeight,
                    targetWeight = user.targetPregnancyWeight
                )
            }

            // C. LOG WEIGHT RECORD FORM
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✍️ Ghi Nhận Chỉ Số Cân Nặng",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newWeightWeekStr,
                                onValueChange = { newWeightWeekStr = it },
                                label = { Text("Tuần thai (1-41)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newWeightStr,
                                onValueChange = { newWeightStr = it },
                                label = { Text("Cân nặng (kg)") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newWeightNotes,
                            onValueChange = { newWeightNotes = it },
                            label = { Text("Ghi chú thể chất (Ví dụ: Ổn định, mệt mỏi...)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val wk = newWeightWeekStr.toIntOrNull()?.coerceIn(1, 41)
                                val wt = newWeightStr.toDoubleOrNull()
                                if (wk != null && wt != null) {
                                    viewModel.addOrUpdateWeight(wk, wt, newWeightNotes)
                                    newWeightStr = ""
                                    newWeightNotes = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Báo Cáo Cân Nặng Tuần", color = Color.White)
                        }
                    }
                }
            }

            // D. HISTORY RECALL LOG ITEMS
            if (allWeightRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WarmBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Chưa có chỉ ghi nhận cân nặng nào được cập nhật. Hãy khai báo tuần đầu tiên để vẽ đường biểu đồ chỉ số nhé mẹ!",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val sortedList = allWeightRecords.sortedByDescending { it.weekNumber }
                items(sortedList) { rec ->
                    val diff = rec.weightKg - user.gestationStartWeight
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, LightBorder.copy(alpha=0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SoftPeachPrimary.copy(alpha = 0.4f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Tuần thai ${rec.weekNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepBrownSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rec.dateRecorded ?: "",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ghi nhận: " + rec.notes,
                                    fontSize = 12.sp,
                                    color = TextSlate,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${rec.weightKg} kg",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepBrownSecondary
                                    )
                                    Text(
                                        text = if (diff >= 0) "+${String.format(Locale.US, "%.1f", diff)} kg" else "${String.format(Locale.US, "%.1f", diff)} kg",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diff >= 0) EmeraldText else AlertRed
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                IconButton(
                                    onClick = { viewModel.deleteWeightRecord(rec.weekNumber) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa dòng lịch sử",
                                        tint = AlertRed.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightProgressionChart(
    records: List<WeightRecordEntity>,
    startWeight: Double,
    targetWeight: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📈 Biểu Đồ Xu Hướng Cân Nặng Thai Kỳ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBrownSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(WarmBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, LightBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (records.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa đủ điểm đo để vẽ biểu đồ", fontSize = 11.sp, color = TextMuted)
                    }
                } else {
                    val sortedRecords = records.sortedBy { it.weekNumber }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        val minWeek = 1
                        val maxWeek = 40
                        val minW = startWeight - 2.0
                        val maxW = targetWeight + 5.0
                        
                        val weekRange = maxWeek - minWeek
                        val weightRange = maxW - minW
                        
                        // Target trend line (from starting weight at week 1 to target weight at week 40)
                        val startX = 0f
                        val startY = height - ((startWeight.toFloat() - minW.toFloat()) / weightRange.toFloat() * height)
                        val endX = width
                        val endY = height - ((targetWeight.toFloat() - minW.toFloat()) / weightRange.toFloat() * height)
                        
                        drawLine(
                            color = SoftPeachPrimary,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 3f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        
                        // User points and connections
                        var prevX = 0f
                        var prevY = 0f
                        for (i in sortedRecords.indices) {
                            val rec = sortedRecords[i]
                            val x = ((rec.weekNumber - minWeek).toFloat() / weekRange.toFloat()) * width
                            val y = height - ((rec.weightKg.toFloat() - minW.toFloat()) / weightRange.toFloat() * height)
                            
                            drawCircle(
                                color = DeepBrownSecondary,
                                radius = 6f,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            
                            if (i > 0) {
                                drawLine(
                                    color = DeepBrownSecondary,
                                    start = androidx.compose.ui.geometry.Offset(prevX, prevY),
                                    end = androidx.compose.ui.geometry.Offset(x, y),
                                    strokeWidth = 5f
                                )
                            }
                            prevX = x
                            prevY = y
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tuần 1 (${startWeight}kg)", fontSize = 9.sp, color = TextMuted)
                Text("- - Đường xu hướng chuẩn", fontSize = 9.sp, color = DarkBrownText)
                Text("Tuần 40 (${targetWeight}kg)", fontSize = 9.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun WavyBackgroundAccent(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.5f)
            cubicTo(
                width * 0.25f, height * 0.35f,
                width * 0.75f, height * 0.65f,
                width, height * 0.48f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = WhiteCard,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            modifier = Modifier.testTag("tab_button_today"),
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            label = { Text("Hôm nay", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = "Hôm nay") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepBrownSecondary,
                selectedTextColor = DeepBrownSecondary,
                indicatorColor = SoftPeachPrimary,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            modifier = Modifier.testTag("tab_button_appointments"),
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            label = { Text("Lịch khám", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "Lịch khám") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepBrownSecondary,
                selectedTextColor = DeepBrownSecondary,
                indicatorColor = SoftPeachPrimary,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            modifier = Modifier.testTag("tab_button_nutrition"),
            selected = currentTab == 2,
            onClick = { onTabSelected(2) },
            label = { Text("Dinh dưỡng", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = "Dinh dưỡng") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepBrownSecondary,
                selectedTextColor = DeepBrownSecondary,
                indicatorColor = SoftPeachPrimary,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            modifier = Modifier.testTag("tab_button_reminders"),
            selected = currentTab == 3,
            onClick = { onTabSelected(3) },
            label = { Text("Lịch nhắc", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Default.Notifications, contentDescription = "Sổ nhắc nhở") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepBrownSecondary,
                selectedTextColor = DeepBrownSecondary,
                indicatorColor = SoftPeachPrimary,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            modifier = Modifier.testTag("tab_button_profile"),
            selected = currentTab == 4,
            onClick = { onTabSelected(4) },
            label = { Text("Cá nhân", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Cá nhân") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepBrownSecondary,
                selectedTextColor = DeepBrownSecondary,
                indicatorColor = SoftPeachPrimary,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}

@Composable
fun LaborDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = AlertLightBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, AlertRed)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "8 DẤU HIỆU CHUYỂN DẠ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AlertDarkText
                        )
                    }
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add, // Chevron representation or simple cross rotation
                            contentDescription = "Đóng",
                            tint = AlertRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Khi gần đến ngày dự sinh, Mẹ bầu hãy chú ý theo dõi 8 dấu hiệu chuyển dạ quan trọng dưới đây. Nếu xuất hiện bất kỳ biểu hiện nào, Mẹ hãy chuẩn bị đồ đạc và nhanh chóng đến bệnh viện sản khoa ngay nhé:",
                    fontSize = 12.5.sp,
                    color = AlertDarkText,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LaborPoint("1", "Cơn co tử cung đều đặn và tăng dần", "Các cơn gò xuất hiện liên tục cứ mỗi 5-10 phút, kéo dài từ 30-60 giây, cường độ đau tăng dần và không giảm bớt ngay cả khi mẹ nghỉ ngơi.")
                LaborPoint("2", "Vỡ màng ối hoặc rò rỉ nước ối", "Có dòng dịch lỏng, trong suốt hoặc hơi đục chảy ra từ âm đạo đột ngột hoặc rỉ rả liên tục, không thể kiểm soát được.")
                LaborPoint("3", "Xuất hiện dịch nhầy hồng (Bloody Show)", "Bong nút nhầy cổ tử cung khi cổ tử cung bắt đầu giãn mở, tạo ra chất nhầy có lẫn chút máu màu hồng, đỏ hoặc nâu nhạt ở quần lót.")
                LaborPoint("4", "Đau thắt lưng và vùng bụng dưới dữ dội", "Cơn đau ê ẩm vùng thắt lưng chạy dọc ra phía trước bụng dưới, kèm theo cảm giác căng cứng bụng cực kỳ khó chịu.")
                LaborPoint("5", "Khớp vùng chậu căng giãn, nặng trĩu", "Đầu của thai nhi di chuyển xuống sâu khớp háng để chuẩn bị chào đời, tạo cảm giác nặng nề lỏng lẻo ở vùng xương chậu.")
                LaborPoint("6", "Đi tiểu liên tục hoặc tiêu chảy sinh lý", "Đầu thai nhi chèn ép lên bàng quang gây buồn tiểu liên tục. Đồng thời, hormone prostaglandin thúc đẩy ruột hoạt động mạnh hơn gây đi ngoài thực tế.")
                LaborPoint("7", "Cơ thể run rẩy hoặc ớn lạnh sống lưng", "Đôi khi mẹ sẽ cảm thấy run rẩy toàn thân hoặc lạnh sống lưng. Đây là phản xạ tự nhiên của hệ thần kinh khi chuẩn bị bước vào cuộc sinh nở.")
                LaborPoint("8", "Bản năng \"Làm tổ\" muốn sửa soạn đồ đạc", "Mẹ đột ngột cảm thấy tràn đầy năng lượng, bồn chồn muốn dọn dẹp nhà cửa, giặt giũ tã sữa và chuẩn bị giỏ đồ đi sinh gấp rút.")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text(
                        text = "Tôi đã hiểu",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LaborPoint(number: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AlertRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = AlertDarkText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.5.sp,
                color = AlertDarkText.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )
        }
    }
}

fun isGoldenWeek(week: Int): Boolean {
    return week == 11 || week == 12 || week == 13 ||
           week == 20 || week == 21 || week == 22 || week == 23 || week == 24 ||
           week == 25 || week == 26 || week == 27 || week == 28 ||
           week == 32 || week >= 36
}

fun getEmojiForTheme(week: Int, theme: String): String {
    return when (theme) {
        "FRUIT" -> when (week) {
            1 -> "🌱"
            2 -> "💫"
            3 -> "✨"
            4 -> "🍒"
            5 -> "🍎"
            6 -> "🫐"
            7 -> "🍓"
            8 -> "🍇"
            9 -> "🍒"
            10 -> "🍊"
            11 -> "🧁"
            12 -> "🍋"
            13 -> "🍑"
            14 -> "🍋"
            15 -> "🍏"
            16 -> "🥑"
            17 -> "🍊"
            18 -> "🥔"
            19 -> "🍅"
            20 -> "🍌"
            21 -> "🥒"
            22 -> "🥭"
            23 -> "🍊"
            24 -> "🍈"
            25 -> " Broccoli"
            26 -> "🎃"
            27 -> "🥬"
            28 -> "🥥"
            29 -> " Pineapple"
            30 -> "🌽"
            31 -> "🥬"
            32 -> "🍉"
            33 -> "🍈"
            34 -> "🍈"
            35 -> "🍐"
            36 -> "🥭"
            37 -> "🥥"
            38 -> "🍊"
            39 -> "🍉"
            40 -> "🎃"
            41 -> "🍈"
            else -> "🍎"
        }
        "BAKERY" -> when (week) {
            1 -> "🌾"
            2 -> "🧁"
            3 -> "🍬"
            4 -> "🍬"
            5 -> "🍫"
            6 -> "🍬"
            7 -> "🧁"
            8 -> "🍮"
            9 -> "🧸"
            10 -> "🧁"
            11 -> "🍭"
            12 -> "🧁"
            13 -> "🥧"
            14 -> "🍪"
            15 -> "🧁"
            16 -> "🍩"
            17 -> "🥖"
            18 -> "🥐"
            19 -> "🍞"
            20 -> "🥞"
            21 -> "🍦"
            22 -> "🍰"
            23 -> "🍞"
            24 -> "🧁"
            25 -> "🧇"
            26 -> "🥐"
            27 -> "🎂"
            28 -> "🍰"
            29 -> "🍞"
            30 -> "🍞"
            31 -> "🥧"
            32 -> "🎂"
            33 -> "🥖"
            34 -> "🪵"
            35 -> "🎂"
            36 -> "🧄"
            37 -> "🎂"
            38 -> "🍯"
            39 -> "💒"
            40 -> "🎂"
            41 -> "🎂"
            else -> "🧁"
        }
        "ANIMAL" -> when (week) {
            1 -> "🥚"
            2 -> "🦋"
            3 -> "🐟"
            4 -> "🐞"
            5 -> "🐞"
            6 -> "🐜"
            7 -> "🐠"
            8 -> "🐌"
            9 -> "🦋"
            10 -> "🐛"
            11 -> "🐢"
            12 -> "🐥"
            13 -> "🦐"
            14 -> "🐹"
            15 -> "🐿"
            16 -> "🐦"
            17 -> "🐿"
            18 -> "🐹"
            19 -> "🐿"
            20 -> "🐢"
            21 -> "🦎"
            22 -> "🐒"
            23 -> "🐱"
            24 -> "🐿"
            25 -> "🦔"
            26 -> "🐕"
            27 -> "🦦"
            28 -> "🐰"
            29 -> "🐑"
            30 -> "🐨"
            31 -> "🐒"
            32 -> "🐷"
            33 -> "🐼"
            34 -> "🐰"
            35 -> "🐱"
            36 -> "🦝"
            37 -> "🐐"
            38 -> "🐑"
            39 -> "🐕"
            40 -> "🦭"
            41 -> "🐻"
            else -> "🐹"
        }
        else -> "❤️"
    }
}

@Composable
fun RemindersTab(
    viewModel: PregnancyViewModel,
    allReminders: List<MedicationReminderEntity>
) {
    val focusManager = LocalFocusManager.current
    val allLogs by viewModel.allLogsState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<MedicationReminderEntity?>(null) }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val medicineReminders = allReminders.filter { it.type == "MEDICINE" }
    val dietReminders = allReminders.filter { it.type == "DIET" }

    // STATISTICS CALCULATION FOR QUICK COMPLIANCE DASHBOARD
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val past7Days = (0..6).map { i ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -i)
        sdf.format(cal.time)
    }

    val past30Days = (0..29).map { i ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -i)
        sdf.format(cal.time)
    }

    val logs7 = allLogs.filter { it.date in past7Days }
    val taken7 = logs7.count { it.status == "TAKEN" }
    val rejected7 = logs7.count { it.status == "REJECTED" }
    val total7 = taken7 + rejected7
    val complianceRate7 = if (total7 > 0) (taken7 * 100) / total7 else 100

    val logs30 = allLogs.filter { it.date in past30Days }
    val taken30 = logs30.count { it.status == "TAKEN" }
    val rejected30 = logs30.count { it.status == "REJECTED" }
    val total30 = taken30 + rejected30
    val complianceRate30 = if (total30 > 0) (taken30 * 100) / total30 else 100

    // Popup dialog for ADDING a reminder
    if (showAddDialog) {
        var addName by remember { mutableStateOf("") }
        var addDosage by remember { mutableStateOf("") }
        var addTimeStr by remember { mutableStateOf("08:00") }
        var addType by remember { mutableStateOf("MEDICINE") }
        var addFreq by remember { mutableStateOf("Hàng ngày") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⏰ Lên Lịch Nhắc Cố Định",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrownSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeSelectorButton(
                            text = "Vi Chất / Thuốc",
                            emoji = "💊",
                            isActive = addType == "MEDICINE",
                            onClick = { addType = "MEDICINE" },
                            modifier = Modifier.weight(1f)
                        )
                        TypeSelectorButton(
                            text = "Dinh Dưỡng",
                            emoji = "🍎",
                            isActive = addType == "DIET",
                            onClick = { addType = "DIET" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PregnancyTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = if (addType == "MEDICINE") "Tên thuốc / vi chất" else "Món ăn bồi bổ",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = addDosage,
                        onValueChange = { addDosage = it },
                        label = "Liều lượng / cách dùng",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTimePickerField(
                        label = "Giờ nhắc nở",
                        currentTimeHm = addTimeStr,
                        onTimeSelected = { addTimeStr = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "📅 Chu kỳ lặp cố định",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Hàng ngày",
                            "Thứ 2-4-6",
                            "Thứ 3-5-7",
                            "Cuối tuần (T7, CN)",
                            "Mỗi 2 ngày"
                        ).forEach { freq ->
                            val isSel = addFreq == freq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DeepBrownSecondary else WarmBackground)
                                    .border(1.dp, if (isSel) DeepBrownSecondary else LightBorder, RoundedCornerShape(8.dp))
                                    .clickable { addFreq = freq }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = freq,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextSlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Text("Hủy bỏ", color = TextSlate, fontWeight = FontWeight.Bold)
                        }

                        LiquidGlassButton(
                            onClick = {
                                if (addName.isNotBlank() && addDosage.isNotBlank()) {
                                    viewModel.addReminder(
                                        name = addName,
                                        dosage = "$addDosage (Lặp: $addFreq)",
                                        time = addTimeStr,
                                        type = addType
                                    )
                                    showAddDialog = false
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("Kích Hoạt", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog editing reminder details
    if (editingReminder != null) {
        val reminderToEdit = editingReminder!!
        var editName by remember(reminderToEdit) { mutableStateOf(reminderToEdit.medicationName) }

        // Extract base dosage if it contains " (Lặp: "
        val baseDosageInit = if (reminderToEdit.dosage.contains(" (Lặp: ")) {
            reminderToEdit.dosage.substringBefore(" (Lặp: ")
        } else {
            reminderToEdit.dosage
        }
        val editFreqInit = if (reminderToEdit.dosage.contains(" (Lặp: ")) {
            reminderToEdit.dosage.substringAfter(" (Lặp: ").removeSuffix(")")
        } else {
            "Hàng ngày"
        }

        var editDosage by remember(reminderToEdit) { mutableStateOf(baseDosageInit) }
        var editTimeStr by remember(reminderToEdit) { mutableStateOf(reminderToEdit.scheduledTime) }
        var editType by remember(reminderToEdit) { mutableStateOf(reminderToEdit.type) }
        var editFreq by remember(reminderToEdit) { mutableStateOf(editFreqInit) }

        Dialog(onDismissRequest = { editingReminder = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "✏️ Chỉnh Sửa Nhắc Nhở",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrownSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeSelectorButton(
                            text = "Vi Chất / Thuốc",
                            emoji = "💊",
                            isActive = editType == "MEDICINE",
                            onClick = { editType = "MEDICINE" },
                            modifier = Modifier.weight(1f)
                        )
                        TypeSelectorButton(
                            text = "Dinh Dưỡng",
                            emoji = "🍎",
                            isActive = editType == "DIET",
                            onClick = { editType = "DIET" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PregnancyTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "Tên nhắc nhở",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = editDosage,
                        onValueChange = { editDosage = it },
                        label = "Liều lượng / Cách dùng",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTimePickerField(
                        label = "Giờ nhắc",
                        currentTimeHm = editTimeStr,
                        onTimeSelected = { editTimeStr = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "📅 Lặp nhắc nhở",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Hàng ngày",
                            "Thứ 2-4-6",
                            "Thứ 3-5-7",
                            "Cuối tuần (T7, CN)",
                            "Mỗi 2 ngày"
                        ).forEach { freq ->
                            val isSel = editFreq == freq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DeepBrownSecondary else WarmBackground)
                                    .border(1.dp, if (isSel) DeepBrownSecondary else LightBorder, RoundedCornerShape(8.dp))
                                    .clickable { editFreq = freq }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = freq,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextSlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingReminder = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Text("Hủy", color = TextSlate, fontWeight = FontWeight.Bold)
                        }

                        LiquidGlassButton(
                            onClick = {
                                if (editName.isNotBlank() && editDosage.isNotBlank()) {
                                    val fullDosage = "$editDosage (Lặp: $editFreq)"
                                    viewModel.updateReminderDetails(
                                        reminderId = reminderToEdit.id,
                                        name = editName,
                                        dosage = fullDosage,
                                        time = editTimeStr,
                                        type = editType
                                    )
                                    editingReminder = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cập Nhật", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 86.dp, top = 10.dp)
        ) {
            // COMPACT COMPLIANCE DASHBOARD CARD FOR ACCURATE INSIGHTS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SoftPeachPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = DeepBrownSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📊 BÁO CÁO TUÂN THỦ LỊCH NHẮC",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepBrownSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Weekly Report
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tuần này (7 ngày)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$complianceRate7%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (complianceRate7 >= 80) EmeraldText else AlertRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "tỉ lệ",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { complianceRate7 / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (complianceRate7 >= 80) EmeraldText else AlertRed,
                                    trackColor = WarmBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "👍 Đúng giờ: $taken7  •  ❌ Bỏ qua: $rejected7",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            // Monthly Report
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tháng này (30 ngày)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$complianceRate30%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (complianceRate30 >= 80) EmeraldText else AlertRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "tỉ lệ",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { complianceRate30 / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (complianceRate30 >= 80) EmeraldText else AlertRed,
                                    trackColor = WarmBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "👍 Đúng giờ: $taken30  •  ❌ Bỏ qua: $rejected30",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Hệ thống tự động reset trạng thái nhắc nhở mỗi ngày mới nhằm đảm bảo thông tin uống thuốc / ăn uống bổ sung luôn tươi mới! 🥰",
                            fontSize = 10.sp,
                            color = DarkBrownText,
                            lineHeight = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // Doze mode advice notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WhiteCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Tránh trễ báo thức",
                            tint = DeepBrownSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Để ứng dụng nhắc nhở chính xác không bị chế độ tối ưu pin (Doze Mode) của hệ điều hành tắt âm, mẹ bầu vui lòng mở: Cài đặt -> Pin -> Tắt tối ưu hóa pin cho ứng dụng này.",
                            fontSize = 10.5.sp,
                            color = TextSlate,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Live status bar test action card
            item {
                val context = LocalContext.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PregnancyNotifier.sendNotificationImmediately(
                                context = context,
                                title = "💊 Giờ uống vi chất bổ sung thai kỳ",
                                message = "Mẹ bầu ơi! Đã đến khung giờ bổ sung Sắt, Canxi và Acid Folic vàng rồi. Đừng quên đánh dấu tích hoàn thành ngày hôm nay nhé!",
                                itemId = "DAILY_MOCK_REMINDER",
                                type = "REMINDER",
                                targetTab = 3
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = EmeraldBg.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldText.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = EmeraldText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔔 Thử nghiệm chu báo uống vi chất / ăn uống",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldText
                            )
                            Text(
                                text = "Bấm vào card này để test thử hiển thị thông báo uống vi chất lập tức trên điện thoại của mẹ!",
                                fontSize = 11.sp,
                                color = DarkBrownText
                            )
                        }
                        Text("TEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldText, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            // 1. Group MEDICINE Reminders
            item {
                Text(
                    text = "💊 Trình Nhắc Vi Chất & Thuốc Hôm Nay (Nhấn xác nhận)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepBrownSecondary
                )
            }

            if (medicineReminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WhiteCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Text(
                            text = "Chưa có trình nhắc vi chất nào hoạt động hôm nay.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(medicineReminders) { reminder ->
                    val todayLog = allLogs.find { it.reminderId == reminder.id && it.date == todayStr }
                    ReminderScheduleCard(
                        reminder = reminder,
                        todayLog = todayLog,
                        viewModel = viewModel,
                        activeColor = SoftPeachPrimary,
                        onEditClick = { editingReminder = reminder }
                    )
                }
            }

            // 2. Group DIET Reminders
            item {
                Text(
                    text = "🍎 Chế Độ Ăn Dinh Dưỡng & Bữa Phụ (Nhấn xác nhận)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepBrownSecondary
                )
            }

            if (dietReminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WhiteCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Text(
                            text = "Chưa có chế độ ăn uống bồi bổ nhắc nhở nào hoạt động.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(dietReminders) { reminder ->
                    val todayLog = allLogs.find { it.reminderId == reminder.id && it.date == todayStr }
                    ReminderScheduleCard(
                        reminder = reminder,
                        todayLog = todayLog,
                        viewModel = viewModel,
                        activeColor = AmberBg,
                        onEditClick = { editingReminder = reminder }
                    )
                }
            }
        }

        // Floating Action Button (FAB) for adding new fixed day reminders
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = DeepBrownSecondary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_reminder")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Thêm lịch nhắc mới",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PregnancyOnboardingScreen(
    viewModel: PregnancyViewModel,
    user: UserEntity
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf(user.name) }
    var dobDate by remember { mutableStateOf(user.dobDate ?: "") }
    var lmpDate by remember { mutableStateOf(user.lmpDate ?: "") }
    var eddDate by remember { mutableStateOf(user.eddDate ?: "") }
    var conceptionDate by remember { mutableStateOf(user.conceptionDate ?: "") }
    var startWeightKg by remember { mutableStateOf(user.gestationStartWeight.toString()) }
    var targetWeightKg by remember { mutableStateOf(user.targetPregnancyWeight.toString()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Charming Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌸 Thiết Lập Hồ Sơ Thai Kỳ",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrownSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Mẹ bầu khai báo một lần duy nhất để lưu trữ chuẩn xác mọi thông tin sức khỏe của thai kỳ, tránh trường hợp thoát ứng dụng bị lãng quên hoặc phải điền lại từ đầu.",
                        fontSize = 12.5.sp,
                        color = DarkBrownText,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Seed prefill option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.5.dp, LightBorder),
                onClick = {
                    name = "Trần Khánh Vy"
                    dobDate = "1997-08-20"
                    lmpDate = "2026-02-10"
                    eddDate = "2026-11-17"
                    conceptionDate = "2026-02-24"
                    startWeightKg = "48.5"
                    targetWeightKg = "60.0"
                }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Prefill",
                        tint = DeepBrownSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💡 Nhập nhanh mẫu trải nghiệm của mẹ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary
                        )
                        Text(
                            text = "Bấm vào thẻ này để tự động điền các thông tin thai kỳ mẫu và thử ngay.",
                            fontSize = 10.5.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Profiling Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "✍️ Khai Báo Hồ Sơ Của Mẹ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AmberBg.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, AmberText.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Gợi ý",
                                tint = AmberText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mẹ chỉ cần nhập 1 trong 3 mốc (LMP, EDD hoặc Ngày thụ thai), hệ thống sẽ tự động ước tính chính xác 2 mốc còn lại!",
                                fontSize = 11.sp,
                                color = DarkBrownText,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    PregnancyTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Họ và Tên Mẹ bầu",
                        placeholder = "Ví dụ: Nguyễn Thị Lan",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyDatePickerField(
                        label = "Ngày sinh của Mẹ",
                        currentDateYmd = dobDate,
                        onDateSelected = { dobDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyDatePickerField(
                        label = "Ngày đầu kỳ kinh cuối (LMP)",
                        currentDateYmd = lmpDate,
                        onDateSelected = { selectedDate ->
                            lmpDate = selectedDate
                            viewModel.estimateDatesFromLMP(selectedDate.trim())?.let { pair ->
                                eddDate = pair.first
                                conceptionDate = pair.second
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyDatePickerField(
                        label = "Ngày bác sĩ dự sinh (EDD)",
                        currentDateYmd = eddDate,
                        onDateSelected = { selectedDate ->
                            eddDate = selectedDate
                            viewModel.estimateDatesFromEDD(selectedDate.trim())?.let { pair ->
                                lmpDate = pair.first
                                conceptionDate = pair.second
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyDatePickerField(
                        label = "Ngày thụ thai chính thức",
                        currentDateYmd = conceptionDate,
                        onDateSelected = { selectedDate ->
                            conceptionDate = selectedDate
                            viewModel.estimateDatesFromConception(selectedDate.trim())?.let { pair ->
                                lmpDate = pair.first
                                eddDate = pair.second
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PregnancyTextField(
                            value = startWeightKg,
                            onValueChange = { startWeightKg = it },
                            label = "Cân nặng trước (kg)",
                            modifier = Modifier.weight(1f)
                        )

                        PregnancyTextField(
                            value = targetWeightKg,
                            onValueChange = { targetWeightKg = it },
                            label = "Mục tiêu tăng (kg)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LiquidGlassButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                val sWeight = startWeightKg.toDoubleOrNull() ?: 50.0
                                val tWeight = targetWeightKg.toDoubleOrNull() ?: 62.0
                                viewModel.saveInitialProfileAndComplete(
                                    name = name,
                                    lmp = lmpDate,
                                    edd = eddDate,
                                    dob = dobDate,
                                    conception = conceptionDate,
                                    startWeight = sWeight,
                                    targetWeight = tWeight
                                )
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .testTag("save_onboarding_btn")
                            .fillMaxWidth(),
                        enabled = name.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lưu hồ sơ & Bắt đầu ngay", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = Color(0xFF4285F4),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PregnancyAuthScreen(viewModel: PregnancyViewModel) {
    var isSignUpTab by remember { mutableStateOf(false) }
    
    // Auth inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    
    // Google Credential picker dialog
    var showGooglePicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements for premium feel (beautiful radial/linear gradient)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WarmBackground,
                            SoftPeachPrimary.copy(alpha = 0.25f),
                            WarmPeachCard.copy(alpha = 0.15f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Icon and Brand Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(WarmPeachCard),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🤰", fontSize = 36.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "MẸ & BÉ",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBrownSecondary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Trợ lý sức khỏe thai kỳ cao cấp",
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Auth Tab Selector (Premium Rounded design)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightBorder)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val activeTabColor = ButtonDefaults.buttonColors(containerColor = WhiteCard)
                val inactiveTabColor = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                
                Button(
                    onClick = { 
                        isSignUpTab = false 
                        authError = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = if (!isSignUpTab) activeTabColor else inactiveTabColor,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = if (!isSignUpTab) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text(
                        text = "Đăng nhập", 
                        color = if (!isSignUpTab) DeepBrownSecondary else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { 
                        isSignUpTab = true 
                        authError = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = if (isSignUpTab) activeTabColor else inactiveTabColor,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = if (isSignUpTab) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text(
                        text = "Đăng ký", 
                        color = if (isSignUpTab) DeepBrownSecondary else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Display Auth Error if any
                    authError?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AlertLightBg),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, AlertBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = AlertRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = AlertDarkText,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    if (isSignUpTab) {
                        // FULL NAME (Only for sign up)
                        PregnancyTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = "Họ và tên của Mẹ",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // EMAIL input
                    PregnancyTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Địa chỉ Email",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // PASSWORD input
                    PregnancyTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Mật khẩu bảo mật",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (isSignUpTab) {
                        // CONFIRM PASSWORD (Only for sign up)
                        PregnancyTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Nhập lại mật khẩu",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ACTION BUTTON (Submit flow with Liquid Glass styling)
                    LiquidGlassButton(
                        onClick = {
                            authError = null
                            if (isSignUpTab) {
                                // Validation for registration
                                if (fullName.isBlank()) {
                                    authError = "Vui lòng nhập Họ và tên để tạo hồ sơ."
                                    return@LiquidGlassButton
                                }
                                if (!email.contains("@") || !email.contains(".")) {
                                    authError = "Vui lòng nhập định dạng email hợp lệ."
                                    return@LiquidGlassButton
                                }
                                if (password.length < 6) {
                                    authError = "Mật khẩu phải chứa ít nhất 6 ký tự."
                                    return@LiquidGlassButton
                                }
                                if (password != confirmPassword) {
                                    authError = "Mật khẩu nhập lại không trùng khớp."
                                    return@LiquidGlassButton
                                }
                                val errorMsg = viewModel.registerWithEmail(email, password, fullName)
                                if (errorMsg != null) {
                                    authError = errorMsg
                                }
                            } else {
                                // Validation for login
                                if (email.isBlank() || password.isBlank()) {
                                    authError = "Vui lòng điền đầy đủ email và mật khẩu."
                                    return@LiquidGlassButton
                                }
                                val errorMsg = viewModel.loginWithEmail(email, password)
                                if (errorMsg != null) {
                                    authError = errorMsg
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSignUpTab) "Hoàn tất đăng ký" else "Đăng nhập ngay",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Visual Divider
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = LightBorder)
                Text(
                    text = "Hoặc",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = TextMuted,
                    fontSize = 11.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = LightBorder)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // GOOGLE SIGN IN BUTTON (Sleek, beautifully styled pill with Liquid Glass premium shine)
            LiquidGlassButton(
                onClick = { 
                    viewModel.loginWithGoogle("vankhoai690@gmail.com", "Nguyễn Văn Khoai")
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = WhiteCard,
                contentColor = TextSlate
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleLogoIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tiếp tục với Google",
                        color = TextSlate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Google credential selector dialog
    if (showGooglePicker) {
        Dialog(onDismissRequest = { showGooglePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đăng nhập Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextSlate
                        )
                        IconButton(
                            onClick = { showGooglePicker = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Chọn tài khoản Google của bạn để liên kết nhanh chóng và bắt đầu theo dõi sức khỏe:",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    // Profile options representing realistic logins
                    val googleAccounts = listOf(
                        Pair("vankhoai690@gmail.com", "Nguyễn Văn Khoai"),
                        Pair("mebau_hanhphuc@gmail.com", "Nguyễn Minh Anh"),
                        Pair("thanhvy_pregnancy@gmail.com", "Trần Khánh Vy")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        googleAccounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarmBackground)
                                    .clickable {
                                        viewModel.loginWithGoogle(account.first, account.second)
                                        showGooglePicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(SoftPeachPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = account.second.first().toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepBrownSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = account.second,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSlate
                                    )
                                    Text(
                                        text = account.first,
                                        fontSize = 10.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PregnancyDatePickerField(
    label: String,
    currentDateYmd: String, // format yyyy-MM-dd
    onDateSelected: (String) -> Unit, // passes yyyy-MM-dd
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Convert current selected date from yyyy-MM-dd to dd-MM-yyyy for elegant display
    val displayValue = remember(currentDateYmd) {
        if (currentDateYmd.isBlank()) "" else {
            try {
                val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outputSdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val parsed = inputSdf.parse(currentDateYmd.trim())
                if (parsed != null) outputSdf.format(parsed) else currentDateYmd
            } catch (e: Exception) {
                currentDateYmd
            }
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Chọn ngày",
                    tint = DeepBrownSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextSlate,
                unfocusedTextColor = TextSlate,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = DeepBrownSecondary,
                unfocusedLabelColor = TextMuted,
                focusedBorderColor = DeepBrownSecondary,
                unfocusedBorderColor = LightBorder,
                focusedPlaceholderColor = TextMuted.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        // Hidden transparent clickable overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val calendar = Calendar.getInstance()
                    if (currentDateYmd.isNotBlank()) {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val dateObj = sdf.parse(currentDateYmd.trim())
                            if (dateObj != null) calendar.time = dateObj
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    
                    android.app.DatePickerDialog(
                        context,
                        { _, sYear, sMonth, sDay ->
                            val selCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, sYear)
                                set(Calendar.MONTH, sMonth)
                                set(Calendar.DAY_OF_MONTH, sDay)
                            }
                            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selCal.time)
                            onDateSelected(formatted)
                        },
                        year,
                        month,
                        day
                    ).show()
                }
        )
    }
}

@Composable
fun PregnancyTimePickerField(
    label: String,
    currentTimeHm: String, // format HH:mm
    onTimeSelected: (String) -> Unit, // passes HH:mm
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = currentTimeHm,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Chọn giờ",
                    tint = DeepBrownSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextSlate,
                unfocusedTextColor = TextSlate,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = DeepBrownSecondary,
                unfocusedLabelColor = TextMuted,
                focusedBorderColor = DeepBrownSecondary,
                unfocusedBorderColor = LightBorder,
                focusedPlaceholderColor = TextMuted.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        // Hidden transparent clickable overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val parts = currentTimeHm.trim().split(":")
                    val hour = if (parts.size >= 1) parts[0].toIntOrNull() ?: 8 else 8
                    val minute = if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
                    
                    android.app.TimePickerDialog(
                        context,
                        { _, sHour, sMinute ->
                            val formatted = String.format(Locale.US, "%02d:%01d0", sHour, sMinute).replace("00", "00").replace("a", "b") // format properly
                            val finalFormatted = String.format(Locale.US, "%02d:%02d", sHour, sMinute)
                            onTimeSelected(finalFormatted)
                        },
                        hour,
                        minute,
                        true // 24-hour mode
                    ).show()
                }
        )
    }
}

@Composable
fun PregnancyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextSlate,
            unfocusedTextColor = TextSlate,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedLabelColor = DeepBrownSecondary,
            unfocusedLabelColor = TextMuted,
            focusedBorderColor = DeepBrownSecondary,
            unfocusedBorderColor = LightBorder,
            focusedPlaceholderColor = TextMuted.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = DeepBrownSecondary,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    val mainBrush = Brush.verticalGradient(
        colors = listOf(
            containerColor.copy(alpha = 0.95f),
            containerColor,
            containerColor.copy(alpha = 0.92f)
        )
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(mainBrush)
            .clickable(enabled = enabled, onClick = onClick)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-end Gloss reflection overlay (representing liquid glass)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        endY = 40f
                    )
                )
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

// ==========================================
// CUSTOM ENHANCEMENT: FETAL KICK COUNTER (ĐỘNG THAI MÁY)
// ==========================================
@Composable
fun KickCounterCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // Timer & Count states
    var isTracking by remember { mutableStateOf(false) }
    var currentCount by remember { mutableStateOf(0) }
    var timeLeftSeconds by remember { mutableStateOf(3600) } // 60 minutes = 3600s
    
    // Local list of saved logs
    var historyLogs by remember { mutableStateOf(emptyList<String>()) }
    
    // Load existing logs on start
    LaunchedEffect(Unit) {
        val raw = prefs.getString("fetal_kick_logs", "") ?: ""
        if (raw.isNotBlank()) {
            historyLogs = raw.split(";;").filter { it.isNotBlank() }
        }
    }
    
    // Save helper
    val saveLogToPrefs = { dateStr: String, count: Int, durationMins: Int ->
        val newEntry = "$dateStr|${count} cử động|${durationMins} phút"
        val updatedList = (listOf(newEntry) + historyLogs).take(20) // Limit to top 20 logs
        historyLogs = updatedList
        prefs.edit().putString("fetal_kick_logs", updatedList.joinToString(";;")).apply()
    }
    
    // Timer Coroutine
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (timeLeftSeconds > 0 && isTracking) {
                delay(1000L)
                timeLeftSeconds -= 1
            }
            if (timeLeftSeconds == 0 && isTracking) {
                // Done! Auto-save
                val formatter = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
                val dateStr = formatter.format(Date())
                saveLogToPrefs(dateStr, currentCount, 60)
                isTracking = false
                // Reset session
                timeLeftSeconds = 3600
                currentCount = 0
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SoftPeachPrimary.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👣", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Máy Đếm Cử Động Thai (Thai Máy)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                }
                
                // Active/Inactive Indicator
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTracking) EmeraldBg else WarmBackground
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isTracking) "Đang đếm" else "Nhàn rỗi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTracking) EmeraldText else TextSlate,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Helpful Guide Text
            Text(
                text = "Bác sĩ khuyên đếm thai máy từ tuần 28. Ghi nhận ít nhất 4 cử động trong 1 giờ vào khung giờ cố định (sau ăn ăn tối là tốt nhất) để biết bé yêu luôn khỏe mạnh.",
                fontSize = 11.5.sp,
                color = TextSlate,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Session Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarmPeachCard.copy(alpha = 0.3f))
                    .border(0.5.dp, DeepBrownSecondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tracking Controls & Numbers
                Column(modifier = Modifier.weight(1.3f)) {
                    val minutesSymbol = timeLeftSeconds / 60
                    val secondsSymbol = timeLeftSeconds % 60
                    val timeString = String.format("%02d:%02d", minutesSymbol, secondsSymbol)
                    
                    Text(
                        text = "Thời gian: $timeString",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBrownText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cử động: $currentCount lần",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrownSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!isTracking) {
                            Button(
                                onClick = { 
                                    isTracking = true 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bắt đầu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { 
                                    // Pause & Save
                                    if (currentCount > 0) {
                                        val formatter = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
                                        val dateStr = formatter.format(Date())
                                        val elapsedMins = (3600 - timeLeftSeconds) / 60
                                        saveLogToPrefs(dateStr, currentCount, if (elapsedMins < 1) 1 else elapsedMins)
                                    }
                                    isTracking = false
                                    timeLeftSeconds = 3600
                                    currentCount = 0
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lưu & Dừng", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        
                        // Reset session
                        if (currentCount > 0 || timeLeftSeconds < 3600) {
                            OutlinedButton(
                                onClick = {
                                    isTracking = false
                                    timeLeftSeconds = 3600
                                    currentCount = 0
                                },
                                border = BorderStroke(1.dp, LightBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSlate)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Đặt lại", fontSize = 11.sp, color = TextSlate)
                            }
                        }
                    }
                }
                
                // Giant Tappable Heart / Footprint Button to count a kick
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isTracking) SoftPeachPrimary else WarmBackground)
                        .border(1.5.dp, if (isTracking) DeepBrownSecondary else LightBorder, CircleShape)
                        .clickable {
                            if (!isTracking) {
                                isTracking = true
                            }
                            currentCount += 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👣",
                            fontSize = 30.sp,
                            modifier = Modifier.animateContentSize()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "BÉ ĐẠP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTracking) DeepBrownSecondary else TextSlate
                        )
                    }
                }
            }
            
            // Evaluation advice text based on current count
            if (isTracking && currentCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val isNormal = currentCount >= 4
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNormal) EmeraldBg.copy(alpha = 0.3f) else SoftPeachPrimary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isNormal) "🎉" else "💡", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isNormal) {
                                "Số thai máy bình thường (${currentCount}/4)! Bé hiếu động đáng yêu và cực kỳ khỏe mạnh."
                            } else {
                                "Hơi ít (${currentCount}/4). Thử uống nước ấm, nghiêng trái, xoa dịu liên lạc với bé mẹ nhé!"
                            },
                            fontSize = 11.sp,
                            color = if (isNormal) EmeraldText else DarkBrownText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // History list collapsible section
            Spacer(modifier = Modifier.height(10.dp))
            var isHistoryExpanded by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isHistoryExpanded = !isHistoryExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📋 Nhật Ký Ghi Nhận Lần Đếm (${historyLogs.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrownSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHistoryExpanded) "Thu lại" else "Chi tiết",
                        fontSize = 11.sp,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isHistoryExpanded) "▲" else "▼",
                        fontSize = 9.sp,
                        color = TextSlate
                    )
                }
            }
            
            if (isHistoryExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (historyLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có lưu lịch sử thai máy. Bắt đầu đếm ngay hôm nay nhé!",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        historyLogs.forEach { log ->
                            val parts = log.split("|")
                            if (parts.size >= 3) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(WarmBackground, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = parts[0], fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
                                        Text(text = "Thời lượng tích lũy: ${parts[2]}", fontSize = 10.sp, color = TextSlate)
                                    }
                                    
                                    val countNum = parts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (countNum >= 4) EmeraldBg else SoftPeachPrimary
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${parts[1]} (${if (countNum >= 4) "Tốt ✨" else "Hơi thấp"})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (countNum >= 4) EmeraldText else DeepBrownSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        // Clear history
                        TextButton(
                            onClick = {
                                prefs.edit().remove("fetal_kick_logs").apply()
                                historyLogs = emptyList()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = AlertRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa sạch lịch sử", fontSize = 11.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOM ENHANCEMENT: WATER TRACKER (THEO DÕI NƯỚC ỐI)
// ==========================================
@Composable
fun WaterTrackerCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // Date key to persist daily tracking
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val waterKey = remember { "daily_water_$todayDateStr" }
    
    var currentWaterMl by remember { mutableStateOf(0) }
    val targetWaterMl = 2500 // Daily recommended for pregnant mothers
    
    // Load existing water log on launch
    LaunchedEffect(Unit) {
        currentWaterMl = prefs.getInt(waterKey, 0)
    }
    
    val updateWater = { ml: Int ->
        val newVal = (currentWaterMl + ml).coerceIn(0, 5000)
        currentWaterMl = newVal
        prefs.edit().putInt(waterKey, newVal).apply()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💧", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nhật Ký Uống Nước Giữ Đầu Ối",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                }
                
                // Clear button
                if (currentWaterMl > 0) {
                    TextButton(
                        onClick = {
                            currentWaterMl = 0
                            prefs.edit().putInt(waterKey, 0).apply()
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Đặt lại", fontSize = 10.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Medical Tip
            Text(
                text = "Nguồn uống trực tiếp nuôi dưỡng và tái tạo lượng nước ối bảo bọc thai nhi. Mẹ bầu cần hoàn thành 2.5 lít nước ấm trải đều cả ngày để giữ nồng độ ối vàng an toàn đạt chuẩn.",
                fontSize = 11.5.sp,
                color = TextSlate,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visual Wave Progress Card
            val progressFactor = (currentWaterMl.toFloat() / targetWaterMl.toFloat()).coerceIn(0f, 1f)
            val isTargetReached = currentWaterMl >= targetWaterMl
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F8E9))
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (isTargetReached) EmeraldText.copy(alpha = 0.3f) else Color(0xFFBBDEFB)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                // Background blue wave progress simulator
                if (progressFactor > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFactor)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9))
                                )
                            )
                    )
                }
                
                // Content over the background
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTargetReached) "🏆 ĐÃ HOÀN THÀNH TIÊU CHUẨN ĐẦU ỐI" else "Đang bồi đắp lượng ối",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTargetReached) EmeraldText else DeepBrownSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currentWaterMl",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTargetReached) EmeraldText else Color(0xFF1565C0)
                            )
                            Text(
                                text = " / $targetWaterMl ml",
                                fontSize = 13.sp,
                                color = TextSlate,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                    }
                    
                    // Cute animated bottle or drops
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTargetReached) "🥛" else "🥤",
                            fontSize = 26.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Buttons to Log Water
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { updateWater(250) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("🥛 +250ml", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Button(
                    onClick = { updateWater(500) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("🍼 +500ml", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                if (currentWaterMl > 0) {
                    OutlinedButton(
                        onClick = { updateWater(-250) },
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("-250ml", fontSize = 11.sp, color = TextSlate)
                    }
                }
            }
        }
    }
}


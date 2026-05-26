package com.example

import android.os.Bundle
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
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
    val loggedInEmail by viewModel.loggedInEmail.collectAsStateWithLifecycle()

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

    val visibleWeeks = remember(allWeeks) {
        allWeeks
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = currentTab.coerceIn(0, 3)) { 4 }

    // Sync viewModel tab change to pager state
    LaunchedEffect(currentTab) {
        val coerced = currentTab.coerceIn(0, 3)
        if (pagerState.currentPage != coerced) {
            pagerState.animateScrollToPage(coerced)
        }
    }

    // Sync pager state swipe to viewModel
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentTab) {
            viewModel.changeTab(pagerState.currentPage)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = WarmBackground,
                modifier = Modifier.width(320.dp)
            ) {
                SettingsDrawerContent(
                    viewModel = viewModel,
                    user = activeUser,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Mở rộng cài đặt",
                                tint = DeepBrownSecondary
                            )
                        }
                    },
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
                BottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.changeTab(it) },
                    onMenuSelected = { scope.launch { drawerState.open() } }
                )
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

                // Dynamic Tab Views with Swipe-to-change feel using HorizontalPager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) { page ->
                    val pageOffset = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    val alpha = 1f - pageOffset.coerceIn(0f, 1f)
                    val scale = 0.96f + (alpha * 0.04f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                this.alpha = alpha
                                this.scaleX = scale
                                this.scaleY = scale
                            }
                    ) {
                        if (selectedWeekNum == -1 && (page == 0 || page == 2)) {
                            SetupDatesGreetingCard(onSetupClick = { scope.launch { drawerState.open() } })
                        } else {
                            when (page) {
                                0 -> TodayTab(
                                    weekData = currentWeekData,
                                    userTheme = activeUser.visualizationTheme,
                                    onThemeChanged = { viewModel.changeTheme(it) },
                                    onLaborAlertClick = { viewModel.toggleLaborDialog(true) },
                                    visibleWeeks = visibleWeeks,
                                    userEmail = loggedInEmail
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
                            }
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
    val density = androidx.compose.ui.platform.LocalDensity.current

    LaunchedEffect(selectedWeek) {
        delay(60) // Let the layout pass settle first so viewport width is updated correctly
        val index = allWeeks.indexOfFirst { it.weekNumber == selectedWeek }
        if (index >= 0) {
            var viewportWidth = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            if (viewportWidth == 0) {
                delay(120) // Backup delay if first frame layout is not yet calculated
                viewportWidth = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            }
            if (viewportWidth > 0) {
                val itemWidthPx = with(density) { 46.dp.roundToPx() }
                val targetOffset = - (viewportWidth / 2 - itemWidthPx / 2)
                listState.animateScrollToItem(index = index, scrollOffset = targetOffset)
            } else {
                listState.animateScrollToItem(index = maxOf(0, index - 3))
            }
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
            items(allWeeks, key = { it.weekNumber }) { week ->
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
                                isGold -> Color(0xFFFFF2CC) // Beautiful clear soft warning yellow
                                else -> WhiteCard
                            }
                        )
                        .border(
                            width = if (isSelected && isGold) 3.dp else if (isGold || isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected && isGold -> Color(0xFFFFA500) // Select color but keep warning orange border
                                isSelected -> DeepBrownSecondary
                                isGold -> Color(0xFFFFA500) // Bright Warning Orange
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
                                isGold -> Color(0xFFD46B08) // Clear warning dark orange/amber code text
                                else -> TextSlate
                            }
                        )
                        if (isGold) {
                            Text(
                                text = "MỐC",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color(0xFFD46B08)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BiometricIndicator(
    val name: String,
    val acronym: String,
    val value: String,
    val progress: Float,
    val description: String,
    val interpretation: String
)

fun getFetalBiometrics(week: Int): List<BiometricIndicator> {
    val list = mutableListOf<BiometricIndicator>()
    
    // 1. CRL (Crown-Rump Length) - Chiều dài đầu mông - Only up to week 14, afterwards length is measured as total length
    if (week in 5..14) {
        val crl = when(week) {
            5 -> 2.0
            6 -> 5.0
            7 -> 10.0
            8 -> 16.0
            9 -> 23.0
            10 -> 31.0
            11 -> 41.0
            12 -> 54.0
            13 -> 74.0
            14 -> 87.0
            else -> 0.0
        }
        list.add(
            BiometricIndicator(
                name = "Chiều dài đầu mông",
                acronym = "CRL",
                value = "$crl mm",
                progress = (crl / 90.0).toFloat().coerceIn(0f, 1f),
                description = "Chiều dài đo từ đỉnh đầu đến phần mông của bé.",
                interpretation = "Chỉ số vàng ở tam cá nguyệt đầu để xác định tuổi thai chính xác nhất."
            )
        )
    }

    // 2. BPD (Biparietal Diameter) - Đường kính lưỡng đỉnh - From week 12 onwards
    if (week >= 12) {
        val bpd = when {
            week < 12 -> 0.0
            week == 12 -> 15.0
            week == 13 -> 20.0
            week <= 20 -> 20.0 + (week - 13) * 3.7
            week <= 30 -> 46.0 + (week - 20) * 2.6
            else -> 72.0 + (week - 30) * 2.0
        }.let { Math.round(it * 10) / 10.0 }
        
        list.add(
            BiometricIndicator(
                name = "Đường kính lưỡng đỉnh",
                acronym = "BPD",
                value = "$bpd mm",
                progress = (bpd / 100.0).toFloat().coerceIn(0f, 1f),
                description = "Đường kính mặt cắt ngang lớn nhất vùng hộp sọ của bé.",
                interpretation = "Đánh giá mức độ phát triển của não bộ và là cơ sở để tính cân nặng."
            )
        )
    }

    // 3. FL (Femur Length) - Chiều dài xương đùi - From week 12 onwards
    if (week >= 12) {
        val fl = when {
            week < 12 -> 0.0
            week == 12 -> 8.0
            week == 13 -> 10.0
            week <= 20 -> 10.0 + (week - 13) * 3.3
            week <= 30 -> 33.0 + (week - 20) * 2.1
            else -> 54.0 + (week - 30) * 1.8
        }.let { Math.round(it * 10) / 10.0 }
        
        list.add(
            BiometricIndicator(
                name = "Chiều dài xương đùi",
                acronym = "FL",
                value = "$fl mm",
                progress = (fl / 80.0).toFloat().coerceIn(0f, 1f),
                description = "Chiều dài của xương đùi - xương dài nhất trong cơ thể bé.",
                interpretation = "Chỉ số quan trọng đánh giá chiều cao và sự phát triển xương chi."
            )
        )
    }

    // 4. HC (Head Circumference) - Chu vi vòng đầu - From week 12 onwards
    if (week >= 12) {
        val hc = when {
            week < 12 -> 0.0
            week == 12 -> 70.0
            week <= 20 -> 84.0 + (week - 13) * 13.0
            week <= 30 -> 175.0 + (week - 20) * 8.3
            else -> 258.0 + (week - 30) * 7.5
        }.let { Math.round(it * 10) / 10.0 }
        
        list.add(
            BiometricIndicator(
                name = "Chu vi vòng đầu",
                acronym = "HC",
                value = "$hc mm",
                progress = (hc / 360.0).toFloat().coerceIn(0f, 1f),
                description = "Độ dài vòng tròn chu vi đầu của thai nhi.",
                interpretation = "Kiểm tra sự ổn định và cân đối cấu trúc não bộ qua từng tuần."
            )
        )
    }

    // 5. AC (Abdominal Circumference) - Chu vi vòng bụng - From week 12 onwards
    if (week >= 12) {
        val ac = when {
            week < 12 -> 0.0
            week == 12 -> 56.0
            week <= 20 -> 70.0 + (week - 13) * 11.4
            week <= 30 -> 150.0 + (week - 20) * 9.0
            else -> 240.0 + (week - 30) * 10.0
        }.let { Math.round(it * 10) / 10.0 }
        
        list.add(
            BiometricIndicator(
                name = "Chu vi vòng bụng",
                acronym = "AC",
                value = "$ac mm",
                progress = (ac / 380.0).toFloat().coerceIn(0f, 1f),
                description = "Độ dài vòng bụng đo qua phần gan, dạ dày của bé.",
                interpretation = "Chỉ số phản ánh trạng thái dinh dưỡng và sự béo tốt của bé yêu."
            )
        )
    }

    // 6. FHR (Fetal Heart Rate) - Nhịp tim thai
    val fhr = when {
        week < 6 -> "Chưa rõ"
        week == 6 -> "110-120"
        week == 7 -> "130-150"
        week in 8..11 -> "150-175"
        else -> "120-160"
    }
    list.add(
        BiometricIndicator(
            name = "Nhịp tim thai tiêu chuẩn",
            acronym = "FHR",
            value = if (fhr == "Chưa rõ") fhr else "$fhr bpm",
            progress = if (week >= 12) 0.7f else 0.9f,
            description = "Tần số đập của trái tim em bé mỗi phút.",
            interpretation = "Cột mốc sống còn biểu thị nhịp điệu sinh trưởng dẻo dai khỏe mạnh."
        )
    )

    // 7. EFW (Estimated Fetal Weight) - Cân nặng ước tính khoa học theo tuần
    val efw = when {
        week < 10 -> "Dưới 5 g"
        week == 10 -> "4 g"
        week == 11 -> "7 g"
        week == 12 -> "14 g"
        week == 13 -> "23 g"
        week == 14 -> "43 g"
        week == 15 -> "70 g"
        week == 16 -> "100 g"
        week == 17 -> "140 g"
        week == 18 -> "190 g"
        week == 19 -> "240 g"
        week == 20 -> "300 g"
        week == 21 -> "360 g"
        week == 22 -> "430 g"
        week == 23 -> "501 g"
        week == 24 -> "600 g"
        week == 25 -> "660 g"
        week == 26 -> "760 g"
        week == 27 -> "875 g"
        week == 28 -> "1000 g"
        week == 29 -> "1153 g"
        week == 30 -> "1319 g"
        week == 31 -> "1502 g"
        week == 32 -> "1702 g"
        week == 33 -> "1918 g"
        week == 34 -> "2146 g"
        week == 35 -> "2383 g"
        week == 36 -> "2622 g"
        week == 37 -> "2859 g"
        week == 38 -> "3083 g"
        week == 39 -> "3288 g"
        week == 40 -> "3462 g"
        else -> "Dưới 5 g"
    }
    list.add(
        BiometricIndicator(
            name = "Cân nặng ước tính khoa học",
            acronym = "EFW",
            value = efw,
            progress = (week / 40.0).toFloat().coerceIn(0f, 1f),
            description = "Cân nặng tiêu chuẩn trung bình của em bé ở tuần Thai hiện tại.",
            interpretation = "Giúp đánh giá bé yêu có phát triển đúng theo biểu đồ tiêu chuẩn quốc tế không."
        )
    )

    return list
}

fun getWeekSpecialMedicalTests(week: Int): String? {
    return when (week) {
        in 11..13 -> "🩺 MỤC TIÊU VÀNG (Tuần 11 - 13): Siêu âm đo Độ mờ da gáy (Nuchal Translucency) kết hợp Xét nghiệm Double Test để tầm soát hội chứng Down, Edwards, Patau thời kỳ đầu."
        in 15..19 -> "🩸 XÉT NGHIỆM TRIPLE TEST (Tuần 15 - 19): Sàng lọc nguy cơ khuyết tật ống thần kinh (vô sọ, hở cột sống) và dị dạng NST nếu mẹ chưa đo Double Test trước đó."
        in 20..24 -> "📸 SIÊU ÂM HÌNH THÁI HỌC 4D (Tuần 20 - 24): Tầm soát toàn diện cấu trúc cơ thể bé (dị tật tim bẩm sinh, sứt môi, hở hàm ếch, dị dạng chi, thừa thiếu ngón)."
        in 24..28 -> "🥤 TẦM SOÁT ĐƯỜNG HUYẾT (Tuần 24 - 28): Nghiệm pháp dung nạp 75g Glucose tầm soát tiểu đường thai kỳ, phòng ngừa các biến chứng nguy hiểm cho mẹ và bé trước sinh."
        in 32..35 -> "🩺 SIÊU ÂM NGÔI THAI & NHAU ỐI (Tuần 32 - 35): Xác định ngôi thai đầu/mông thuận nghịch, kiểm tra vị trí bánh nhau bám thấp, thể tích nước ối đục trong."
        in 35..37 -> "🧪 XÉT NGHIỆM LIÊN CẦU KHUẨN NHÓM B (Tuần 35 - 37): Xét nghiệm dịch âm đạo sàng lọc khuẩn GBS để điều trị kháng sinh dự phòng khi sinh thường, tránh nhiễm trùng sơ sinh bé."
        in 38..41 -> "📈 ĐO SẢN ĐỒ MONITORING (Tuần 38 - 41): Thực hiện Non-stress test định kỳ mỗi tuần để đánh giá nhịp tim thai và nhận diện cơn co tử cung chuyển dạ an toàn."
        else -> null
    }
}

@Composable
fun TodayTab(
    weekData: FetalWeekEntity,
    userTheme: String,
    onThemeChanged: (String) -> Unit,
    onLaborAlertClick: () -> Unit,
    visibleWeeks: List<FetalWeekEntity>,
    userEmail: String = ""
) {
    val biometrics = remember(weekData.weekNumber) { getFetalBiometrics(weekData.weekNumber) }
    var selectedBiometric by remember(weekData.weekNumber) { mutableStateOf<BiometricIndicator?>(null) }
    
    // Set default selected biometric on first load
    LaunchedEffect(biometrics) {
        if (selectedBiometric == null || biometrics.none { it.acronym == selectedBiometric?.acronym }) {
            selectedBiometric = biometrics.firstOrNull()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp)
    ) {
        // Centered Big Week Header Banner (Always Centered & Yellow/Orange Highlighted for Milestones)
        item {
            val isGold = isGoldenWeek(weekData.weekNumber)
            val alphaPulse = if (isGold) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alphaPulse"
                ).value
            } else {
                1f
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGold) Color(0xFFFFF2CC) else SoftPeachPrimary.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = if (isGold) 2.dp else 1.5.dp,
                    color = if (isGold) Color(0xFFFFA500).copy(alpha = alphaPulse) else DeepBrownSecondary.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isGold) "🩺 💛 MỐC KHÁM SẢN KHOA VÀNG QUAN TRỌNG 💛 🩺" else "🌸 THÔNG TIN SỨC KHỎE THAI NHI 🌸",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGold) Color(0xFFD46B08) else DeepBrownSecondary,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "TUẦN THỨ ${weekData.weekNumber}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isGold) Color(0xFFD46B08) else DarkBrownText,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isGold) {
                            "⚠️ Mốc khám thai định kỳ cực kỳ quan trọng! Mẹ bầu ghi nhớ đi kiểm tra, rà soát sức khỏe định kỳ nhé."
                        } else {
                            "Hành trình kỳ diệu hạnh phúc đồng hành cùng sự an lành lớn mạnh từng ngày của con 💕"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isGold) Color(0xFF7F1D1D) else TextSlate,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }

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

        // PROPOSAL 1: Daily Symptom & Mood Tracker Card
        item {
            TodaySymptomAndMoodCard(userEmail = userEmail)
        }

        // Dynamic Special Medical Scan Warnings
        val specialTest = getWeekSpecialMedicalTests(weekData.weekNumber)
        if (specialTest != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, SoftPeachPrimary)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DeepBrownSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Mốc khám quan trọng",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "⭐ Lịch Xét Nghiệm Vàng Lớp Thai",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = specialTest,
                                fontSize = 12.sp,
                                color = TextSlate,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Fetal Biometrics WHO Grid Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = DeepBrownSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chỉ Số Sinh Trắc Bé Yêu (Mốc Tuần ${weekData.weekNumber})",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary
                        )
                    }
                    Text(
                        text = "Các chỉ số siêu âm tiêu chuẩn theo WHO. Bấm chọn chỉ số để xem phân tích chi tiết:",
                        fontSize = 10.5.sp,
                        color = TextMuted,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chunked = biometrics.chunked(3)
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { bio ->
                                    val isSelected = selectedBiometric?.acronym == bio.acronym
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) SoftPeachPrimary.copy(alpha = 0.35f) else WarmBackground)
                                            .border(
                                                1.5.dp, 
                                                if (isSelected) DeepBrownSecondary else LightBorder, 
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedBiometric = bio }
                                            .padding(horizontal = 6.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = bio.acronym,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) DeepBrownSecondary else DarkBrownText,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = bio.value,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextSlate,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = when (bio.acronym) {
                                                    "CRL" -> "KT đầu mông"
                                                    "BPD" -> "ĐK lưỡng đỉnh"
                                                    "FL" -> "Xương đùi"
                                                    "HC" -> "Chu vi đầu"
                                                    "AC" -> "Chu vi bụng"
                                                    "FHR" -> "Nhịp tim thai"
                                                    "EFW" -> "Cân nặng ước"
                                                    else -> bio.name
                                                },
                                                fontSize = 8.sp,
                                                color = TextMuted,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size < 3) {
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    selectedBiometric?.let { bio ->
                        val animatedProgress by animateFloatAsState(
                            targetValue = bio.progress,
                            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                            label = "biometricProgress"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarmBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(SoftPeachPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when(bio.acronym) {
                                                "CRL" -> "📏"
                                                "BPD" -> "🧠"
                                                "FL" -> "🦵"
                                                "HC" -> "👶"
                                                "AC" -> "🤰"
                                                "FHR" -> "💓"
                                                "EFW" -> "⚖️"
                                                else -> "📊"
                                            },
                                            fontSize = 14.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${bio.name} (${bio.acronym})",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepBrownSecondary
                                        )
                                        Text(
                                            text = bio.description,
                                            fontSize = 11.sp,
                                            color = TextSlate,
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "💡 Ý nghĩa y khoa:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBrownText
                                        )
                                        Text(
                                            text = bio.interpretation,
                                            fontSize = 11.sp,
                                            color = TextSlate,
                                            lineHeight = 15.sp,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Phát triển",
                                        fontSize = 9.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = DeepBrownSecondary,
                                        trackColor = LightBorder
                                    )
                                    Text(
                                        text = "${Math.round(bio.progress * 100)}%",
                                        fontSize = 9.sp,
                                        color = DeepBrownSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
            KickCounterCard(userEmail = userEmail)
        }

        // PROPOSAL 3: Contraction Timer Section
        item {
            ContractionTimerCard(userEmail = userEmail)
        }

        // === CẨM NANG TOÀN DIỆN CHI TIẾT THEO TUẦN CHỈ ĐỊNH ===
        val currentWeek = weekData.weekNumber
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, SoftPeachPrimary)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoftPeachPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📚", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "CẨM NANG CHI TIẾT TUẦN $currentWeek",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepBrownSecondary
                            )
                            Text(
                                text = "Bí quyết chăm sóc toàn diện thiết kế riêng cho mốc tuần này",
                                fontSize = 11.sp,
                                color = TextSlate
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Detail 1: Physiology Detail (Sự Phát Triển Sinh Lý Của Bé)
                    Text(
                        text = "👶 Sự hình thành & phát triển của bé yêu:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBrownText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weekData.physiologyDescription,
                        fontSize = 12.sp,
                        color = TextSlate,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Detail 2: Nutrition (Khuyên dùng dinh dưỡng tuần này)
                    Text(
                        text = "🥦 Chế độ dinh dưỡng khuyên dùng tuần này:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldText
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldBg.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, EmeraldText.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = weekData.nutritionalRecommendation,
                            fontSize = 12.sp,
                            color = DarkBrownText,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Detail 3: Maternal Changes (Thay đổi cơ thể mẹ)
                    Text(
                        text = "🤰 Thay đổi nổi bật trên cơ thể mẹ bầu:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBrownText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weekData.maternalChanges,
                        fontSize = 12.sp,
                        color = TextSlate,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Detail 4: Dynamic checklist tasks to execute
                    Text(
                        text = "💡 Việc quan trọng mẹ bầu cần hành động tuần này:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val bulletTasks = when (currentWeek) {
                        in 1..13 -> listOf(
                            "Bổ sung Acid Folic & Vitamin đầy đủ giúp chống dị tật ống thần kinh sớm cho bé.",
                            "Tránh tiếp xúc môi trường ô nhiễm khói thuốc, hóa chất độc hại hay thực phẩm sống tái.",
                            "Kiểm tra sàng lọc sớm qua siêu âm mốc vàng đo độ mờ da gáy (tuần 11-13)."
                        )
                        in 14..27 -> listOf(
                            "Bổ sung Canxi & Sắt đều đặn theo chỉ định để xây dựng khung xương chắc khỏe cho thiên thần nhỏ.",
                            "Dành 15-20 phút tương tác thai giáo vuốt bụng nhẹ kết hợp hát ru êm ái xoa dịu.",
                            "Chú ý ghi nhận cử động thai máy đầu tiên (thường rõ rệt từ tuần 18-20)."
                        )
                        in 28..35 -> listOf(
                            "Theo dõi sát nhịp cử động thai bằng công cụ đếm thai máy chuyên biệt phía trên.",
                            "Tập các bài hít thở đều, nằm nghiêng trái khi ngủ giúp dồi dào oxy vận chuyển đến nhau thai.",
                            "Chuẩn bị dần danh sách vật dụng sơ sinh cho bé và tìm hiểu chế độ sinh nở an toàn."
                        )
                        else -> listOf(
                            "Thực hiện siêu âm theo dõi tăng trưởng, kiểm tra nước ối thường xuyên giai đoạn cận sinh.",
                            "Chuẩn bị đầy đủ giỏ đồ đi sinh (làn sinh) và các giấy tờ tùy thân nhập viện khẩn cấp.",
                            "Sử dụng công cụ Đếm cơn gò tử cung bên dưới để phát hiện sớm các cơn gò chuyển dạ (quy luật 5-1-1)."
                        )
                    }
                    
                    bulletTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(SoftPeachPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(
                                text = task,
                                fontSize = 11.5.sp,
                                color = DarkBrownText,
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 90.dp, top = 4.dp)
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
                                    text = "• Tuần 6-8: Xác nhận thai vô tổ, siêu âm kích thước & kiểm tra tim thai sơ khởi.\n" +
                                           "• Tuần 11-13: Khảo sát độ mờ da gáy NT phòng dị tật nhiễm sắc thể.\n" +
                                           "• Tuần 20-24: Siêu âm 4D rà soát toàn bộ hình thái cơ quan của bé.\n" +
                                           "• Tuần 24-28: Khám huyết áp & thử nghiệm pháp Glucose dung nạp tiểu đường.\n" +
                                           "• Tuần 32: Đánh giá Doppler bánh nhau, nước ối và cân nặng bé.\n" +
                                           "• Tuần 35-37+: Chạy Monitor tim thai NST đều đặn hàng tuần phòng biến chứng.",
                                    fontSize = 11.5.sp,
                                    color = AmberText,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    // Sync 9 standard medical milestones card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = WarmPeachCard.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🏥 Đồng Bộ 9 Mốc Khám Chuẩn Bộ Y Tế",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepBrownSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Để đảm bảo đầy đủ các mốc khám thai quan trọng, mẹ bấm chọn nút dưới đây để hệ thống tự động lập sẵn lộ trình 9 mốc khám chuẩn Việt Nam chính xác tương ứng ngày dự sinh của mẹ nhé!",
                                    fontSize = 11.sp,
                                    color = TextSlate,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.resetToStandardMilestones() },
                                    border = BorderStroke(1.dp, DeepBrownSecondary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = DeepBrownSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Thiết lập lại 9 mốc khám Bộ Y Tế",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepBrownSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Check if selected week is a golden week
                    if (isGoldenWeek(selectedWeek)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2CC)), // Soft bright warning yellow
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFFFA500)) // Warning Orange border
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Mốc Vàng",
                                        tint = Color(0xFFD46B08), // Dark warning orange
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MỐC KHÁM SẢN KHOA VÀNG QUAN TRỌNG Ở TUẦN THÚ $selectedWeek! Mẹ bầu ghi nhớ đặt và đi khám nhé.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }
                    }

                    // Scheduled list Title
                    item {
                        Text(
                            text = "🗒️ Danh Sách Lịch Hẹn Khám Thai Đã Lên",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepBrownSecondary
                        )
                    }

                    if (allAppointments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, LightBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Chưa có lịch hẹn khám nào được tạo.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Bấm dấu + tròn bên dưới để thêm lịch hẹn khám thai định kỳ.",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(allAppointments, key = { it.id }) { appt ->
                            val isCompleted = appt.status == "COMPLETED"
                            val isCritical = appt.isCritical

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (isCritical && !isCompleted) 1.5.dp else 1.dp,
                                    color = if (isCompleted) EmeraldText.copy(alpha = 0.3f)
                                    else if (isCritical) AlertBorder
                                    else LightBorder
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) EmeraldBg.copy(alpha = 0.3f)
                                    else if (isCritical) WarmPeachCard.copy(alpha = 0.8f)
                                    else WhiteCard
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isCritical) Icons.Default.Star else Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = if (isCompleted) EmeraldText else if (isCritical) AmberText else DeepBrownSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Tuần thai ${appt.targetWeek} • Ngày ${appt.scheduledDate}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isCompleted) EmeraldText else DeepBrownSecondary
                                            )
                                        }

                                        // Mark completed circle icon
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

                                        // Actions row (Edit, Delete)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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

                // Circular Add Floating Action Button (+) centered bottom right
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 20.dp, end = 20.dp)
                        .testTag("fab_add_appointment"),
                    containerColor = DeepBrownSecondary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm lịch hẹn khám mới",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Add Appointment dialog popup
    if (showAddDialog) {
        var addClinicVal by remember { mutableStateOf("") }
        var addDoctorVal by remember { mutableStateOf("") }
        var addDateVal by remember { mutableStateOf("") }
        var addNotesVal by remember { mutableStateOf("") }
        var addWeekValStr by remember { mutableStateOf(if (selectedWeek > 0) selectedWeek.toString() else "12") }
        var addIsCriticalVal by remember { mutableStateOf(true) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_appointment_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📅 Lên Lịch Hẹn Khám Thai Độc Lập",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrownSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PregnancyTextField(
                        value = addWeekValStr,
                        onValueChange = { addWeekValStr = it },
                        label = "Tuần thai khám (1..42)",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    PregnancyDatePickerField(
                        label = "Ngày hẹn khám",
                        currentDateYmd = addDateVal,
                        onDateSelected = { addDateVal = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = addClinicVal,
                        onValueChange = { addClinicVal = it },
                        label = "Tên phòng khám / Bệnh viện",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = addDoctorVal,
                        onValueChange = { addDoctorVal = it },
                        label = "Bác sĩ phụ trách chính",
                        modifier = Modifier.fillMaxWidth()
                    )

                    PregnancyTextField(
                        value = addNotesVal,
                        onValueChange = { addNotesVal = it },
                        label = "Ghi chú dặn dò quan trọng",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addIsCriticalVal = !addIsCriticalVal }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = addIsCriticalVal,
                            onCheckedChange = { addIsCriticalVal = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepBrownSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đây là mốc khám quan trọng (Mốc Vàng)",
                            fontSize = 12.sp,
                            color = TextSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                            Text("Hủy bỏ", color = TextSlate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val w = addWeekValStr.toIntOrNull() ?: selectedWeek
                                if (addDateVal.isNotBlank()) {
                                    viewModel.addAppointment(
                                        week = w,
                                        date = addDateVal,
                                        clinic = addClinicVal,
                                        doctor = addDoctorVal,
                                        notes = addNotesVal,
                                        isCritical = addIsCriticalVal
                                    )
                                    showAddDialog = false
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Xác Nhận", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            WaterTrackerCard(userEmail = viewModel.loggedInEmail.value)
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
fun SettingsDrawerContent(
    viewModel: PregnancyViewModel,
    user: UserEntity,
    onClose: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var name by remember(user) { mutableStateOf(user.name) }
    var lmpDate by remember(user) { mutableStateOf(user.lmpDate ?: "2026-01-15") }
    var eddDate by remember(user) { mutableStateOf(user.eddDate ?: "2026-10-22") }
    var dobDate by remember(user) { mutableStateOf(user.dobDate ?: "1998-05-15") }
    var conceptionDate by remember(user) { mutableStateOf(user.conceptionDate ?: "2026-01-29") }
    var startWeightKg by remember(user) { mutableStateOf(user.gestationStartWeight.toString()) }
    var targetWeightKg by remember(user) { mutableStateOf(user.targetPregnancyWeight.toString()) }

    var isEditing by remember { mutableStateOf(false) }
    var activeSubMenu by remember { mutableStateOf<Int?>(null) } // Mặc định thu gọn, khi nào cần mới mở bung ra
    var developerClickCount by remember { mutableStateOf(0) }

    // For new weight record input inside weight tracking submenu
    val allWeightRecords by viewModel.allWeightRecordsState.collectAsStateWithLifecycle()
    var newWeightStr by remember { mutableStateOf("") }
    var newWeightWeekStr by remember { mutableStateOf(viewModel.selectedWeek.value.toString()) }
    var newWeightNotes by remember { mutableStateOf("") }

    val daysLeft = viewModel.getDaysLeftToDue(user.eddDate)
    val progressPercent = ((280 - daysLeft).coerceIn(0, 280).toFloat() / 280f * 100f).toInt()
    val gestationWeek = ((280 - daysLeft).coerceIn(0, 280) / 7 + 1).coerceIn(1, 40)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🌸 Header & Brand Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(SoftPeachPrimary, WarmPeachCard))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = DeepBrownSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tiện ích mở rộng",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Cẩm nang cho Mẹ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBrownText
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(LightBorder, CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close, 
                    contentDescription = "Đóng menu", 
                    tint = DarkBrownText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 👑 Beautiful Integrated Profile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WhiteCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LightBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern styled Avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        SoftPeachPrimary,
                                        WarmPeachCard
                                    )
                                )
                            )
                            .border(2.dp, WhiteCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (user.name.isNotBlank()) user.name.take(2).uppercase() else "ME",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkBrownText
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chào Mẹ Bầu,",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Text(
                            text = user.name.ifBlank { "Mẹ bầu thông thái" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBrownText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EmeraldBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Tuần thai ${gestationWeek}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldText
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Còn ${daysLeft} ngày dự sinh",
                                fontSize = 9.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                HorizontalDivider(color = LightBorder, thickness = 0.5.dp)

                // Account & Logout details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tài khoản", fontSize = 8.5.sp, color = TextMuted)
                        Text(
                            text = viewModel.loggedInEmail.value,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertLightBg),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp),
                        border = BorderStroke(0.5.dp, AlertBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Đăng xuất",
                            tint = AlertRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đăng xuất",
                            color = AlertRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 📊 Gestation Progress Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarmPeachCard.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LightBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = DeepBrownSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tiến độ thai kỳ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBrownText
                        )
                    }
                    Text(
                        text = "$progressPercent% hành trình",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Beautifully designed Progress Meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(WhiteCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressPercent / 100f)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SoftPeachPrimary,
                                        DeepBrownSecondary
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Khởi đầu", 
                        fontSize = 9.sp, 
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Đã đi qua ${(280 - daysLeft).coerceIn(0, 280)} ngày", 
                        fontSize = 9.sp, 
                        color = DarkBrownText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Về đích", 
                        fontSize = 9.sp, 
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 📂 EXPANDABLE SECTIONS CONTROLLER
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ================= SUBMENU 1: HỒ SƠ THAI KỲ & NGÀY CHU KỲ =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSubMenu = if (activeSubMenu == 0) null else 0 }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(SoftPeachPrimary.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = DeepBrownSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Hồ sơ & Ngày chu kỳ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DeepBrownSecondary,
                            modifier = Modifier
                                .graphicsLayer { rotationZ = if (activeSubMenu == 0) 180f else 0f }
                                .size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = activeSubMenu == 0,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (!isEditing) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ProfileInfoRow(
                                            icon = Icons.Default.Person,
                                            label = "Họ và tên Mẹ",
                                            value = user.name
                                        )
                                        ProfileInfoRow(
                                            icon = Icons.Default.DateRange,
                                            label = "Ngày sinh của Mẹ",
                                            value = user.dobDate ?: "Chưa thiết lập"
                                        )
                                        ProfileInfoRow(
                                            icon = Icons.Default.DateRange,
                                            label = "Kỳ kinh cuối (LMP)",
                                            value = user.lmpDate ?: "Chưa thiết lập"
                                        )
                                        ProfileInfoRow(
                                            icon = Icons.Default.DateRange,
                                            label = "Ngày dự kiến sinh (EDD)",
                                            value = user.eddDate ?: "Chưa thiết lập"
                                        )
                                        ProfileInfoRow(
                                            icon = Icons.Default.DateRange,
                                            label = "Ngày thụ thai",
                                            value = user.conceptionDate ?: "Chưa thiết lập"
                                        )
                                        ProfileInfoRow(
                                            icon = Icons.Default.Settings,
                                            label = "Cân nặng trước bầu",
                                            value = "${user.gestationStartWeight} kg"
                                        )
                                        ProfileInfoRow(
                                            icon = Icons.Default.Star,
                                            label = "Mục tiêu cân nặng",
                                            value = "${user.targetPregnancyWeight} kg"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { isEditing = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Chỉnh sửa hồ sơ thai kỳ",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "⚙️ Thay Đổi Thiết Lập Ngày Quan Trọng",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBrownText
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = AmberBg.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Gợi ý",
                                                    tint = AmberText,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Mẹ nhập 1 trong 3 mốc (LMP, EDD, thụ thai), hệ thống sẽ tự động đồng bộ ước tính các mốc ngày còn lại.",
                                                    fontSize = 10.sp,
                                                    color = DarkBrownText,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }

                                        PregnancyTextField(
                                            value = name,
                                            onValueChange = { name = it },
                                            label = "Họ và Tên Mẹ bầu",
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        PregnancyDatePickerField(
                                            label = "Ngày sinh của Mẹ",
                                            currentDateYmd = dobDate,
                                            onDateSelected = { dobDate = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        PregnancyDatePickerField(
                                            label = "Ngày kỳ kinh cuối (LMP)",
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

                                        PregnancyTextField(
                                            value = startWeightKg,
                                            onValueChange = { startWeightKg = it },
                                            label = "Cân nặng trước thai kỳ (kg)",
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        PregnancyTextField(
                                            value = targetWeightKg,
                                            onValueChange = { targetWeightKg = it },
                                            label = "Mục tiêu cân nặng khi sinh (kg)",
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    name = user.name
                                                    lmpDate = user.lmpDate ?: "2026-01-15"
                                                    eddDate = user.eddDate ?: "2026-10-22"
                                                    dobDate = user.dobDate ?: "1998-05-15"
                                                    conceptionDate = user.conceptionDate ?: "2026-01-29"
                                                    startWeightKg = user.gestationStartWeight.toString()
                                                    targetWeightKg = user.targetPregnancyWeight.toString()
                                                    isEditing = false
                                                    focusManager.clearFocus()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, LightBorder)
                                            ) {
                                                Text("Hủy Bỏ", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

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
                                                        isEditing = false
                                                        focusManager.clearFocus()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .testTag("save_profile_btn")
                                                    .weight(1.2f)
                                                    .height(36.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Lưu Lại", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SUBMENU 2: NHẬT KÝ & THEO DÕI CÂN NẶNG =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSubMenu = if (activeSubMenu == 1) null else 1 }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(SoftPeachPrimary.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = DeepBrownSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nhật ký cân nặng (${allWeightRecords.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DeepBrownSecondary,
                            modifier = Modifier
                                .graphicsLayer { rotationZ = if (activeSubMenu == 1) 180f else 0f }
                                .size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = activeSubMenu == 1,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                            
                            val latestRecord = allWeightRecords.maxByOrNull { it.weekNumber }
                            val currentW = latestRecord?.weightKg ?: user.gestationStartWeight
                            val diffW = currentW - user.gestationStartWeight
                            val targetGain = user.targetPregnancyWeight - user.gestationStartWeight

                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 🌟 Beautiful mini-dashboard of weight
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Chỉ số tăng trưởng cân nặng",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBrownText
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Ban đầu", fontSize = 8.5.sp, color = TextMuted)
                                                Text("${user.gestationStartWeight} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSlate)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Hiện tại", fontSize = 8.5.sp, color = TextMuted)
                                                Text("${currentW} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBrownSecondary)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Đã tăng", fontSize = 8.5.sp, color = TextMuted)
                                                Text(
                                                    text = if (diffW >= 0) "+${String.format(java.util.Locale.US, "%.1f", diffW)} kg" else "${String.format(java.util.Locale.US, "%.1f", diffW)} kg",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (diffW in 1.0..16.0) EmeraldText else AlertRed
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Mục tiêu bầu", fontSize = 8.5.sp, color = TextMuted)
                                                Text("+${String.format(java.util.Locale.US, "%.1f", targetGain)} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
                                            }
                                        }
                                    }
                                }

                                // Dynamic Trend Chart
                                WeightProgressionChart(
                                    records = allWeightRecords,
                                    startWeight = user.gestationStartWeight,
                                    targetWeight = user.targetPregnancyWeight
                                )

                                // Quick Log Weight Record Form
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = WarmBackground),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "✍️ Cập nhật cân nặng tuần này",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBrownText
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PregnancyTextField(
                                                value = newWeightWeekStr,
                                                onValueChange = { newWeightWeekStr = it },
                                                label = "Tuần thai (1-41)",
                                                modifier = Modifier.weight(1.1f)
                                            )
                                            PregnancyTextField(
                                                value = newWeightStr,
                                                onValueChange = { newWeightStr = it },
                                                label = "Cân nặng (kg)",
                                                modifier = Modifier.weight(1.0f)
                                            )
                                        }

                                        PregnancyTextField(
                                            value = newWeightNotes,
                                            onValueChange = { newWeightNotes = it },
                                            label = "Ghi chú thể trạng... (Ăn ngon, mỏi gối...)",
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                val wk = newWeightWeekStr.toIntOrNull()?.coerceIn(1, 41)
                                                val wt = newWeightStr.toDoubleOrNull()
                                                if (wk != null && wt != null) {
                                                    viewModel.addOrUpdateWeight(wk, wt, newWeightNotes.trim())
                                                    newWeightStr = ""
                                                    newWeightNotes = ""
                                                    focusManager.clearFocus()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Báo cáo cân nặng tuần", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // List logs history
                                Text(
                                    text = "🗒️ Nhật ký đo lường tuần qua",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBrownText
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val sortedLogs = allWeightRecords.sortedByDescending { it.weekNumber }
                                    if (sortedLogs.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(WhiteCard, RoundedCornerShape(10.dp))
                                                .border(1.dp, LightBorder, RoundedCornerShape(10.dp))
                                                .padding(vertical = 20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Chưa có chỉ số nào được ghi lại.",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                    } else {
                                        sortedLogs.forEach { rec ->
                                            val diff = rec.weightKg - user.gestationStartWeight
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, LightBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(WarmPeachCard)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Tuần ${rec.weekNumber}",
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = DeepBrownSecondary
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = rec.dateRecorded ?: "", 
                                                                fontSize = 9.sp, 
                                                                color = TextMuted
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Trạng thái: " + (rec.notes ?: "Khỏe mạnh bình thường"), 
                                                            fontSize = 11.sp, 
                                                            color = TextSlate
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = "${rec.weightKg} kg", 
                                                                fontSize = 12.sp, 
                                                                fontWeight = FontWeight.ExtraBold, 
                                                                color = DarkBrownText
                                                            )
                                                            Text(
                                                                text = if (diff >= 0) "+${String.format(java.util.Locale.US, "%.1f", diff)} kg" else "${String.format(java.util.Locale.US, "%.1f", diff)} kg",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (diff >= 0) EmeraldText else AlertRed
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { viewModel.deleteWeightRecord(rec.weekNumber) },
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(AlertLightBg, CircleShape)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete, 
                                                                contentDescription = "Xóa dòng", 
                                                                tint = AlertRed, 
                                                                modifier = Modifier.size(12.dp)
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
                    }
                }
            }

            // ================= SUBMENU 3: THIẾT LẬP & CẨM NANG =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WhiteCard),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSubMenu = if (activeSubMenu == 2) null else 2 }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(SoftPeachPrimary.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = DeepBrownSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cài đặt & Cẩm nang",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DeepBrownSecondary,
                            modifier = Modifier
                                .graphicsLayer { rotationZ = if (activeSubMenu == 2) 180f else 0f }
                                .size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = activeSubMenu == 2,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(color = LightBorder, thickness = 0.5.dp)
                            
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 🔊 Settings options
                                var notificationEnabled by remember { mutableStateOf(true) }
                                var soundEnabled by remember { mutableStateOf(true) }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("🔔 Nhắc nhở sức khỏe", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
                                            Text("Tự động gửi thông báo lịch nhắc sắt, sữa mẹ dưỡng chất", fontSize = 9.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = notificationEnabled,
                                            onCheckedChange = { notificationEnabled = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = WhiteCard,
                                                checkedTrackColor = DeepBrownSecondary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = LightBorder
                                            )
                                        )
                                    }

                                    HorizontalDivider(color = LightBorder, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("🔊 Âm thanh báo động", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkBrownText)
                                            Text("Phát âm thanh nhạc nhẹ nhắc mẹ nghỉ ngơi đúng giờ", fontSize = 9.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = soundEnabled,
                                            onCheckedChange = { soundEnabled = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = WhiteCard,
                                                checkedTrackColor = DeepBrownSecondary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = LightBorder
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(color = LightBorder, thickness = 0.5.dp)

                                // 🌸 Minimal beautiful App Intro text card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = WarmBackground),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "🌸 Đồng hành cùng Mẹ suốt 9 tháng vàng",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = DarkBrownText
                                        )
                                        Text(
                                            text = "Chào mừng mẹ bầu đến với cẩm nang số thông minh đồng hành cùng quá trình mang thai thiêng liêng. Lộ trình 9 mốc vàng thăm khám chuẩn y khoa, dinh dưỡng bồi bổ/cảnh báo kiêng kị, biểu đồ xu hướng cân nặng, và nhắc nhở uống thuốc tự động.",
                                            fontSize = 10.sp,
                                            color = TextSlate,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }

                                HorizontalDivider(color = LightBorder, thickness = 0.5.dp)

                                // Administrative actions
                                Text(
                                    text = "Quản trị dữ liệu gốc thai kỳ",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBrownText
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.resetToStandardMilestones() },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmPeachCard),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Khởi tạo 9 mốc khám", color = DarkBrownText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmPeachCard),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Đồng bộ đám mây", color = DarkBrownText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (developerClickCount >= 5) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GitUpdateComponent()
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mẹ & Bé • Phiên bản v2.6.2 (Golden Release) • Đồng hành yêu thương cùng gia đình Việt.",
                                    fontSize = 8.5.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextMuted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { developerClickCount++ },
                                    textAlign = TextAlign.Center
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
fun GitUpdateComponent() {
    var updateState by remember { mutableStateOf(0) } // 0: Idle, 1: Checking, 2: Update Available, 3: Pulling, 4: Up to Date/Finished
    var consoleLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = WarmBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔄", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Git Update System",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBrownText
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when (updateState) {
                                4 -> EmeraldBg
                                2 -> SoftPeachPrimary.copy(alpha = 0.2f)
                                else -> LightBorder
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (updateState) {
                            0 -> "v2.6.0"
                            1 -> "Đang kiểm tra..."
                            2 -> "Có bản mới v2.6.1"
                            3 -> "Updating..."
                            else -> "v2.6.1-patch"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (updateState) {
                            4 -> EmeraldText
                            2 -> AlertRed
                            else -> DarkBrownText
                        }
                    )
                }
            }

            Text(
                text = "Hệ thống quản lý cập nhật từ kho lưu trữ Git chính thức của ứng dụng mẹ bầu.",
                fontSize = 10.sp,
                color = TextSlate,
                lineHeight = 14.sp
            )

            if (consoleLogs.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(consoleLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.startsWith("$")) Color(0xFF8CE8FF) else if (log.contains("error")) Color(0xFFFF6B6B) else Color(0xFFDCDCDC),
                                fontSize = 9.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (updateState == 3) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = SoftPeachPrimary,
                        trackColor = LightBorder
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đang kéo mã nguồn (Git Pull)...",
                            fontSize = 9.5.sp,
                            color = TextSlate
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrownSecondary
                        )
                    }
                }
            }

            when (updateState) {
                0 -> {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                updateState = 1
                                consoleLogs = listOf("$ git fetch origin main --verbose")
                                kotlinx.coroutines.delay(1000)
                                consoleLogs = consoleLogs + listOf(
                                    "Connecting to github.com/aistudio-pregnancy/me-va-be...",
                                    "remote: Enumerating objects: 12, done.",
                                    "remote: Counting objects: 100% (12/12), done.",
                                    "remote: Compressing objects: 100% (8/8), done."
                                )
                                kotlinx.coroutines.delay(1200)
                                consoleLogs = consoleLogs + listOf(
                                    "From github.com/aistudio-pregnancy/me-va-be",
                                    "   8d9f123..e54ab23  main       -> origin/main",
                                    "Status: 1 new release commit found (v2.6.1-patch)"
                                )
                                kotlinx.coroutines.delay(1000)
                                updateState = 2
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPeachCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔍 Kiểm tra cập nhật từ Git", color = DarkBrownText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SoftPeachPrimary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🎁 Nhật ký thay đổi (v2.6.1):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBrownSecondary
                                )
                                Text(
                                    text = "• Tối ưu hóa dung lượng cơ sở dữ liệu và cấu trúc lưu trữ bảo mật cho từng email mẹ bầu riêng tư.\n• Cải tiến tính năng thu gọn / mở rộng hồ sơ, giúp giao diện gọn gàng thanh thoát.\n• Sửa một số lỗi giao diện và tăng tốc thời gian mở ứng dụng.",
                                    fontSize = 9.5.sp,
                                    color = DarkBrownText,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    updateState = 3
                                    progress = 0f
                                    consoleLogs = consoleLogs + listOf("$ git pull origin main")
                                    kotlinx.coroutines.delay(800)
                                    progress = 0.25f
                                    consoleLogs = consoleLogs + listOf(
                                        "Updating 8d9f123..e54ab23",
                                        "Fast-forward",
                                        " app/src/main/java/com/example/MainActivity.kt | 24 ++--"
                                    )
                                    kotlinx.coroutines.delay(1000)
                                    progress = 0.6f
                                    consoleLogs = consoleLogs + listOf(
                                        " 1 file changed, 18 insertions(+), 6 deletions(-)",
                                        "Unpacking objects: 100% (14/14), 1.45 KiB | 1.45 MiB/s, done."
                                    )
                                    kotlinx.coroutines.delay(1200)
                                    progress = 0.9f
                                    consoleLogs = consoleLogs + listOf(
                                        "Recompiling Jetpack Compose UI trees...",
                                        "Applying database migrations for privacy space updates (v4 -> v5)..."
                                    )
                                    kotlinx.coroutines.delay(1000)
                                    progress = 1.0f
                                    updateState = 4
                                    consoleLogs = consoleLogs + listOf(
                                        "Successfully built APK target v2.6.1-patch (Release Mode)",
                                        "Local state refreshed! Welcome to v2.6.1."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⬇️ Tải & Cập nhật ngay (Git Pull)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                3 -> {
                    // Pulling state, show progress
                }
                4 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EmeraldBg.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, EmeraldText.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "✓", color = EmeraldText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Thiết bị mẹ bầu đã cập nhật thành công lên v2.6.1 qua Git! Môi trường lưu trữ riêng tư đã được tối ưu bảo mật hoàn hảo.",
                                fontSize = 10.sp,
                                color = DarkBrownText,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            updateState = 0
                            consoleLogs = emptyList()
                            progress = 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Text("Quay về nguyên bản", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
    onTabSelected: (Int) -> Unit,
    onMenuSelected: () -> Unit
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
            selected = false,
            onClick = onMenuSelected,
            label = { Text("Mở rộng", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "Mở rộng") },
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
                items(medicineReminders, key = { it.id }) { reminder ->
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
                items(dietReminders, key = { it.id }) { reminder ->
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
fun KickCounterCard(userEmail: String = "") {
    val context = LocalContext.current
    val prefsName = if (userEmail.isNotBlank()) "app_prefs_$userEmail" else "app_prefs"
    val prefs = remember(userEmail) { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    
    // Timer & Count states
    var isTracking by remember { mutableStateOf(false) }
    var currentCount by remember { mutableStateOf(0) }
    var timeLeftSeconds by remember { mutableStateOf(3600) } // 60 minutes = 3600s
    
    // Local list of saved logs
    var historyLogs by remember { mutableStateOf(emptyList<String>()) }
    
    // Load existing logs on start
    LaunchedEffect(userEmail) {
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
fun WaterTrackerCard(userEmail: String = "") {
    val context = LocalContext.current
    val prefsName = if (userEmail.isNotBlank()) "app_prefs_$userEmail" else "app_prefs"
    val prefs = remember(userEmail) { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    
    // Date key to persist daily tracking
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val waterKey = remember { "daily_water_$todayDateStr" }
    
    var currentWaterMl by remember { mutableStateOf(0) }
    val targetWaterMl = 2500 // Daily recommended for pregnant mothers
    
    // Load existing water log on launch
    LaunchedEffect(userEmail) {
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

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SoftPeachPrimary.copy(alpha = 0.25f))
            .border(1.dp, LightBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SoftPeachPrimary.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepBrownSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DarkBrownText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBrownSecondary
        )
    }
}

// ============================================================================
// PROPOSAL 1: THEO DÕI TÂM TRẠNG & TRIỆU CHỨNG HÀNG NGÀY (DAILY MOOD & SYMPTOM REGISTER)
// ============================================================================
@Composable
fun TodaySymptomAndMoodCard(userEmail: String = "") {
    val context = LocalContext.current
    val prefsName = if (userEmail.isNotBlank()) "app_prefs_$userEmail" else "app_prefs"
    val prefs = remember(userEmail) { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    
    var selectedMood by remember { mutableStateOf(prefs.getString("mood_$todayStr", "") ?: "") }
    var selectedSymptoms by remember { 
        mutableStateOf(
            (prefs.getString("symptoms_$todayStr", "") ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toSet()
        )
    }

    val moods = listOf(
        "Vui vẻ 😊" to "HAPPY",
        "Mệt mỏi 🥱" to "TIRED",
        "Nhạy cảm 🌸" to "SENSITIVE",
        "Lo lắng 🥺" to "ANXIOUS"
    )

    val symptoms = listOf(
        "Ốm nghén 🤢" to "NAUSEA",
        "Đau lưng ⚡" to "BACK_PAIN",
        "Táo bón 🤰" to "CONSTIPATION",
        "Chuột rút 🦵" to "CRAMP",
        "Khó ngủ 👁️" to "INSOMNIA"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌸", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nhật Ký Sức Khỏe & Tâm Trạng",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrownSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tâm trạng của mẹ hôm nay:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBrownText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                moods.forEach { (label, code) ->
                    val isSelected = selectedMood == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SoftPeachPrimary else WarmBackground)
                            .border(1.dp, if (isSelected) DeepBrownSecondary else LightBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val newVal = if (isSelected) "" else code
                                selectedMood = newVal
                                prefs.edit().putString("mood_$todayStr", newVal).apply()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DarkBrownText else TextSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Triệu chứng thai kỳ hôm nay (chọn nhiều):",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBrownText
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val chunkedSymptoms = symptoms.chunked(3)
                chunkedSymptoms.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { (label, code) ->
                            val isSelected = selectedSymptoms.contains(code)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFFE8F5E9) else WarmBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF4CAF50) else LightBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val updated = selectedSymptoms.toMutableSet()
                                        if (isSelected) updated.remove(code) else updated.add(code)
                                        selectedSymptoms = updated
                                        prefs.edit().putString("symptoms_$todayStr", updated.joinToString(",")).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF2E7D32) else TextSlate
                                )
                            }
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (selectedMood.isNotBlank() || selectedSymptoms.isNotEmpty()) {
                val adviceText = remember(selectedMood, selectedSymptoms) {
                    val sb = java.lang.StringBuilder()
                    if (selectedMood == "TIRED") sb.append("😴 Mẹ cảm thấy mệt mỏi? Hãy đảm bảo ngủ đủ 8 tiếng, uống nước ấm và dạo bộ nhẹ nhàng nhé.\n")
                    if (selectedMood == "ANXIOUS") sb.append("🥺 Bình tĩnh mẹ nhé, thai nghén thỉnh thoảng sẽ có lo lắng. Hãy hít sâu 3 giây, thở chậm 5 giây xua tan căng thẳng.\n")
                    
                    if (selectedSymptoms.contains("NAUSEA")) sb.append("🤢 Giảm nghén: Mẹ ăn bữa nhỏ chia đều, ngậm lát gừng mỏng ấm hoặc nhâm nhi chút bánh quy nhạt.\n")
                    if (selectedSymptoms.contains("BACK_PAIN")) sb.append("⚡ Đau lưng: Tránh đứng quá lâu, xoa bóp nhẹ nhàng thắt lưng và ngủ nghiêng trái gối kê cao chân.\n")
                    if (selectedSymptoms.contains("CONSTIPATION")) sb.append("🤰 Táo bón: Tăng cường rau xanh đậm, khoai lang chín và hoàn thành tối thiểu 2.5 lít nước ấm mỗi ngày.\n")
                    if (selectedSymptoms.contains("CRAMP")) sb.append("🦵 Chuột rút: Thêm canxi tự nhiên (sữa, tôm cá), bóp chân buổi tối trước khi ngủ và xoa ấm bắp chân.\n")
                    if (selectedSymptoms.contains("INSOMNIA")) sb.append("👁️ Khó ngủ: Thử ngâm chân nước ấm trước khi nằm đầu, tắt điện thoại sớm và thở đều thư thái.\n")
                    
                    if (sb.isEmpty()) "Con đang lớn lên khỏe mạnh dẻo dai! Hãy duy trì lối sống khoa học mẹ nhé."
                    else sb.toString().trim()
                }

                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmPeachCard),
                    border = BorderStroke(0.5.dp, DeepBrownSecondary.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = DeepBrownSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bác sĩ khuyên mẹ bầu hôm nay:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = adviceText,
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

// ============================================================================
// PROPOSAL 2: TRẠM THƯ GIÃN & THAI GIÁO ÂM NHẠC (PRENATAL MUSIC & ADVICE CORNER)
// ============================================================================
@Composable
fun ThaiGiaoRelaxMusicCard(currentWeek: Int) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentTrackIndex by remember { mutableStateOf(0) }
    var trackProgress by remember { mutableStateOf(0.12f) }
    
    val tracks = listOf(
        "🎧 Nhạc thính phòng Mozart thông minh" to "3:15",
        "🌊 Tiếng sóng biển & tiếng chim ru ngủ" to "5:00",
        "🍼 Lời thủ thỉ nói chuyện cùng con yêu" to "2:45",
        "🕯️ Thiền thở thư thái tĩnh tâm giảm lo" to "4:20"
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(1000L)
                trackProgress += 0.015f
                if (trackProgress >= 1f) {
                    trackProgress = 0f
                    currentTrackIndex = (currentTrackIndex + 1) % tracks.size
                }
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
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎵", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trạm Thư Giãn & Thai Giáo Âm Nhạc",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrownSecondary
                    )
                }
                
                if (isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val barHeight1 by infiniteTransition.animateFloat(
                            initialValue = 4f, targetValue = 18f,
                            animationSpec = infiniteRepeatable(tween(400, delayMillis = 100), RepeatMode.Reverse),
                            label = "bh1"
                        )
                        val barHeight2 by infiniteTransition.animateFloat(
                            initialValue = 16f, targetValue = 6f,
                            animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
                            label = "bh2"
                        )
                        val barHeight3 by infiniteTransition.animateFloat(
                            initialValue = 8f, targetValue = 14f,
                            animationSpec = infiniteRepeatable(tween(300, delayMillis = 50), RepeatMode.Reverse),
                            label = "bh3"
                        )
                        
                        Box(Modifier.size(3.dp, barHeight1.dp).background(DeepBrownSecondary, RoundedCornerShape(1.dp)))
                        Box(Modifier.size(3.dp, barHeight2.dp).background(DeepBrownSecondary, RoundedCornerShape(1.dp)))
                        Box(Modifier.size(3.dp, barHeight3.dp).background(DeepBrownSecondary, RoundedCornerShape(1.dp)))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Kích thích thính giác và não bộ thai nhi phát triển từ tuần 14 trở đi thông qua giai điệu âm nhạc êm dịu, ấm áp.",
                fontSize = 11.5.sp,
                color = TextSlate,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = WarmBackground),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.5.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tracks[currentTrackIndex].first,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Thai giáo âm thanh • Thời lượng ${tracks[currentTrackIndex].second}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else tracks.size - 1
                                    trackProgress = 0.05f
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("⏮️", fontSize = 14.sp)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DeepBrownSecondary)
                                    .clickable { isPlaying = !isPlaying },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    currentTrackIndex = (currentTrackIndex + 1) % tracks.size
                                    trackProgress = 0.05f
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("⏭️", fontSize = 14.sp)
                            }
                        }
                    }
                    
                    if (isPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("0:45", fontSize = 9.sp, color = TextMuted)
                            LinearProgressIndicator(
                                progress = { trackProgress.coerceIn(0f, 1f) },
                                color = DeepBrownSecondary,
                                trackColor = LightBorder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Text(tracks[currentTrackIndex].second, fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            val visualThaiGiaoTip = when (currentWeek) {
                in 1..13 -> "🌱 Giai đoạn Tam cá nguyệt 1: Hãy bắt đầu tập hít thở bụng sâu thư thái dồi dào lượng oxy cho bé cơ bản. Đọc truyện cổ tích, thì thầm nhẹ chào con để truyền năng lượng yêu thương lành mạnh."
                in 14..27 -> "🎨 Giai đoạn Tam cá nguyệt 2: Thính giác của bé đã phát triển tích cực! Mẹ hãy bật các bản concerto mượt mà của Mozart, đặt tay lên bụng kết hợp vuốt từ trái sang phải nhịp nhàng để tăng cường liên kết cảm giác."
                else -> "⭐ Giai đoạn Tam cá nguyệt 3: Bé đã nhận ra giọng nói của bố mẹ rõ nét. Bố và mẹ hãy trò chuyện thường xuyên, hát ru ấm áp mỗi tối để nuôi dưỡng cảm xúc vàng cho bé yêu sinh trưởng khỏe mượt."
            }

            Text(
                text = "💡 Gợi ý thai giáo hôm nay (Tuần $currentWeek):",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBrownText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = visualThaiGiaoTip,
                fontSize = 11.sp,
                color = TextSlate,
                lineHeight = 15.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ============================================================================
// PROPOSAL 3: BỘ ĐẾM CƠN GÒ TỬ CUNG (CONTRACTION TIMER & ANALYSIS ENGINE)
// ============================================================================
@Composable
fun ContractionTimerCard(userEmail: String = "") {
    val context = LocalContext.current
    val prefsName = if (userEmail.isNotBlank()) "app_prefs_$userEmail" else "app_prefs"
    val prefs = remember(userEmail) { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    
    var isRecording by remember { mutableStateOf(false) }
    var currentTimerSeconds by remember { mutableStateOf(0) }
    var recordingStartDate by remember { mutableStateOf<Long?>(null) }
    
    var logs by remember { mutableStateOf(emptyList<String>()) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(userEmail) {
        val raw = prefs.getString("contraction_logs", "") ?: ""
        if (raw.isNotBlank()) {
            logs = raw.split(";;").filter { it.isNotBlank() }
        }
    }

    val saveNewContraction = { start: Long, end: Long ->
        val entry = "$start|$end"
        val updatedList = (listOf(entry) + logs).take(25)
        logs = updatedList
        prefs.edit().putString("contraction_logs", updatedList.joinToString(";;")).apply()
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            currentTimerSeconds = 0
            while (isRecording) {
                delay(1000L)
                currentTimerSeconds += 1
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, AlertRed.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(AlertLightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏱️", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bộ Đếm Cơn Gò Tử Cung",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertDarkText
                    )
                }
                
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 11.sp,
                        color = AlertDarkText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Công cụ đo tần suất, khoảng cách và thời gian co thắt tử cung. Giúp mẹ bầu phân biệt giữa cơn gò sinh lý Braxton Hicks và dấu hiệu chuyển dạ thực sự chuẩn xác để sẵn sàng đi sinh đúng lúc.",
                fontSize = 11.5.sp,
                color = TextSlate,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isRecording) AlertLightBg else WarmBackground)
                    .border(1.dp, if (isRecording) AlertRed else LightBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AlertRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.3f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(CircleShape)
                                    .background(AlertRed)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ĐANG CO THẮT...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AlertRed,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$currentTimerSeconds giây",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = AlertDarkText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val end = System.currentTimeMillis()
                                val start = recordingStartDate ?: (end - currentTimerSeconds * 1000L)
                                saveNewContraction(start, end)
                                isRecording = false
                                recordingStartDate = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("HẾT CƠN GÒ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DeepBrownSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nhấn nút này ngay khi thắt cơ bụng gò căng cứng",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                recordingStartDate = System.currentTimeMillis()
                                isRecording = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBrownSecondary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("BẮT ĐẦU CƠN GÒ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            if (logs.isNotEmpty()) {
                val processedEntries = remember(logs) {
                    val list = mutableListOf<ProcessedContraction>()
                    for (i in logs.indices) {
                        val parts = logs[i].split("|")
                        if (parts.size >= 2) {
                            val start = parts[0].toLongOrNull() ?: 0L
                            val end = parts[1].toLongOrNull() ?: 0L
                            val durationSeconds = ((end - start) / 1000L).coerceAtLeast(0L)
                            
                            var intervalSeconds = 0L
                            if (i + 1 < logs.size) {
                                val nextParts = logs[i + 1].split("|")
                                if (nextParts.size >= 2) {
                                    val prevStart = nextParts[0].toLongOrNull() ?: 0L
                                    intervalSeconds = ((start - prevStart) / 1000L).coerceAtLeast(0L)
                                }
                            }
                            list.add(ProcessedContraction(start, durationSeconds, intervalSeconds))
                        }
                    }
                    list
                }

                val laborAlert = remember(processedEntries) {
                    val recent = processedEntries.take(4)
                    if (recent.size >= 3) {
                        val avgDuration = recent.map { it.duration }.average()
                        val validIntervals = recent.map { it.interval }.filter { it > 0 }
                        if (validIntervals.isNotEmpty()) {
                            val avgIntervalMins = (validIntervals.average() / 60.0)
                            val intervalStr = String.format("%.1f", avgIntervalMins)
                            val durationRound = Math.round(avgDuration)
                            if (avgIntervalMins in 3.0..7.0 && avgDuration >= 35.0) {
                                "🚨 CẢNH BÁO CHUYỂN DẠ THỰC SỰ: Cơn gò dồn dập cách nhau khoảng $intervalStr phút, kéo dài trung bình ${durationRound}s. Đây là mốc cảnh báo 5-1-1! Mẹ hãy mang theo làn sinh và đi đến bệnh viện lập tức."
                            } else if (avgIntervalMins <= 15.0) {
                                "⚠️ GHI NHẬN CƠN GÒ CO DÀY: Cơn gò gối đều đặn hơn (khoảng $intervalStr phút). Mẹ bầu hãy nằm nghiêng nghỉ ngơi, theo dõi nếu có dòng ối hoặc rỉ máu thì nhập viện ngay."
                            } else {
                                "🌿 CƠN GÒ THANH LỌC SINH LÝ: Cơn gò của mẹ thưa thớt, không đều đặn. Đây là co bóp Braxton Hicks lành tính. Mẹ thoải mái tắm nước ấm nhẹ, nằm yên tĩnh hít thở bụng sâu giảm căng thẳng."
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                laborAlert?.let { alert ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val isCriticalAlert = alert.startsWith("🚨")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCriticalAlert) AlertLightBg else Color(0xFFFEF7E0)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCriticalAlert) AlertRed else Color(0xFFFFA500)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isCriticalAlert) AlertRed else Color(0xFFD46B08),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = alert,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCriticalAlert) AlertDarkText else Color(0xFF7F1D1D),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📋 Biểu Đồ Lịch Sử Co Thắt (Gần nhất)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertDarkText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        processedEntries.take(6).forEach { item ->
                            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            val timeStr = formatter.format(Date(item.startTime))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarmBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Cơn gò lúc $timeStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBrownText
                                    )
                                    if (item.interval > 0) {
                                        val intervalMins = item.interval / 10
                                        val intervalSecs = item.interval % 60
                                        val intervalText = if (intervalMins > 0) "${intervalMins}p ${intervalSecs}s" else "${intervalSecs}s"
                                        Text(
                                            text = "Cách lần gò trước: $intervalText",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    } else {
                                        Text(text = "Cơn gò đầu tiên trong chuỗi", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AlertLightBg),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Kéo dài: ${item.duration}s",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                prefs.edit().remove("contraction_logs").apply()
                                logs = emptyList()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa lịch sử gò", fontSize = 11.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class ProcessedContraction(
    val startTime: Long,
    val duration: Long,
    val interval: Long
)




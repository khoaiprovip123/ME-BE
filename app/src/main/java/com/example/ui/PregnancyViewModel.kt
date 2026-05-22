package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PregnancyViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryState = MutableStateFlow<PregnancyRepository?>(null)
    val passwordStore = mutableMapOf<String, String>() // email -> password in RAM/Preferences

    private val repository: PregnancyRepository?
        get() = repositoryState.value

    val isLoggedIn = MutableStateFlow(false)
    val loggedInEmail = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserState: StateFlow<UserEntity?> = repositoryState
        .flatMapLatest { repo -> repo?.currentUserFlow ?: flowOf(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWeeksState: StateFlow<List<FetalWeekEntity>> = repositoryState
        .flatMapLatest { repo -> repo?.allWeeksFlow ?: flowOf(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allAppointmentsState: StateFlow<List<AppointmentEntity>> = repositoryState
        .flatMapLatest { repo -> repo?.allAppointmentsFlow ?: flowOf(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRemindersState: StateFlow<List<MedicationReminderEntity>> = repositoryState
        .flatMapLatest { repo -> repo?.allRemindersFlow ?: flowOf(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWeightRecordsState: StateFlow<List<WeightRecordEntity>> = repositoryState
        .flatMapLatest { repo -> repo?.allWeightRecordsFlow ?: flowOf(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedWeek = MutableStateFlow(24) // Default to week 24 like the visual theme mockup
    val currentTab = MutableStateFlow(0) // 0: Hôm nay, 1: Lịch khám, 2: Dinh dưỡng, 3: Lịch nhắc, 4: Cá nhân
    val showLaborDialog = MutableStateFlow(false)
    val hasSetupProfile = MutableStateFlow(false)

    init {
        // Load stored active login session
        val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val activeEmail = prefs.getString("active_email", "") ?: ""
        
        // Load registered accounts to passwordStore
        val accountsString = prefs.getString("registered_accounts_v1", "") ?: ""
        if (accountsString.isNotBlank()) {
            accountsString.split(";").forEach { item ->
                val split = item.split("|")
                if (split.size == 2) {
                    passwordStore[split[0]] = split[1]
                }
            }
        }

        if (activeEmail.isNotBlank()) {
            isLoggedIn.value = true
            loggedInEmail.value = activeEmail
            initializeUserSession(activeEmail)
        } else {
            // No user logged in yet
            isLoggedIn.value = false
        }
    }

    fun initializeUserSession(email: String) {
        val app = getApplication<Application>()
        val dbSuffix = email.replace("@", "_").replace(".", "_")
        val database = AppDatabase.getDatabase(app, dbSuffix)
        val repo = PregnancyRepository(database)
        repositoryState.value = repo

        val prefs = app.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        hasSetupProfile.value = prefs.getBoolean("has_setup_profile_$email", false)

        viewModelScope.launch {
            repo.seedDatabaseIfNeeded()
            
            var user = repo.getCurrentUser()
            if (user != null && user.name.isBlank()) {
                val defaultName = email.substringBefore("@")
                user = user.copy(name = defaultName)
                repo.saveUser(user)
            }

            val hasLmpOrEdd = user != null && (!user.lmpDate.isNullOrBlank() || !user.eddDate.isNullOrBlank())
            if (hasLmpOrEdd) {
                hasSetupProfile.value = true
                prefs.edit().putBoolean("has_setup_profile_$email", true).apply()
            }

            calculateCurrentWeekFromUser()
            // Schedule repeating weight update weekly reminders
            PregnancyNotifier.scheduleWeeklyWeightAlert(app)
        }
    }

    // AUTH ACTIONS
    fun registerWithEmail(email: String, password: String, name: String): String? {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || password.isBlank()) {
            return "Email và mật khẩu không được để trống."
        }
        if (passwordStore.containsKey(normalized)) {
            return "Tài khoản email này đã được đăng ký."
        }
        
        passwordStore[normalized] = password
        saveAccountsToPrefs()

        // Auto login after signup
        loginWithEmail(normalized, password)
        
        // Pre-fill registered mother name if provided
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val repo = repositoryState.value
                val user = repo?.getCurrentUser()
                if (user != null) {
                    repo.saveUser(user.copy(name = name))
                }
            }
        }
        return null
    }

    fun loginWithEmail(email: String, password: String): String? {
        val normalized = email.trim().lowercase()
        if (!passwordStore.containsKey(normalized)) {
            return "Tài khoản không tồn tại. Vui lòng đăng ký trước."
        }
        val correctPassword = passwordStore[normalized]
        if (correctPassword != password) {
            return "Mật khẩu không chính xác."
        }

        performLoginSuccess(normalized)
        return null
    }

    fun loginWithGoogle(email: String, name: String) {
        val normalized = email.trim().lowercase()
        // If not exist in credential store, register immediately as passwordless Google account
        if (!passwordStore.containsKey(normalized)) {
            passwordStore[normalized] = "google_authenticated_passwordless_account"
            saveAccountsToPrefs()
        }

        performLoginSuccess(normalized)

        // Pre-fill profile name from Google account
        viewModelScope.launch {
            val repo = repositoryState.value
            val user = repo?.getCurrentUser()
            if (user != null && (user.name == "Nguyễn Thị Ngọc Vy" || user.name == "Nguyễn Văn Khoai" || user.name.isBlank())) {
                repo.saveUser(user.copy(name = name))
            }
        }
    }

    private fun performLoginSuccess(email: String) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("active_email", email).apply()

        loggedInEmail.value = email
        isLoggedIn.value = true

        initializeUserSession(email)
    }

    fun logout() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("active_email").apply()

        isLoggedIn.value = false
        loggedInEmail.value = ""
        repositoryState.value = null
        currentTab.value = 0
    }

    private fun saveAccountsToPrefs() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val serialized = passwordStore.entries.joinToString(";") { "${it.key}|${it.value}" }
        prefs.edit().putString("registered_accounts_v1", serialized).apply()
    }

    private suspend fun calculateCurrentWeekFromUser() {
        val user = repository?.getCurrentUser() ?: return
        val calculatedWeek = calculatePregnancyWeek(user.lmpDate, user.eddDate)
        selectedWeek.value = calculatedWeek
    }

    fun updateSelectedWeek(week: Int) {
        if (week in 1..41) {
            selectedWeek.value = week
        }
    }

    fun changeTab(tabIndex: Int) {
        currentTab.value = tabIndex
    }

    fun toggleLaborDialog(show: Boolean) {
        showLaborDialog.value = show
    }

    fun changeTheme(theme: String) {
        viewModelScope.launch {
            val user = currentUserState.value ?: return@launch
            repository?.saveUser(user.copy(visualizationTheme = theme))
        }
    }

    fun updateProfile(
        name: String, 
        lmp: String, 
        edd: String,
        dob: String? = "1998-05-15",
        conception: String? = "2026-01-29",
        startWeight: Double = 50.0,
        targetWeight: Double = 62.0
    ) {
        viewModelScope.launch {
            val user = currentUserState.value ?: UserEntity(
                name = name, 
                lmpDate = lmp, 
                eddDate = edd,
                dobDate = dob,
                conceptionDate = conception,
                gestationStartWeight = startWeight,
                targetPregnancyWeight = targetWeight
            )
            val updatedUser = user.copy(
                name = name,
                lmpDate = if (lmp.isNotBlank()) lmp else null,
                eddDate = if (edd.isNotBlank()) edd else null,
                dobDate = if (!dob.isNullOrBlank()) dob else null,
                conceptionDate = if (!conception.isNullOrBlank()) conception else null,
                gestationStartWeight = startWeight,
                targetPregnancyWeight = targetWeight
            )
            repository?.saveUser(updatedUser)
            val calculatedWeek = calculatePregnancyWeek(updatedUser.lmpDate, updatedUser.eddDate)
            selectedWeek.value = calculatedWeek
        }
    }

    fun saveInitialProfileAndComplete(
        name: String,
        lmp: String,
        edd: String,
        dob: String? = "1998-05-15",
        conception: String? = "2026-01-29",
        startWeight: Double = 50.0,
        targetWeight: Double = 62.0
    ) {
        val email = loggedInEmail.value
        viewModelScope.launch {
            updateProfile(name, lmp, edd, dob, conception, startWeight, targetWeight)
            val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("has_setup_profile_$email", true).apply()
            hasSetupProfile.value = true
        }
    }

    fun addOrUpdateWeight(week: Int, weight: Double, notes: String? = null) {
        viewModelScope.launch {
            val record = WeightRecordEntity(
                weekNumber = week,
                weightKg = weight,
                dateRecorded = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                notes = if (!notes.isNullOrBlank()) notes else "Cân nặng tự ghi mốc tuần thai $week"
            )
            repository?.saveWeightRecord(record)
        }
    }

    fun deleteWeightRecord(week: Int) {
        viewModelScope.launch {
            repository?.deleteWeightRecord(week)
        }
    }

    fun addReminder(name: String, dosage: String, time: String, type: String = "MEDICINE") {
        viewModelScope.launch {
            val user = currentUserState.value ?: return@launch
            val reminder = MedicationReminderEntity(
                userId = user.id,
                medicationName = name,
                dosage = dosage,
                scheduledTime = time,
                isActive = true,
                type = type
            )
            repository?.saveReminder(reminder)

            val app = getApplication<Application>()
            PregnancyNotifier.scheduleReminderAlert(
                context = app,
                id = reminder.id,
                title = if (type == "MEDICINE") "💊 Giờ uống vi chất: $name" else "🍎 Giờ ăn chất lượng: $name",
                message = "Mẹ bầu ơi, đến giờ dùng $dosage rồi nè!",
                timeStr = time
            )
        }
    }

    fun toggleReminderTakenToday(reminder: MedicationReminderEntity) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newTakenDate = if (reminder.lastTakenDate == todayStr) {
                null
            } else {
                todayStr
            }
            repository?.saveReminder(reminder.copy(lastTakenDate = newTakenDate))
        }
    }

    fun toggleReminderActive(reminder: MedicationReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isActive = !reminder.isActive)
            repository?.saveReminder(updated)

            val app = getApplication<Application>()
            if (updated.isActive) {
                PregnancyNotifier.scheduleReminderAlert(
                    context = app,
                    id = updated.id,
                    title = if (updated.type == "MEDICINE") "💊 Giờ uống vi chất: ${updated.medicationName}" else "🍎 Giờ ăn chất lượng: ${updated.medicationName}",
                    message = "Mẹ bầu ơi, đến giờ dùng ${updated.dosage} rồi nè!",
                    timeStr = updated.scheduledTime
                )
            } else {
                PregnancyNotifier.cancelScheduledAlert(app, updated.id)
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repository?.deleteReminder(id)
            val app = getApplication<Application>()
            PregnancyNotifier.cancelScheduledAlert(app, id)
        }
    }

    fun updateReminderDetails(reminderId: String, name: String, dosage: String, time: String, type: String) {
        viewModelScope.launch {
            val list = allRemindersState.value
            val existing = list.find { it.id == reminderId } ?: return@launch
            val updated = existing.copy(
                medicationName = name,
                dosage = dosage,
                scheduledTime = time,
                type = type
            )
            repository?.saveReminder(updated)

            val app = getApplication<Application>()
            PregnancyNotifier.cancelScheduledAlert(app, updated.id)
            if (updated.isActive) {
                PregnancyNotifier.scheduleReminderAlert(
                    context = app,
                    id = updated.id,
                    title = if (updated.type == "MEDICINE") "💊 Giờ uống vi chất: ${updated.medicationName}" else "🍎 Giờ ăn chất lượng: ${updated.medicationName}",
                    message = "Mẹ bầu ơi, đến giờ dùng ${updated.dosage} rồi nè!",
                    timeStr = updated.scheduledTime
                )
            }
        }
    }

    fun addAppointment(week: Int, date: String, clinic: String, doctor: String, notes: String, isCritical: Boolean) {
        viewModelScope.launch {
            val user = currentUserState.value ?: return@launch
            val appointment = AppointmentEntity(
                userId = user.id,
                targetWeek = week,
                scheduledDate = date,
                clinicName = if (clinic.isNotBlank()) clinic else null,
                doctorName = if (doctor.isNotBlank()) doctor else null,
                medicalNotes = if (notes.isNotBlank()) notes else null,
                isCritical = isCritical
            )
            repository?.saveAppointment(appointment)

            val app = getApplication<Application>()
            PregnancyNotifier.scheduleAppointmentAlert(
                context = app,
                id = appointment.id,
                title = "📅 Lịch khám sản khoa vào tuần $week",
                message = "Mẹ khoẻ bé khoẻ! Hôm nay bé có lịch hẹn khám tại ${clinic.ifBlank { "phòng khám" }}. Chúc mẹ đi khám thuận lợi!",
                dateStr = date
            )
        }
    }

    fun updateAppointment(id: String, week: Int, date: String, clinic: String, doctor: String, notes: String, isCritical: Boolean, status: String = "PENDING") {
        viewModelScope.launch {
            val user = currentUserState.value ?: return@launch
            val appointment = AppointmentEntity(
                id = id,
                userId = user.id,
                targetWeek = week,
                scheduledDate = date,
                clinicName = if (clinic.isNotBlank()) clinic else null,
                doctorName = if (doctor.isNotBlank()) doctor else null,
                medicalNotes = if (notes.isNotBlank()) notes else null,
                isCritical = isCritical,
                status = status
            )
            repository?.saveAppointment(appointment)

            val app = getApplication<Application>()
            PregnancyNotifier.cancelScheduledAlert(app, id)
            PregnancyNotifier.scheduleAppointmentAlert(
                context = app,
                id = id,
                title = "📅 Lịch khám sản khoa vào tuần $week",
                message = "Mẹ khoẻ bé khoẻ! Hôm nay bé có lịch hẹn khám tại ${clinic.ifBlank { "phòng khám" }}. Chúc mẹ đi khám thuận lợi!",
                dateStr = date
            )
        }
    }

    fun toggleAppointmentStatus(appointment: AppointmentEntity) {
        viewModelScope.launch {
            val nextStatus = when (appointment.status) {
                "PENDING" -> "COMPLETED"
                "COMPLETED" -> "PENDING"
                else -> "PENDING"
            }
            repository?.saveAppointment(appointment.copy(status = nextStatus))
        }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch {
            repository?.deleteAppointment(id)
            val app = getApplication<Application>()
            PregnancyNotifier.cancelScheduledAlert(app, id)
        }
    }

    // Helper functions for date calculations
    fun calculatePregnancyWeek(lmpStr: String?, eddStr: String?): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().time

        if (!lmpStr.isNullOrBlank()) {
            try {
                val lmpDate = sdf.parse(lmpStr)
                if (lmpDate != null) {
                    val diff = today.time - lmpDate.time
                    val diffDays = diff / (1000 * 60 * 60 * 24)
                    val week = (diffDays / 7) + 1
                    return week.toInt().coerceIn(1, 41)
                }
            } catch (e: Exception) {
                // Parse failed
            }
        }

        if (!eddStr.isNullOrBlank()) {
            try {
                val eddDate = sdf.parse(eddStr)
                if (eddDate != null) {
                    val diff = eddDate.time - today.time
                    val diffDays = diff / (1000 * 60 * 60 * 24)
                    val completedWeeks = (280 - diffDays) / 7
                    val currentWeek = completedWeeks + 1
                    return currentWeek.toInt().coerceIn(1, 41)
                }
            } catch (e: Exception) {
                // Parse failed
            }
        }

        return -1 // Fallback indicating not set
    }

    fun getDaysLeftToDue(eddStr: String?): Long {
        if (eddStr.isNullOrBlank()) return -1 // Return -1 indicating no dates set
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val eddDate = sdf.parse(eddStr) ?: return 112
            val today = Calendar.getInstance()
            // Reset hours, minutes, seconds for clean day bounds
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val diff = eddDate.time - today.time.time
            val diffDays = diff / (1000 * 60 * 60 * 24)
            return if (diffDays < 0) 0 else diffDays
        } catch (e: Exception) {
            return 112
        }
    }

    fun estimateDatesFromEDD(eddStr: String): Pair<String, String>? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.isLenient = false
        return try {
            val eddDate = sdf.parse(eddStr) ?: return null
            
            val calLmp = Calendar.getInstance()
            calLmp.time = eddDate
            calLmp.add(Calendar.DAY_OF_YEAR, -280)
            val lmpRes = sdf.format(calLmp.time)
            
            val calConception = Calendar.getInstance()
            calConception.time = eddDate
            calConception.add(Calendar.DAY_OF_YEAR, -266)
            val conceptionRes = sdf.format(calConception.time)
            
            Pair(lmpRes, conceptionRes)
        } catch (e: Exception) {
            null
        }
    }

    fun estimateDatesFromLMP(lmpStr: String): Pair<String, String>? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.isLenient = false
        return try {
            val lmpDate = sdf.parse(lmpStr) ?: return null
            
            val calEdd = Calendar.getInstance()
            calEdd.time = lmpDate
            calEdd.add(Calendar.DAY_OF_YEAR, 280)
            val eddRes = sdf.format(calEdd.time)
            
            val calConception = Calendar.getInstance()
            calConception.time = lmpDate
            calConception.add(Calendar.DAY_OF_YEAR, 14)
            val conceptionRes = sdf.format(calConception.time)
            
            Pair(eddRes, conceptionRes)
        } catch (e: Exception) {
            null
        }
    }

    fun estimateDatesFromConception(conceptionStr: String): Pair<String, String>? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.isLenient = false
        return try {
            val conceptionDate = sdf.parse(conceptionStr) ?: return null
            
            val calLmp = Calendar.getInstance()
            calLmp.time = conceptionDate
            calLmp.add(Calendar.DAY_OF_YEAR, -14)
            val lmpRes = sdf.format(calLmp.time)
            
            val calEdd = Calendar.getInstance()
            calEdd.time = conceptionDate
            calEdd.add(Calendar.DAY_OF_YEAR, 266)
            val eddRes = sdf.format(calEdd.time)
            
            Pair(lmpRes, eddRes)
        } catch (e: Exception) {
            null
        }
    }

    fun calculateDetailedGestationalAge(lmpStr: String?, eddStr: String?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        var lmpParsed: java.util.Date? = null

        if (!lmpStr.isNullOrBlank()) {
            try {
                lmpParsed = sdf.parse(lmpStr)
            } catch (e: Exception) {}
        }

        if (lmpParsed == null && !eddStr.isNullOrBlank()) {
            try {
                val eddDate = sdf.parse(eddStr)
                if (eddDate != null) {
                    val cal = Calendar.getInstance()
                    cal.time = eddDate
                    cal.add(Calendar.DAY_OF_YEAR, -280)
                    lmpParsed = cal.time
                }
            } catch (e: Exception) {}
        }

        if (lmpParsed != null) {
            val diff = today.time.time - lmpParsed.time
            val diffDays = diff / (1000 * 60 * 60 * 24)
            if (diffDays >= 0) {
                val weeks = diffDays / 7
                val days = diffDays % 7
                return "Tuần $weeks + $days ngày"
            }
        }
        return "Chưa thiết lập"
    }
}

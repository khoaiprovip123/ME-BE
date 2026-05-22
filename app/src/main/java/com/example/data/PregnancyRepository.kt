package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class PregnancyRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val fetalWeekDao = database.fetalWeekDao()
    private val appointmentDao = database.appointmentDao()
    private val medicationReminderDao = database.medicationReminderDao()
    private val weightRecordDao = database.weightRecordDao()

    val currentUserFlow: Flow<UserEntity?> = userDao.getUserFlow()
    val allWeeksFlow: Flow<List<FetalWeekEntity>> = fetalWeekDao.getAllWeeksFlow()
    val allAppointmentsFlow: Flow<List<AppointmentEntity>> = appointmentDao.getAllAppointmentsFlow()
    val allRemindersFlow: Flow<List<MedicationReminderEntity>> = medicationReminderDao.getAllRemindersFlow()
    val allWeightRecordsFlow: Flow<List<WeightRecordEntity>> = weightRecordDao.getAllWeightRecordsFlow()

    fun getWeekFlow(week: Int): Flow<FetalWeekEntity?> = fetalWeekDao.getWeekFlow(week)

    suspend fun getCurrentUser(): UserEntity? = userDao.getUserSync()

    suspend fun saveUser(user: UserEntity) {
        userDao.insertOrUpdate(user)
        seedDefaultDataForUser(user) // Seed if they edited/reset profile but have empty agenda
    }

    suspend fun saveWeightRecord(record: WeightRecordEntity) {
        weightRecordDao.insertOrUpdate(record)
    }

    suspend fun deleteWeightRecord(week: Int) {
        weightRecordDao.deleteByWeek(week)
    }

    suspend fun saveAppointment(appointment: AppointmentEntity) {
        appointmentDao.insertOrUpdate(appointment)
    }

    suspend fun deleteAppointment(id: String) {
        appointmentDao.deleteById(id)
    }

    suspend fun saveReminder(reminder: MedicationReminderEntity) {
        medicationReminderDao.insertOrUpdate(reminder)
    }

    suspend fun deleteReminder(id: String) {
        medicationReminderDao.deleteById(id)
    }

    suspend fun seedDatabaseIfNeeded() {
        val weekCount = fetalWeekDao.countWeeks()
        if (weekCount == 0) {
            val seedData = FetalWeekSeeder.getSeedData()
            fetalWeekDao.insertWeeks(seedData)
        }

        var currentUser = userDao.getUserSync()
        if (currentUser == null) {
            currentUser = UserEntity(
                id = UUID.randomUUID().toString(),
                name = "",
                lmpDate = null,
                eddDate = null,
                visualizationTheme = "FRUIT",
                dobDate = null,
                conceptionDate = null
            )
            userDao.insertOrUpdate(currentUser)
        }

        seedDefaultDataForUser(currentUser)
    }

    suspend fun seedDefaultDataForUser(user: UserEntity) {
        val hasGestationDates = !user.lmpDate.isNullOrBlank() || !user.eddDate.isNullOrBlank()

        // Seed default critical medical milestones if the table empty and user has setup gestation dates
        val apptCount = appointmentDao.countAppointments()
        if (apptCount == 0 && hasGestationDates) {
            val milestoneWeeks = listOf(12, 22, 26, 32, 37)
            val milestoneNames = listOf(
                "Khám thai Mốc 1: Siêu âm đo Độ mờ da gáy NT & Double Test",
                "Khám thai Mốc 2: Siêu âm 4D hình thái toàn diện bé yêu",
                "Khám thai Mốc 3: Nghiệm pháp dung nạp Glucose của mẹ",
                "Khám thai Mốc 4: Siêu âm Doppler mạch, ngôi thai lý tưởng",
                "Khám thai Mốc cận sinh: Chạy Monitor NST kiểm tra tim thai"
            )
            val milestoneClinics = listOf(
                "Phòng khám Sản Phụ khoa Trung ương",
                "Phòng siêu âm 4D hình thái học Sản khoa",
                "Khoa Xét nghiệm - Bệnh viện Phụ sản Hà Nội",
                "Bệnh viện Sản Phụ khoa Quốc tế",
                "Phòng cấp cứu Sản khoa & Chạy máy NST"
            )
            val milestoneNotes = listOf(
                "Đặc biệt quan trọng tầm soát sụn da gáy, dị tật nhiễm sắc thể Down/Patau ở mốc tuần 11-13.",
                "Rà soát 2 bán cầu não bộ, sứt môi hở hàm ếch, chi tiết ổ bụng các ngón chân mầm tim của bé.",
                "Nhịn ăn sáng ít nhất 8 tiếng trước khi thực hiện lấy máu thử dung nạp tiểu đường thai kỳ.",
                "Đánh giá bánh nhau vôi hóa, dây rốn thắt nút, độ dày ối đo lường nguy cơ hạn chế phát triển.",
                "Chạy Monitor đo biểu đồ cơn gò sinh lý tử cung và đo tim thai dự báo thiếu oxy cấp."
            )

            for (i in milestoneWeeks.indices) {
                val targetW = milestoneWeeks[i]
                val calcDate = calculateDateForWeek(user.lmpDate, targetW, user.eddDate)
                appointmentDao.insertOrUpdate(
                    AppointmentEntity(
                        userId = user.id,
                        targetWeek = targetW,
                        scheduledDate = calcDate,
                        clinicName = milestoneClinics[i],
                        doctorName = "Bác sĩ Trưởng khoa Chẩn đoán",
                        medicalNotes = milestoneNotes[i],
                        status = "PENDING",
                        isCritical = true
                    )
                )
            }
        }

        // Seed medicine (vi chất) & diet (dinh dưỡng) if reminders are empty (can be seeded regardless of dates)
        val reminderCount = medicationReminderDao.countReminders()
        if (reminderCount == 0) {
            // Seeding daily supplements
            val defaultMedicines = listOf(
                Triple("Bổ sung Sắt hữu cơ & Axit Folic giải phóng kéo dài", "1 viên uống sau ăn sáng 1-2 tiếng (giúp nạp máu, giảm thiếu máu)", "08:15"),
                Triple("Bổ sung Canxi Chewable Canxi Nano dễ tan", "1 viên uống bổ sung buổi sáng (uống xa bữa sắt tránh ức chế)", "10:00"),
                Triple("Bổ sung DHA tinh khiết & Vitamin tổng hợp bầu", "1 viên nang dầu mềm uống ngay chung với cơm trưa cho hấp thụ tốt nhất", "12:30")
            )
            for (med in defaultMedicines) {
                medicationReminderDao.insertOrUpdate(
                    MedicationReminderEntity(
                        userId = user.id,
                        medicationName = med.first,
                        dosage = med.second,
                        scheduledTime = med.third,
                        isActive = true,
                        type = "MEDICINE"
                    )
                )
            }

            // Seeding daily nutritional diet items
            val defaultDiets = listOf(
                Triple("Uống ly sữa bầu dinh dưỡng ấm", "Pha 4 muỗng sữa bầu tăng cường bổ sung chất xơ hòa tan (FOS)", "07:30"),
                Triple("Ăn bữa phụ nhẹ trái cây giàu Vitamin C (Cam, KiWi, Ổi chín)", "Giúp hỗ trợ hấp thụ sắt triệt để bầu máu khỏe mạnh", "15:00"),
                Triple("Nhai vốc hạt dinh dưỡng sấy (Óc chó, Macca, Hạnh nhân)", "Giàu canxi, Omega 3, kẽm vàng kích thích não bộ mầm thai", "16:30"),
                Triple("Uống ly sữa ấm trước khi ngủ", "200ml ấm dập ấm ruột, chống đói đêm và hỗ trợ mẹ ngủ sâu giấc", "21:30")
            )
            for (diet in defaultDiets) {
                medicationReminderDao.insertOrUpdate(
                    MedicationReminderEntity(
                        userId = user.id,
                        medicationName = diet.first,
                        dosage = diet.second,
                        scheduledTime = diet.third,
                        isActive = true,
                        type = "DIET"
                    )
                )
            }
        }

        // Seed initial weight records if empty and user has setup gestation dates
        val weightCount = weightRecordDao.countWeightRecords()
        if (weightCount == 0 && hasGestationDates) {
            val startW = user.gestationStartWeight
            val defaultRecordedWeights = listOf(
                Pair(1, startW),
                Pair(4, startW + 0.2),
                Pair(8, startW + 0.6),
                Pair(12, startW + 1.2),
                Pair(16, startW + 2.3),
                Pair(20, startW + 3.6),
                Pair(24, startW + 5.2)
            )
            for (p in defaultRecordedWeights) {
                val calcDate = calculateDateForWeek(user.lmpDate, p.first, user.eddDate)
                weightRecordDao.insertOrUpdate(
                    WeightRecordEntity(
                        weekNumber = p.first,
                        weightKg = p.second,
                        dateRecorded = calcDate,
                        notes = "Cân nặng ổn định lành mạnh mốc tuần thai ${p.first}"
                    )
                )
            }
        }
    }

    fun calculateDateForWeek(lmpStr: String?, targetWeek: Int, eddStr: String?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        if (!lmpStr.isNullOrBlank()) {
            try {
                val lmpDate = sdf.parse(lmpStr)
                if (lmpDate != null) {
                    cal.time = lmpDate
                    cal.add(Calendar.DAY_OF_YEAR, targetWeek * 7)
                    return sdf.format(cal.time)
                }
            } catch (e: Exception) {}
        }
        if (!eddStr.isNullOrBlank()) {
            try {
                val eddDate = sdf.parse(eddStr)
                if (eddDate != null) {
                    cal.time = eddDate
                    val daysBefore = (40 - targetWeek) * 7
                    cal.add(Calendar.DAY_OF_YEAR, -daysBefore)
                    return sdf.format(cal.time)
                }
            } catch (e: Exception) {}
        }
        val diffWeeks = targetWeek - 24
        cal.add(Calendar.DAY_OF_YEAR, diffWeeks * 7)
        return sdf.format(cal.time)
    }
}

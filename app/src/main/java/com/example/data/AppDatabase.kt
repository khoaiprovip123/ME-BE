package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserSync(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)
}

@Dao
interface FetalWeekDao {
    @Query("SELECT * FROM fetal_development_master ORDER BY weekNumber ASC")
    fun getAllWeeksFlow(): Flow<List<FetalWeekEntity>>

    @Query("SELECT * FROM fetal_development_master WHERE weekNumber = :week LIMIT 1")
    fun getWeekFlow(week: Int): Flow<FetalWeekEntity?>

    @Query("SELECT * FROM fetal_development_master WHERE weekNumber = :week LIMIT 1")
    suspend fun getWeekSync(week: Int): FetalWeekEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeks(weeks: List<FetalWeekEntity>)

    @Query("SELECT COUNT(*) FROM fetal_development_master")
    suspend fun countWeeks(): Int
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM medical_appointments ORDER BY scheduledDate ASC")
    fun getAllAppointmentsFlow(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM medical_appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentById(id: String): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)

    @Query("DELETE FROM medical_appointments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM medical_appointments")
    suspend fun countAppointments(): Int
}

@Dao
interface MedicationReminderDao {
    @Query("SELECT * FROM medication_reminders ORDER BY scheduledTime ASC")
    fun getAllRemindersFlow(): Flow<List<MedicationReminderEntity>>

    @Query("SELECT * FROM medication_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): MedicationReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reminder: MedicationReminderEntity)

    @Delete
    suspend fun delete(reminder: MedicationReminderEntity)

    @Query("DELETE FROM medication_reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM medication_reminders")
    suspend fun countReminders(): Int
}

@Dao
interface WeightRecordDao {
    @Query("SELECT * FROM weight_records ORDER BY weekNumber ASC")
    fun getAllWeightRecordsFlow(): Flow<List<WeightRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: WeightRecordEntity)

    @Query("DELETE FROM weight_records WHERE weekNumber = :week")
    suspend fun deleteByWeek(week: Int)

    @Query("SELECT COUNT(*) FROM weight_records")
    suspend fun countWeightRecords(): Int
}

@Dao
interface ReminderLogDao {
    @Query("SELECT * FROM reminder_logs ORDER BY date DESC")
    fun getAllLogsFlow(): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_logs WHERE date = :date")
    suspend fun getLogsByDate(date: String): List<ReminderLogEntity>

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId AND date = :date LIMIT 1")
    suspend fun getLogForReminderAndDate(reminderId: String, date: String): ReminderLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: ReminderLogEntity)

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId AND date = :date")
    suspend fun deleteLogForReminderAndDate(reminderId: String, date: String)

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId")
    suspend fun deleteLogsForReminder(reminderId: String)
}

@Database(
    entities = [
        UserEntity::class,
        FetalWeekEntity::class,
        AppointmentEntity::class,
        MedicationReminderEntity::class,
        WeightRecordEntity::class,
        ReminderLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun fetalWeekDao(): FetalWeekDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun weightRecordDao(): WeightRecordDao
    abstract fun reminderLogDao(): ReminderLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val INSTANCES = mutableMapOf<String, AppDatabase>()

        fun getDatabase(context: Context): AppDatabase {
            return getDatabase(context, "")
        }

        fun getDatabase(context: Context, suffix: String): AppDatabase {
            val dbKey = if (suffix.isBlank()) "default" else suffix
            val dbName = if (suffix.isBlank()) "me_va_be_database" else "me_va_be_database_${suffix.replace("@", "_").replace(".", "_")}"
            return INSTANCES[dbKey] ?: synchronized(this) {
                INSTANCES[dbKey] ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                .fallbackToDestructiveMigration()
                .build().also {
                    INSTANCES[dbKey] = it
                }
            }
        }
    }
}

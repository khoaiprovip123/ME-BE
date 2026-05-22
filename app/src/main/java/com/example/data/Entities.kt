package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lmpDate: String?, // yyyy-MM-dd
    val eddDate: String?, // yyyy-MM-dd
    val visualizationTheme: String = "FRUIT", // FRUIT, BAKERY, ANIMAL
    val dobDate: String? = "1998-05-15", // Ngày sinh của mẹ mẹ bầu
    val conceptionDate: String? = "2026-01-29", // Ngày thụ thai / thời gian chính thức có thai
    val gestationStartWeight: Double = 50.0, // Cân nặng bắt đầu thai kỳ (kg)
    val targetPregnancyWeight: Double = 62.0 // Cân nặng mục tiêu trước sinh (kg)
)

@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey val weekNumber: Int, // Tuần thai ghi nhận (1 tới 42)
    val weightKg: Double, // Cân nặng thực tế mẹ bầu đo (kg)
    val dateRecorded: String, // Ngày cân đo (yyyy-MM-dd)
    val notes: String? = null // Ghi chú thể chất ăn uống hoặc tâm lý tuần đó
)

@Entity(tableName = "fetal_development_master")
data class FetalWeekEntity(
    @PrimaryKey val weekNumber: Int, // 1 to 41
    val avgWeightG: Double,
    val avgLengthCm: Double,
    val fruitEquivalent: String,
    val bakeryEquivalent: String,
    val animalEquivalent: String,
    val physiologyDescription: String,
    val maternalChanges: String,
    val nutritionalRecommendation: String,
    val exceptionAlerts: String
)

@Entity(tableName = "medical_appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val targetWeek: Int,
    val scheduledDate: String, // yyyy-MM-dd
    val clinicName: String?,
    val doctorName: String?,
    val medicalNotes: String?,
    val status: String = "PENDING", // PENDING, COMPLETED, CANCELLED, MISSED
    val isCritical: Boolean = false
)

@Entity(tableName = "medication_reminders")
data class MedicationReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val medicationName: String,
    val dosage: String,
    val scheduledTime: String, // HH:mm
    val isActive: Boolean = true,
    val lastTakenDate: String? = null, // To prevent double intake in the same day
    val type: String = "MEDICINE" // MEDICINE or DIET
)

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val time: String,
    val type: String, // "Presensi Masuk", "Presensi Pulang", "Izin", "Sakit"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

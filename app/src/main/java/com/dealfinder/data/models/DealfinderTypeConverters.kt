package com.dealfinder.data.models

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Type converters for Room to handle custom types
 */
class DealfinderTypeConverters {
    
    // UserType converters
    @TypeConverter
    fun fromUserType(type: UserType): String = type.name
    
    @TypeConverter
    fun toUserType(value: String): UserType = UserType.valueOf(value)
    
    // BookingType converters
    @TypeConverter
    fun fromBookingType(type: BookingType): String = type.name
    
    @TypeConverter
    fun toBookingType(value: String): BookingType = BookingType.valueOf(value)
    
    // VerificationStatus converters
    @TypeConverter
    fun fromVerificationStatus(status: VerificationStatus): String = status.name
    
    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus = VerificationStatus.valueOf(value)
    
    // BookingStatus converters
    @TypeConverter
    fun fromBookingStatus(status: BookingStatus): String = status.name
    
    @TypeConverter
    fun toBookingStatus(value: String): BookingStatus = BookingStatus.valueOf(value)
    
    // Map<String, Float> converters for criteria
    @TypeConverter
    fun fromCriteriaMap(map: Map<String, Float>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
    
    @TypeConverter
    fun toCriteriaMap(value: String): Map<String, Float> {
        return value.split(",").associate {
            val (key, v) = it.split(":")
            key to v.toFloat()
        }
    }
    
    // Instant converters
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()
    
    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }
}

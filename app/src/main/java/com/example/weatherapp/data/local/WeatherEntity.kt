package com.example.weatherapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database Entity for weather data
 *
 * Defines the schema for a record in the database
 */
@Entity(tableName = "weather_table")
data class WeatherEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val aqi: String,
    val timestamp: String,
)

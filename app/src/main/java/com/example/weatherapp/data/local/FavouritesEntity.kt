package com.example.weatherapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database Entity for favourited cities
 *
 * Defines the schema for a record in the database
 */
@Entity(tableName = "favourites")
data class FavouritesEntity (
    @PrimaryKey
    val cityName: String,
    val country: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
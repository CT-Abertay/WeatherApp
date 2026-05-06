package com.example.weatherapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Database Access Object for the weather data table
 *
 * Defines the CRUD operations for weather data.
 * Designed to maintain a rolling history where older data is overwritten
 */
@Dao
interface WeatherDao {

    /**
     * Retrieves all stored weather data with the newest record at index 0
     * Returns a Flow that acts as a flowing stream of data,
     * automatically updating the list whenever the data changes
     */
    @Query("select * from weather_table order by id desc")
    fun getCachedWeather(): Flow<List<WeatherEntity>>

    /**
     * Stores a new record in the database
     * OnConflictStrategy.REPLACE overwrites old data in case of conflict
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    /**
     * Limits the database size to 10 records by deleting the oldest records
     *
     * Subquery finds the 10 newest records, main query deletes any other record
     */
    @Query("delete from weather_table where id not in (select id from weather_table order by id desc limit 10)")
    suspend fun deleteOldRecords()

    /**
     * @Transaction ensures both insert and delete happen simultaneously
     * Preventing the database from becoming inconsistent
     */
    @Transaction
    suspend fun insertAndTrim(weather: WeatherEntity) {
        insertWeather(weather)
        deleteOldRecords()
    }
}
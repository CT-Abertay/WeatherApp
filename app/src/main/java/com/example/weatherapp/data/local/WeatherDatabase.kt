package com.example.weatherapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database Layer for configuring the main database
 *
 * This class serves as the main access point to the SQLite database
 * Listing all entities (Tables) and the database version
 *
 * Version must be incremented each time the database schema is changed
 */
@Database(entities = [WeatherEntity::class, FavouritesEntity::class], version = 11)
abstract class WeatherDatabase : RoomDatabase() {

    /**
     * Abstract functions to access the DAOs
     * Room will generate the actual implementation code
     */
    abstract fun weatherDao(): WeatherDao
    abstract fun favouritesDao(): FavouritesDao

    companion object {
        /**
         * @Volatile: Ensures that the database is always up to date,
         * and the same across all threads.
         */
        @Volatile private var INSTANCE: WeatherDatabase? = null

        /**
         * Function to check if the database exists, returns if true, creates it if false
         */
        fun getDatabase(context: Context): WeatherDatabase {
            /**
             * Returns INSTANCE if it exists, if not, enter the synchronized block
             * synchronized(this) prevents multiple threads from creating two database
             * instances at the same time.
             */
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // applicationContext prevents memory leaks by only keeping the instance alive until the app is terminated
                    WeatherDatabase::class.java,
                    "weather_database" // Database filename
                ).fallbackToDestructiveMigration().build() // If DB version changes, delete the DB and recreate it

                // Sets INSTANCE to the new db and returns it
                INSTANCE = instance
                instance
            }
        }
    }
}
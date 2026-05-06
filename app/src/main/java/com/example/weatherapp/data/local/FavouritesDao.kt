package com.example.weatherapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the favourites table
 * Defines the CRUD operations for favourited locations
 */
@Dao
interface FavouritesDao {
    /**
     * Retrieves all favourited locations from the database.
     * Returns a Flow which acts as a reactive stream of data.
     * Whenever data in the database is modified,
     * The flow will automatically update the list.
     */
    @Query("select * from favourites order by addedAt desc")
    fun getFavourites(): Flow<List<FavouritesEntity>>

    /**
     * Inserts a new location to the database.
     * OnConflictStrategy.REPLACE instructs the database to
     * overwrite the old record in case of a conflict.
     * suspend function to make it run on a background thread
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(location: FavouritesEntity)

    /**
     * Removes a location from the database
     */
    @Delete
    suspend fun deleteFavourite(location: FavouritesEntity)

    /**
     * Checks if a specific city is already stored as a favourite
     * @return  true if the city exists
     */
    @Query("select exists(select 1 from favourites where cityName=:name)")
    suspend fun isFavourite(name: String): Boolean
}
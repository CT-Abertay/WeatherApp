package com.example.weatherapp.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.FavouritesEntity
import com.example.weatherapp.data.local.WeatherEntity
import com.example.weatherapp.data.repository.WeatherRepository
import kotlinx.coroutines.launch

/**
 * ViewModel that includes an application reference.
 * Allowing access to SharedPreferences or context without memory leaks
 */
class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository,
) : AndroidViewModel(application) {

    // Fetch SharedPreferences to access user unit preference
    private val sharedPref = application.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)

    // Obtains a stream of weather data
    val allWeatherData: LiveData<List<WeatherEntity>> = repository.allWeather.asLiveData()

    // Obtains a stream of favourites data
    val allFavorites: LiveData<List<FavouritesEntity>> = repository.allFavourites.asLiveData()

    // Automatically extracts the most recent weather record when data updates
    val latestWeather = allWeatherData.map { list -> list.firstOrNull() }

    // Tracks which city is currently being displayed on the home screen
    val selectedCity = MutableLiveData<String?>(null)

    /**
     * Function to update the currently selected city
     *
     * @param city  Name of the city being displayed
     */
    fun selectCity(city: String) {
        selectedCity.value = city
        fetchWeather(city)
    }

    /**
     * Function to fetch weather data from API
     *
     * @param city  City to fetch data for
     */
    fun fetchWeather(city: String) {
        // Fetch user unit preference
        val unit = sharedPref.getString("unit_type", "metric") ?: "metric"

        viewModelScope.launch {
            repository.fetchAndSaveWeather(city, unit)
        }
    }

    /**
     * Toggles whether a city is saved in the favourites list or not
     */
    fun toggleFavourite(city: String) {
        viewModelScope.launch {
            repository.toggleFavourite(city)
        }
    }
}
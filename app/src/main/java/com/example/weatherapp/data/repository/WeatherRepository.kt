package com.example.weatherapp.data.repository

import android.util.Log
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.data.local.FavouritesDao
import com.example.weatherapp.data.local.FavouritesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import com.example.weatherapp.data.local.WeatherDao
import com.example.weatherapp.data.local.WeatherEntity
import com.example.weatherapp.utils.NetworkManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to weather data source.
 */
class WeatherRepository(private val weatherDao: WeatherDao,
                        private val favouritesDao: FavouritesDao,
                        private val networkManager: NetworkManager
) {

    // API Key retrieved from props file via buildConfig
    private val apiKey = BuildConfig.API_KEY

    // Streams of cached data
    val allWeather: Flow<List<WeatherEntity>> = weatherDao.getCachedWeather()
    val allFavourites: Flow<List<FavouritesEntity>> = favouritesDao.getFavourites()

    /**
     * Performs 2 API calls (Weather and AQI) and saves the result to local db
     *
     * Uses withContext to execute on the IO thread pool, ensuring UI responsiveness
     *
     * @param city  name of the city to query
     * @param unit  measurement system
     *
     */
    suspend fun fetchAndSaveWeather(city: String, unit: String) = withContext(Dispatchers.IO) {
        // Check for internet connection before continuing
        if (!networkManager.isOnline()) {
            Log.e("WeatherRepository", "No internet connection. Skipping fetch.")
            return@withContext
        }

        try {
            // Encode city name to prepare for URL
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")

            // Construct request and fetch weather data from API
            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedCity&key=$apiKey"
            val geoResponse = networkManager.performGetRequest(url)
            val geoJson = JSONObject(geoResponse)
            val results = geoJson.getJSONArray("results")

            // Check if results were returned
            if (results.length() == 0) {
                Log.e("WeatherRepository", "No results found for city: $city")
                return@withContext
            }

            val json = results.getJSONObject(0)

            // Extract coordinates for air quality API call
            val location = json.getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")

            // Extract official city name and country
            val addressComponents = json.getJSONArray("address_components")
            val cityName = addressComponents.getJSONObject(0).getString("long_name")
            val country = addressComponents.optJSONObject(addressComponents.length() - 1)?.getString("short_name") ?: ""

            // Fetch weather data from API
            val weatherData = fetchGoogleWeather(lat, lng, unit)

            // Fetch AQI data from API
            val aqi = fetchGoogleAQI(lat, lng)

            // Store retrieved data as a weather entity
            val entity = WeatherEntity(
                cityName = cityName,
                country = country,
                temperature = weatherData.temperature,
                feelsLike = weatherData.feelsLike,
                humidity = weatherData.humidity,
                windSpeed = weatherData.windSpeed,
                aqi = aqi,
                timestamp = DateFormat.getDateTimeInstance().format(Date())
            )

            // Pass entity to the dao layer to be inserted into the db
            weatherDao.insertAndTrim(entity)

        } catch (e: Exception) {
            // Catch and log any exceptions
            Log.e("WeatherRepository", "Error fetching weather", e)
        }
    }

    /**
     * Fetches current weather data from google weather api
     *
     * @param lat   latitude of chosen city
     * @param lng   longitude of the city
     * @param unit  User unit preference
     * @return WeatherResult object containing parsed data
     */
    private fun fetchGoogleWeather(lat: Double, lng: Double, unit: String): WeatherResult {
        // Converts stored unit system to specific ENUM required by google
        val unitsSystem = if (unit.equals("imperial", ignoreCase = true)) "IMPERIAL" else "METRIC"

        // Construct GET Request
        val url = "https://weather.googleapis.com/v1/currentConditions:lookup" +
                "?key=$apiKey" +
                "&location.latitude=$lat" +
                "&location.longitude=$lng" +
                "&unitsSystem=$unitsSystem"

        // Execute the request
        val response = networkManager.performGetRequest(url)

        // Initialize JSONObject
        val root = JSONObject(response)

        // Error check
        if (root.has("error")) {
            val errorMsg = root.getJSONObject("error").optString("message", "Unknown Google API Error")
            throw Exception(errorMsg)
        }

        // Extract Values from JSONObject
        val tempObj = root.optJSONObject("temperature")
        val tempValue = tempObj?.optDouble("degrees", 0.0) ?: 0.0

        val feelsLikeObj = root.optJSONObject("feelsLikeTemperature")
        val feelsLikeValue = feelsLikeObj?.optDouble("degrees", 0.0) ?: 0.0

        val humidityValue = root.optInt("relativeHumidity", 0)

        val windObj = root.optJSONObject("wind")
        val speedObj = windObj?.optJSONObject("speed")
        val windSpeedValue = speedObj?.optDouble("value", 0.0) ?: 0.0

        // return WeatherResult of data
        return WeatherResult(
            temperature = tempValue,
            feelsLike = feelsLikeValue,
            humidity = humidityValue,
            windSpeed = windSpeedValue
        )
    }

    private fun fetchGoogleAQI(lat: Double, lng: Double): String {
        // Construct POST Request
        val url = "https://airquality.googleapis.com/v1/currentConditions:lookup?key=$apiKey"
        val body = """{"location": {"latitude": $lat, "longitude": $lng}}"""

        // Execute the request
        val response = networkManager.performPostRequest(url, body)

        // Initialize JSONObject
        val json = JSONObject(response)

        // Extract data from JSONObject and return it
        return json.getJSONArray("indexes").getJSONObject(0).getString("category")
    }

    /**
     * Function to flip a cities favourite status
     *
     * @param city  name of the city to flip
     */
    suspend fun toggleFavourite(city: String) = withContext(Dispatchers.IO) {
        // Query the db to check if the city exists
        if (favouritesDao.isFavourite(city)) {
            // If true, remove it
            favouritesDao.deleteFavourite(FavouritesEntity(city))
        } else {
            // If false, add it
            favouritesDao.insertFavourite(FavouritesEntity(city))
        }
    }

    // Simple data wrapper for the weather fetch
    data class WeatherResult(
        val temperature: Double,
        val feelsLike: Double,
        val humidity: Int,
        val windSpeed: Double
    )
}
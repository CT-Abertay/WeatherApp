package com.example.weatherapp.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.R
import com.example.weatherapp.data.local.WeatherDatabase
import com.example.weatherapp.data.local.WeatherEntity
import com.example.weatherapp.databinding.FragmentHomeBinding
import com.example.weatherapp.ui.WeatherViewModel
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Observer
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.utils.NetworkManager


/**
 * Fragment responsible for the weather display
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    // View binding properties to access XML views
    // _binding to allow for binding to be set null, freeing memory
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Initialize ViewModel & SharedPreferences
    private lateinit var viewModel: WeatherViewModel
    private lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize binding
        _binding = FragmentHomeBinding.bind(view)

        // Initialize SharedPreferences to check user settings
        sharedPref = requireContext().getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)

        // Initialize db access
        val wDao = WeatherDatabase.getDatabase(requireContext()).weatherDao()
        val fDao = WeatherDatabase.getDatabase(requireContext()).favouritesDao()
        val networkManager = NetworkManager(requireContext())

        val repository = WeatherRepository(wDao, fDao, networkManager)

        /**
         * Setup ViewModel using requireActivity allowing it to survive tab switches
         * and share data with other fragments
         */
        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Manually pass dependencies to ViewModel
                return WeatherViewModel(requireActivity().application, repository) as T
            }
        })[WeatherViewModel::class.java]

        // Call functions to setup observers and listeners
        setupObservers()
        setupClickListeners()
    }

    /**
     * Function to nullify binding reference to avoid memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Function to sets up LiveData observers. when data in the db changes, the
     * observers allow the UI to update automatically
     */
    private fun setupObservers() {
        // Observer to manage which city's data is being displayed
        viewModel.selectedCity.observe(viewLifecycleOwner) { city ->
            if (city == null) {
                // If no city is selected, load the last cached city (or default to london)
                val lastWeather = viewModel.latestWeather.value
                val cityToFetch = if (lastWeather == null) "London" else lastWeather.cityName
                viewModel.fetchWeather(cityToFetch)
            }
        }

        // Observer to update city's weather data
        viewModel.latestWeather.observe(viewLifecycleOwner) { weather ->
            weather?.let {
                // Determine units based on user settings
                val unit = sharedPref.getString("unit_type", "metric") ?: "metric"
                val temp = if (unit == "imperial") "°F" else "°C"
                val speed = if (unit == "imperial") "mph" else "km/h"

                // Bind data to TextViews
                binding.tvTemp.text = "${it.temperature}$temp"
                binding.tvCity.text = "${it.cityName}, ${it.country}"
                binding.tvTimestamp.text = "Last updated: ${it.timestamp}"
                binding.tvFeelsLike.text = "${it.feelsLike}$temp"
                binding.tvHumidity.text = "${it.humidity}%"
                binding.tvWindSpeed.text = "${it.windSpeed} $speed"
                binding.tvAQI.text = "${it.aqi}"

                // selectedCity == null on app launch
                // Ensures the viewModel knows which city is currently being displayed
                if (viewModel.selectedCity.value == null) {
                    viewModel.selectedCity.postValue(it.cityName)
                }

                // Update heart icon based on city favourite status
                refreshFavouriteStatus()
            }
        }

        // Observer to listen for changes in the favourites list and update heart icon accordingly
        viewModel.allFavorites.observe(viewLifecycleOwner) {
            refreshFavouriteStatus()
        }
    }

    /**
     * Function to configure buttons
     */
    private fun setupClickListeners() {
        // Sets up search button, triggering an API call for entered text
        binding.btnSearch.setOnClickListener {
            val city = binding.etCityInput.text.toString().trim()
            if (city.isNotEmpty()) {
                viewModel.selectCity(city)
                binding.etCityInput.text.clear()

                // Hide keyboard after search
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
            } else {
                binding.etCityInput.error = "Please enter a city"
            }
        }

        // Sets up favourite button, Toggling the cities status in the favourites database
        binding.btnAddToFavorites.setOnClickListener {
            val city = binding.tvCity.text.toString()
            if (city.isNotEmpty()) {
                viewModel.toggleFavourite(city)
            }
        }

        // Sets up favourite locations dropdown
        binding.btnFavourites.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            val favoritesList = viewModel.allFavorites.value ?: emptyList()

            if (favoritesList.isEmpty()) {
                popup.menu.add("No saved locations")
            } else {
                // Dynamically update favourites list
                favoritesList.forEachIndexed { index, favorite ->
                    popup.menu.add(0, index, index, favorite.cityName)
                }
            }

            // Setup ClickListener for each city in the favourites dropdown
            popup.setOnMenuItemClickListener { menuItem ->
                val selectedCity = menuItem.title.toString()
                if (selectedCity != "No saved locations") {
                    viewModel.fetchWeather(selectedCity) // Fetch weather for chosen city
                }
                true
            }
            popup.show()
        }
    }

    /**
     * Checks if the currently displayed city is in the users favourites list
     * And updates the heart icon accordingly
     */
    private fun refreshFavouriteStatus() {
        val favorites = viewModel.allFavorites.value ?: emptyList()
        val currentDisplay = binding.tvCity.text.toString()

        if (currentDisplay.isNotEmpty()) {
            val isSaved = favorites.any { it.cityName.equals(currentDisplay, ignoreCase = true) }
            updateHeartIcon(isSaved)
        }
    }

    /**
     * Updates the heart icon based on input
     */
    private fun updateHeartIcon(isSaved: Boolean) {
        if (isSaved) {
            binding.btnAddToFavorites.setImageResource(R.drawable.baseline_favorite_filled_24)
            binding.btnAddToFavorites.setColorFilter(Color.RED)
        } else {
            binding.btnAddToFavorites.setImageResource(R.drawable.baseline_favorite_border_24)
            binding.btnAddToFavorites.clearColorFilter()
        }
    }
}
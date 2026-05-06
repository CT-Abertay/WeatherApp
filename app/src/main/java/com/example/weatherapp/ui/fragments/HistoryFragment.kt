package com.example.weatherapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.R
import com.example.weatherapp.data.local.WeatherDatabase
import com.example.weatherapp.data.repository.WeatherRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.weatherapp.databinding.FragmentHistoryBinding
import com.example.weatherapp.ui.HistoryAdapter
import com.example.weatherapp.ui.WeatherViewModel
import com.example.weatherapp.utils.NetworkManager

/**
 * Fragment for displaying the history of weather searches
 * Uses a recycler view to show cached weather data from the local db
 */
class HistoryFragment : Fragment(R.layout.fragment_history) {

    // View binding properties to access XML views
    // _binding to allow for binding to be set null, freeing memory
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // Initialize viewModel
    private lateinit var viewModel: WeatherViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize binding
        _binding = FragmentHistoryBinding.bind(view)

        // Access room database to retrieve DAOs
        val wDao = WeatherDatabase.getDatabase(requireContext()).weatherDao()
        val fDao = WeatherDatabase.getDatabase(requireContext()).favouritesDao()
        val networkManager = NetworkManager(requireContext())

        // Initialize repository
        val repository = WeatherRepository(wDao, fDao, networkManager)

        /**
         * Setup ViewModel using requireActivity to ensure the ViewModel is scoped to the activity
         * Allowing multiple fragments to share the same data instance
         */
        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                // Manually pass dependencies to ViewModel constructor
                return WeatherViewModel(requireActivity().application, repository) as T
            }
        })[WeatherViewModel::class.java]

        /**
         * historyAdapter redirects user and updates home screen city selection
         * When a user selects a city in their history
         */
        val historyAdapter = HistoryAdapter { cityQuery ->
            // Update 'selectedCity' in the ViewModel
            viewModel.selectCity(cityQuery)

            // Switch the nav bar to home
            val navBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            navBar.selectedItemId = R.id.nav_home
        }

        /**
         * RecyclerView configuration
         */
        binding.rvHistory.apply {
            // layoutManager to arrange cities in a standard list
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        /**
         * Observer to get LiveData from the db
         * Whenever a new search is made, this list will automatically update
         */
        viewModel.allWeatherData.observe(viewLifecycleOwner) { list ->
            historyAdapter.submitList(list)
        }
    }

    /**
     * Nullify the fragments reference to avoid memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
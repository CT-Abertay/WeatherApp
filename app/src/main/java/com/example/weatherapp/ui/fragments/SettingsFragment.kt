package com.example.weatherapp.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentSettingsBinding

/**
 * Fragment responsible for managing user preferences
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    // View binding
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        // Accessing SharedPreferences to gather user settings
        // MODE_PRIVATE ensures only this app can read/write
        val sharedPref = requireContext().getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)

        // Load user settings, defaulting to metric if first launch
        val currentUnit = sharedPref.getString("unit_type", "metric")

        // Sync radio buttons with saved values
        if (currentUnit == "imperial") {
            binding.rbImperial.isChecked = true
        } else {
            binding.rbMetric.isChecked = true
        }

        /**
         * Listener for the radio buttons
         *
         * @param checkedId ID of the button selected
         */
        binding.rgUnits.setOnCheckedChangeListener { _, checkedId ->
            // Initialize editor to allow preference file changes
            val editor = sharedPref.edit()

            if (checkedId == R.id.rbMetric) {
                editor.putString("unit_type", "metric")
            } else {
                editor.putString("unit_type", "imperial")
            }
            editor.apply()

            // Provide visual feedback to user
            Toast.makeText(requireContext(), "Units updated!", Toast.LENGTH_SHORT).show()
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
package com.example.weatherapp.ui


import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.ui.fragments.HomeFragment
import com.example.weatherapp.ui.fragments.HistoryFragment
import com.example.weatherapp.ui.fragments.SettingsFragment
import com.example.weatherapp.utils.NetworkManager

/**
 * Entry point of the application
 * Acts as a container for all fragments and manages the nav bar
 */
class MainActivity : AppCompatActivity() {

    // View binding
    private lateinit var binding: ActivityMainBinding

    // Utility class to monitor network connectivity
    private lateinit var networkManager: NetworkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate XML
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Extend UI behind nav bar
        enableEdgeToEdge()

        // Initialize network manager
        networkManager = NetworkManager(this)
        networkManager.observeNetwork(object : NetworkManager.NetworkListener {
            override fun onNetworkAvailable() {
                // runOnUiThread ensures popup displays correctly
                runOnUiThread { Toast.makeText(this@MainActivity, "Online", Toast.LENGTH_SHORT).show() }
            }

            override fun onNetworkLost() {
                runOnUiThread { Toast.makeText(this@MainActivity, "Offline", Toast.LENGTH_SHORT).show() }
            }
        })

        /**
         * Listener to check size of systemBars and add an equal amount of padding
         * To ensure fragment container isn't hidden
         */
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Check savedInstanceState to avoid overlapping fragments on screen rotation
        // Also sets the default fragment for app launch
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        /**
         * Listener to handle nav bar clicks
         */
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                else -> false
            }
            true
        }
    }

    /**
     * Tells the network manager to stop observing when the activity is destroyed
     * Preventing battery and memory waste
     */
    override fun onDestroy() {
        super.onDestroy()
        networkManager.stopObserving()
    }

    /**
     * Function to swap fragments in the fragment container.
     *
     * @param fragment  The new fragment to display
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
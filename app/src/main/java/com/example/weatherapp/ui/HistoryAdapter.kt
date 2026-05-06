package com.example.weatherapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.data.local.WeatherEntity

/**
 * Adapter for the History RecyclerView
 */
class HistoryAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<WeatherEntity, HistoryAdapter.ViewHolder>(DiffCallback) {

    /**
     * ViewHolder describes an item view and metadata about its place within the RecyclerView
     * It caches references to the views so findViewById() doesn't need to be called repeatedly
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCity: TextView = view.findViewById(R.id.tvHistoryCity)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvTemp: TextView = view.findViewById(R.id.tvHistoryTemp)
    }

    /**
     * Function called when RecyclerView needs a new ViewHolder to represent an item
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the custom XML layout for a single row
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    /**
     * Function to display the data at a specified position.
     * Updates the contents of the ViewHolder to reflect the item
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        // Connect entity fields to UI components
        holder.tvCity.text = "${item.cityName}, ${item.country}"
        holder.tvDate.text = item.timestamp
        holder.tvTemp.text = "${item.temperature.toInt()}°"

        // Listener to handle item click
        holder.itemView.setOnClickListener {
            // Pass the formatted query back to the Fragment
            onItemClick("${item.cityName},${item.country}")
        }
    }

    /**
     * Utility class that calculates the difference between 2 lists.
     * Used to animate changes instead of refreshing the whole list
     */
    companion object DiffCallback : DiffUtil.ItemCallback<WeatherEntity>() {
        // Checks if its the same item
        override fun areItemsTheSame(oldItem: WeatherEntity, newItem: WeatherEntity) = oldItem.id == newItem.id

        // Checks if the items contents have changed
        override fun areContentsTheSame(oldItem: WeatherEntity, newItem: WeatherEntity) = oldItem == newItem
    }
}
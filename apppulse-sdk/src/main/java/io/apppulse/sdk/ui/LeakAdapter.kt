package io.apppulse.sdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.memory.model.LeakCandidate
import java.text.DateFormat
import java.util.Date

/**
 * Renders activity and fragment leak candidates.
 */
class LeakAdapter : ListAdapter<LeakCandidate, LeakAdapter.LeakHolder>(DIFF) {

    /**
     * Inflates one leak row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeakHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leak_candidate, parent, false)
        return LeakHolder(view)
    }

    /**
     * Binds the candidate at [position].
     */
    override fun onBindViewHolder(holder: LeakHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Row for one [LeakCandidate].
     */
    class LeakHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.bind(R.id.leakName)
        private val type: TextView = itemView.bind(R.id.leakType)
        private val times: TextView = itemView.bind(R.id.leakTimes)

        /**
         * Fills the row from [candidate].
         */
        fun bind(candidate: LeakCandidate) {
            name.text = candidate.componentName
            type.text = candidate.componentType
            times.text = itemView.context.getString(
                R.string.memory_leak_times,
                formatWhen(candidate.destroyedTimestamp),
                formatWhen(candidate.detectedTimestamp)
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LeakCandidate>() {
            override fun areItemsTheSame(oldItem: LeakCandidate, newItem: LeakCandidate): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LeakCandidate, newItem: LeakCandidate): Boolean {
                return oldItem == newItem
            }
        }

        fun formatWhen(epochMillis: Long): String {
            return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(epochMillis))
        }
    }
}

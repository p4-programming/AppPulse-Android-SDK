package io.apppulse.sdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.memory.model.MemoryMetric
import java.text.DateFormat
import java.util.Date

/**
 * Renders heap samples for the memory timeline.
 */
class MemoryAdapter : ListAdapter<MemoryMetric, MemoryAdapter.MemoryHolder>(DIFF) {

    /**
     * Inflates one timeline row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memory_metric, parent, false)
        return MemoryHolder(view)
    }

    /**
     * Binds the sample at [position].
     */
    override fun onBindViewHolder(holder: MemoryHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Row for one [MemoryMetric].
     */
    class MemoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val used: TextView = itemView.bind(R.id.memoryUsed)
        private val whenText: TextView = itemView.bind(R.id.memoryWhen)
        private val detail: TextView = itemView.bind(R.id.memoryDetail)

        /**
         * Fills the row from [metric].
         */
        fun bind(metric: MemoryMetric) {
            val context = itemView.context
            used.text = context.getString(R.string.memory_megabytes, metric.usedMemoryMb)
            whenText.text = formatWhen(metric.timestamp)
            detail.text = context.getString(
                R.string.memory_heap_detail,
                metric.freeMemoryMb,
                metric.totalMemoryMb,
                metric.maxMemoryMb
            ) + "\n" + context.getString(R.string.memory_native, metric.nativeMemoryMb)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<MemoryMetric>() {
            override fun areItemsTheSame(oldItem: MemoryMetric, newItem: MemoryMetric): Boolean {
                return oldItem.id == newItem.id && oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: MemoryMetric, newItem: MemoryMetric): Boolean {
                return oldItem == newItem
            }
        }

        fun formatWhen(epochMillis: Long): String {
            return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(epochMillis))
        }
    }
}

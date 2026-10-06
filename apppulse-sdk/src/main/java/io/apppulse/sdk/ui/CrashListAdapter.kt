package io.apppulse.sdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.crash.CrashEntity

/**
 * Renders stored crashes in the debug dashboard list.
 *
 * Each row shows the exception type, message, and capture time. Tapping a row
 * calls [onCrashClick].
 *
 * @param onCrashClick Invoked with the crash the user selected.
 */
class CrashListAdapter(
    private val onCrashClick: (CrashEntity) -> Unit
) : ListAdapter<CrashEntity, CrashListAdapter.CrashViewHolder>(CrashDiff) {

    /**
     * Inflates a crash row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrashViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crash, parent, false)
        return CrashViewHolder(view, onCrashClick)
    }

    /**
     * Binds the crash at [position].
     */
    override fun onBindViewHolder(holder: CrashViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Row that displays one stored crash.
     *
     * @param itemView Inflated `item_crash` layout.
     * @param onCrashClick Selection callback shared by every row.
     */
    class CrashViewHolder(
        itemView: View,
        private val onCrashClick: (CrashEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val exceptionType: TextView = itemView.bind(R.id.crashExceptionType)
        private val message: TextView = itemView.bind(R.id.crashMessage)
        private val timestamp: TextView = itemView.bind(R.id.crashTimestamp)
        private var crash: CrashEntity? = null

        init {
            itemView.setOnClickListener {
                crash?.let(onCrashClick)
            }
        }

        /**
         * Fills the row and keeps [item] for the click listener.
         *
         * @param item Crash represented by this row.
         */
        fun bind(item: CrashEntity) {
            crash = item
            exceptionType.text = item.simpleExceptionType()
            message.text = item.displayMessage()
            timestamp.text = item.formattedTimestamp()
            itemView.contentDescription = itemView.context.getString(
                R.string.crash_list_row_description,
                item.simpleExceptionType(),
                item.displayMessage(),
                item.formattedTimestamp()
            )
        }
    }

}

private object CrashDiff : DiffUtil.ItemCallback<CrashEntity>() {
    override fun areItemsTheSame(oldItem: CrashEntity, newItem: CrashEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CrashEntity, newItem: CrashEntity): Boolean {
        return oldItem == newItem
    }
}

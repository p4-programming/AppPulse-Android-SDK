package io.apppulse.sdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.health.model.HealthLevel
import io.apppulse.sdk.health.model.HealthScore
import java.util.Locale

/**
 * Renders crash, performance, memory, and security scores.
 */
class HealthSummaryAdapter(
    private val onCategoryClick: (HealthScore) -> Unit
) : ListAdapter<HealthScore, HealthSummaryAdapter.ScoreHolder>(DIFF) {

    /**
     * Inflates one category row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_health_score, parent, false)
        return ScoreHolder(view, onCategoryClick)
    }

    /**
     * Binds the score at [position].
     */
    override fun onBindViewHolder(holder: ScoreHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Row for one [HealthScore].
     */
    class ScoreHolder(
        itemView: View,
        private val onCategoryClick: (HealthScore) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val category: TextView = itemView.bind(R.id.healthCategory)
        private val score: TextView = itemView.bind(R.id.healthCategoryScore)
        private val detail: TextView = itemView.bind(R.id.healthCategoryDetail)

        /**
         * Fills the row from [item].
         */
        fun bind(item: HealthScore) {
            val weight = (item.weight * 100).toInt()
            category.text = itemView.context.getString(
                R.string.health_category_weight,
                label(item.category.name),
                weight
            )
            score.text = itemView.context.getString(
                R.string.health_category_score,
                item.score,
                item.level.name
            )
            score.setTextColor(ContextCompat.getColor(itemView.context, colorFor(item.level)))
            detail.text = item.detail
            itemView.setOnClickListener { onCategoryClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<HealthScore>() {
            override fun areItemsTheSame(oldItem: HealthScore, newItem: HealthScore): Boolean {
                return oldItem.category == newItem.category
            }

            override fun areContentsTheSame(oldItem: HealthScore, newItem: HealthScore): Boolean {
                return oldItem == newItem
            }
        }

        fun label(name: String): String {
            val lower = name.lowercase(Locale.US)
            return lower.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }

        fun colorFor(level: HealthLevel): Int {
            return when (level) {
                HealthLevel.EXCELLENT, HealthLevel.GOOD -> R.color.purple_500
                HealthLevel.WARNING -> R.color.memory_warning
                HealthLevel.CRITICAL -> R.color.memory_critical
            }
        }
    }
}

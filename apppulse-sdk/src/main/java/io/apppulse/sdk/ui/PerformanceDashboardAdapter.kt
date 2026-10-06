package io.apppulse.sdk.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.performance.PerformanceRow
import io.apppulse.sdk.performance.model.PerformanceReport

/**
 * Renders the performance dashboard as a summary card, section headers, and metric rows.
 */
class PerformanceDashboardAdapter :
    ListAdapter<PerformanceRow, RecyclerView.ViewHolder>(RowDiff) {

    /**
     * Inflates the layout for [viewType].
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PerformanceRow.Summary -> VIEW_SUMMARY
            is PerformanceRow.Header -> VIEW_HEADER
            is PerformanceRow.Metric -> VIEW_METRIC
        }
    }

    /**
     * Creates a holder for [viewType].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_SUMMARY -> SummaryHolder(inflater.inflate(R.layout.item_performance_summary, parent, false))
            VIEW_HEADER -> HeaderHolder(inflater.inflate(R.layout.item_performance_header, parent, false))
            else -> MetricHolder(inflater.inflate(R.layout.item_performance_metric, parent, false))
        }
    }

    /**
     * Binds the row at [position].
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is PerformanceRow.Summary -> (holder as SummaryHolder).bind(row.report)
            is PerformanceRow.Header -> (holder as HeaderHolder).bind(row.title)
            is PerformanceRow.Metric -> (holder as MetricHolder).bind(row)
        }
    }

    private class SummaryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val startup: TextView = itemView.bind(R.id.performanceAverageStartup)
        private val fps: TextView = itemView.bind(R.id.performanceAverageFps)
        private val screen: TextView = itemView.bind(R.id.performanceSlowestScreen)
        private val api: TextView = itemView.bind(R.id.performanceSlowestApi)
        private val anr: TextView = itemView.bind(R.id.performanceAnrCount)

        /**
         * Fills the summary card from [report].
         */
        fun bind(report: PerformanceReport) {
            val context = itemView.context
            val unavailable = context.getString(R.string.performance_unavailable)
            startup.text = report.averageStartupMs?.let { millis ->
                context.getString(R.string.performance_milliseconds, millis)
            } ?: unavailable
            fps.text = report.averageFps?.let { value ->
                context.getString(R.string.performance_fps_value, value)
            } ?: unavailable
            screen.text = if (report.slowestScreenName != null && report.slowestScreenMs != null) {
                context.getString(
                    R.string.performance_summary_pair,
                    report.slowestScreenName,
                    context.getString(R.string.performance_milliseconds, report.slowestScreenMs)
                )
            } else {
                unavailable
            }
            api.text = if (report.slowestApiLabel != null && report.slowestApiMs != null) {
                context.getString(
                    R.string.performance_summary_pair,
                    report.slowestApiLabel,
                    context.getString(R.string.performance_milliseconds, report.slowestApiMs)
                )
            } else {
                unavailable
            }
            anr.text = report.anrCount.toString()
        }
    }

    private class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.bind(R.id.performanceSectionTitle)

        /**
         * Shows [text] as a section title.
         */
        fun bind(text: String) {
            title.text = text
        }
    }

    private class MetricHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.bind(R.id.performanceMetricTitle)
        private val subtitle: TextView = itemView.bind(R.id.performanceMetricSubtitle)
        private val value: TextView = itemView.bind(R.id.performanceMetricValue)
        private val flag: TextView = itemView.bind(R.id.performanceMetricFlag)

        /**
         * Shows one sample.
         */
        fun bind(row: PerformanceRow.Metric) {
            title.text = row.title
            subtitle.text = row.subtitle
            subtitle.visibility = if (row.subtitle.isBlank()) View.GONE else View.VISIBLE
            value.text = row.value
            value.visibility = if (row.value.isBlank()) View.GONE else View.VISIBLE
            if (row.flag == null) {
                flag.visibility = View.GONE
            } else {
                flag.visibility = View.VISIBLE
                flag.text = row.flag
            }
            itemView.contentDescription = itemView.context.getString(
                R.string.performance_row_description,
                row.title,
                row.subtitle,
                row.value
            )
        }
    }

    private companion object {
        const val VIEW_SUMMARY = 0
        const val VIEW_HEADER = 1
        const val VIEW_METRIC = 2
    }
}

private object RowDiff : DiffUtil.ItemCallback<PerformanceRow>() {
    override fun areItemsTheSame(oldItem: PerformanceRow, newItem: PerformanceRow): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: PerformanceRow, newItem: PerformanceRow): Boolean {
        return oldItem == newItem
    }
}

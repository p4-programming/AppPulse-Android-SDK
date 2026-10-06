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
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity

/**
 * Renders security findings, critical first.
 */
class SecurityIssueAdapter : ListAdapter<SecurityIssue, SecurityIssueAdapter.IssueHolder>(DIFF) {

    /**
     * Inflates one finding row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_security_issue, parent, false)
        return IssueHolder(view)
    }

    /**
     * Binds the finding at [position].
     */
    override fun onBindViewHolder(holder: IssueHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Row for one [SecurityIssue].
     */
    class IssueHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val severity: TextView = itemView.bind(R.id.securitySeverity)
        private val title: TextView = itemView.bind(R.id.securityTitle)
        private val target: TextView = itemView.bind(R.id.securityTarget)
        private val description: TextView = itemView.bind(R.id.securityDescription)

        /**
         * Fills the row from [issue].
         */
        fun bind(issue: SecurityIssue) {
            val color = when (issue.severity) {
                SecuritySeverity.CRITICAL -> R.color.memory_critical
                SecuritySeverity.HIGH -> R.color.security_high
                SecuritySeverity.MEDIUM -> R.color.memory_warning
                SecuritySeverity.LOW -> R.color.teal_700
            }
            severity.setTextColor(ContextCompat.getColor(itemView.context, color))
            severity.text = itemView.context.getString(
                R.string.security_issue_meta,
                issue.severity.name,
                issue.category
            )
            title.text = issue.title
            target.text = issue.target
            description.text = issue.description
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<SecurityIssue>() {
            override fun areItemsTheSame(oldItem: SecurityIssue, newItem: SecurityIssue): Boolean {
                return if (oldItem.id != 0L && newItem.id != 0L) {
                    oldItem.id == newItem.id
                } else {
                    oldItem.code == newItem.code && oldItem.target == newItem.target
                }
            }

            override fun areContentsTheSame(oldItem: SecurityIssue, newItem: SecurityIssue): Boolean {
                return oldItem == newItem
            }
        }
    }
}

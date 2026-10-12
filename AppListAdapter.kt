package com.smoothy.kidguard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smoothy.kidguard.R
import com.smoothy.kidguard.model.AppItem

class AppListAdapter(
    private var allApps: List<AppItem>,
    private val onLockToggle: (AppItem, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private var filteredApps: List<AppItem> = allApps

    fun updateData(newApps: List<AppItem>) {
        this.allApps = newApps
        this.filteredApps = newApps
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredApps = if (query.trim().isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = filteredApps.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvPackage: TextView = itemView.findViewById(R.id.tvPackageName)
        private val ivLockStatus: ImageView = itemView.findViewById(R.id.ivLockStatus)

        fun bind(app: AppItem) {
            tvName.text = app.appName
            tvPackage.text = app.packageName
            ivIcon.setImageDrawable(app.icon)

            updateLockIcon(app.isLocked)

            ivLockStatus.setOnClickListener {
                app.isLocked = !app.isLocked
                updateLockIcon(app.isLocked)
                onLockToggle(app, app.isLocked)
            }
        }

        private fun updateLockIcon(isLocked: Boolean) {
            val context = itemView.context
            if (isLocked) {
                ivLockStatus.setImageResource(R.drawable.ic_lock)
                ivLockStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_locked))
            } else {
                ivLockStatus.setImageResource(R.drawable.ic_unlock)
                ivLockStatus.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary_dark))
            }
        }
    }
}

package com.familyguard.screentime.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppInfo

/**
 * Selection state lives in [selectedPackages], owned by the caller, NOT in
 * the AppInfo items themselves. This way, filtering the visible list (e.g.
 * via search) never loses a selection made on an item that's since been
 * scrolled or filtered out of view.
 */
class AppListAdapter(
    private val apps: List<AppInfo>,
    private val selectedPackages: MutableSet<String>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val icon = itemView.findViewById<ImageView>(R.id.imgIcon)
        private val name = itemView.findViewById<TextView>(R.id.textAppName)
        private val checkbox = itemView.findViewById<CheckBox>(R.id.checkSelected)

        fun bind(app: AppInfo) {
            icon.setImageDrawable(app.icon)
            name.text = app.label

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = app.packageName in selectedPackages
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedPackages.add(app.packageName) else selectedPackages.remove(app.packageName)
            }

            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
        }
    }
}

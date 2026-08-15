package com.familyguard.screentime.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppInfo

class AppListAdapter(
    private val apps: MutableList<AppInfo>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    fun selectedPackages(): Set<String> =
        apps.filter { it.isSelected }.map { it.packageName }.toSet()

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

            // Avoid firing the listener while RecyclerView recycles views.
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = app.isSelected
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                app.isSelected = isChecked
            }

            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
        }
    }
}

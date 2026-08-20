package com.familyguard.screentime.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.Schedule

class ScheduleAdapter(
    private var schedules: List<Schedule>,
    private val onEdit: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit,
    private val onToggleEnabled: (Schedule, Boolean) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    fun updateData(newSchedules: List<Schedule>) {
        schedules = newSchedules
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(schedules[position])
    }

    override fun getItemCount(): Int = schedules.size

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName = itemView.findViewById<TextView>(R.id.textScheduleName)
        private val textTime = itemView.findViewById<TextView>(R.id.textScheduleTime)
        private val textDays = itemView.findViewById<TextView>(R.id.textScheduleDays)
        private val textAppCount = itemView.findViewById<TextView>(R.id.textScheduleAppCount)
        private val switchEnabled = itemView.findViewById<Switch>(R.id.switchScheduleEnabled)
        private val btnEdit = itemView.findViewById<Button>(R.id.btnEditSchedule)
        private val btnDelete = itemView.findViewById<Button>(R.id.btnDeleteSchedule)

        fun bind(schedule: Schedule) {
            textName.text = schedule.name
            textTime.text = schedule.timeRangeLabel()
            textDays.text = schedule.daysLabel()
            textAppCount.text = "${schedule.restrictedPackages.size} app(s) restricted"

            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = schedule.enabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnabled(schedule, isChecked)
            }

            btnEdit.setOnClickListener { onEdit(schedule) }
            btnDelete.setOnClickListener { onDelete(schedule) }
        }
    }
}

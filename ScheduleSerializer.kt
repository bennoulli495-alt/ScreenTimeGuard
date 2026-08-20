package com.familyguard.screentime.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts Schedule <-> JSON using org.json, which ships with the Android
 * SDK, so no new dependency (Gson/Moshi/Room) is introduced.
 */
object ScheduleSerializer {

    fun toJson(schedule: Schedule): JSONObject = JSONObject().apply {
        put("id", schedule.id)
        put("name", schedule.name)
        put("startHour", schedule.startHour)
        put("startMinute", schedule.startMinute)
        put("endHour", schedule.endHour)
        put("endMinute", schedule.endMinute)
        put("repeatDays", JSONArray(schedule.repeatDays.toList()))
        put("restrictedPackages", JSONArray(schedule.restrictedPackages.toList()))
        put("enabled", schedule.enabled)
    }

    fun fromJson(obj: JSONObject): Schedule {
        val repeatDaysArray = obj.optJSONArray("repeatDays") ?: JSONArray()
        val repeatDays = (0 until repeatDaysArray.length()).map { repeatDaysArray.getInt(it) }.toSet()

        val packagesArray = obj.optJSONArray("restrictedPackages") ?: JSONArray()
        val packages = (0 until packagesArray.length()).map { packagesArray.getString(it) }.toSet()

        return Schedule(
            id = obj.getString("id"),
            name = obj.getString("name"),
            startHour = obj.getInt("startHour"),
            startMinute = obj.getInt("startMinute"),
            endHour = obj.getInt("endHour"),
            endMinute = obj.getInt("endMinute"),
            repeatDays = repeatDays,
            restrictedPackages = packages,
            enabled = obj.optBoolean("enabled", true)
        )
    }

    fun listToJson(schedules: List<Schedule>): String {
        val array = JSONArray()
        schedules.forEach { array.put(toJson(it)) }
        return array.toString()
    }

    fun listFromJson(json: String?): MutableList<Schedule> {
        if (json.isNullOrBlank()) return mutableListOf()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { fromJson(array.getJSONObject(it)) }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }
}

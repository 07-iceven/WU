package com.iceven.wu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ScheduledNotification(
    val id: Int,
    val timeInMillis: Long,
    val message: String
)

class NotificationStorage(context: Context) {
    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private val key = "scheduled_notifications"

    fun getNotifications(): List<ScheduledNotification> {
        val json = prefs.getString(key, "[]")
        val list = mutableListOf<ScheduledNotification>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ScheduledNotification(
                        id = obj.getInt("id"),
                        timeInMillis = obj.getLong("timeInMillis"),
                        message = obj.getString("message")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedBy { it.timeInMillis }
    }

    fun saveNotification(notification: ScheduledNotification) {
        val list = getNotifications().toMutableList()
        list.add(notification)
        saveList(list)
    }

    fun removeNotification(id: Int) {
        val list = getNotifications().toMutableList()
        list.removeAll { it.id == id }
        saveList(list)
    }

    private fun saveList(list: List<ScheduledNotification>) {
        val jsonArray = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("timeInMillis", it.timeInMillis)
            obj.put("message", it.message)
            jsonArray.put(obj)
        }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }
}
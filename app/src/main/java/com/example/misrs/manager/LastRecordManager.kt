package com.example.misrs.manager
import android.content.Context
import android.content.SharedPreferences
import com.example.misrs.data.entities.StatusRecord
import com.google.gson.Gson

class LastRecordManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("last_record_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLastRecord(record: StatusRecord) {
        val recordJson = gson.toJson(record)
        sharedPreferences.edit().putString("last_record", recordJson).apply()
    }

    fun getLastRecord(): StatusRecord? {
        val recordJson = sharedPreferences.getString("last_record", null)
        return if (recordJson != null) {
            gson.fromJson(recordJson, StatusRecord::class.java)
        } else {
            null
        }
    }
}

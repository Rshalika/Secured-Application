package com.strawhat.securedapplication.sttings

import android.content.Context
import com.google.gson.Gson
import java.util.*

class SettingsRepository(private val context: Context, private val gson: Gson) {

    fun savePassword(password: String): SettingsModel {
        val previousModel = readSettingsModel()
        val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            previousModel.password = password
            previousModel.lastAttemptTime = null
            previousModel.failedAttempts = 0
            putString(SETTINGS_PREFERENCES_KEY, gson.toJson(previousModel))
            commit()
        }
        return previousModel
    }

    fun removePassword(): SettingsModel {
        val previousModel = readSettingsModel()
        val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            previousModel.password = null
            previousModel.lastAttemptTime = null
            previousModel.failedAttempts = 0
            putString(SETTINGS_PREFERENCES_KEY, gson.toJson(previousModel))
            commit()
        }
        return previousModel
    }

    fun saveAttempt() {
        val previousModel = readSettingsModel()
        val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            previousModel.lastAttemptTime = Date()
            putString(SETTINGS_PREFERENCES_KEY, gson.toJson(previousModel))
            commit()
        }
    }

    fun attempt(password: String): Pair<Boolean, SettingsModel> {
        val previousModel = readSettingsModel()
        if (previousModel.password != null && password == previousModel.password) {
            val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                previousModel.lastAttemptTime = null
                previousModel.failedAttempts = 0
                putString(SETTINGS_PREFERENCES_KEY, gson.toJson(previousModel))
                commit()
            }
            return Pair(true, previousModel)
        } else {
            val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                previousModel.lastAttemptTime = Date()
                previousModel.failedAttempts = previousModel.failedAttempts + 1
                putString(SETTINGS_PREFERENCES_KEY, gson.toJson(previousModel))
                commit()
            }
            return Pair(false, previousModel)
        }
    }

    fun readSettingsModel(): SettingsModel {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val savedState = sharedPreferences.getString(SETTINGS_PREFERENCES_KEY, null)
        return if (savedState != null) {
            gson.fromJson(savedState, SettingsModel::class.java)
        } else {
            SettingsModel()
        }
    }

}

private const val SETTINGS_PREFERENCES_NAME = "com.strawhat.securedapplication.SETTINGS_PREFERENCES_NAME"
private const val SETTINGS_PREFERENCES_KEY = "com.strawhat.securedapplication.SETTINGS_PREFERENCES_KEY"
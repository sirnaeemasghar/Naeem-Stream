package com.naeem.streamnest

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val theme: String = "Dark",
    val wifiOnly: Boolean = false,
    val defaultHeight: Int = 720,
    val downloadFolder: String = "NaeemVideos",
    val customTreeUri: String = ""
)
class Preferences(context: Context) {
    private val file = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(UserPreferences(
        file.getString("theme", "Dark") ?: "Dark",
        file.getBoolean("wifi", false),
        file.getInt("height", 720),
        file.getString("folder", "NaeemVideos") ?: "NaeemVideos",
        file.getString("tree_uri", "") ?: ""
    ))
    val state = mutable.asStateFlow()
    fun update(value: UserPreferences) {
        file.edit()
            .putString("theme", value.theme)
            .putBoolean("wifi", value.wifiOnly)
            .putInt("height", value.defaultHeight)
            .putString("folder", value.downloadFolder)
            .putString("tree_uri", value.customTreeUri)
            .apply()
        mutable.value = value
    }
}

package com.example.prototyx.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson

data class InboxEntry(val id: Long, val title: String, val body: String, val read: Boolean = false)

class AppPreferences(context: Context) {
    private val disk = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val exports = java.io.File(context.cacheDir,"exports")
    var darkTheme by mutableStateOf(disk.getBoolean("dark", false)); private set
    var detailed by mutableStateOf(disk.getBoolean("detailed", true)); private set
    var appLock by mutableStateOf(disk.getBoolean("lock", false)); private set
    var reminders by mutableStateOf(disk.getBoolean("reminders", false)); private set
    var analysisCurrency by mutableStateOf(disk.getString("currency","USD") ?: "USD"); private set
    var benchmark by mutableStateOf(disk.getString("benchmark","") ?: ""); private set
    var inbox by mutableStateOf(loadInbox()); private set
    fun setDark(value: Boolean) { darkTheme=value; disk.edit().putBoolean("dark",value).apply() }
    fun updateDetailed(value: Boolean) { detailed=value; disk.edit().putBoolean("detailed",value).apply() }
    fun setLock(value: Boolean) { appLock=value; disk.edit().putBoolean("lock",value).apply() }
    fun updateReminders(value: Boolean) { reminders=value; disk.edit().putBoolean("reminders",value).apply() }
    fun updateCurrency(value:String) { require(value in listOf("USD","INR"));analysisCurrency=value;disk.edit().putString("currency",value).apply() }
    fun updateBenchmark(value:String) { require(value in listOf("","^GSPC","^NSEI"));benchmark=value;disk.edit().putString("benchmark",value).apply() }
    private fun loadInbox(): List<InboxEntry> = runCatching {
        Gson().fromJson(disk.getString("inbox","[]"),Array<InboxEntry>::class.java).toList()
    }.getOrDefault(emptyList())
    @Synchronized fun addNotice(title: String, body: String): InboxEntry {
        val entry=InboxEntry(System.currentTimeMillis().coerceAtLeast((inbox.firstOrNull()?.id ?: 0)+1),title,body)
        inbox=(listOf(entry)+inbox).take(50); saveInbox(); return entry
    }
    @Synchronized fun markAllRead() { inbox=inbox.map { it.copy(read=true) }; saveInbox() }
    @Synchronized fun markRead(id:Long) { inbox=inbox.map { if(it.id==id) it.copy(read=true) else it };saveInbox() }
    @Synchronized fun clearInbox() { inbox=emptyList();saveInbox() }
    @Synchronized fun clearAccountData() { inbox=emptyList(); updateReminders(false); setLock(false); saveInbox();exports.listFiles()?.forEach { it.delete() } }
    @Synchronized fun removeNotice(id:Long) { inbox=inbox.filterNot { it.id==id }; saveInbox() }
    private fun saveInbox() { disk.edit().putString("inbox",Gson().toJson(inbox)).apply() }
}

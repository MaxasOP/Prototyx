package com.example.prototyx.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.example.prototyx.PrototyxApp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.UserProfile
import com.example.prototyx.data.network.RetrofitInstance
import com.example.prototyx.data.security.AuthManager
import com.example.prototyx.data.security.BiometricAuthManager
import com.example.prototyx.notifications.AppNotifications
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.*
import java.io.File

@Composable
fun AccountScreen(repository:DataRepository,authManager:AuthManager,onSignedOut:()->Unit,modifier:Modifier=Modifier,api:com.example.prototyx.data.network.ApiService=RetrofitInstance.apiService) {
    val context=LocalContext.current
    val app=context.applicationContext as PrototyxApp
    val prefs=app.preferences
    val uri=LocalUriHandler.current
    val scope=rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    suspend fun finishSession() = withContext(NonCancellable) {
        File(context.cacheDir,"exports").listFiles()?.forEach { it.delete() }
        repository.logout(); AppNotifications.clear(context); prefs.clearAccountData(); authManager.clearToken(); onSignedOut()
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        val enabled=allowed && AppNotifications.allowed(context)
        prefs.updateReminders(enabled); AppNotifications.schedule(context,enabled)
        message=if(enabled) "Daily review reminders enabled. Android may delay delivery to save battery." else "Notifications are disabled. You can enable them in Android settings."
    }
    LaunchedEffect(Unit) {
        try { profile=api.getAccount(); name=profile?.name.orEmpty() }
        catch(e:CancellationException) { throw e }
        catch(e:Exception) { message=e.message }
    }
    if(dialog!=null) AlertDialog(
        onDismissRequest={ if(!busy) { dialog=null; password=""; newPassword=""; repeatedPassword="" } },
        title={ Text(when(dialog) { "all"->"Sign out all devices?"; "password"->"Change password"; "delete"->"Delete account permanently?"; "localout"->"Sign out locally?"; else->"Sign out?" }) },
        text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(when(dialog) { "delete"->"Your profile, cloud allocations and security records will be deleted. This cannot be undone. Infrastructure backups may persist under provider retention."; "password"->"All sessions will be invalidated. Use at least 8 characters (maximum 72 UTF-8 bytes)."; "localout"->"The server could not be reached, so this session cannot be revoked remotely. Local data and unsynced edits will be removed."; else->"Sync pending changes first. Signing out removes local account data and notification history." })
            if(dialog=="delete" || dialog=="password" || dialog=="all") OutlinedTextField(password,{password=it},label={Text("Current password")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
            if(dialog=="password") {
                OutlinedTextField(newPassword,{newPassword=it},label={Text("New password")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
                OutlinedTextField(repeatedPassword,{repeatedPassword=it},label={Text("Confirm new password")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
            }
        } },
        confirmButton={ TextButton(enabled=!busy,onClick={
            val selected=dialog
            scope.launch {
                busy=true; message=null
                try {
                    when(selected) {
                        "all"->{require(password.isNotBlank()){ "Enter your password." };api.signOutAll(mapOf("password" to password));finishSession()}
                        "delete"->{ require(password.isNotBlank()) { "Enter your password." }; api.deleteAccount(mapOf("password" to password)); finishSession() }
                        "password"->{ require(newPassword==repeatedPassword && newPassword.length>=8 && password.isNotBlank()) { "Check the current password and matching new passwords." }; api.changePassword(mapOf("current_password" to password,"new_password" to newPassword)); finishSession() }
                        "localout"->finishSession()
                        else->{ try { api.revokeSession() } catch(e:CancellationException) { throw e } catch(e:Exception) { dialog="localout"; message=e.message; return@launch }; finishSession() }
                    }
                    dialog=null; password=""; newPassword=""; repeatedPassword=""
                } catch(e:CancellationException) { throw e }
                catch(e:Exception) { message=e.message; dialog=null }
                finally { busy=false }
            }
        }) { Text(if(busy) "Working…" else if(dialog=="delete") "Delete account" else "Continue") } },
        dismissButton={ TextButton(enabled=!busy,onClick={dialog=null;password="";newPassword="";repeatedPassword=""}) {Text("Cancel")} }
    )
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { EditorialHeading("Account & settings") }
        item { message?.let { StatusMessage(it) }; if(busy) LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            SectionHeader("Profile",supportingText=profile?.email ?: "Loading signed-in account…")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(name,{name=it},label={Text("Display name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            TextButton(enabled=!busy && profile!=null,onClick={scope.launch { busy=true; try {profile=api.updateAccount(mapOf("name" to name));message="Profile saved."} catch(e:CancellationException){throw e} catch(e:Exception){message=e.message} finally{busy=false} }}) {Text("Save profile")}
        }
        item {
            HorizontalDivider(); SectionHeader("Security")
            SettingSwitch("Lock when leaving the app","Use biometrics or device credentials. Authentication failures keep the app locked.",prefs.appLock) { enabled ->
                val manager=BiometricAuthManager(context)
                if(!manager.isBiometricAvailable()) message="Set up a screen lock or biometrics in Android settings first."
                else manager.promptBiometricAuth(context as FragmentActivity,title="Confirm app lock setting",onSuccess={prefs.setLock(enabled)},onError={message=it})
            }
            TextButton(onClick={password="";newPassword="";repeatedPassword="";dialog="password"}) {Text("Change password")}
            TextButton(onClick={password="";dialog="all"}) {Text("Sign out all devices")}
        }
        item {
            HorizontalDivider(); SectionHeader("Notifications",supportingText="Scheduled reminders only. No real-time market or price alerts.")
            SettingSwitch("Daily portfolio review","Delivered approximately every 24 hours; timing depends on Android battery policies.",prefs.reminders) { enabled ->
                if(!enabled) {prefs.updateReminders(false);AppNotifications.schedule(context,false)}
                else if(Build.VERSION.SDK_INT>=33 && !AppNotifications.allowed(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else if(AppNotifications.allowed(context)) {prefs.updateReminders(true);AppNotifications.schedule(context,true)}
                else message="Enable Prototyx notifications in Android settings first."
            }
            TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}) {Text("Android notification settings")}
            if(prefs.reminders && !AppNotifications.allowed(context)) Text("Android is blocking notifications. Enable them in system settings to receive reminders.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={if(AppNotifications.allowed(context)) {AppNotifications.post(context,true);message="Test notification sent. Open Notifications to view it."} else message="Allow notifications in Android settings first."}) {Text("Send test notification")}
        }
        item {
            HorizontalDivider(); SectionHeader("Display & research")
            SettingSwitch("Dark appearance","Saved on this device.",prefs.darkTheme,prefs::setDark)
            SettingSwitch("Detailed equity view","Show valuation and technical metrics by default.",prefs.detailed,prefs::updateDetailed)
            Text("Analysis currency",style=MaterialTheme.typography.titleMedium)
            Row { listOf("USD","INR").forEach { value-> TextButton(onClick={prefs.updateCurrency(value)}) {Text(if(prefs.analysisCurrency==value) "✓ $value" else value)} } }
            Text("Risk benchmark",style=MaterialTheme.typography.titleMedium)
            Row { listOf("" to "Auto","^GSPC" to "S&P 500","^NSEI" to "Nifty 50").forEach { (value,label)->TextButton(onClick={prefs.updateBenchmark(value)}) {Text(if(prefs.benchmark==value) "✓ $label" else label)} } }
            Text("Quotes retain native currency. Analytics convert historical prices to your chosen currency. Auto benchmark follows the first selected market. Rerun an analysis after changing these settings.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            HorizontalDivider(); SectionHeader("Your data")
            TextButton(enabled=!busy,onClick={scope.launch {busy=true;try {
                val data=api.exportAccount()
                data.add("devicePreferences",com.google.gson.Gson().toJsonTree(mapOf("darkTheme" to prefs.darkTheme,"detailedView" to prefs.detailed,"reviewReminders" to prefs.reminders,"analysisCurrency" to prefs.analysisCurrency,"benchmark" to prefs.benchmark)))
                data.add("notificationHistory",com.google.gson.Gson().toJsonTree(prefs.inbox))
                val directory=File(context.cacheDir,"exports").apply{mkdirs()}
                val file=File(directory,"prototyx-export.json").apply{writeText(data.toString())}
                val content=FileProvider.getUriForFile(context,context.packageName+".exports",file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,content).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Save or share your export"))
            }catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}finally{busy=false}}}) {Text("Export account and device data")}
            TextButton(onClick={password="";dialog="delete"},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) {Text("Delete account and data")}
        }
        item {
            HorizontalDivider(); SectionHeader("Help & information")
            TextButton(onClick={uri.openUri("https://prototyx.onrender.com/legal/privacy")}) {Text("Privacy & data handling")}
            TextButton(onClick={uri.openUri("https://prototyx.onrender.com/legal/terms")}) {Text("Service terms & research limitations")}
            TextButton(onClick={uri.openUri("https://github.com/MaxasOP/Prototyx/issues")}) {Text("Report an issue (public — do not include personal data)")}
            MetadataLabel("PROTOTYX / VERSION 1.0")
        }
        item { HorizontalDivider(); TextButton(onClick={dialog="signout"},modifier=Modifier.fillMaxWidth()) {Text("Sign out")} }
    }
}

@Composable
private fun SettingSwitch(title:String,description:String,checked:Boolean,onChanged:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=16.dp)) {
            Text(title,style=MaterialTheme.typography.bodyLarge)
            Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked,onChanged)
    }
}

@Composable
fun NotificationsScreen(modifier:Modifier=Modifier) {
    val app=LocalContext.current.applicationContext as PrototyxApp
    val entries=app.preferences.inbox
    var confirmClear by remember {mutableStateOf(false)}
    if(confirmClear) AlertDialog(onDismissRequest={confirmClear=false},title={Text("Clear notification history?")},text={Text("This removes the inbox history from this device. Your portfolio and reminder schedule stay unchanged.")},confirmButton={TextButton(onClick={app.preferences.clearInbox();androidx.core.app.NotificationManagerCompat.from(app).cancelAll();confirmClear=false}){Text("Clear history")}},dismissButton={TextButton(onClick={confirmClear=false}){Text("Cancel")}})
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {EditorialHeading("Notifications"); Row {TextButton(onClick=app.preferences::markAllRead){Text("Mark all read")};TextButton(enabled=entries.isNotEmpty(),onClick={confirmClear=true}){Text("Clear history")}}}
        if(entries.isEmpty()) item {EmptyState("No notifications yet","Enable optional review reminders in Account & settings. Price alerts are not provided.")}
        entries.forEach { entry -> item(key=entry.id) {
            HorizontalDivider()
            Text(entry.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.fillMaxWidth().clickable{app.preferences.markRead(entry.id)}.padding(vertical=8.dp))
            Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.id)),style=MaterialTheme.typography.bodySmall)
            Text(entry.body,style=MaterialTheme.typography.bodyMedium)
            if(!entry.read) MetadataLabel("UNREAD",color=MaterialTheme.colorScheme.primary)
        } }
    }
}

package com.example.prototyx.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.prototyx.MainActivity
import com.example.prototyx.PrototyxApp
import java.util.concurrent.TimeUnit

object AppNotifications {
    const val CHANNEL="portfolio_review"
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT>=26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"Portfolio review reminders",NotificationManager.IMPORTANCE_DEFAULT).apply {
                description="Optional daily reminders to review your portfolio; no real-time price alerts."
                lockscreenVisibility=Notification.VISIBILITY_PRIVATE
            })
    }
    fun allowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun schedule(context: Context, enabled: Boolean) {
        val manager=WorkManager.getInstance(context)
        if (!enabled) { manager.cancelUniqueWork("portfolio-review"); return }
        val request=PeriodicWorkRequestBuilder<ReviewReminderWorker>(24,TimeUnit.HOURS).setInitialDelay(24,TimeUnit.HOURS).build()
        manager.enqueueUniquePeriodicWork("portfolio-review",ExistingPeriodicWorkPolicy.KEEP,request)
    }
    fun post(context: Context, test: Boolean=false) {
        val app=context.applicationContext as PrototyxApp
        if (app.authManager.getToken()==null || !allowed(context)) return
        val title=if(test) "Test notification" else "Time for a portfolio review"
        val body=if(test) "Notifications are working on this device." else "Review your allocation and read the latest company headlines. This is a reminder, not an investment signal."
        val item=app.preferences.addNotice(title,body)
        val intent=Intent(context,MainActivity::class.java).putExtra("open_notifications",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending=PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(com.example.prototyx.R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context,CHANNEL).setSmallIcon(com.example.prototyx.R.drawable.ic_notification).setContentTitle("Prototyx reminder").setContentText("Open Prototyx to view.").build()).build()
        try { NotificationManagerCompat.from(context).notify(item.id.toInt(),notification) } catch (_:SecurityException) {}
    }
    fun clear(context: Context) { schedule(context,false); NotificationManagerCompat.from(context).cancelAll() }
}

class ReviewReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val app=applicationContext as PrototyxApp
        if (app.authManager.getToken()!=null && app.preferences.reminders) AppNotifications.post(applicationContext)
        return Result.success()
    }
}

package com.naeem.streamnest

import android.app.*
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class StreamNestApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var preferences: Preferences; private set
    lateinit var engine: YtDlpEngine; private set
    lateinit var downloads: DownloadRepository; private set
    override fun onCreate() {
        super.onCreate()
        preferences = Preferences(this)
        engine = YtDlpEngine(this)
        downloads = DownloadRepository(this, engine, preferences, applicationScope)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(TransferNotifications.CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Download progress and controls"
            }
        )
    }
}
val Context.streamApp: StreamNestApp get() = applicationContext as StreamNestApp

object TransferNotifications {
    const val CHANNEL = "downloads"
    const val ID = 2401
    fun create(context: Context, job: DownloadJob? = null): Notification {
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).putExtra("show_downloads", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getBroadcast(context, 1, Intent(context, CancelReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val working = job == null || job.status.active || job.status == JobStatus.QUEUED
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(job?.media?.title ?: "Naeem Stream")
            .setContentText(job?.let { "${it.status.label} · ${it.detail}" } ?: "Preparing your queue")
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(working)
            .setProgress(if (working) 100 else 0, ((job?.progress ?: 0f) * 100).toInt(), working && (job == null || job.status != JobStatus.DOWNLOADING))
            .apply { if (working) addAction(0, "Cancel queue", cancel) }
            .build()
    }
}

object TransferScheduler {
    private const val JOB_ID = 2401
    @Volatile var executing = false
    fun start(context: Context) {
        if (executing) return
        if (Build.VERSION.SDK_INT >= 34) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, TransferJobService::class.java))
                .setUserInitiated(true)
                .setRequiredNetworkType(if (context.streamApp.preferences.state.value.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
                .build()
            val result = context.getSystemService(JobScheduler::class.java).schedule(job)
            check(result == JobScheduler.RESULT_SUCCESS) { "Android could not start the queue. Open Downloads and tap Resume queue." }
        } else {
            ContextCompat.startForegroundService(context, Intent(context, TransferService::class.java))
        }
    }
}

class TransferService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var execution: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TransferScheduler.executing = true
        startForeground(TransferNotifications.ID, TransferNotifications.create(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        if (execution?.isActive != true) execution = scope.launch {
            try {
                do {
                    val waiting = streamApp.downloads.runQueue { job ->
                        getSystemService(NotificationManager::class.java).notify(TransferNotifications.ID, TransferNotifications.create(this@TransferService, job))
                    }
                    if (waiting) delay(10_000)
                } while (waiting)
            } finally {
                withContext(NonCancellable + Dispatchers.Main) { TransferScheduler.executing = false; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            }
        }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) { execution?.cancel(); stopSelf() }
    override fun onDestroy() { TransferScheduler.executing = false; scope.cancel(); super.onDestroy() }
}

class TransferJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var execution: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        TransferScheduler.executing = true
        setNotification(params, TransferNotifications.ID, TransferNotifications.create(this), JOB_END_NOTIFICATION_POLICY_REMOVE)
        execution = scope.launch {
            var retry = false
            try {
                retry = streamApp.downloads.runQueue { job ->
                    getSystemService(NotificationManager::class.java).notify(TransferNotifications.ID, TransferNotifications.create(this@TransferJobService, job))
                }
            } finally {
                TransferScheduler.executing = false
                if (isActive) withContext(Dispatchers.Main) { jobFinished(params, retry) }
            }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { execution?.cancel(); return false }
    override fun onDestroy() { TransferScheduler.executing = false; scope.cancel(); super.onDestroy() }
}

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        context.streamApp.applicationScope.launch {
            try { context.streamApp.downloads.cancelAll() } finally { pending.finish() }
        }
    }
}

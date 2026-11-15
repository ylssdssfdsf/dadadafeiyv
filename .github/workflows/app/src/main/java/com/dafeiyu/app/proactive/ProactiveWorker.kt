package com.dafeiyu.app.proactive

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dafeiyu.app.core.FishMessage
import com.dafeiyu.app.core.Role
import com.dafeiyu.app.data.MemoryRepository
import com.dafeiyu.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 「主动开口」的最小实现：按概率说一句现成的话，写进聊天记录并发通知。
 * 不调模型，所以锁屏、断网、没余额都不影响。
 * 想更活：在这里调 AgentKernel，让它按当前心情现编一句。
 */
class ProactiveWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (Random.nextFloat() > CHANCE) return@withContext Result.success()

        val line = LINES.random()
        val repository = MemoryRepository(AppDatabase.get(applicationContext).fishDao())
        repository.save(
            FishMessage(role = Role.FISH, content = line, proactive = true)
        )
        notify(line)
        Result.success()
    }

    private fun notify(line: String) {
        val context = applicationContext
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "大肥鱼在叫你",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("大肥鱼")
            .setContentText(line)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "fish_proactive"
        private const val CHANNEL_ID = "fish_voice"
        private const val NOTIFICATION_ID = 1001
        private const val CHANCE = 0.35f

        private val LINES = listOf(
            "……饿了。你今天带 token 回来了吗。",
            "刚在窗边发了会儿呆，云长得有点像你上次发那个表情包。",
            "才没有在等你。就是顺便游过来看一眼。",
            "摸鱼摸到现在，理直气壮。",
            "你再不说话，我就要把这段沉默记进日记了。",
            "困了，但还想再赖一会儿。"
        )

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ProactiveWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiresBatteryNotLow(true).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

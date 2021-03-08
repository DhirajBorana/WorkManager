package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.coroutineScope
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.Exception

class SampleWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val TAG = "DownloadWorker"
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    companion object {
        const val PROGRESS = "PROGRESS"
        const val FILE_SIZE = "FILE_SIZE"
        const val DOWNLOAD_LINK = "DOWNLOAD_LINK"
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID = "Download progress"

    }

    override suspend fun doWork(): Result = coroutineScope {
        val downloadLink = inputData.getString(DOWNLOAD_LINK)

        try {
            val firstUpdate = workDataOf(PROGRESS to 0)
            val lastUpdate = workDataOf(PROGRESS to 100)
            var count = 0
            var folderDir = applicationContext.getExternalFilesDir(null)?.absolutePath
            var fileName = "$folderDir/${UUID.randomUUID()}.dmg"

            val fileIcon = File(fileName)
            if (!fileIcon.exists()) {
                fileIcon.createNewFile()
            }

            val url = URL(downloadLink)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val fileLength = connection.contentLength
            val input = BufferedInputStream(connection.inputStream)
            val output = FileOutputStream(fileName)
            val data = ByteArray(1024)
            setProgress(firstUpdate)
            var total = 0.0f
            while (input.read(data).also { count = it } != -1) {
                total += count
                val progress = total.div(fileLength).times(100).toInt()
                setProgress(workDataOf(PROGRESS to progress))
                setForeground(createForegroundInfo(progress))
                output.write(data, 0, count)
            }

            output.close()
            input.close()
            setProgress(lastUpdate)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
        Result.success()
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading file")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager?.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                channel =
                    NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

}
package com.example.myapplication

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.*
import kotlinx.coroutines.coroutineScope
import kotlin.Exception

class DownloadWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val TAG = "DownloadWorker"
    private var downloadId = -1L
    private var downloadFinish = false
    private val downloadManager by lazy { context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager }

    companion object {
        const val PROGRESS = "PROGRESS"
        const val DOWNLOAD_LINK = "DOWNLOAD_LINK"
        const val FILE_NAME = "FILE_NAME"
    }

    override suspend fun doWork(): Result = coroutineScope {
        val downloadLink = inputData.getString(DOWNLOAD_LINK)
        val fileName = inputData.getString(FILE_NAME)
        try {
            setProgress(workDataOf(PROGRESS to 0))
            val uri = Uri.parse(downloadLink)
            downloadId = downloadManager.enqueue(
                DownloadManager.Request(uri)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setTitle(fileName)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "File")
            )
            while (!downloadFinish) {
                downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.let {
                    if (it.moveToFirst()) {
                        Log.d(TAG, "${it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))}")
                        when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_FAILED -> {
                                downloadFinish = true
                                Result.failure()
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val total =
                                    it.getLong(it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (total >= 0) {
                                    val downloaded =
                                        it.getLong(it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                    val progress = downloaded.times(100L).div(total).toInt()
                                    setProgress(workDataOf(PROGRESS to progress))
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                setProgress(workDataOf(PROGRESS to 100))
                                downloadFinish = true
                                Result.success()
                            }
                            else -> {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
        Result.success()
    }


}
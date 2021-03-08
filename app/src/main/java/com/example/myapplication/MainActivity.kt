package com.example.myapplication

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.work.*
import com.example.myapplication.DownloadWorker.Companion.FILE_NAME
import com.example.myapplication.DownloadWorker.Companion.PROGRESS
import com.example.myapplication.SampleWorker.Companion.DOWNLOAD_LINK
import com.example.myapplication.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {

    private var downloadLink = "https://storage.googleapis.com/full_it/Team_Viewer.dmg"
    private lateinit var workManager: WorkManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var downloadRequest: OneTimeWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        workManager = WorkManager.getInstance(this)

        binding.apply {
            startBtn.setOnClickListener { startDownload() }
            resetProgress()
        }

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        registerReceiver(
            onNotificationClick,
            IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        )
    }

    private fun resetProgress() {
        binding.apply {
            progressBar.max = 100
            progressBar.progress = 0
            progressTv.text = "0%"
        }
    }


    private fun startDownload() {
        val fileName = URLUtil.guessFileName(
            downloadLink,
            null,
            MimeTypeMap.getFileExtensionFromUrl(downloadLink)
        )
        binding.fileNameTv.text = fileName
        val input = Data.Builder().apply {
            putString(DOWNLOAD_LINK, downloadLink)
            putString(FILE_NAME, fileName)
        }.build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        workManager.enqueue(downloadRequest)
        observeDownloadWorker()
    }

    private fun observeDownloadWorker() {
        workManager.getWorkInfoByIdLiveData(downloadRequest.id).observe(this) { it ->
            it?.let { workInfo ->
                workInfo.progress.getInt(PROGRESS, 0).let { progress ->
                    binding.progressTv.text = "$progress%"
                    binding.progressBar.progress = progress
                }
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                        binding.startBtn.isEnabled = false
                    }
                    WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->  {
                        binding.startBtn.isEnabled = true
                        resetProgress()
                    }
                }
            }
        }
    }

    private val onComplete = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            resetProgress()
        }
    }

    private val onNotificationClick = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            startActivity(Intent(this@MainActivity, MainActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
        unregisterReceiver(onNotificationClick)
    }
}
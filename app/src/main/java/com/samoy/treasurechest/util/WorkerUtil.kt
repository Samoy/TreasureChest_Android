package com.samoy.treasurechest.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DownloadWorker(context: Context, workerParams: WorkerParameters) :
	CoroutineWorker(context, workerParams) {

	companion object {
		const val Progress = "Progress"
		const val SAF_PATH = "SafPath"
	}

	override suspend fun doWork(): Result {
		return withContext(Dispatchers.IO) {
			Result.success()
		}
	}
}

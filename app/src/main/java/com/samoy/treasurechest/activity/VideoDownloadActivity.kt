package com.samoy.treasurechest.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.samoy.treasurechest.databinding.ActivityVideoDownloadBinding
import com.samoy.treasurechest.util.DownloadWorker
import com.samoy.treasurechest.util.DownloadWorker.Companion.Progress
import com.samoy.treasurechest.util.DownloadWorker.Companion.SAF_PATH
import com.samoy.treasurechest.util.FileUtil

private const val REQUEST_SAVE_VIDEO = 0

class VideoDownloadActivity : BaseActivity() {
	private lateinit var binding: ActivityVideoDownloadBinding
	private lateinit var mediaPlayer: SimpleExoPlayer
	private lateinit var inputUrl: String

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoDownloadBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initPlayer()
		binding.tfAddress.setOnEditorActionListener { v, actionId, _ ->
			return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_GO) {
				inputUrl = v.text.toString()
				handleVideoUri(v.text.toString())
				true
			} else false
		}
		binding.btnStartDownload.setOnClickListener { startDownload() }
	}


	override fun getActivityTitle(): CharSequence {
		return "视频下载"
	}

	private fun initPlayer() {
		mediaPlayer = SimpleExoPlayer.Builder(this).build()
		binding.videoView.player = mediaPlayer
	}

	private fun handleVideoUri(url: String) {
		if (url.endsWith("m3u8")) {
			// 播放m3u8视频
			val userAgent = Util.getUserAgent(this, application.packageName)
			val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
			val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
				.createMediaSource(MediaItem.fromUri(url))
			mediaPlayer.setMediaSource(mediaSource)
		} else {
			// 播放普通视频
			mediaPlayer.setMediaItem(MediaItem.fromUri(inputUrl))
		}
		mediaPlayer.prepare()
	}

	private fun startDownload() {
		val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
			type = "video/*"
			putExtra(
				Intent.EXTRA_TITLE,
				"下载的视频.${FileUtil.getFileExtension(if (inputUrl.endsWith("m3u8")) "mp4" else inputUrl)}"
			)
			addCategory(Intent.CATEGORY_OPENABLE)
		}
		startActivityForResult(intent, REQUEST_SAVE_VIDEO)
	}

	private fun download(uri: Uri) {
		// 工作需要传递的参数
		val inputDataBuilder = Data.Builder()
		inputDataBuilder.putString(SAF_PATH, FFmpegKitConfig.getSafParameterForWrite(this, uri))
		// 创建工作请求
		val downloadWorkerRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
			.setInputData(inputDataBuilder.build())
			.build()
		val instance = WorkManager.getInstance(this)
		instance.enqueue(downloadWorkerRequest)
		// 监听工作进度
		instance.getWorkInfoByIdLiveData(downloadWorkerRequest.id)
			.observe(this) {
				if (it != null) {
					val progress = it.progress
					val value = progress.getInt(Progress, 0)
					// Do something with progress information
					Log.i("工作进度", "$progress---$value")
				}
			}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_SAVE_VIDEO && resultCode == RESULT_OK) {
			data?.data?.also {
				download(it)
			}
		}
	}

}

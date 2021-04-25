package com.samoy.treasurechest.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.arthenica.ffmpegkit.*
import com.samoy.treasurechest.R
import com.samoy.treasurechest.databinding.ActivityVideoExtractBinding
import com.samoy.treasurechest.databinding.ProgressDialogBinding
import com.samoy.treasurechest.util.AVUtil
import com.samoy.treasurechest.util.FileUtil
import com.samoy.treasurechest.view.ProgressDialog
import java.io.File


private const val POSITION_LOCAL_VIDEO = 0
private const val POSITION_NET_VIDEO = 1

private const val REQUEST_PICK_VIDEO = 0
private const val REQUEST_EXTRACT_AUDIO = 1
private const val REQUEST_EXTRACT_VIDEO = 2

class VideoExtractActivity : BaseActivity() {
	private lateinit var binding: ActivityVideoExtractBinding
	private lateinit var inUri: Uri

	private lateinit var audioUri: Uri
	private lateinit var videoUri: Uri

	private lateinit var audioExtension: String
	private lateinit var videoExtension: String

	private lateinit var cacheFilePath: String

	private lateinit var progressDialog: AlertDialog
	private lateinit var progressDialogBinding: ProgressDialogBinding

	// 媒体时长，以毫秒为单位
	private var mediaDuration: Double = 0.00
	private lateinit var statistics: Statistics

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoExtractBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initPlayer()
	}

	private fun initPlayer() {
		binding.player.apply {
			fullscreenButton.visibility = View.GONE
			titleTextView.visibility = View.GONE
			backButton.visibility = View.GONE
		}
		binding.btnSelectVideo.setOnClickListener { openFile() }
		binding.btnExtractAudio.setOnClickListener { extraAudio() }
		binding.btnExtractVideo.setOnClickListener { extraVideo() }
		FFmpegKitConfig.enableStatisticsCallback {
			statistics = it
			updateProgress(it)
		}
	}

	private fun updateProgress(statistics: Statistics?) {
		if (statistics == null) {
			return
		}
		val timeInMilliseconds = statistics.time
		if (timeInMilliseconds > 0) {
			val completePercentage = timeInMilliseconds.toDouble() / mediaDuration * 100
			runOnUiThread {
				progressDialogBinding.barProgress.progress = completePercentage.toInt()
				progressDialogBinding.tvProgress.text =
					String.format(getString(R.string.percent), completePercentage)
			}
		}
	}

	private fun handleInFile(callback: (file: File) -> Unit) {
		if (!this::inUri.isInitialized) {
			Toast.makeText(this, "您还没有加载任何视频", Toast.LENGTH_SHORT).show()
			return
		}
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, inUri)
		FFprobeKit.getMediaInformationAsync(videoPath) { o ->
			val information = (o as MediaInformationSession).mediaInformation
			if (information == null) {
				Toast.makeText(this, "获取视频信息失败，无法提取", Toast.LENGTH_SHORT).show()
				return@getMediaInformationAsync
			}
			mediaDuration = information.duration.toDouble() * 1000
			information.streams.forEach {
				if (it.type == "video") {
					videoExtension = AVUtil.getVideoExtension(it.codec)
				}
				if (it.type == "audio") {
					audioExtension = AVUtil.getAudioExtension(it.codec)
				}
			}
			val fileExtension = FileUtil.getFileExtension(videoPath)
			val fileName = FileUtil.getFileName(videoPath)
			val file = File(externalCacheDir, "$fileName.${fileExtension}")
			if (!file.exists()) {
				file.delete()
			}
			val inputStream = contentResolver.openInputStream(inUri)
			file.writeBytes(inputStream!!.readBytes())
			cacheFilePath = file.absolutePath
			callback(file)
		}
	}

	// 提取音频
	private fun extraAudio() {
		handleInFile {
			val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
				type = "audio/*"
				putExtra(Intent.EXTRA_TITLE, "${it.nameWithoutExtension}.$audioExtension")
				addCategory(Intent.CATEGORY_OPENABLE)
			}
			startActivityForResult(intent, REQUEST_EXTRACT_AUDIO)
		}
	}

	// 提取视频
	private fun extraVideo() {
		handleInFile {
			val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
				type = "video/*"
				putExtra(Intent.EXTRA_TITLE, "${it.nameWithoutExtension}.$videoExtension")
				addCategory(Intent.CATEGORY_OPENABLE)
			}
			startActivityForResult(intent, REQUEST_EXTRACT_VIDEO)
		}
	}

	private fun openFile() {
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			addCategory(Intent.CATEGORY_OPENABLE)
			type = "video/*"
		}
		startActivityForResult(intent, REQUEST_PICK_VIDEO)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
			data?.data?.also { uri ->
				inUri = uri
				binding.player.setUp(uri.toString(), true, "video")
			}
			return
		}
		if (requestCode == REQUEST_EXTRACT_AUDIO && resultCode == Activity.RESULT_OK) {
			audioUri = data?.data!!
			saveAudio()
			return
		}
		if (requestCode == REQUEST_EXTRACT_VIDEO && resultCode == Activity.RESULT_OK) {
			videoUri = data?.data!!
			saveVideo()
			return
		}
	}

	private fun showDialog() {
		val pair = ProgressDialog.create(this, "请稍等...")
		progressDialog = pair.first
		progressDialogBinding = pair.second
		progressDialog.show()
	}

	private fun saveAudio() {
		showDialog()
		// ffmpeg -i 3.mp4 -vn -y 3.aac
		val audioPath = FFmpegKitConfig.getSafParameterForWrite(this, audioUri)
		val command = "-i \"$cacheFilePath\" -vn -y \"$audioPath\""
		FFmpegKit.executeAsync(command) {
			if (ReturnCode.isSuccess(it.returnCode)) {
				runOnUiThread {
					progressDialog.dismiss()
					Toast.makeText(this, "提取成功", Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	private fun saveVideo() {
		showDialog()
		// ffmpeg -i 1.mp4 -an -y 2.mp4
		val videoPath = FFmpegKitConfig.getSafParameterForWrite(this, videoUri)
		val command = "-i \"$cacheFilePath\" -an -y \"$videoPath\""
		FFmpegKit.executeAsync(command) {
			if (ReturnCode.isSuccess(it.returnCode)) {
				runOnUiThread {
					progressDialog.dismiss()
					Toast.makeText(this, "提取成功", Toast.LENGTH_SHORT).show()
				}
			}
		}
	}
}

package com.samoy.treasurechest.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.arthenica.ffmpegkit.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.samoy.treasurechest.R
import com.samoy.treasurechest.databinding.ActivityVideoSwitchBinding
import com.samoy.treasurechest.databinding.ViewProgressDialogBinding
import com.samoy.treasurechest.view.ProgressDialog

private const val REQUEST_PICK_VIDEO = 0
private const val REQUEST_SAVE_VIDEO = 1

class VideoSwitchActivity : BaseActivity(), AdapterView.OnItemSelectedListener {
	private lateinit var binding: ActivityVideoSwitchBinding
	private lateinit var videoFormat: String
	private lateinit var mediaPlayer: SimpleExoPlayer
	private lateinit var inputUri: Uri
	private lateinit var outputUri: Uri
	private lateinit var progressDialog: AlertDialog
	private lateinit var progressDialogBinding: ViewProgressDialogBinding

	// 媒体时长，以毫秒为单位
	private var mediaDuration: Double = 0.0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoSwitchBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initPlayer()
		binding.spinnerFormat.onItemSelectedListener = this
		binding.btnSelectVideo.setOnClickListener { selectVideo() }
		binding.btnSwitch.setOnClickListener { startSwitch() }
		initFFmpeg()
	}

	private fun initPlayer() {
		mediaPlayer = SimpleExoPlayer.Builder(this).build()
		binding.videoView.player = mediaPlayer
	}

	private fun initFFmpeg() {
		FFmpegKitConfig.enableStatisticsCallback {
			updateProgress(it)
		}
	}

	private fun showDialog() {
		val pair = ProgressDialog.create(this, "请稍等...")
		progressDialog = pair.first
		progressDialogBinding = pair.second
		progressDialog.show()
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

	private fun selectVideo() {
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			addCategory(Intent.CATEGORY_OPENABLE)
			type = "video/*"
		}
		startActivityForResult(intent, REQUEST_PICK_VIDEO)
	}

	private fun startSwitch() {
		if (!this::videoFormat.isInitialized) {
			Toast.makeText(this, "您还没有选择视频", Toast.LENGTH_SHORT).show()
			return
		}
		val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
			type = "video/*"
			addCategory(Intent.CATEGORY_OPENABLE)
			putExtra(
				Intent.EXTRA_TITLE, "已转码的视频.$videoFormat"
			)
		}
		startActivityForResult(intent, REQUEST_SAVE_VIDEO)
	}

	private fun switchVideo(uri: Uri) {
		val inputPath = FFmpegKitConfig.getSafParameterForRead(this, inputUri)
		val outputPath = FFmpegKitConfig.getSafParameterForWrite(this, uri)
		// ffmpeg -i video.mp4 video.avi
		showDialog()
		val command = "-i $inputPath $outputPath"
		FFmpegKit.executeAsync(command) {
			runOnUiThread {
				progressDialog.dismiss()
				Toast.makeText(
					this,
					"转码${if (ReturnCode.isSuccess(it.returnCode)) "成功" else "失败"}",
					Toast.LENGTH_SHORT
				).show()
			}
		}
	}


	private fun getMediaInfo() {
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, inputUri)
		val session = FFprobeKit.getMediaInformation(videoPath) as MediaInformationSession
		mediaDuration = session.mediaInformation.duration.toDouble() * 1000
	}

	override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
		val formats = resources.getStringArray(R.array.video_formats)
		videoFormat = formats[position]
	}

	override fun onNothingSelected(parent: AdapterView<*>?) {

	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK) {
			data?.data?.also {
				inputUri = it
				mediaPlayer.setMediaItem(MediaItem.fromUri(it))
				mediaPlayer.prepare()
				getMediaInfo()
			}
			return
		}
		if (requestCode == REQUEST_SAVE_VIDEO && resultCode == RESULT_OK) {
			data?.data?.also {
				outputUri = it
				switchVideo(it)
			}
		}
	}
}

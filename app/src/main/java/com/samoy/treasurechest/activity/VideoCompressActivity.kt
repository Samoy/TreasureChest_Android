package com.samoy.treasurechest.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.arthenica.ffmpegkit.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.samoy.treasurechest.R
import com.samoy.treasurechest.databinding.ActivityVideoCompressBinding
import com.samoy.treasurechest.databinding.ViewProgressDialogBinding
import com.samoy.treasurechest.util.FileUtil
import com.samoy.treasurechest.view.ProgressDialog

private const val REQUEST_SELECT_VIDEO = 0
private const val REQUEST_SAVE_VIDEO = 1

class VideoCompressActivity : BaseActivity() {

	private lateinit var binding: ActivityVideoCompressBinding
	private lateinit var mediaPlayer: SimpleExoPlayer
	private lateinit var inputUri: Uri
	private lateinit var progressDialog: AlertDialog
	private lateinit var progressDialogBinding: ViewProgressDialogBinding

	// 媒体时长，以毫秒为单位
	private var mediaDuration: Double = 0.00

	override fun getActivityTitle(): CharSequence = "视频压缩"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoCompressBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initPlayer()
		initFFmepg()
		binding.btnSelectVideo.setOnClickListener { selectVideo() }
		binding.btnStartCompress.setOnClickListener { startCompress() }
	}

	private fun initFFmepg() {
		FFmpegKitConfig.enableStatisticsCallback {
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

	private fun selectVideo() {
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			addCategory(Intent.CATEGORY_OPENABLE)
			type = "video/*"
		}
		startActivityForResult(intent, REQUEST_SELECT_VIDEO)
	}

	private fun initPlayer() {
		mediaPlayer = SimpleExoPlayer.Builder(this).build()
		binding.videoView.player = mediaPlayer
	}

	private fun getMediaInfo(uri: Uri) {
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, inputUri)
		val session = FFprobeKit.getMediaInformation(videoPath) as MediaInformationSession
		mediaDuration = session.mediaInformation.duration.toDouble() * 1000
	}

	private fun startCompress() {
		if (!this::inputUri.isInitialized) {
			Toast.makeText(this, "您还没有选择任何视频", Toast.LENGTH_SHORT).show()
			return
		}
		mediaPlayer.stop()
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, inputUri)
		val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
			addCategory(Intent.CATEGORY_OPENABLE)
			type = "video/*"
			putExtra(Intent.EXTRA_TITLE, "压缩后的视频.${FileUtil.getFileExtension(videoPath)}")
		}
		startActivityForResult(intent, REQUEST_SAVE_VIDEO)
	}


	private fun compress(uri: Uri) {
		val inputPath = FFmpegKitConfig.getSafParameterForRead(this, inputUri)
		val outputPath = FFmpegKitConfig.getSafParameterForWrite(this, uri)
		// ffmpeg -i input.mp4 -vcodec libx265 -crf 20 output.mp4
		val command = "-i $inputPath -crf 20 $outputPath"
		showDialog()
		FFmpegKit.executeAsync(command) {
			runOnUiThread {
				progressDialog.dismiss()
				Toast.makeText(
					this,
					"压缩${if (ReturnCode.isSuccess(it.returnCode)) "成功" else "失败"}",
					Toast.LENGTH_SHORT
				).show()
			}
		}
	}

	private fun showDialog() {
		val (first, second) = ProgressDialog.create(this, "请稍等...")
		progressDialog = first
		progressDialogBinding = second
		progressDialog.show()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_SELECT_VIDEO && resultCode == RESULT_OK) {
			data?.data?.also {
				inputUri = it
				mediaPlayer.setMediaItem(MediaItem.fromUri(it))
				mediaPlayer.prepare()
				getMediaInfo(it)
			}
			return
		}
		if (requestCode == REQUEST_SAVE_VIDEO && resultCode == RESULT_OK) {
			data?.data?.also {
				compress(it)
			}
		}
	}

}

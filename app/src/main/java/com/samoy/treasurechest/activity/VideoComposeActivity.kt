package com.samoy.treasurechest.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.ImageView
import android.widget.Toast
import com.arthenica.ffmpegkit.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.slider.Slider
import com.samoy.treasurechest.R
import com.samoy.treasurechest.databinding.ActivityVideoComposeBinding
import com.samoy.treasurechest.databinding.ViewProgressDialogBinding
import com.samoy.treasurechest.util.FileUtil
import com.samoy.treasurechest.view.ProgressDialog
import java.util.*


private const val REQUEST_PICK_VIDEO = 0
private const val REQUEST_PICK_AUDIO = 1
private const val REQUEST_COMPOSE_VIDEO = 2

class VideoComposeActivity : BaseActivity() {

	private lateinit var binding: ActivityVideoComposeBinding
	private lateinit var videoUri: Uri
	private lateinit var audioUri: Uri
	private lateinit var audioMediaPlayer: MediaPlayer
	private lateinit var videoMediaPlayer: SimpleExoPlayer
	private lateinit var progressDialog: AlertDialog
	private lateinit var progressDialogBinding: ViewProgressDialogBinding
	private var isPlaying = false
	private var timer: Timer? = Timer()
	private var changeByUser = false

	// 视频时长，以毫秒为单位
	private var mediaDuration: Double = 0.0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoComposeBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.btnSelectVideo.setOnClickListener { selectVideo() }
		binding.btnSelectAudio.setOnClickListener { selectAudio() }
		binding.btnStartCompose.setOnClickListener { startCompose() }
		initVideoPlayer()
		initFFmpeg()
	}

	private fun initFFmpeg() {
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
		startActivityForResult(intent, REQUEST_PICK_VIDEO)
	}

	private fun selectAudio() {
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			addCategory(Intent.CATEGORY_OPENABLE)
			type = "audio/*"
		}
		startActivityForResult(intent, REQUEST_PICK_AUDIO)
	}

	private fun initVideoPlayer() {
		videoMediaPlayer = SimpleExoPlayer.Builder(this).build()
		binding.player.player = videoMediaPlayer
	}

	private fun initAudioPlayer() {
		isPlaying = false
		timer?.cancel()
		audioMediaPlayer = MediaPlayer.create(this, audioUri)
		audioMediaPlayer.setOnCompletionListener {
			binding.imgAudioPlay.setImageResource(R.drawable.ic_play)
			binding.slideAudio.value = 0.0f
			audioMediaPlayer.reset()
			initAudioPlayer()
		}
	}

	private fun handleAudio() {
		initAudioPlayer()
		binding.imgAudioPlay.setOnClickListener {
			if (!this::audioUri.isInitialized) {
				Toast.makeText(this, "您还没有选择音频", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}
			if (isPlaying) {
				audioMediaPlayer.pause()
				timer?.cancel()
				(it as ImageView).setImageResource(R.drawable.ic_play)
			} else {
				audioMediaPlayer.start()
				(it as ImageView).setImageResource(R.drawable.ic_pause)
				timer = Timer()
				timer?.schedule(object : TimerTask() {
					override fun run() {
						if (!changeByUser) {
							runOnUiThread {
								val position = audioMediaPlayer.currentPosition.toFloat()
								binding.slideAudio.value = if (position < 0) 0.0f else position
							}
						}
					}
				}, 0, 10)
			}
			isPlaying = !isPlaying
		}
		binding.slideAudio.valueTo = audioMediaPlayer.duration.toFloat()
		binding.slideAudio.addOnChangeListener { _, _, _ ->
			runOnUiThread {
				binding.tvAudioTime.text =
					DateUtils.formatElapsedTime((audioMediaPlayer.currentPosition / 1000).toLong())
			}
		}
		binding.slideAudio.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
			override fun onStartTrackingTouch(slider: Slider) {
				changeByUser = true
				runOnUiThread {
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
						slider.setLabelFormatter {
							DateUtils.formatElapsedTime(it.toLong() / 1000)
						}
					}
				}
			}

			override fun onStopTrackingTouch(slider: Slider) {
				changeByUser = false
				runOnUiThread {
					audioMediaPlayer.seekTo(slider.value.toInt())
					binding.tvAudioTime.text =
						DateUtils.formatElapsedTime((audioMediaPlayer.currentPosition / 1000).toLong())
				}
			}
		})
	}

	private fun startCompose() {
		if (!this::videoUri.isInitialized) {
			Toast.makeText(this, "您还没有选择视频", Toast.LENGTH_SHORT).show()
			return
		}
		if (!this::audioUri.isInitialized) {
			Toast.makeText(this, "您还没有选择音频", Toast.LENGTH_SHORT).show()
			return
		}
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, videoUri)
		FFprobeKit.getMediaInformationAsync(videoPath) {
			val information = (it as MediaInformationSession).mediaInformation
			mediaDuration = information.duration.toDouble()*1000
		}
		val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
			type = "video/*"
			putExtra(
				Intent.EXTRA_TITLE, "合成的视频.${FileUtil.getFileExtension(videoPath)}"
			)
			addCategory(Intent.CATEGORY_OPENABLE)
		}
		startActivityForResult(intent, REQUEST_COMPOSE_VIDEO)
	}


	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
			data?.data?.also { uri ->
				videoUri = uri
				videoMediaPlayer.setMediaItem(MediaItem.fromUri(videoUri))
				videoMediaPlayer.prepare()
				videoMediaPlayer.stop()
			}
			return
		}
		if (requestCode == REQUEST_PICK_AUDIO && resultCode == Activity.RESULT_OK) {
			data?.data?.also { uri ->
				audioUri = uri
				handleAudio()
			}
			return
		}
		if (requestCode == REQUEST_COMPOSE_VIDEO && resultCode == Activity.RESULT_OK) {
			data?.data?.also {
				compose(it)
			}
		}
	}

	private fun compose(uri: Uri) {
		val audioPath = FFmpegKitConfig.getSafParameterForRead(this, audioUri)
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, videoUri)
		val outputPath = FFmpegKitConfig.getSafParameterForWrite(this, uri)
		showDialog()
		// ffmpeg -i 1.mp4 -i 1.mp3 -shortest output.mp4
		val command = "-i $audioPath -i $videoPath -shortest $outputPath"
		FFmpegKit.executeAsync(command) {
			runOnUiThread {
				progressDialog.dismiss()
				Toast.makeText(
					this,
					"合成${if (ReturnCode.isSuccess(it.returnCode)) "成功" else "失败"}",
					Toast.LENGTH_SHORT
				).show()
			}
		}
	}

	private fun showDialog() {
		val pair = ProgressDialog.create(this, "请稍等...")
		progressDialog = pair.first
		progressDialogBinding = pair.second
		progressDialog.show()
	}

	override fun onDestroy() {
		super.onDestroy()
		timer?.cancel()
		timer = null
		if (this::audioMediaPlayer.isInitialized) {
			audioMediaPlayer.release()
		}
		if (this::videoUri.isInitialized) {
			videoMediaPlayer.release()
		}
	}
}

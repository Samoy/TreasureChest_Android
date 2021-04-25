package com.samoy.treasurechest.activity

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import com.google.android.material.slider.Slider
import com.samoy.treasurechest.R
import com.samoy.treasurechest.databinding.ActivityVideoComposeBinding
import java.util.*

private const val REQUEST_PICK_VIDEO = 0
private const val REQUEST_PICK_AUDIO = 1

class VideoComposeActivity : BaseActivity() {

	private lateinit var binding: ActivityVideoComposeBinding
	private lateinit var videoUri: Uri
	private lateinit var audioUri: Uri
	private lateinit var mediaPlayer: MediaPlayer
	private var isPlaying = false
	private var timer: Timer? = Timer()
	private var changeByUser = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoComposeBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initPlayer()
		binding.btnSelectVideo.setOnClickListener {
			selectVideo()
		}
		binding.btnSelectAudio.setOnClickListener { selectAudio() }
	}

	private fun initPlayer() {
		binding.player.apply {
			fullscreenButton.visibility = View.GONE
			titleTextView.visibility = View.GONE
			backButton.visibility = View.GONE
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

	private fun initAudioPlayer() {
		isPlaying = false
		timer?.cancel()
		mediaPlayer = MediaPlayer.create(this, audioUri)
		mediaPlayer.setOnCompletionListener {
			binding.imgAudioPlay.setImageResource(R.drawable.ic_play)
			binding.slideAudio.value = 0.0f
			mediaPlayer.reset()
			initAudioPlayer()
		}
	}

	private fun handleAudio() {
		initAudioPlayer()
		binding.imgAudioPlay.setOnClickListener {
			if (isPlaying) {
				mediaPlayer.pause()
				timer?.cancel()
				(it as ImageView).setImageResource(R.drawable.ic_play)
			} else {
				mediaPlayer.start()
				(it as ImageView).setImageResource(R.drawable.ic_pause)
				timer = Timer()
				timer?.schedule(object : TimerTask() {
					override fun run() {
						if (!changeByUser) {
							runOnUiThread {
								val position = mediaPlayer.currentPosition.toFloat()
								binding.slideAudio.value = if (position < 0) 0.0f else position
							}
						}
					}
				}, 0, 10)
			}
			isPlaying = !isPlaying
		}
		binding.slideAudio.valueTo = mediaPlayer.duration.toFloat()
		binding.slideAudio.addOnChangeListener { _, _, _ ->
			runOnUiThread {
				binding.tvAudioTime.text =
					DateUtils.formatElapsedTime((mediaPlayer.currentPosition / 1000).toLong())
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
					mediaPlayer.seekTo(slider.value.toInt())
					binding.tvAudioTime.text =
						DateUtils.formatElapsedTime((mediaPlayer.currentPosition / 1000).toLong())
				}
			}

		})
	}


	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
			data?.data?.also { uri ->
				videoUri = uri
				binding.player.setUp(uri.toString(), true, "video")
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
	}

	override fun onDestroy() {
		super.onDestroy()
		timer?.cancel()
		timer = null
		if (this::mediaPlayer.isInitialized) {
			mediaPlayer.release()
		}
	}
}

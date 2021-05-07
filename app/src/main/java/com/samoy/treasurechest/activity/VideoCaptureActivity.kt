package com.samoy.treasurechest.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.samoy.treasurechest.databinding.ActivityVideoCaptureBinding
import java.io.*

private const val REQUEST_SELECT_VIDEO = 0
private const val REQUEST_SAVE_IMAGE = 1

class VideoCaptureActivity : BaseActivity() {
	private lateinit var binding: ActivityVideoCaptureBinding
	private lateinit var mediaPlayer: SimpleExoPlayer
	private lateinit var inputUri: Uri
	private lateinit var outputFile: File

	override fun getActivityTitle(): CharSequence {
		return "视频截图"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityVideoCaptureBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initPlayer()
		binding.btnSelectVideo.setOnClickListener { selectVideo() }
		binding.btnCapture.setOnClickListener { shotVideo() }
		binding.btnSaveImg.setOnClickListener { saveImage() }
	}

	private fun initPlayer() {
		mediaPlayer = SimpleExoPlayer.Builder(this).build()
		binding.videoView.player = mediaPlayer
	}

	private fun selectVideo() {
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			addCategory(Intent.CATEGORY_OPENABLE)
			type = "video/*"
		}
		startActivityForResult(intent, REQUEST_SELECT_VIDEO)
	}

	private fun shotVideo() {
		if (mediaPlayer.currentPosition <= 0) {
			Toast.makeText(this, "请先播放视频", Toast.LENGTH_SHORT).show()
			return
		}
		val time = mediaPlayer.currentPosition.toDouble().div(1000)
		// ffmpeg -ss 00:05 -i video.mp4 -vframes 1 -q:v 2 output.jpg
		val videoPath = FFmpegKitConfig.getSafParameterForRead(this, inputUri)
		val file = File(externalCacheDir, "${System.currentTimeMillis()}.JPG")
		val command = "-ss $time -i $videoPath -vframes 1 -q:v 2 ${file.absolutePath}"
		FFmpegKit.executeAsync(command) {
			val isSuccess = ReturnCode.isSuccess(it.returnCode)
			runOnUiThread {
				Toast.makeText(
					this,
					"截图${if (isSuccess) "成功" else "失败"}",
					Toast.LENGTH_SHORT
				).show()
				if (isSuccess) {
					outputFile = file
					binding.ivPreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
				}
			}
		}
	}

	private fun saveImage() {
		if (!this::outputFile.isInitialized) {
			Toast.makeText(this, "您还没有进行截图", Toast.LENGTH_SHORT).show()
			return
		}
		val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
			type = "video/*"
			addCategory(Intent.CATEGORY_OPENABLE)
			putExtra(Intent.EXTRA_TITLE, outputFile.name)
		}
		startActivityForResult(intent, REQUEST_SAVE_IMAGE)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_SELECT_VIDEO && resultCode == RESULT_OK) {
			data?.data?.also {
				inputUri = it
				mediaPlayer.setMediaItem(MediaItem.fromUri(it))
				mediaPlayer.prepare()
			}
			return
		}
		if (requestCode == REQUEST_SAVE_IMAGE && resultCode == RESULT_OK) {
			data?.data?.also { uri ->
				val outputStream = contentResolver.openOutputStream(uri)
				outputStream?.use {
					try {
						it.write(outputFile.readBytes())
						it.flush()
						it.close()
						Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show()
					} catch (e: IOException) {
						Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
						DocumentsContract.deleteDocument(contentResolver, uri)
					} finally {
						it.close()
					}
				}
			}
		}
	}
}

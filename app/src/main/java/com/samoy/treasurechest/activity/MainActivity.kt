package com.samoy.treasurechest.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.samoy.treasurechest.R
import com.samoy.treasurechest.adapter.CommandViewAdapter
import com.samoy.treasurechest.bean.Command
import com.samoy.treasurechest.bean.CommandBean
import com.samoy.treasurechest.databinding.ActivityMainBinding
import com.samoy.treasurechest.view.CommonDecoration

// 每行3个
private const val GRID_SPAN_COUNT = 3

class MainActivity : AppCompatActivity(), CommandViewAdapter.OnCommandItemClick {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val gridLayoutManager = GridLayoutManager(this, GRID_SPAN_COUNT)
		binding.gridView.layoutManager = gridLayoutManager
		val adapter = CommandViewAdapter(getVideoCommands(), this)
		binding.gridView.adapter = adapter
		binding.gridView.setHasFixedSize(true)
		binding.gridView.addItemDecoration(CommonDecoration(this))
	}

	private fun getVideoCommands(): List<CommandBean> {
		return listOf(
			CommandBean(Command.VIDEO_DOWNLOAD, "视频下载", R.drawable.ic_download_video),
			CommandBean(Command.VIDEO_EXTRACT, "视频提取", R.drawable.ic_music_video),
			CommandBean(Command.VIDEO_COMPOSE, "视频合成", R.drawable.ic_compose_video),
			CommandBean(Command.VIDEO_FORMAT, "视频转码", R.drawable.ic_switch_video),
			CommandBean(Command.VIDEO_COMPRESS, "视频压缩", R.drawable.ic_compress_video),
			CommandBean(Command.VIDEO_CAPTURE, "视频截图", R.drawable.ic_shot_video),
			CommandBean(Command.VIDEO_CUT, "视频裁剪", R.drawable.ic_cut_video),
			CommandBean(Command.VIDEO_INVERSE, "视频倒放", R.drawable.ic_inverse_video),
			CommandBean(Command.VIDEO_GIF, "视频转GIF", R.drawable.ic_video_gif)
		)
	}

	override fun onClick(command: CommandBean, item: View, position: Int) {
		when (command.command) {
//			Command.VIDEO_DOWNLOAD -> startActivity(Intent(this, VideoDownloadActivity::class.java))
			Command.VIDEO_EXTRACT -> startActivity(Intent(this, VideoExtractActivity::class.java))
			Command.VIDEO_COMPOSE -> startActivity(Intent(this, VideoComposeActivity::class.java))
			Command.VIDEO_FORMAT -> startActivity(Intent(this, VideoSwitchActivity::class.java))
			Command.VIDEO_COMPRESS -> startActivity(Intent(this, VideoCompressActivity::class.java))
			Command.VIDEO_CAPTURE -> startActivity(Intent(this, VideoCaptureActivity::class.java))
			else -> return
		}
	}
}

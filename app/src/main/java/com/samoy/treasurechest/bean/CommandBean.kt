package com.samoy.treasurechest.bean

import androidx.annotation.DrawableRes

enum class Command {
	VIDEO_DOWNLOAD, // 视频下载
	VIDEO_EXTRACT, // 视频拆分
	VIDEO_COMPOSE, // 视频合成
	VIDEO_FORMAT, // 视频转码
	VIDEO_COMPRESS, // 视频压缩
	VIDEO_CAPTURE, // 视频截图
	VIDEO_CUT, // 视频裁剪
	VIDEO_GIF, // 视频转GIF
	VIDEO_INVERSE // 视频倒放
}

data class CommandBean(val command: Command, val title: String, @DrawableRes val image: Int)

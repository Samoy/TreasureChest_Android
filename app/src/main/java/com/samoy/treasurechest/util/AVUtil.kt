package com.samoy.treasurechest.util

import java.io.File

object AVUtil {
	fun getVideoCodec(videoPath: String): String {
		var extension = "mp4"
		val pos = videoPath.lastIndexOf('.')
		if (pos >= 0) extension = videoPath.substring(pos + 1)
		return when (extension) {
			"webm" -> "vp8"
			"mkv" -> "aom"
			"ogv" -> "theora"
			"mov" -> "hap"
			"mp4" -> "mpeg4"
			else -> "mpeg4"
		}
	}

	fun getVideoExtension(videoCodec: String): String {
		return when (videoCodec) {
			"vp8", "vp9" -> "webm"
			"aom" -> "mkv"
			"theora" -> "ogv"
			"hap" -> "mov"
			else ->
				// mpeg4, x264, x265, xvid, kvazaar
				"mp4"
		}
	}

	fun getAudioExtension(audioCodec: String): String {
		return when (audioCodec) {
			"mp2" -> "mpg"
			"mp3" -> "mp3"
			"vorbis" -> "ogg"
			"opus" -> "opus"
			"amr-nb", "amr-wb" -> "amr"
			"ilbc" -> "lbc"
			"speex" -> "spx"
			"wavpack" -> "wv"
			else ->
				// soxr
				"wav"
		}
	}
}

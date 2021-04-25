package com.samoy.treasurechest.util

object DateUtil {
	fun formatToMinutesAndSeconds(timeMs: Long): String {
		val totalSeconds = timeMs / 1000
		val seconds = totalSeconds % 60
		val minutes = totalSeconds / 60 % 60
		return String.format("%02d:%02d", minutes, seconds)
	}
}

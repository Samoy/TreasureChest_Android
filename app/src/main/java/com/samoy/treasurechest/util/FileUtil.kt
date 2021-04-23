package com.samoy.treasurechest.util

object FileUtil {
	fun getSafFileNameAndExtension(safFilePath: String): String {
		return safFilePath.split("/").last()
	}

	fun getFileName(path: String): String {
		return path.split("/").last().split(".").first()
	}

	fun getFileExtension(path: String): String {
		return path.split(".").last()
	}
}

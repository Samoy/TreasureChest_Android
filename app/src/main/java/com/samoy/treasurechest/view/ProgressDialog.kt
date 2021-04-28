package com.samoy.treasurechest.view

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import com.samoy.treasurechest.databinding.ViewProgressDialogBinding

object ProgressDialog {
	fun create(context: Context, title: String): Pair<AlertDialog, ViewProgressDialogBinding> {
		val binding =
			ViewProgressDialogBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
		val dialog = AlertDialog.Builder(context)
			.setView(binding.root)
			.setTitle(title)
			.setCancelable(false)
			.create()
		return Pair(dialog, binding)
	}
}

package com.samoy.treasurechest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.samoy.treasurechest.R
import com.samoy.treasurechest.bean.CommandBean
import com.samoy.treasurechest.databinding.ViewCommandItemBinding

class CommandViewAdapter(
	private val commandList: List<CommandBean>,
	private val onItemClick: OnCommandItemClick?
) :
	RecyclerView.Adapter<CommandViewAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding = DataBindingUtil.inflate<ViewCommandItemBinding>(
			LayoutInflater.from(parent.context),
			R.layout.view_command_item,
			parent,
			false
		)
		return ViewHolder(binding.root)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val binding = DataBindingUtil.getBinding<ViewCommandItemBinding>(holder.itemView)
		binding?.data = commandList[position]
		binding?.container?.setOnClickListener {
			onItemClick?.onClick(commandList[position], it, position)
		}
	}

	override fun getItemCount(): Int = commandList.size

	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

	interface OnCommandItemClick {
		fun onClick(command: CommandBean, item: View, position: Int)
	}
}

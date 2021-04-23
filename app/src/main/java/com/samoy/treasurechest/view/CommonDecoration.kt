package com.samoy.treasurechest.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class CommonDecoration(context: Context) : ItemDecoration() {
	private val mDivider: Drawable?
	override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		drawHorizontal(c, parent)
		drawVertical(c, parent)
	}

	private fun getSpanCount(parent: RecyclerView): Int {
		// 列数
		var spanCount = -1
		val layoutManager = parent.layoutManager
		if (layoutManager is GridLayoutManager) {
			spanCount = layoutManager.spanCount
		} else if (layoutManager is StaggeredGridLayoutManager) {
			spanCount = layoutManager
				.spanCount
		}
		return spanCount
	}

	private fun drawHorizontal(c: Canvas?, parent: RecyclerView) {
		val childCount = parent.childCount
		for (i in 0 until childCount) {
			val child = parent.getChildAt(i)
			val params = child
				.layoutParams as RecyclerView.LayoutParams
			val left = child.left - params.leftMargin
			val right = (child.right + params.rightMargin
					+ mDivider!!.intrinsicWidth)
			val top = child.bottom + params.bottomMargin
			val bottom = top + mDivider.intrinsicHeight
			mDivider.setBounds(left, top, right, bottom)
			mDivider.draw(c!!)
		}
	}

	private fun drawVertical(c: Canvas?, parent: RecyclerView) {
		val childCount = parent.childCount
		for (i in 0 until childCount) {
			val child = parent.getChildAt(i)
			val params = child
				.layoutParams as RecyclerView.LayoutParams
			val top = child.top - params.topMargin
			val bottom = child.bottom + params.bottomMargin
			val left = child.right + params.rightMargin
			val right = left + mDivider!!.intrinsicWidth
			mDivider.setBounds(left, top, right, bottom)
			mDivider.draw(c!!)
		}
	}

	private fun isLastColumn(
		parent: RecyclerView, pos: Int, spanCount: Int,
		childCount: Int
	): Boolean {
		var myChildCount = childCount
		val layoutManager = parent.layoutManager
		if (layoutManager is GridLayoutManager) {
			// 如果是最后一列，则不需要绘制右边
			if ((pos + 1) % spanCount == 0) {
				return true
			}
		} else if (layoutManager is StaggeredGridLayoutManager) {
			val orientation = layoutManager
				.orientation
			if (orientation == StaggeredGridLayoutManager.VERTICAL) {
				// 如果是最后一列，则不需要绘制右边
				if ((pos + 1) % spanCount == 0) {
					return true
				}
			} else {
				myChildCount -= myChildCount % spanCount
				if (pos >= myChildCount) { // 如果是最后一列，则不需要绘制右边
					return true
				}
			}
		}
		return false
	}

	private fun isLastRaw(
		parent: RecyclerView,
		pos: Int,
		spanCount: Int,
		childCount: Int
	): Boolean {
		var myChildCount = childCount
		val layoutManager = parent.layoutManager
		if (layoutManager is GridLayoutManager) {
			var last = myChildCount % spanCount
			if (last == 0) {
				last = spanCount
			}
			myChildCount -= last
			if (pos >= myChildCount) { // 如果是最后一行，则不需要绘制底部
				return true
			}
		} else if (layoutManager is StaggeredGridLayoutManager) {
			val orientation = layoutManager
				.orientation
			// StaggeredGridLayoutManager 且纵向滚动
			if (orientation == StaggeredGridLayoutManager.VERTICAL) {
				var last = myChildCount % spanCount
				if (last == 0) {
					last = spanCount
				}
				myChildCount -= last
				// 如果是最后一行，则不需要绘制底部
				if (pos >= myChildCount) {
					return true
				}
			} else { // StaggeredGridLayoutManager 且横向滚动
				// 如果是最后一行，则不需要绘制底部
				if ((pos + 1) % spanCount == 0) {
					return true
				}
			}
		}
		return false
	}

	override fun getItemOffsets(
		outRect: Rect, itemPosition: Int,
		parent: RecyclerView
	) {
		val spanCount = getSpanCount(parent)
		val childCount = parent.adapter!!.itemCount
		if (isLastColumn(parent, itemPosition, spanCount, childCount)) { // 如果是最后一列，则不需要绘制右边
			if (itemPosition == childCount - 1) {
				outRect[0, 0, 0] = 0
			} else {
				outRect[0, 0, 0] = mDivider!!.intrinsicHeight
			}
		} else if (isLastRaw(parent, itemPosition, spanCount, childCount)) { // 如果是最后一行，则不需要绘制底部
			outRect[0, 0, mDivider!!.intrinsicWidth] = 0
		} else {
			outRect[0, 0, mDivider!!.intrinsicWidth] = mDivider.intrinsicHeight
		}
	}

	companion object {
		private val ATTRS = intArrayOf(android.R.attr.listDivider)
	}

	init {
		val a = context.obtainStyledAttributes(ATTRS)
		mDivider = a.getDrawable(0)
		a.recycle()
	}
}

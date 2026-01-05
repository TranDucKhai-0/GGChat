package com.example.ggchat

import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class RoomItemAnimator : DefaultItemAnimator() {

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        val v = holder.itemView

        // start state
        v.alpha = 0f
        v.translationY = 120f

        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(addDuration)
            .withEndAction {
                dispatchAddFinished(holder)
            }
            .start()

        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        val v = holder.itemView

        v.animate()
            .alpha(0f)
            .translationX(v.width.toFloat())
            .setDuration(removeDuration)
            .withEndAction {
                // reset để reuse view holder không bị dính trạng thái
                v.alpha = 1f
                v.translationX = 0f
                dispatchRemoveFinished(holder)
            }
            .start()

        return true
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        item.itemView.animate().cancel()
        super.endAnimation(item)
    }

    override fun endAnimations() {
        super.endAnimations()
    }

    override fun isRunning(): Boolean {
        return super.isRunning()
    }
}

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.ggchat.AvatarCodec
import com.example.ggchat.FriendInfo
import com.example.ggchat.R

class FriendsAdapter(
    private val data: MutableList<FriendInfo>
) : androidx.recyclerview.widget.RecyclerView.Adapter<FriendsAdapter.VH>() {

    // Cache bitmaps by (ip + avatar hash) to avoid repeated Base64 decoding that can cause jank / OOM on rapid refresh.
    private val avatarCache = object : LruCache<String, Bitmap>(20) {}

    class VH(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
        val img = v.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.imgAvatar)
        val tv = v.findViewById<android.widget.TextView>(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return VH(v)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val f = data[i]
        h.tv.text = f.name.ifBlank { f.ip }

        val key = f.ip + ":" + (f.avatarBase64?.hashCode() ?: 0)
        val cached = avatarCache.get(key)
        if (cached != null) {
            h.img.setImageBitmap(cached)
            return
        }

        // If avatarBase64 is available, decode it and set it on the ImageView (see AvatarCodec.kt).
        val bmp = AvatarCodec.decodeBase64ToBitmap(f.avatarBase64)
        if (bmp != null) {
            avatarCache.put(key, bmp)
            h.img.setImageBitmap(bmp)
        } else {
            h.img.setImageResource(R.drawable.default_avatar)
        }
    }

    fun submit(newList: List<FriendInfo>) {
        data.clear()
        data.addAll(newList)
        notifyDataSetChanged()
    }
}

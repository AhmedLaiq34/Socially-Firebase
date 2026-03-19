package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class ProfilePostAdapter(
    private val posts: List<Post> // Thumbnails in profile grid
) : RecyclerView.Adapter<ProfilePostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val imageView = ImageView(parent.context)
        val size = parent.resources.displayMetrics.widthPixels / 3
        imageView.layoutParams = ViewGroup.LayoutParams(size, size)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setPadding(1, 1, 1, 1)
        return PostViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Load the first image thumbnail
        if (post.imagesBase64.isNotEmpty()) {
            val firstImageBase64 = post.imagesBase64[0]
            val bytes = Base64.decode(firstImageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.imageView.setImageBitmap(bitmap)
        }

        // --- Click opens full feed for that user ---
        holder.imageView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, own_posts_feed::class.java)
            intent.putExtra("USER_ID", post.userId) // pass the userId from this post
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = posts.size
}


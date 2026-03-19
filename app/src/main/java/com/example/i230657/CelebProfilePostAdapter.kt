package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CelebProfilePostAdapter(
    private val posts: List<Post>
) : RecyclerView.Adapter<CelebProfilePostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val imageView: ImageView = root.findViewById(R.id.ivPostImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_post_item, parent, false) // false is correct
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        if (post.imagesBase64.isNotEmpty()) {
            try {
                val firstImageBase64 = post.imagesBase64[0]
                val bytes = Base64.decode(firstImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.imageView.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_pfp)
        }

        holder.imageView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, own_posts_feed::class.java)
            intent.putExtra("userId", post.userId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = posts.size
}


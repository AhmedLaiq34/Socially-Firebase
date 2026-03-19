package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ForYouPageAdapter(
    private val posts: ArrayList<Post>
) : RecyclerView.Adapter<ForYouPageAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.ivPostImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.for_you_post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        if (post.imagesBase64.isNotEmpty()) {
            val firstImageBase64 = post.imagesBase64[0]
            try {
                val imageBytes = Base64.decode(firstImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.postImage.setImageResource(R.drawable.placeholder_post)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.placeholder_post)
        }

        holder.itemView.setOnClickListener {
            val postId = post.postId
            val intent = Intent(holder.itemView.context, post_view_page::class.java)
            intent.putExtra("postId", postId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = posts.size
}

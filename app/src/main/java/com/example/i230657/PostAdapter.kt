package com.example.i230657

import User
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(
    private val posts: ArrayList<Pair<Post, User>>,
    private val onUsernameClick: (String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val likesRef: DatabaseReference = database.getReference("posts")

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.postProfileImage)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val caption: TextView = itemView.findViewById(R.id.postCaption)
        val likesCounter: TextView = itemView.findViewById(R.id.likesCounter)
        val postImagesPager: androidx.viewpager2.widget.ViewPager2 = itemView.findViewById(R.id.vpPostImages)

        // Action icons
        val likeIcon: ImageView = itemView.findViewById(R.id.post1LikeIcon)
        val commentIcon: ImageView = itemView.findViewById(R.id.post1CommentIcon)
        val shareIcon: ImageView = itemView.findViewById(R.id.post1ShareIcon)
        val saveIcon: ImageView = itemView.findViewById(R.id.post1SaveIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val (post, user) = posts[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // --- User Info ---
        holder.username.text = user.username

        if (user.profilePictureUrl.isNotEmpty()) {
            try {
                val profileBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                val profileBitmap = BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
                holder.profileImage.setImageBitmap(profileBitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // --- Post Images (ViewPager2) ---
        val pagerAdapter = ImagePagerAdapter(post.imagesBase64)
        holder.postImagesPager.adapter = pagerAdapter

        // --- Dynamic dots setup ---
        val dotsContainer = holder.itemView.findViewById<LinearLayout>(R.id.post1Dots)
        dotsContainer.removeAllViews()
        val dotList = mutableListOf<ImageView>()
        for (i in post.imagesBase64.indices) {
            val dot = ImageView(holder.itemView.context)
            val size = 6.dpToPx()
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = 4.dpToPx()
            dot.layoutParams = params
            dot.setImageResource(if (i == 0) R.drawable.brown_circle else R.drawable.white_circle)
            dotsContainer.addView(dot)
            dotList.add(dot)
        }

        holder.postImagesPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                dotList.forEachIndexed { index, imageView ->
                    imageView.setImageResource(if (index == position) R.drawable.brown_circle else R.drawable.white_circle)
                }
            }
        })

        // --- Caption with bold username + clickable ---
        val spannable = android.text.SpannableStringBuilder()
        spannable.append(user.username)
        spannable.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            user.username.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(holder.itemView.context, profile_page::class.java)
                    intent.putExtra("username", user.username)
                    holder.itemView.context.startActivity(intent)
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = android.graphics.Color.BLACK
                }
            },
            0,
            user.username.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (post.caption.isNotEmpty()) {
            spannable.append(" ")
            spannable.append(post.caption)
        }

        holder.caption.text = spannable
        holder.caption.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        // --- Likes setup ---
        holder.likesCounter.text = post.likes.toString()
        holder.likeIcon.setImageResource(if (post.likedBy.containsKey(currentUserId)) R.drawable.red_like else R.drawable.like)

        holder.likeIcon.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val postRef = FirebaseDatabase.getInstance().getReference("posts").child(post.postId)
            postRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                    val p = currentData.getValue(Post::class.java) ?: return com.google.firebase.database.Transaction.success(currentData)
                    val updatedLikedBy = p.likedBy.toMutableMap()
                    if (updatedLikedBy.containsKey(userId)) {
                        updatedLikedBy.remove(userId)
                        p.likes = (p.likes - 1).coerceAtLeast(0)
                    } else {
                        updatedLikedBy[userId] = true
                        p.likes += 1
                    }
                    p.likedBy = updatedLikedBy
                    currentData.value = p
                    return com.google.firebase.database.Transaction.success(currentData)
                }

                override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, currentData: com.google.firebase.database.DataSnapshot?) {
                    if (committed) {
                        val updatedPost = currentData?.getValue(Post::class.java)
                        if (updatedPost != null) {
                            post.likes = updatedPost.likes
                            post.likedBy = updatedPost.likedBy
                            holder.likesCounter.text = post.likes.toString()
                            holder.likeIcon.setImageResource(if (post.likedBy.containsKey(userId)) R.drawable.red_like else R.drawable.like)
                        }
                    }
                }
            })
        }

        // --- Username/Profile click ---
        holder.username.setOnClickListener {
            if (user.userId == currentUserId) {
                holder.itemView.context.startActivity(Intent(holder.itemView.context, profile_page::class.java))
            } else {
                val intent = Intent(holder.itemView.context, celebrity_follow_page::class.java)
                intent.putExtra("userId", user.userId)
                holder.itemView.context.startActivity(intent)
            }
        }
        holder.profileImage.setOnClickListener { holder.username.performClick() }

        // --- Comment icon click ---
        holder.commentIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, comments_page::class.java)
            intent.putExtra("postId", post.postId)
            holder.itemView.context.startActivity(intent)
        }

        // --- Share icon click ---
        holder.shareIcon.setOnClickListener {
            val context = holder.itemView.context
            val firstImage = if (post.imagesBase64.isNotEmpty()) post.imagesBase64[0] else ""
            if (context is AppCompatActivity) {
                val dialog = SharePostDialogFragment.newInstance(
                    postId = post.postId,
                    userId = user.userId,
                    userProfilePicture = user.profilePictureUrl,
                    postCaption = post.caption,
                    postImage = firstImage,
                    timestamp = post.createdAt
                )
                dialog.show(context.supportFragmentManager, "SharePostDialog")
            }
        }

        // --- Save icon click (placeholder) ---
        holder.saveIcon.setOnClickListener {
            // Implement save logic later
        }
    }


    override fun getItemCount(): Int = posts.size


}

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

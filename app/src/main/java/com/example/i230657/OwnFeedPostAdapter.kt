package com.example.i230657

import User
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import de.hdodenhof.circleimageview.CircleImageView

class OwnFeedPostAdapter(
    private val posts: ArrayList<Pair<Post, User>>,
    private val onUsernameClick: (String) -> Unit
) : RecyclerView.Adapter<OwnFeedPostAdapter.PostViewHolder>() {


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

        // --- User Info ---
        holder.username.text = user.username

        if (user.profilePictureUrl.isNotEmpty()) {
            val profileBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
            val profileBitmap =
                BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
            holder.profileImage.setImageBitmap(profileBitmap)
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // --- Post Images (multiple) ---
        if (post.imagesBase64.isNotEmpty()) {
            val pagerAdapter = ImagePagerAdapter(post.imagesBase64)
            holder.postImagesPager.adapter = pagerAdapter
        } else {
            // If no image, you could show placeholder with single adapter
            holder.postImagesPager.adapter = ImagePagerAdapter(listOf(""))
        }


        // --- Caption with username bold + clickable ---
        val spannable = android.text.SpannableStringBuilder()

        // Append username
        spannable.append(user.username)

        // Make username bold
        spannable.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            user.username.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Make username clickable
        spannable.setSpan(
            object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(holder.itemView.context, profile_page::class.java)
                    intent.putExtra("username", user.username)
                    holder.itemView.context.startActivity(intent)
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false       // no underline
                    ds.color = android.graphics.Color.BLACK // set your desired color
                }
            },
            0,
            user.username.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        // Append caption text (if exists)
        if (post.caption.isNotEmpty()) {
            spannable.append(" ")
            spannable.append(post.caption)
        }

        holder.caption.text = spannable
        holder.caption.movementMethod = android.text.method.LinkMovementMethod.getInstance()


        // --- Likes (basic counter for now) ---
        holder.likesCounter.text = post.likes.toString()

        // --- Actions (basic demo functionality) ---
        holder.likeIcon.setOnClickListener {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val database = FirebaseDatabase.getInstance()
            val postRef = database.getReference("posts").child(post.postId)

            postRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val p = currentData.getValue(Post::class.java) ?: return Transaction.success(currentData)

                    val updatedLikedBy = p.likedBy.toMutableMap()
                    if (updatedLikedBy.containsKey(currentUserId)) {
                        // User already liked → remove like
                        updatedLikedBy.remove(currentUserId)
                        p.likes = (p.likes - 1).coerceAtLeast(0)  // prevent negative likes
                    } else {
                        // Add like
                        updatedLikedBy[currentUserId] = true
                        p.likes += 1
                    }

                    p.likedBy = updatedLikedBy
                    currentData.value = p
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (committed) {
                        // Update UI
                        val updatedPost = currentData?.getValue(Post::class.java)
                        if (updatedPost != null) {
                            post.likes = updatedPost.likes
                            post.likedBy = updatedPost.likedBy
                            holder.likesCounter.text = post.likes.toString()

                            // Optional: change like icon based on user's like
                            if (post.likedBy.containsKey(currentUserId)) {
                                holder.likeIcon.setImageResource(R.drawable.red_like) // filled heart
                            } else {
                                holder.likeIcon.setImageResource(R.drawable.like) // empty heart
                            }
                        }
                    }
                }
            })
        }



        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid


        holder.username.setOnClickListener {
            if (user.userId == currentUserId) {
                // Go to own profile
                val intent = Intent(holder.itemView.context, profile_page::class.java)
                holder.itemView.context.startActivity(intent)
            } else {
                // Go to celebrity page
                val intent = Intent(holder.itemView.context, celebrity_follow_page::class.java)
                intent.putExtra("userId", user.userId) // pass the author's userId
                holder.itemView.context.startActivity(intent)
            }
        }

        holder.profileImage.setOnClickListener {
            if (user.userId == currentUserId) {
                // Go to own profile
                val intent = Intent(holder.itemView.context, profile_page::class.java)
                holder.itemView.context.startActivity(intent)
            } else {
                // Go to celebrity page
                val intent = Intent(holder.itemView.context, celebrity_follow_page::class.java)
                intent.putExtra("userId", user.userId)
                holder.itemView.context.startActivity(intent)
            }
        }


        holder.commentIcon.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, comments_page::class.java)
            intent.putExtra("postId", post.postId) // pass the postId
            context.startActivity(intent)
        }


        holder.shareIcon.setOnClickListener {
            // Later → share intent
        }

        holder.saveIcon.setOnClickListener {
            // Later → toggle save
        }
    }

    override fun getItemCount(): Int = posts.size
}

package com.yourapp.models

data class Message(
    var senderId: String = "",
    var receiverId: String = "",
    var messageText: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var type: String = "text",          // "text" or "image"
    var imageUrl: String = "",          // base64 string for image
    var messageId: String? = null,       // ✅ used for delete / identification
    var post: SharedPost? = null

)

data class SharedPost(
    val postId: String = "",
    val userId: String = "",
    val userProfilePicture: String = "",
    val postCaption: String = "",
    val postImage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

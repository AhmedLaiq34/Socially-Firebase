package com.example.i230657

data class Post(
    val postId: String = "",
    val userId: String = "",
    var likes: Int = 0,
    var likedBy: Map<String, Boolean> = emptyMap(),
    val caption: String = "",
    val imagesBase64: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()

)



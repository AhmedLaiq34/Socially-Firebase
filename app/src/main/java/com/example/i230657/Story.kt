package com.example.i230657

data class Story(
    var storyId: String = "",
    var storyImageBase64: String = "",
    var createdAt: Long = System.currentTimeMillis()
)

data class UserStories(
    var userId: String = "",
    var username: String = "",
    var profilePictureBase64: String = "",
    var stories: MutableList<Story> = mutableListOf()
)

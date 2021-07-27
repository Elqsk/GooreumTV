package com.example.gooreumtv

data class Video(var uid: String,
                 var user: String,
                 val datetime: String,
                 val title: String,
                 val description: String?,
                 val video: String,
                 val thumbnail: String)

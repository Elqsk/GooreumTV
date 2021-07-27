package com.example.gooreumtv

data class User(var email: String,
                var password: String,
                var name: String,
                var image: String)
// image는 Uri를 그대로 toString()해놓은 것임

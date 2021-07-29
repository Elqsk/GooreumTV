package com.example.gooreumtv

data class User(var email: String? = null,
                var password: String? = null,
                var name: String? = null,
                var image: String? = null)
// image는 Uri를 그대로 toString()해놓은 것임

package com.example.gooreumtv

// username과 user_image는 처음에 동영상 목록 로드할 때에는 데이터에 함께 포함되어 있지 않고, user(Uid)로 따로
// 사용자 DB(문서)에서 불러온다.
data class VideoData(
    var uid: Long? = null,
    var user: String?  = null,
    var title: String? = null,
    var video: String? = null,
    var username: String?  = null,
    var datetime: String?  = null,
    var thumbnail: String? = null,
    var user_image: String?  = null,
    var description: String? = null
)
package com.example.gooreumtv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.ActivityMyVideoBinding
import com.example.gooreumtv.databinding.ContentMyVideoBinding
import com.google.firebase.firestore.ktx.toObject

class MyVideoActivity : AppCompatActivity() {

    private lateinit var binding:  ActivityMyVideoBinding
    private lateinit var _binding: ContentMyVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding  = ActivityMyVideoBinding.inflate(layoutInflater)
        _binding = binding.content
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadData()

        goToUploadVideo()
    }










    private fun loadData() {
        Toolbox.showProgressBar(_binding.list, _binding.progressBar, true)

        val uid = CurrentUser.getUid(this)

        Log.d(TAG, "UserFragment >> Load Data / uid: $uid")

        if (uid != null) {
            MyFirebase.getMyVideos(uid)
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "                동영상 정보 로드 성공! ${documents.size()}")
                    Log.d(TAG, " ")

                    if (!documents.isEmpty) {
                        for (document in documents) {

                            val data = document.toObject<VideoData>()
                            // HomeFragment와 다르게 데이터 클래스에 이미지를 따로 넣지 않는다.
                            // PlayerActivity를 시작하고 인텐트로 변수를 받아올 때 이미지가 null이면 사용자
                            // 본인이 올린 동영상이라는 뜻이며, 메인 액티비티에 변수로 저장된 byte array를
                            // 가져다 쓰면 된다.
                            data.username = MainActivity.USER_NAME

                            Log.d(TAG, "                            uid:   ${data.uid}")
                            Log.d(TAG, "                            title: ${data.title}")
                            Log.d(TAG, "                            desc:  ${data.description}")
                            Log.d(TAG, "                            thumbDir: ${data.thumbnail}")
                            Log.d(TAG, "                            videoDir: ${data.video}")
                            Log.d(TAG, "                            datetime: ${data.datetime}")
                            Log.d(TAG, "                            userUid:  ${data.user}")
                            Log.d(TAG, "                            username: ${MainActivity.USER_NAME}")
                            Log.d(TAG, " ")

                            // 아이템 하나씩 목록에 삽입
                            dataset.add(data)
                        }
                        initializeList()
                    } else {
                        Toolbox.showProgressBar(_binding.list, _binding.progressBar, false)
                        Log.w(TAG, "                빈 문서")
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "                동영상 로드 실패 $e")
                    Log.e(TAG, "                              ${e.message}")
                    Log.e(TAG, "                              ${e.cause}")
                }
        }
    }

    private var list: RecyclerView? = null
    private var adapter: VideoListAdapter? = null
    private var dataset: ArrayList<VideoData> = ArrayList<VideoData>()

    private fun initializeList() {
        adapter = VideoListAdapter(this, dataset, true)
        if (adapter != null) {
            adapter!!.setOnItemClickListener(object : VideoListAdapter.OnItemClickListener {
                override fun onItemClick(v: View, position: Int) {

                    val path  = dataset[position].video
                    val title = dataset[position].title
                    val desc  = dataset[position].description
                    val userUid   = dataset[position].user
                    val username  = dataset[position].username
                    val datetime  = Toolbox.getRelativeDatetime(dataset[position].datetime.toString())
                    val userImage = MainActivity.USER_IMAGE

                    Log.d(TAG, "UserFragment [ Go to Player Activity ] title: $title")
                    Log.d(TAG, "                                       desc:  $desc")
                    Log.d(TAG, "                                       path:  $path")
                    Log.d(TAG, "                                       username:  $username")
                    Log.d(TAG, "                                       datetime:  $datetime")
                    Log.d(TAG, "                                       userImage: $userImage")
                    Log.d(TAG, "                                       userUid:   $userUid")

                    // 동영상 상세 페이지로 이동
                    val intent = Intent(application, PlayerActivity::class.java)
                        .putExtra("path",  path) // 동영상을 로드하기 위해 파일 경로 전달
                        .putExtra("title", title)
                        .putExtra("user_uid", userUid)
                        .putExtra("username", username)
                        .putExtra("datetime", datetime)
                        .putExtra("user_image",  userImage)
                        .putExtra("description", desc)
                    startActivity(intent)
                }
            })
            list = _binding.list
            if (list != null) {
                list!!.adapter = adapter
                list!!.layoutManager = LinearLayoutManager(this)

                Toolbox.showProgressBar(list!!, _binding.progressBar, false)
            } else
                Toolbox.showErrorMessage(this, "UserFragment > initializeList() / List Null", "로드 실패")
        } else
            Toolbox.showErrorMessage(this, "UserFragment > initializeList() / Adapter Null", "로드 실패")
    }










    private fun goToUploadVideo() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, UploadVideoActivity::class.java)
            startUploadVideoActivity.launch(intent)
        }
    }
    private val startUploadVideoActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val thumbDir = result.data?.getStringExtra("thumbnail")
                val datetime = result.data?.getStringExtra("datetime")
                val videoDir = result.data?.getStringExtra("video")
                val userUid  = result.data?.getStringExtra("user")
                val title = result.data?.getStringExtra("title")
                val desc  = result.data?.getStringExtra("description")

                Log.d(TAG, "UserFragment << Add Video Item / thumbDir: $thumbDir")
                Log.d(TAG, "                                 datetime: $datetime")
                Log.d(TAG, "                                 videoDir: $videoDir")
                Log.d(TAG, "                                 userUid:  $userUid")
                Log.d(TAG, "                                 title: $title")
                Log.d(TAG, "                                 desc:  $desc")
                Log.d(TAG, " ")

                if (thumbDir != null && datetime != null && title != null) {

                    val data = VideoData()
                    data.datetime    = datetime
                    data.thumbnail   = thumbDir
                    data.description = desc
                    data.title = title
                    data.user  = userUid
                    data.video = videoDir

                    dataset.add(0, data)
                    if (adapter != null) {
                        adapter!!.notifyItemInserted(0)
                    } else {
                        initializeList()
                    }
                }
            }
        }
}
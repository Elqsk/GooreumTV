package com.example.gooreumtv

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.ActivityUploadVideoBinding
import com.example.gooreumtv.databinding.ContentUploadVideoBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UploadVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadVideoBinding
    private lateinit var _binding: ContentUploadVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding  = ActivityUploadVideoBinding.inflate(layoutInflater)
        _binding = binding.content
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listenTextChanged()
        addVideo()
        upload()
    }










    private fun addVideo() {
        _binding.addVideoButton.setOnClickListener {
            checkPermissions()
        }
    }
    // 요청할 권한
    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private fun checkPermissions() {
        // 권한이 모두 수락되었는지 체크
        if (ContextCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED
        ) {
            // 동영상 로드
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType("video/*")
            getVideo.launch(intent)
        } else {
            // 하나라도 수락되지 않은 게 있으면 하나씩 수락 요청
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission.launch(permission)
                }
            }
        }
    }
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if (isGranted) {
                // 권한이 수락되면 다시 권한이 모두 수락되었는지 체크
                checkPermissions()
            } else {
                // 아니면 토스트
                Toolbox.makeToast(this, "계속하려면 권한을 허용해 주세요")
            }
        }
    private val getVideo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode == Activity.RESULT_OK) {

                videoUri = result.data!!.data
                checkBlanks()

                // 동영상 로드 성공
                if (videoUri != null) {
                    Log.d(TAG, "UploadVideoActivity > [ Add Video ] Extract Thumbnail / videoUri:    $videoUri")

                    // 임의로 섬네일 추출
                    thumbnailBitmap = Toolbox.extractThumbnailFromVideo(this, videoUri!!)

                    Log.d(TAG, "                                                        thumbBitmap: $thumbnailBitmap")

                    // 뷰에 삽입
                    Glide.with(this)
                        .load(thumbnailBitmap)
                        .into(_binding.thumbnailView)
                }
                Log.d(TAG, " ")
            }
        }

    private var videoUri: Uri? = null
    private var thumbnailBitmap: Bitmap? = null










    private fun upload() {
        _binding.uploadButton.setOnClickListener {
            Toolbox.showProgressBar(_binding.ui, _binding.progressBar, true)
            Toolbox.hideKeyboard(this, _binding.uploadButton)

            val title = _binding.titleEditText.text.toString()
            val desc  = _binding.descEditText.text.toString()
            val thumbBytes = Toolbox.bitmapToByteArray(thumbnailBitmap)
            val datetime   = Toolbox.getCurrentDatetime()
            val userUid    = CurrentUser.getUid(this)

            // 동영상 너무 커서 Storage에 따로 저장하고, 동영상 정보에는 파일 경로를 저장한다. 'video/' 폴더 안에
            // 저장한다는 뜻인데, 그냥 파일명만 쓰면 폴더 없이 파일만 저장된다. 가져올 때에는 'video/'를 붙이지
            // 않아도 된다.
            val videoDir = "videos/" + Toolbox.createFilename() + ".mp4"
            val thumbDir = "images/" + Toolbox.createFilename() + ".jpeg"

            Log.d(TAG, "UploadVideoActivity [ Upload ] title: $title")
            Log.d(TAG, "                               thumb: $thumbBytes")
            Log.d(TAG, "                               desc:  $desc")
            Log.d(TAG, "                               user:  $userUid")
            Log.d(TAG, "                               datetime: $datetime")
            Log.d(TAG, "                               videoDir: $videoDir")
            Log.d(TAG, "                               thumbDir: $thumbDir")

            if (videoUri != null && thumbBytes != null && datetime != null) {

                MyFirebase.uploadFileWithUri(videoUri!!, videoDir)
                    .addOnSuccessListener {
                        Log.d(TAG, "UploadVideoActivity [ Upload ] 동영상 업로드 성공..")

                        MyFirebase.uploadFileWithByteArray(thumbBytes, thumbDir)
                            .addOnSuccessListener {
                                Log.d(TAG, "                               섬네일 업로드 성공..")

                                // 전체 문서 개수를 함수 하나로 받아오지는 못하고, 전체 문서를 받아와서
                                // 카운트하는 방법으로 얻는다.
                                MyFirebase.getVideos()
                                    .addOnSuccessListener { documents ->
                                        // uid를 따로 만들어서 저장하는 이유는 최신 동영상 순으로 정렬해서
                                        // 가져와야 하기 때문이다. MySQL과 다르게 자동 인덱스 증가 기능이 없다.
                                        val uid = documents.size() + 1
                                        Log.d(TAG, "                               uid: $uid")

                                        // 동영상 정보 저장
                                        val data = hashMapOf(
                                            "uid"   to uid,
                                            "user"  to userUid,
                                            "title" to title,
                                            "video" to videoDir,
                                            "datetime"    to datetime,
                                            "description" to desc,
                                            "thumbnail"   to thumbDir
                                        )
                                        MyFirebase.uploadVideoWithAutoIndex(data)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Log.d(TAG, "                               동영상 정보 추가 성공..업로드 성공! $uid")

                                                    // MyVideoActivity 갱신
                                                    val intent = Intent()
                                                        .putExtra("thumbnail",   thumbDir)
                                                        .putExtra("datetime",    datetime)
                                                        .putExtra("description", desc)
                                                        .putExtra("title", title)
                                                        .putExtra("video", videoDir)
                                                        .putExtra("user",  userUid)
                                                    setResult(Activity.RESULT_OK, intent)
                                                    finish()
                                                } else {
                                                    Toolbox.showErrorMessage(
                                                        this,
                                                        "                               섬네일 저장 실패 ${task.exception}",
                                                        "업로드 실패"
                                                    )
                                                }
                                            }
                                    }.addOnFailureListener { e ->
                                        Toolbox.showErrorMessage(
                                            this,
                                            "                               전체 동영상 로드 실패 $e",
                                            "업로드 실패"
                                        )
                                    }
                            }.addOnFailureListener { e ->
                                Toolbox.showErrorMessage(this, "                               섬네일 업로드 실패 $e", "업로드 실패")
                            }
                    }.addOnFailureListener { e ->
                        Toolbox.showErrorMessage(this, "                               동영상 업로드 실패 $e", "업로드 실패")
                    }
            } else {
                Toolbox.showErrorMessage(this, "thumb: $thumbBytes, datetime: $datetime", "업로드 실패")
            }
        }
    }
//    private fun updateMyVideoActivity(thumbnail: String, datetime: String, title: String) {
//        val intent = Intent()
//            .putExtra("thumbnail", thumbnail)
//            .putExtra("datetime", datetime)
//            .putExtra("title", title)
//        setResult(Activity.RESULT_OK, intent)
//        finish()
//    }










    private fun listenTextChanged() {
        _binding.titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (_binding.titleEditText.text.toString().trim().isNotEmpty() &&
                    _binding.descEditText.text.toString().trim().isNotEmpty() &&
                    videoUri != null
                ) {
                    _binding.uploadButtonInactive.visibility = View.INVISIBLE
                    _binding.uploadButton.visibility         = View.VISIBLE
                } else {
                    _binding.uploadButtonInactive.visibility = View.VISIBLE
                    _binding.uploadButton.visibility         = View.INVISIBLE
                }
            }
        })

        _binding.descEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (_binding.titleEditText.text.toString().trim().isNotEmpty() &&
                    _binding.descEditText.text.toString().trim().isNotEmpty() &&
                    videoUri != null
                ) {
                    _binding.uploadButtonInactive.visibility = View.INVISIBLE
                    _binding.uploadButton.visibility         = View.VISIBLE
                } else {
                    _binding.uploadButtonInactive.visibility = View.VISIBLE
                    _binding.uploadButton.visibility         = View.INVISIBLE
                }
            }
        })
    }

    private fun checkBlanks() {
        if (_binding.titleEditText.text.toString().trim().isNotEmpty() &&
            _binding.descEditText.text.toString().trim().isNotEmpty() &&
            videoUri != null
        ) {
            _binding.uploadButtonInactive.visibility = View.INVISIBLE
            _binding.uploadButton.visibility         = View.VISIBLE
        } else {
            _binding.uploadButtonInactive.visibility = View.VISIBLE
            _binding.uploadButton.visibility         = View.INVISIBLE
        }
    }
}
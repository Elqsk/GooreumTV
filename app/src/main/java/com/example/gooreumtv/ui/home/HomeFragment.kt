package com.example.gooreumtv.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gooreumtv.*
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.FragmentHomeBinding
import com.google.firebase.firestore.ktx.toObject

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

//        binding.temp.setOnClickListener {
//            val intent = Intent(requireActivity(), PlayerActivity::class.java)
//            startActivity(intent)
//        }

        loadData()

        return root
    }










    private fun loadData() {
        MyFirebase.getVideos()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "HomeFragment >> 동영상 목록 로드 성공..")

                if (!documents.isEmpty) {
                    for ((i, document) in documents.withIndex()) {
                        val videoData = document.toObject<VideoData>()

                        Log.d(TAG, "                title: ${videoData.title}")
                        Log.d(TAG, "                desc:  ${videoData.description}")
                        Log.d(TAG, "                thumbDir: ${videoData.thumbnail}")
                        Log.d(TAG, "                videoDir: ${videoData.video}")
                        Log.d(TAG, "                datetime: ${videoData.datetime}")
                        Log.d(TAG, "                userUid:  ${videoData.user}")

                        // 동영상 정보에 저장된 사용자 uid로 업로드한 사용자의 이름을 불러온다. 사용자의 이름을
                        // 따로 불러오는 이유는 이름을 변경했을 때 새로 반영하기 위해서다.
                        MyFirebase.findUserWithUid(videoData.user!!)
                            .addOnSuccessListener {
                                Log.d(TAG, "                사용자 정보 로드 성공..")

                                val userData = it.toObject<UserData>()
                                videoData.username   = userData?.name
                                videoData.user_image = userData?.image

                                Log.d(TAG, "                username:   ${userData?.name}")
                                Log.d(TAG, "                user_image: ${userData?.image}")
                                Log.d(TAG, " ")

                                // 아이템 하나씩 목록에 삽입
                                dataset.add(videoData)

                                if (i == documents.size() - 1) {
                                    Log.d(TAG, "                동영상 데이터 로드 성공!")

                                    // 데이터를 서버에서 받아오는 것은 메인 스레드에서 진행되지 않는다. 동영상
                                    // 데이터만 불러온 것 까지는 좋았는데 사용자 정보를 얻기 위해 한 번 더
                                    // 서버에 갔다오면서 시간 차이가 발생한다. 따라서 데이터를 로드하는 코드가
                                    // 끝나고 리스트를 초기화하면, 아직 서버에서 데이터를 다 받아오지 않은
                                    // 상태이기 때문에, 화면에는 아무 것도 뜨지 않는다. 모든 데이터가 다 로드된
                                    // 것을 확인하고 리스트를 초기화해야 한다.
                                    // 반면, 내 동영상 액티비티에서는 어차피 다 사용자 본인의 정보인 데다가 이미
                                    // 메인 액티비티에 변수로 저장되어 있으니 추가로 또 서버에서 불러올 필요가
                                    // 없다. 따라서 동영상 데이터를 한 개씩 받아올 때 바로바로 아이템으로
                                    // 추가하면 끝이고, for문만 종료되어도 리스트를 초기화하기에는 충분하다.
                                    initializeList()
                                }
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "                사용자 정보 로드 실패 $e")
                            }
                    }
                }
            }.addOnFailureListener { e ->
                Toolbox.showErrorMessage(requireActivity(), "                동영상 로드 실패 $e", null)
            }
    }

    private lateinit var list: RecyclerView
    private lateinit var adapter: VideoListAdapter
    private var dataset: ArrayList<VideoData> = ArrayList<VideoData>()

    private fun initializeList() {
        adapter = VideoListAdapter(requireActivity(), dataset, false)
        adapter.setOnItemClickListener(object : VideoListAdapter.OnItemClickListener {
            // 동영상 상세 페이지로 이동
            override fun onItemClick(v: View, position: Int) {
                MyFirebase.downloadFileWithPath(dataset[position].user_image)
                    ?.addOnSuccessListener {
                        Log.d(TAG, "HomeFragment [ Go to Player Activity ] 사용자 이미지 로드 성공..Player Activity를 시작합니다!")

                        val path  = dataset[position].video
                        val title = dataset[position].title
                        val desc  = dataset[position].description
                        val userUid   = dataset[position].user
                        val username  = dataset[position].username
                        val datetime  = Toolbox.getRelativeDatetime(dataset[position].datetime.toString())
                        val userImage = it

                        Log.d(TAG, "                                       title: $title")
                        Log.d(TAG, "                                       desc:  $desc")
                        Log.d(TAG, "                                       path:  $path")
                        Log.d(TAG, "                                       username:  $username")
                        Log.d(TAG, "                                       userImage: $userImage")
                        Log.d(TAG, "                                       datetime:  $datetime")
                        Log.d(TAG, "                                       userUid:   $userUid")

                        val intent = Intent(context, PlayerActivity::class.java)
                            .putExtra("path",  path) // 동영상을 로드하기 위해 파일 경로 전달
                            .putExtra("title", title)
                            .putExtra("user_uid", userUid)
                            .putExtra("username", username)
                            .putExtra("datetime", datetime)
                            .putExtra("description", desc)
                            .putExtra("user_image",  userImage)
                startActivity(intent)
                    }?.addOnFailureListener { e ->
                        Log.e(TAG, "             사용자 이미지 로드 실패 $e")
                    }
            }
        })
        list = binding.list
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(requireActivity())

        Log.d(TAG, "HomeFragment >> List Initialized ${dataset.size}")
    }










    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
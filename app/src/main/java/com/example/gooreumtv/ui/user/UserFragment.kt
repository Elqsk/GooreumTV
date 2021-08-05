package com.example.gooreumtv.ui.user

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.gooreumtv.*
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.FragmentUserBinding
import com.google.firebase.firestore.ktx.toObject

class UserFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private var _binding: FragmentUserBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userViewModel =
            ViewModelProvider(this).get(UserViewModel::class.java)

        _binding = FragmentUserBinding.inflate(inflater, container, false)
        val root: View = binding.root

        checkLoginState()

        goToAccountSetting()
        goToLogin()
        goToMyVideo()

//        val textView: TextView = binding.textNotifications
//        userViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

        return root
    }

    private fun checkLoginState() {
        val uid = CurrentUser.getUid(requireActivity())

        Log.d(TAG, "UserFragment >> Check Login / uid: $uid")

        // 로그인 되어 있음
        if (uid != null) {
            // 매번 서버에서 데이터를 가져오지 않기 위해, 앱을 실행 중인 동안에는 액티비티에 변수로 저장해 놓고 사용한다.
            if (MainActivity.USER_EMAIL != null && MainActivity.USER_IMAGE != null && MainActivity.USER_NAME != null) {
                updateUI(
                    MainActivity.USER_NAME,
                    MainActivity.USER_IMAGE!!
                )
            } else {
                showProgressBar(true)

                // 사용자 정보 로드
                MyFirebase.getMyData(uid)
                    .addOnSuccessListener { document ->
                        Log.d(TAG, "                사용자 정보 로드 성공..")

                        val data = document.toObject<UserData>()
                        if (data != null) {

                            val name  = data.name
                            val email = data.email

                            Log.d(TAG, "                              name:  $name")
                            Log.d(TAG, "                              email: $email")
                            Log.d(TAG, "                              image: ${data.image}")

                            // 프로필 사진 다운로드
                            MyFirebase.downloadFileWithPath(data.image)
                                ?.addOnSuccessListener {
                                    if (it != null) {
                                        Log.d(TAG, "                프로필 사진 다운로드 성공..로그인 성공!")

                                        // 앱을 실행중인 동안에는 매번 사용자 화면으로 돌아와도 서버에서
                                        // 받아오지 않고 변수에 저장해놓고 쓴다.
                                        MainActivity.USER_IMAGE = it
                                        MainActivity.USER_EMAIL = email
                                        MainActivity.USER_NAME  = name

                                        updateUI(name, it)

                                        Log.d(TAG, "                              bytes:  $it")
                                        Log.d(TAG, " ")
                                    }
                                }?.addOnFailureListener { e ->
                                    showErrorMessage("                이미지 다운로드 실패 $e")
                                    Log.e(TAG, "                이미지 다운로드 실패 ${e.cause}")
                                    Log.e(TAG, "                이미지 다운로드 실패 ${e.message}")
                                }
                        }
                    }.addOnFailureListener { e ->
                        showErrorMessage("사용자 정보 로드 실패 $e")
                    }
            }
        } else {
            // 로그인 상태 아님
            // UI 게스트 모드
            binding.guestInterface.visibility = View.VISIBLE
        }
    }

    private fun updateUI(name: String?, image: ByteArray) {
        binding.nameView.text = name
        Glide.with(this)
            .load(image)
            .circleCrop()
            .into(binding.imageView)

        showProgressBar(false)
        binding.membershipInterface.visibility = View.VISIBLE
    }










    private fun goToMyVideo() {
        binding.myVideoButton.setOnClickListener {
            val intent = Intent(activity, MyVideoActivity::class.java)
            startActivity(intent)
        }
    }










    private fun goToLogin() {
        binding.goToLoginButton.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)

            startSignInActivity.launch(intent)
            // 로그인 액티비티 시작
        }
    }
    private val startSignInActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            Log.d(TAG, "UserFragment > [ login ] 후")

        if (result.resultCode == Activity.RESULT_OK) {
            // 로그인 액티비티 종료
            val uid  = result.data?.getStringExtra("uid")
            val name = result.data?.getStringExtra("name")
            val imageBytes = result.data?.getByteArrayExtra("image")

            Log.d(TAG, "UserFragment > [ login ] / uid:   $uid")
            Log.d(TAG, "                           image: $imageBytes")
            Log.d(TAG, "                           name:  $name")
            Log.d(TAG, " ")

            val imageBitmap = Toolbox.byteArrayToBitmap(imageBytes)

            if (uid != null && name != null) {
                // ▽ 로그인 성공 시 각 뷰에 사용자 정보 설정
                Glide.with(this)
                    .load(imageBitmap)
                    .circleCrop()
                    .into(binding.imageView)

                binding.nameView.text = name

                binding.membershipInterface.visibility = View.VISIBLE
                binding.guestInterface.visibility = View.INVISIBLE
            }
        }
    }










    private fun goToAccountSetting() {
        binding.accountSettingButton.setOnClickListener {
            val intent = Intent(activity, AccountSettingActivity::class.java)

            startAccountSettingActivity.launch(intent)
            // 계정 설정 액티비티 시작
        }
    }
    private val startAccountSettingActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        // ▽ 로그아웃 시 게스트 모드로 UI 변경
        if (result.data?.getBooleanExtra("login", true) == false) {
            binding.membershipInterface.visibility = View.INVISIBLE
            binding.guestInterface.visibility = View.VISIBLE
        }
    }










    private fun showProgressBar(visible: Boolean) {
        if (visible) {
            binding.guestInterface.visibility = View.INVISIBLE
            binding.membershipInterface.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun showErrorMessage(log: String?) {
        if (log != null) {
            Log.e(TAG, "               $log")
        }
    }










    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
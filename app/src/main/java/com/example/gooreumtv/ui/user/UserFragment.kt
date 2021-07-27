package com.example.gooreumtv.ui.user

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.gooreumtv.*
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.FragmentUserBinding
import com.example.gooreumtv.ui.register.RegisterFragment
import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder

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

        goToAccountSetting()
        goToLogin()

        setClickListeners()
        checkLoginState()

//        val textView: TextView = binding.textNotifications
//        userViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

        return root
    }

    private fun checkLoginState() {

        val session = requireActivity().getSharedPreferences("session", AppCompatActivity.MODE_PRIVATE)
        if (session != null) {

            val userIndex = session.getInt("user", 0)

            Log.d(MainActivity.TAG, "UserFragment > User index: $userIndex")
            Log.d(MainActivity.TAG, " ")

            // 로그인 되어 있음
            if (userIndex > 0) {
                binding.guestInterface.visibility = View.INVISIBLE

                val usersDB = requireActivity().getSharedPreferences("users", AppCompatActivity.MODE_PRIVATE)
                if (usersDB != null) {
                    val value = usersDB.getString(userIndex.toString(), null)

                    val token: TypeToken<User> = object : TypeToken<User>() {}
                    val gson = GsonBuilder().create()

                    val user: User = gson.fromJson(value, token.type)

                    val imageUri = user.imageUri
                    val name = user.name

                    Glide.with(this)
                        .load(imageUri)
                        .circleCrop()
                        .into(binding.imageView)
                    binding.nameView.text = name
                }
                binding.membershipInterface.visibility = View.VISIBLE
            }
            // 로그인 상태 아님
            else {
                binding.membershipInterface.visibility = View.INVISIBLE
                binding.guestInterface.visibility = View.VISIBLE
            }
        }
    }

    private fun setClickListeners() {
        binding.myVideoButton.setOnClickListener {
            val intent = Intent(activity, MyVideoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToLogin() {
        binding.goToLoginButton.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)

            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // 로그인 액티비티 종료
                    val uid  = result.data?.getStringExtra("uid")
                    val name = result.data?.getStringExtra("name")
                    val imageString = result.data?.getStringExtra("image")

                    Log.d(TAG, "UserFragment > [ login ] / uid:   $uid")
                    Log.d(TAG, "                           image: $imageString")
                    Log.d(TAG, "                           name:  $name")
                    Log.d(TAG, " ")

                    if (uid != null && imageString != null && name != null) {
                        // ▽ 로그인 성공 시 각 뷰에 사용자 정보 설정
                        Glide.with(this)
                            .load(imageString)
                            .circleCrop()
                            .into(binding.imageView)

                        binding.nameView.text = name

                        binding.membershipInterface.visibility = View.VISIBLE
                        binding.guestInterface.visibility = View.INVISIBLE
                    }
                }
            }.launch(intent)
            // 로그인 액티비티 시작
        }
    }

    private fun goToAccountSetting() {
        binding.accountSettingButton.setOnClickListener {
            val intent = Intent(activity, AccountSettingActivity::class.java)

            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result ->
                // ▽ 로그아웃 시 게스트 모드로 UI 변경
                if (result.data?.getBooleanExtra("login", true) == false) {
                    binding.membershipInterface.visibility = View.INVISIBLE
                    binding.guestInterface.visibility = View.VISIBLE
                }
            }.launch(intent)
            // 계정 설정 액티비티 시작
        }
    }

    private val getLoginContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val userIndex = result.data?.getIntExtra("user", 0)

                Log.d(MainActivity.TAG, "UserFragment > registerForActivityResult / userIndex: $userIndex")

                val usersDB = requireActivity().getSharedPreferences("users", AppCompatActivity.MODE_PRIVATE)
                if (usersDB != null && usersDB.all.isNotEmpty()) {
                    val token: TypeToken<User> = object : TypeToken<User>() {}
                    val gson = GsonBuilder().create()

                    val value = usersDB.getString(userIndex.toString(), null)
                    val user: User = gson.fromJson(value, token.type)
                    Glide.with(this)
                        .load(Uri.parse(user.imageUri.toString()))
                        .circleCrop()
                        .into(binding.imageView)
                    binding.nameView.text = user.name

                    Log.d(MainActivity.TAG, "UserFragment > registerForActivityResult / imageUri: ${user.imageUri}")
                    Log.d(MainActivity.TAG, "                                           name:     ${user.name}")
                    Log.d(MainActivity.TAG, " ")

                    binding.membershipInterface.visibility = View.VISIBLE
                    binding.guestInterface.visibility = View.INVISIBLE
                }
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.gooreumtv.ui.login

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.gooreumtv.*
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.FragmentLoginBinding
import com.google.firebase.firestore.ktx.toObject

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addTextChangedListener()

        binding.goToRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_LogoutFragment)
        }
        login()
    }










    private fun login() {
        binding.loginButton.setOnClickListener {
            showProgressBar(true)
            Toolbox.hideKeyboard(requireActivity(), binding.loginButton)

            val email    = binding.idEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            MyFirebase.findUserWithEmail(email)
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        Log.d(TAG, "LoginFragment > [ login ] / 이메일 있음..")

                        val uid  = document.id
                        val user = document.toObject<User>()
                        val name = user.name
                        val pass = user.password
                        Log.d(TAG, "                            uid:   $uid")
                        Log.d(TAG, "                            name:  $name")
                        Log.d(TAG, "                            image: ${user.image}")

                        // 비밀번호 까지 일치하는지 확인
                        if (password == pass) {
                            Log.d(TAG, "                            비밀번호 일치함..")

                            // 프로필 사진 다운로드
                            MyFirebase.downloadUserImage(user.image)
                                ?.addOnSuccessListener {
                                    Log.d(TAG, "                            프로필 사진 다운로드 성공..")

                                    // Firebase 로그인
                                    MyFirebase.signIn(email, password).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d(TAG, "                            로그인 성공! ")

                                            // 로컬에 로그인 상태 저장
                                            CurrentUser.signIn(requireActivity(), uid)

                                            Log.d(TAG, "                            bytes: $it")
                                            Log.d(TAG, "                            size:  ${it.size}")
                                            Log.d(TAG, " ")

                                            updateUserFragment(uid, it, name)
                                        } else {
                                            showErrorMessage("Firebase 로그인 실패 ${task.exception}", "로그인 실패")
                                        }
                                    }
                                }?.addOnFailureListener { e ->
                                    showErrorMessage("이미지 다운로드 실패 $e", "로그인 실패")
                                }
                        } else {
                            showErrorMessage("비밀번호 불일치", "계정을 찾을 수 없습니다")
                        }
                    }
                }.addOnFailureListener { exception ->
                    showErrorMessage("이메일 없음 $exception", "계정을 찾을 수 없습니다")
                }
        }
    }

    private fun updateUserFragment(uid: String, image: ByteArray, name: String?) {
        val intent = Intent()
            .putExtra("uid",   uid)
            .putExtra("image", image)
            .putExtra("name",  name)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()

        Log.d(TAG, "                            updateUserFragment() / finish()")
    }










    private fun showErrorMessage(log: String?, toast: String?) {
        if (log != null) {
            Log.e(TAG, "                            $log")
        }
        if (toast != null) {
            Toolbox.makeToast(requireActivity(), toast)
        }
        showProgressBar(false)
    }

    private fun showProgressBar(visible: Boolean) {
        if (visible) {
            binding.ui.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.ui.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun addTextChangedListener() {
        binding.idEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty()
                ) {
                    binding.loginButton.visibility = View.VISIBLE
                    binding.loginButtonInactive.visibility = View.INVISIBLE
                } else {
                    binding.loginButton.visibility = View.INVISIBLE
                    binding.loginButtonInactive.visibility = View.VISIBLE
                }
            }
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty()
                ) {
                    binding.loginButton.visibility = View.VISIBLE
                    binding.loginButtonInactive.visibility = View.INVISIBLE
                } else {
                    binding.loginButton.visibility = View.INVISIBLE
                    binding.loginButtonInactive.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
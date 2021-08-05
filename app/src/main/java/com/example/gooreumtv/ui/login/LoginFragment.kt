package com.example.gooreumtv.ui.login

import android.app.Activity
import android.content.Intent
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
            Toolbox.showProgressBar(binding.ui, binding.progressBar, true)
            Toolbox.hideKeyboard(requireActivity(), binding.loginButton)

            val email    = binding.idEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // 이메일을 검색해서 가입된 사용자인지 확인
            MyFirebase.findUserWithEmail(email)
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "LoginFragment > [ login ] / 사용자 검색 성공..")

                    if (!documents.isEmpty) {
                        Log.d(TAG, "                이메일 있음..")

                        for (document in documents) {
                            val data = document.toObject<UserData>()

                            val uid  = document.id
                            val name = data.name
                            val pass = data.password
                            Log.d(TAG, "                            uid:   $uid")
                            Log.d(TAG, "                            name:  $name")
                            Log.d(TAG, "                            image: ${data.image}")

                            // 비밀번호 까지 일치하는지 확인
                            if (password == pass) {
                                Log.d(TAG, "                비밀번호 일치함..")

                                // 프로필 사진 다운로드
                                MyFirebase.downloadFileWithPath(data.image)
                                    ?.addOnSuccessListener {
                                        Log.d(TAG, "                프로필 사진 다운로드 성공..")

                                        // Firebase 로그인
                                        MyFirebase.signIn(email, password).addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Log.d(TAG, "                로그인 성공! ")

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
                    } else {
                        Toolbox.showProgressBar(binding.ui, binding.progressBar, false)
                        Toolbox.makeToast(requireActivity(), "계정을 찾을 수 없습니다")
                        Log.w(TAG, "                빈 문서")
                    }
                }.addOnFailureListener { e ->
                    showErrorMessage("사용자 검색 실패 $e", "로그인 오류")
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
        Toolbox.showProgressBar(binding.ui, binding.progressBar, false)
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
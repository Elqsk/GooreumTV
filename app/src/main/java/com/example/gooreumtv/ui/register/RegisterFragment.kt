package com.example.gooreumtv.ui.register

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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.gooreumtv.*
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.databinding.FragmentRegisterBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goToSignIn()

        listenTextChanged()
        addImage()
        signUp()
    }











    private fun goToSignIn() {
        binding.goToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_LogoutFragment_to_LoginFragment)
        }
    }











    private var imageUri: Uri? = null

    private fun addImage() {
        binding.profileImageView.setOnClickListener {
            // 권한이 모두 수락되어야 이미지를 가져올 수 있다
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
        if (ContextCompat.checkSelfPermission(requireActivity(), permissions[0]) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireActivity(), permissions[1]) == PackageManager.PERMISSION_GRANTED
        ) {
            // 이미지 로드
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            getImage.launch(intent)
        } else {
            // 하나라도 수락되지 않은 게 있으면 하나씩 수락 요청
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if (isGranted) {
                // 권한이 수락되면 다시 권한이 모두 수락되었는지 체크
                checkPermissions()
            } else {
                // 아니면 토스트
                Toolbox.makeToast(requireActivity(), "계속하려면 권한을 허용해 주세요")
            }
        }
    private val getImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {

            imageUri = result.data!!.data
            // 이미지 로드 성공
            if (imageUri != null) {
                Log.d(TAG, "RegisterFragment > [ addImage ] / uri: $imageUri")

                // 뷰에 삽입
                Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(binding.profileImageView)
            }
            Log.d(TAG, " ")
        }
    }











    private fun signUp() {
        binding.registerButton.setOnClickListener {
            showProgressBar(true)
            Toolbox.hideKeyboard(requireActivity(), binding.registerButton)

            val email    = binding.idEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val name     = binding.nameEditText.text.toString()
            val datetime = Toolbox.getCurrentDatetime()

            val bitmap  = Toolbox.imageUriToBitmap(requireActivity(), imageUri)
            val resized = Toolbox.resizeBitmap(bitmap!!, 500, 500)
            val bytes   = Toolbox.bitmapToByteArray(resized)

            // 이미지는 너무 커서 Storage에 따로 저장하고, 사용자 정보에는 파일 경로를 저장한다. 'images/' 폴더
            // 안에 저장한다는 뜻인데, 그냥 파일명만 쓰면 폴더 없이 파일만 저장된다. 이미지를 선택하지 않은 경우에는
            // 기본 이미지로 대체한다. 가져올 때에는 'images/'를 붙이지 않아도 된다.
            val dir      = "images/" +
                    if (imageUri == null) "user.png"
                    else Toolbox.createFilename() + ".jpeg"

            Log.d(TAG, "RegisterFragment > [ Sign Up ] / email:    $email")
            Log.d(TAG, "                                 password: $password")
            Log.d(TAG, "                                 name:     $name")
            Log.d(TAG, "                                 datetime: $datetime")
            Log.d(TAG, "                                 dir:      $dir")

            // 중복(사용할 수 있는 계정인지) 검사
            MyFirebase.findUserWithEmail(email)
                .addOnSuccessListener { document ->
                    if (document.isEmpty) {
                        Log.d(TAG, "                                  사용할 수 있는 계정..")

                        // Storage를 이용하려면 해당 서비스의 로그인 & 가입과 별개로 Firebase 계정 인증을 해야
                        // 한다.
                        MyFirebase.authenticate(email, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "                                  Firbase 인증 성공..")

                                if (bytes != null) {
                                    // 이미지 업로드
                                    MyFirebase.uploadFileWithByteArray(bytes, dir)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "                                  이미지 업로드 성공..")

                                            // 서버(Firestore)에 사용자 정보 저장
                                            if (datetime != null) {
                                                addUser(email, password, name, dir, datetime)
                                            } else {
                                                showErrorMessage("날짜: $datetime", "가입 실패")
                                            }
                                        }.addOnFailureListener { e ->
                                            showErrorMessage("이미지 업로드 실패 $e", "가입 실패")
                                        }
                                } else {
                                    // 기본 이미지가 들어가는 경우, 이미지 업로드 과정 없이 바로 서버(Firestore)에
                                    // 사용자 정보 저장
                                    if (datetime != null) {
                                        addUser(email, password, name, dir, datetime)
                                    } else {
                                        showErrorMessage("날짜: $datetime", "가입 실패")
                                    }
                                }
                            } else {
                                showErrorMessage("Firebase 인증 실패 ${task.exception}", "가입 실패")
                            }
                        }
                    } else {
                        showErrorMessage("이메일 중복", "사용할 수 없는 계정입니다")
                    }
                }.addOnFailureListener { e ->
                    showErrorMessage("중복 검사 실패 $e", "가입 실패")
                }
        }
    }
    private fun addUser(email: String, password: String, name: String, image: String, datetime: String) {
        val user = hashMapOf(
            "email"    to email,
            "password" to password,
            "name"     to name,
            "image"    to image,
            "datetime" to datetime
        )
        MyFirebase.signUp(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "                                  사용자 추가 성공..")

                // Firbase 로그인
                MyFirebase.signIn(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "                                  회원 가입 성공!")

                            val uid = documentReference.id
                            // 로컬에 로그인 상태 저장
                            CurrentUser.signIn(requireActivity(), uid)

                            Log.d(TAG, "RegisterFragment > [ Register ] / uid from server: ${documentReference.id}")
                            Log.d(TAG, "                                  uid in local:    ${CurrentUser.getUid(requireActivity())}")
                            Log.d(TAG, " ")

                            val bitmap  = Toolbox.imageUriToBitmap(requireActivity(), imageUri)
                            val resized = Toolbox.resizeBitmap(bitmap!!, 500, 500)
                            val bytes   = Toolbox.bitmapToByteArray(resized)

                            updateUserFragment(uid, bytes, name)
                        } else{
                            showErrorMessage("Firebase 로그인 실패 ${task.exception}", "가입 실패")
                        }
                    }
            }.addOnFailureListener { e ->
                showErrorMessage("사용자 추가 실패 $e", "가입 실패")
            }
    }

    private fun updateUserFragment(uid: String, image: ByteArray?, name: String) {
        val intent = Intent()
            .putExtra("uid", uid)
            .putExtra("image", image)
            .putExtra("name", name)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
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

    private fun showErrorMessage(log: String?, toast: String?) {
        if (log != null) {
            Log.e(TAG, "                                  $log")
        }
        if (toast != null) {
            Toolbox.makeToast(requireActivity(), toast)
        }
        showProgressBar(false)
    }

    private fun listenTextChanged() {
        binding.idEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.length() >= 6 &&
                    binding.nameEditText.text.toString().trim().isNotEmpty()
                ) {
                    binding.registerButton.visibility = View.VISIBLE
                    binding.registerButtonInactive.visibility = View.INVISIBLE
                } else {
                    binding.registerButton.visibility = View.INVISIBLE
                    binding.registerButtonInactive.visibility = View.VISIBLE
                }
            }
        })

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.length() >= 6 &&
                    binding.nameEditText.text.toString().trim().isNotEmpty()
                ) {
                    binding.registerButton.visibility = View.VISIBLE
                    binding.registerButtonInactive.visibility = View.INVISIBLE
                } else {
                    binding.registerButton.visibility = View.INVISIBLE
                    binding.registerButtonInactive.visibility = View.VISIBLE
                }
            }
        })

        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty() &&
                    binding.nameEditText.text.toString().trim().isNotEmpty()
                ) {

                    Log.d(TAG, "RegisterFragment > afterTextChanged() / id:       ${binding.idEditText.text}")
                    Log.d(TAG, "                                        password: ${binding.passwordEditText.text}")
                    Log.d(TAG, "                                        name:     ${binding.nameEditText.text}")
                    Log.d(TAG, " ")

                    binding.registerButton.visibility = View.VISIBLE
                    binding.registerButtonInactive.visibility = View.INVISIBLE
                } else {
                    binding.registerButton.visibility = View.INVISIBLE
                    binding.registerButtonInactive.visibility = View.VISIBLE
                }
            }
        })
    }











    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
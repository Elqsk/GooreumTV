package com.example.gooreumtv.ui.register

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.gooreumtv.MainActivity.Companion.TAG
import com.example.gooreumtv.PlayerActivity
import com.example.gooreumtv.R
import com.example.gooreumtv.User
import com.example.gooreumtv.databinding.FragmentRegisterBinding
import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    // 사용자 DB의 사이즈가 곧 마지막 인덱스를 의미하고, 여기서 1을 더해 새로 가입한 사용자의 인덱스로 사용한다.
    private var lastIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.goToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_LogoutFragment_to_LoginFragment)
        }

        binding.profileImageView.setOnClickListener {
            // https://youngest-programming.tistory.com/517
            val intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
            startActivityForResult(intent, gallery)
        }

        addTextChangedListener()
        binding.registerButton.setOnClickListener {
            if (filledAllBlanks) {

                val email = binding.idEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                val name = binding.nameEditText.text.toString()

                Log.d(TAG, "RegisterFragment > Register Button Clicked / email:    $email")
                Log.d(TAG, "                                             password: $password")
                Log.d(TAG, "                                             name:     $name")
                Log.d(TAG, "                                             imageUri: ${imageUri.toString()}")

                val user = User(email, password, name, imageUri.toString())

                val token: TypeToken<User> = object : TypeToken<User>() {}
                val gson = GsonBuilder().create()

                val key = lastIndex + 1
                val value = gson.toJson(user, token.type)

                Log.d(TAG, "                                             key: $key")
                Log.d(TAG, "                                             value: $value")

                val usersDB = requireActivity().getSharedPreferences("users", AppCompatActivity.MODE_PRIVATE)
                if (usersDB != null) {
                    val usersDBEditor = usersDB.edit()
                    usersDBEditor.putString(key.toString(), value)
                    usersDBEditor.apply()

                    Log.d(TAG, "                                             Successfully registered! ${usersDB.getString(key.toString(), null)}")
                }

                val session = requireActivity().getSharedPreferences("session", AppCompatActivity.MODE_PRIVATE)
                if (session != null) {
                    val sessionEditor = session.edit()
                    sessionEditor.putInt("user", key)
                    sessionEditor.apply()

                    Log.d(TAG, "                                             The activity will be finished..${session.getInt("user", 0)}")
                }
                Log.d(TAG, " ")

                val intent = Intent()
                intent.putExtra("user", key)
                intent.putExtra("image", imageUri)
                intent.putExtra("name", name)
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            }
        }
    }

    private val gallery = 1
    private var imageUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == gallery) {
                if (data != null) {
                    imageUri = data.data

                    Log.d(TAG, "RegisterFragment > onActivityResult() / uri: $imageUri")
                    Log.d(TAG, " ")

                    Glide.with(this)
                        .load(imageUri)
                        .circleCrop()
                        .into(binding.profileImageView)
                }
            }
        }
    }

    private var filledAllBlanks = false

    private fun addTextChangedListener() {
        binding.idEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty() &&
                    binding.nameEditText.text.toString().trim().isNotEmpty()) {

                    filledAllBlanks = true

                    binding.registerButton.visibility = View.VISIBLE
                    binding.registerButtonInactive.visibility = View.INVISIBLE
                } else {
                    filledAllBlanks = false

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
                    binding.nameEditText.text.toString().trim().isNotEmpty()) {

                    Log.d(TAG, "RegisterFragment > afterTextChanged() / id:       ${binding.idEditText.text}")
                    Log.d(TAG, "                                        password: ${binding.passwordEditText.text}")
                    Log.d(TAG, "                                        name:     ${binding.nameEditText.text}")
                    Log.d(TAG, " ")

                    filledAllBlanks = true

                    binding.registerButton.visibility = View.VISIBLE
                    binding.registerButtonInactive.visibility = View.INVISIBLE
                } else {
                    filledAllBlanks = false

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
                    binding.nameEditText.text.toString().trim().isNotEmpty()) {

                    Log.d(TAG, "RegisterFragment > afterTextChanged() / id:       ${binding.idEditText.text}")
                    Log.d(TAG, "                                        password: ${binding.passwordEditText.text}")
                    Log.d(TAG, "                                        name:     ${binding.nameEditText.text}")
                    Log.d(TAG, " ")

                    filledAllBlanks = true

                    binding.registerButton.visibility = View.VISIBLE
                    binding.registerButtonInactive.visibility = View.INVISIBLE
                } else {
                    filledAllBlanks = false

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
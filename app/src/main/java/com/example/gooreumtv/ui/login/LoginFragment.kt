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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.example.gooreumtv.MainActivity
import com.example.gooreumtv.R
import com.example.gooreumtv.User
import com.example.gooreumtv.databinding.FragmentLoginBinding
import com.example.gooreumtv.ui.register.RegisterFragment
import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder

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

    private var metRequirements = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addTextChangedListener()

        binding.goToRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_LogoutFragment)
        }

        binding.loginButton.setOnClickListener {
            if (metRequirements) {

                val session = requireActivity().getSharedPreferences("session", AppCompatActivity.MODE_PRIVATE)
                if (session != null) {

                    var key = 0

                    // 해당 사용자 정보가 존재하는지 검색
                    val usersDB = requireActivity().getSharedPreferences("users", AppCompatActivity.MODE_PRIVATE)
                    if (usersDB != null && usersDB.all.isNotEmpty()) {
                        for (i in 1..usersDB.all.size) {
                            val token: TypeToken<User> = object : TypeToken<User>() {}
                            val gson = GsonBuilder().create()

                            val value = usersDB.getString(i.toString(), null)

                            Log.d(MainActivity.TAG, "LoginFragment > size:  ${usersDB.all.size}")
                            Log.d(MainActivity.TAG, "                value: $value")

                            val user: User = gson.fromJson(value, token.type)

                            Log.d(MainActivity.TAG, "LoginFragment > user: $user")

                            if (user.email == binding.idEditText.text.toString()) {

                                Log.d(MainActivity.TAG, "LoginFragment > The email exists!")

                                if (user.password == binding.passwordEditText.text.toString()) {

                                    Log.d(MainActivity.TAG, "LoginFragment > Account checked! key: $key")

                                    key = i
                                    break
                                } else {
                                    Log.d(MainActivity.TAG, "LoginFragment > Password not correct..")
                                }
                            } else {
                                Log.d(MainActivity.TAG, "LoginFragment > The email doesn't exists..${user.email} =? ${binding.idEditText.text}")
                            }
                        }
                    }
                    // 로그인 성공
                    if (key > 0) {
                        val sessionEditor = session.edit()
                        sessionEditor.putInt("user", key)
                        sessionEditor.apply()

                        val intent = Intent()
                        intent.putExtra("user", key)
                        requireActivity().setResult(Activity.RESULT_OK, intent)
                        requireActivity().finish()

                        Log.d(MainActivity.TAG, "LoginFragment > Login succeed! - ${session.getInt("user", 0)}")
                    } else {
                        Toast.makeText(activity, "로그인 실패", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun addTextChangedListener() {
        binding.idEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (binding.idEditText.text.toString().trim().isNotEmpty() &&
                    binding.passwordEditText.text.toString().trim().isNotEmpty()) {

                    metRequirements = true

                    binding.loginButton.visibility = View.VISIBLE
                    binding.loginButtonInactive.visibility = View.INVISIBLE
                } else {
                    metRequirements = false

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
                    binding.passwordEditText.text.toString().trim().isNotEmpty()) {

                    metRequirements = true

                    binding.loginButton.visibility = View.VISIBLE
                    binding.loginButtonInactive.visibility = View.INVISIBLE
                } else {
                    metRequirements = false

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
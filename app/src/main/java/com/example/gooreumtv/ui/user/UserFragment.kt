package com.example.gooreumtv.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.gooreumtv.AccountSettingActivity
import com.example.gooreumtv.LoginActivity
import com.example.gooreumtv.MyVideoActivity
import com.example.gooreumtv.databinding.FragmentUserBinding

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
    ): View? {
        userViewModel =
            ViewModelProvider(this).get(UserViewModel::class.java)

        _binding = FragmentUserBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val goToLoginButton = binding.goToLoginButton
        goToLoginButton.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        val myVideoButton = binding.myVideoButton
        myVideoButton.setOnClickListener {
            val intent = Intent(activity, MyVideoActivity::class.java)
            startActivity(intent)
        }

        val accountSettingButton = binding.accountSettingButton
        accountSettingButton.setOnClickListener {
            val intent = Intent(activity, AccountSettingActivity::class.java)
            startActivity(intent)
        }

//        val textView: TextView = binding.textNotifications
//        userViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
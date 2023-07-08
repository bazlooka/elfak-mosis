package elfak.mosis.caching.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import elfak.mosis.caching.R
import elfak.mosis.caching.data.NewUser
import elfak.mosis.caching.data.User
import elfak.mosis.caching.databinding.FragmentRegisterBinding
import elfak.mosis.caching.model.RegisterViewModel

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener("requestPhoto") { _, bundle ->
            val result = bundle.getString("photoUri")
            val msg = "Slika učitana!"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            Glide.with(requireContext()).load(result).apply(
                RequestOptions.circleCropTransform()
            ).into(binding.imageView2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = Firebase.database
        auth = Firebase.auth
        binding.imageView2.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_cameraFragment)
        }
        binding.btnRegister.setOnClickListener { onRegisterClicked() }
    }

    private fun onRegisterClicked() {
        Toast.makeText(
            requireContext(),
            viewModel.newUser.value!!.firstName,
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun register(newUser: NewUser) {
        auth.createUserWithEmailAndPassword(newUser.username, newUser.password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val authUser = auth.currentUser!!
                    val photoUrl = "/"
                    val user = User(
                        authUser.uid,
                        authUser.email!!,
                        newUser.firstName,
                        newUser.lastName,
                        newUser.phoneNumber,
                        photoUrl
                    )
                    val userRef = database.getReference("users").child(user.uid)
                    userRef.setValue(user)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

}
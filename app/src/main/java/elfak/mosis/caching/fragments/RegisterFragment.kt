package elfak.mosis.caching.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import elfak.mosis.caching.R
import elfak.mosis.caching.data.NewUser
import elfak.mosis.caching.data.User
import elfak.mosis.caching.databinding.FragmentRegisterBinding
import elfak.mosis.caching.model.RegisterViewModel
import java.util.UUID

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private var imageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(CameraFragment.REQUEST_PHOTO) { _, bundle ->
            val result = bundle.getString(CameraFragment.PHOTO_URI)
            Glide.with(requireContext()).load(result).apply(
                RequestOptions.circleCropTransform()
            ).into(binding.imageView2)
            imageUri = result
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

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Registracija"

        database = Firebase.database
        auth = Firebase.auth
        binding.imageView2.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_cameraFragment)
        }
        binding.btnRegister.setOnClickListener { onRegisterClicked() }
    }

    private fun onRegisterClicked() {
        val username = binding.editTextText.text.toString()
        val password1 = binding.editTextTextPassword2.text.toString()
        val password2 = binding.editTextTextPassword3.text.toString()
        val firstName = binding.editTextText3.text.toString()
        val lastName = binding.editTextText4.text.toString()
        val phoneNumber = binding.editTextPhone.text.toString()
        val imageUri = imageUri

        if (username.isEmpty() || password1.isEmpty() || password2.isEmpty()
            || firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty()
            || imageUri == null
        ) {
            Toast.makeText(
                requireContext(),
                "Popunite sva polja",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (password1 != password2) {
            Toast.makeText(
                requireContext(),
                "Lozinke se ne podudaraju",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        binding.pbRegister.visibility = View.VISIBLE
        binding.editTextText.visibility = View.GONE
        binding.editTextTextPassword2.visibility = View.GONE
        binding.editTextTextPassword3.visibility = View.GONE
        binding.editTextText3.visibility = View.GONE
        binding.editTextText4.visibility = View.GONE
        binding.editTextPhone.visibility = View.GONE
        binding.imageView2.visibility = View.GONE
        binding.btnRegister.visibility = View.GONE

        register(NewUser(username, firstName, lastName, phoneNumber, imageUri, password1))
    }

    private fun register(newUser: NewUser) {
        auth.signOut()
        auth.createUserWithEmailAndPassword(newUser.username, newUser.password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val uid = task.result.user?.uid
                    if (uid != null) {
                        val fileName = "p-" + UUID.randomUUID() + ".jpg"
                        val imagePath = "users/$uid/$fileName"

                        val photoRef: StorageReference = Firebase.storage.reference.child(imagePath)
                        val photoLocalUri = Uri.parse(newUser.localPhotoPath)
                        photoRef.putFile(photoLocalUri).addOnSuccessListener {
                            val userRef = database.getReference("users/$uid")
                            userRef.setValue(
                                User(
                                    uid,
                                    newUser.username,
                                    newUser.firstName,
                                    newUser.lastName,
                                    newUser.phoneNumber,
                                    imagePath,
                                    0
                                )
                            ).addOnSuccessListener {
                                findNavController().navigate(R.id.action_registerFragment_to_mapFragment)
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Neuspešna registracija.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }
}
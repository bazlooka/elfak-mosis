package elfak.mosis.caching.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import elfak.mosis.caching.data.User
import elfak.mosis.caching.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database =
            Firebase.database("https://mosiscaching-default-rtdb.europe-west1.firebasedatabase.app")
        auth = Firebase.auth
        storage = Firebase.storage

        binding.button.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()

            Firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        //updateUI(user)
                        if (user != null) {
                            Toast.makeText(
                                requireContext(),
                                "Authentication successful. ${user.uid}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
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

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (auth.uid != null) {
                val myRef = database.getReference("users").child(currentUser.uid)

                val user = User(
                    "bazlooka", "Luka",
                    "Kocić", "0655555", "/skdod.jpg"
                )
                myRef.setValue(user)
            }

            Toast.makeText(
                requireContext(),
                "Current user: ${currentUser.displayName}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

}
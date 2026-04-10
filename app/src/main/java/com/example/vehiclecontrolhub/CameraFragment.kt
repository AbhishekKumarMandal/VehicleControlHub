package com.example.vehiclecontrolhub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {

    private val tag = "CameraFragment"

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!! // This is a property with a custom getter.

    // binding is NOT assigned any value here
    // It is a getter, not a variable with stored value.

    // Non-null assertion operator (!!)
    // Force Kotlin to treat nullable as non-null
    // If _binding is not null → return value
    // If _binding is null → crash
    // it is equivalent to
    /*
    if (_binding == null) {
        throw NullPointerException()
    } else {
       return _binding
    }
    */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLaunchCamera.setOnClickListener {
            launchCameraApp()
        }
    }

    private fun launchCameraApp() {
        try {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

            val resolvedActivity = cameraIntent.resolveActivity(requireActivity().packageManager)

            if (resolvedActivity != null) {
                Log.i(tag, "Launching camera app: $resolvedActivity")
                binding.tvCameraStatus.text = "Launching camera app..."
                startActivity(cameraIntent)
            } else {
                Log.w(tag, "No compatible camera app available")
                binding.tvCameraStatus.text = "No compatible camera app available on this image"
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(tag, "Camera activity not found", e)
            binding.tvCameraStatus.text = "Camera app not found"
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch camera app", e)
            binding.tvCameraStatus.text = "Failed to launch camera app"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
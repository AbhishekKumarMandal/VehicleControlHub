package com.example.vehiclecontrolhub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment() {
    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val KEY_TITLE = "KEY_TITLE"

        fun newInstance(title: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val bundle = Bundle()
            bundle.putString(KEY_TITLE, title)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvPlaceholder.text = arguments?.getString(KEY_TITLE) ?: "Coming soon"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
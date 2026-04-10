package com.example.vehiclecontrolhub

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DashboardPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val pages = listOf(
        "Telemetry",
        "HVAC",
        "Seats",
        "Lights",
        "Body",
        "Camera"
    )

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TelemetryFragment()
            1 -> HvacFragment()
            2 -> PlaceholderFragment.newInstance("Seats - Coming soon")
            3 -> PlaceholderFragment.newInstance("Lights - Coming soon")
            4 -> PlaceholderFragment.newInstance("Body Control - Coming soon")
            5 -> CameraFragment()
            else -> PlaceholderFragment.newInstance("Coming soon")
        }
    }

    fun getPageTitle(position: Int): String = pages[position]
}
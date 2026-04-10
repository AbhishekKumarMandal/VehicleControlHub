package com.example.vehiclecontrolhub

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentTelemetryBinding

class TelemetryFragment : Fragment() {
    private val tag = "TelemetryFragment"
    private var _binding: FragmentTelemetryBinding? = null
    private val binding get() = _binding!!

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    private var latestBatteryLevelKWh: Float? = null
    private var latestBatteryCapacityKWh: Float? = null

    private val telemetryCallback = object : CarPropertyManager.CarPropertyEventCallback {

        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> handleSpeedUpdate(value)
                VehiclePropertyIds.EV_BATTERY_LEVEL -> handleBatteryLevelUpdate(value)
                VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> handleBatteryCapacityUpdate(value)
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> handleChargingStatusUpdate(value)
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(tag, "Error propertyId=$propertyId areaId=$areaId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelemetryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectToCarService()
    }

    private fun connectToCarService() {
        try {
            car = Car.createCar(requireContext())
            carPropertyManager =
                car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

            subscribeTelemetryProperties()

            Log.i(tag, "Telemetry subscriptions registered successfully")
        } catch (e: SecurityException) {
            Log.e(tag, "Missing automotive permission", e)

        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Car Service", e)

        }
    }

    private fun subscribeTelemetryProperties() {
        subscribeToProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED)
        subscribeToProperty(VehiclePropertyIds.EV_BATTERY_LEVEL)
        subscribeToProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)
        subscribeToProperty(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED)
    }

    private fun subscribeToProperty(propertyId: Int) {
        try {
            carPropertyManager?.subscribePropertyEvents(
                propertyId,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
                telemetryCallback
            )
        } catch (e: SecurityException) {
            Log.e(tag, "Failed to subscribe propertyId=$propertyId", e)
        }
    }

    private fun handleSpeedUpdate(value: CarPropertyValue<*>) {
        val speedMs = value.value as? Float ?: return
        activity?.runOnUiThread {
            binding.tvSpeed.text =
                "Speed: %.2f m/s (%.2f km/h)".format(speedMs, speedMs * 3.6f)
        }
    }

    private fun handleBatteryLevelUpdate(value: CarPropertyValue<*>) {
        val batteryWh = value.value as? Float ?: return
        val batteryKWh = batteryWh / 1000f
        latestBatteryLevelKWh = batteryKWh

        activity?.runOnUiThread {
            binding.tvBatteryLevel.text = "Battery level: %.2f kWh".format(batteryKWh)
            updateBatteryPercent()
        }
    }

    private fun handleBatteryCapacityUpdate(value: CarPropertyValue<*>) {
        val capacityWh = value.value as? Float ?: return
        val capacityKWh = capacityWh / 1000f
        latestBatteryCapacityKWh = capacityKWh

        activity?.runOnUiThread {
            binding.tvBatteryCapacity.text = "Battery capacity: %.2f kWh".format(capacityKWh)
            updateBatteryPercent()
        }
    }

    private fun handleChargingStatusUpdate(value: CarPropertyValue<*>) {
        val connected = value.value as? Boolean ?: return
        activity?.runOnUiThread {
            binding.tvChargingStatus.text =
                "Charging connected: ${if (connected) "Yes" else "No"}"
        }
    }

    private fun updateBatteryPercent() {
        val level = latestBatteryLevelKWh
        val capacity = latestBatteryCapacityKWh

        if (level != null && capacity != null && capacity > 0f) {
            val percent = (level / capacity) * 100f
            binding.tvBatteryPercent.text = "Battery: %.2f%%".format(percent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carPropertyManager?.unsubscribePropertyEvents(telemetryCallback)
        car?.disconnect()
        _binding = null
    }
}
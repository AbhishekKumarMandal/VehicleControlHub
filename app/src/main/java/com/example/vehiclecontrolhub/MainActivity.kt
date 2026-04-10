package com.example.vehiclecontrolhub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vehiclecontrolhub.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: DashboardPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
    }

    private fun setupTabs() {
        pagerAdapter = DashboardPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager,
            { tab, position -> tab.text = pagerAdapter.getPageTitle(position)}
        ).attach()

        //If lambda is the LAST parameter, you can move it outside the brackets
        /*TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getPageTitle(position)
        }.attach()*/
    }
}



/*
import Util.VehiclePropertyUtil
import android.car.Car
import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
//import android.widget.TextView // not need this import anymore as we are using view binding
import androidx.appcompat.app.AppCompatActivity
import com.example.vehiclecontrolhub.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val tag = "VehicleControlHub"

    private lateinit var binding: ActivityMainBinding

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    private var latestBatteryLevelKWh: Float? = null
    private var latestBatteryCapacityKWh: Float? = null

    private val telemetryCallback =
        object : CarPropertyManager.CarPropertyEventCallback {

            override fun onChangeEvent(value: CarPropertyValue<*>) {

                when (value.propertyId) {

                    VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                        handleSpeedUpdate(value)

                    VehiclePropertyIds.EV_BATTERY_LEVEL ->
                        handleBatteryLevelUpdate(value)

                    VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY ->
                        handleBatteryCapacityUpdate(value)

                    VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED ->
                        handleChargingStatusUpdate(value)

                    VehiclePropertyIds.HVAC_AC_ON ->
                        handleHvacUpdate(value)
                }
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.e(tag, "Error propertyId=$propertyId areaId=$areaId")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate binding from layout
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Set layout for activity
        setContentView(binding.root)

        connectToCarService()
    }

    private fun connectToCarService() {

        try {

            car = Car.createCar(this)

            carPropertyManager =
                car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

            subscribeTelemetryProperties()
            Log.i(tag, "Telemetry subscriptions registered successfully")

            setAirConditionerValueFromApp()
            Log.i(tag, "HVAC listener setup successfully")

        } catch (e: SecurityException) {
            Log.e(tag, "Missing automotive permission", e)

        } catch (e: Exception) {
            Log.e(tag, "Failed to connect Car Service", e)
        }
    }

    private fun subscribeTelemetryProperties() {
        subscribeToProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED)
        subscribeToProperty(VehiclePropertyIds.EV_BATTERY_LEVEL)
        subscribeToProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)
        subscribeToProperty(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED)
        subscribeToProperty(VehiclePropertyIds.HVAC_AC_ON)
    }

    private fun subscribeToProperty(propertyId: Int) {
        try {
            carPropertyManager?.subscribePropertyEvents(
                propertyId,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
                telemetryCallback
            )
        } catch(e: SecurityException) {
            Log.e(tag, "Failed to subscribe to property ${VehiclePropertyUtil.getVehiclePropertyNameFromId(propertyId)}: ($propertyId)", e)
            if(propertyId == VehiclePropertyIds.HVAC_AC_ON) {
                binding.switchAc.text = "HVAC access requires privileged app"
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to subscribe to property ${VehiclePropertyUtil.getVehiclePropertyNameFromId(propertyId)}: ($propertyId)", e)
            if(propertyId == VehiclePropertyIds.HVAC_AC_ON) {
                binding.switchAc.text = "Failed to get AC state"
            }
        }
    }

    private fun handleSpeedUpdate(value: CarPropertyValue<*>) {

        val speedMs = value.value as? Float ?: return

        runOnUiThread {

            binding.tvSpeed.text =
                "Speed: %.2f m/s (%.2f km/h)".format(speedMs, speedMs * 3.6f)
        }
    }

    private fun handleBatteryLevelUpdate(value: CarPropertyValue<*>) {

        val batteryWh = value.value as? Float ?: return
        val batteryKWh = batteryWh / 1000f

        latestBatteryLevelKWh = batteryKWh

        runOnUiThread {

            binding.tvBatteryLevel.text =
                "Battery level: %.2f kWh".format(batteryKWh)

            updateBatteryPercent()
        }
    }

    private fun handleBatteryCapacityUpdate(value: CarPropertyValue<*>) {

        val capacityWh = value.value as? Float ?: return
        val capacityKWh = capacityWh / 1000f

        latestBatteryCapacityKWh = capacityKWh

        runOnUiThread {

            binding.tvBatteryCapacity.text =
                "Battery capacity: %.2f kWh".format(capacityKWh)

            updateBatteryPercent()
        }
    }

    private fun handleChargingStatusUpdate(value: CarPropertyValue<*>) {

        val connected = value.value as? Boolean ?: return

        runOnUiThread {

            binding.tvChargingStatus.text =
                "Charging connected: ${if (connected) "Yes" else "No"}"
        }
    }

    private fun updateBatteryPercent() {

        val level = latestBatteryLevelKWh
        val capacity = latestBatteryCapacityKWh

        if (level != null && capacity != null && capacity > 0f) {

            val percent = (level / capacity) * 100f

            binding.tvBatteryPercent.text =
                "Battery: %.2f%%".format(percent)
        }
    }

    private fun handleHvacUpdate(value: CarPropertyValue<*>) {
        val acOn = value.value as Boolean

        runOnUiThread {
            binding.switchAc.isChecked = acOn

        }
    }

    private fun setAirConditionerValueFromApp() {
        binding.switchAc.setOnCheckedChangeListener { _, isChecked ->
            try {
                Log.i(tag, "Setting AC state to $isChecked")
                val areaId = VehiclePropertyUtil.getSupportedAreaIdsForHvac(carPropertyManager)
                if(areaId == null) {
                    Log.e(tag, "No supported areaId found for HVAC_AC_ON")
                    binding.switchAc.text = "HVAC requires privileged app"
                    return@setOnCheckedChangeListener
                }
                Log.i(tag, "Setting AC state to $isChecked for areaId=$areaId")
                carPropertyManager?.setBooleanProperty(
                    VehiclePropertyIds.HVAC_AC_ON,
                    areaId,
                    isChecked
                )
            } catch(e: SecurityException) {
                Log.e(tag, "Failed to Set AC state. HVAC access requires privileged app", e)
                binding.switchAc.text = "HVAC access requires privileged app"
            }
            catch (e: Exception) {
                Log.e(tag, "Failed to set AC state", e)
                binding.switchAc.text = "Failed to set AC state"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        carPropertyManager?.unsubscribePropertyEvents(telemetryCallback)

        car?.disconnect()
    }
}
*/
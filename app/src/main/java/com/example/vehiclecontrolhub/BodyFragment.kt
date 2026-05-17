package com.example.vehiclecontrolhub

import Util.VehiclePropertyUtil
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentBodyBinding

class BodyFragment: Fragment() {
    private val tag = "BodyFragment"

    private var _binding: FragmentBodyBinding? = null
    private val binding get() = _binding!!

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    private val PASSENGER_DOOR_AREA_ID = 1
    private val DRIVER_DOOR_AREA_ID = 4
    private val REAR_LEFT_DOOR_AREA_ID = 16
    private val REAR_RIGHT_DOOR_AREA_ID = 64

    private val PASSENGER_WINDOW_AREA_ID = 16
    private val DRIVER_WINDOW_AREA_ID = 64
    private val REAR_LEFT_WINDOW_AREA_ID = 256
    private val REAR_RIGHT_WINDOW_AREA_ID = 1024

    //private var isUpdatingDoorLockFromCode = false
    private var isUpdatingWindowPositionFromCode = false

    private val bodyCallback = object: CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>?) {
            val propertyId = value?.propertyId ?: return
            val areaId = value.areaId
            when(propertyId) {
                VehiclePropertyIds.WINDOW_POS -> {
                    val windowPosValue = value.value as? Int ?: return
                    updateWindowPositionInUi(areaId, windowPosValue)
                }

                VehiclePropertyIds.DOOR_LOCK -> {
                    val doorLockValue = value.value as? Boolean ?: return
                    updateDoorLockInUi(areaId, doorLockValue)
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(tag, "Body Property Error. PropertyId ${VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(propertyId)} areaId $areaId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBodyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectToCarService()
    }

    private fun connectToCarService() {
        car = Car.createCar(requireContext())
        carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        //logBodyProperties()
        subscribeToBodyProperties()
        readInitialBodyValues()
        setupBodyControlsListeners()
    }

    private fun logBodyProperties() {
        val properties = carPropertyManager?.getPropertyList() ?: emptyList()
        for (property in properties) {
            val propertyId = property.propertyId
            val areaIds = property.areaIds
            val propertyName = VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(propertyId)
            if(
                propertyName.contains("GEAR", ignoreCase = true) ||
                propertyName.contains("PARKING", ignoreCase = true) ||
                propertyName.contains("WINDOW_POS", ignoreCase = true) ||
                propertyName.contains("DOOR_LOCK", ignoreCase = true)
            ) {
                val areaId = areaIds.firstOrNull() ?: 0
                val propertyValue = try {
                    when(property.propertyType) {
                        Boolean::class.javaObjectType -> {
                            val v = carPropertyManager?.getBooleanProperty(propertyId, areaId)
                            "Boolean Value=$v"
                        }

                        Int::class.javaObjectType -> {
                            val v = carPropertyManager?.getIntProperty(propertyId, areaId)
                            "Integer Value=$v"
                        }

                        Float::class.javaObjectType -> {
                            val v  = carPropertyManager?.getFloatProperty(propertyId, areaId)
                            "Float Value=$v"
                        }

                        else -> {
                            "Unsupported type=${property.propertyType}"
                        }
                    }
                } catch (e: Exception) {
                    "Error reading value ${e.message}"
                }
                val minValue = property.areaIdConfigs.firstOrNull()?.minValue ?: "unable to find min value"
                val maxValue = property.areaIdConfigs.firstOrNull()?.maxValue ?: "unable to find max value"
                Log.i(tag, "Body property: ${propertyName} type: ${property.propertyType} areaIds: ${areaIds.joinToString()} value: ${propertyValue} minValue: ${minValue} maxValue: ${maxValue}")
            }
        }
    }

    private fun subscribeToBodyProperties() {
        carPropertyManager?.subscribePropertyEvents(VehiclePropertyIds.WINDOW_POS, CarPropertyManager.SENSOR_RATE_ONCHANGE, bodyCallback)
        carPropertyManager?.subscribePropertyEvents(VehiclePropertyIds.DOOR_LOCK, CarPropertyManager.SENSOR_RATE_ONCHANGE, bodyCallback)
    }

    private fun readInitialBodyValues() {
        val doorPropertyAreaIds = listOf(DRIVER_DOOR_AREA_ID, PASSENGER_DOOR_AREA_ID, REAR_LEFT_DOOR_AREA_ID, REAR_RIGHT_DOOR_AREA_ID)
        val windowPropertyAreaIds = listOf(DRIVER_WINDOW_AREA_ID, PASSENGER_WINDOW_AREA_ID, REAR_LEFT_WINDOW_AREA_ID, REAR_RIGHT_WINDOW_AREA_ID)

        for(areaId in doorPropertyAreaIds) {
            val value = carPropertyManager?.getBooleanProperty(VehiclePropertyIds.DOOR_LOCK, areaId)
            updateDoorLockInUi(areaId, value ?: false)
        }

        for (areaId in windowPropertyAreaIds) {
            val value = carPropertyManager?.getIntProperty(VehiclePropertyIds.WINDOW_POS, areaId) ?: 0
            updateWindowPositionInUi(areaId, value)
        }
    }

    private fun setupBodyControlsListeners() {
        setUpWindowSeekBarListener(binding.seekDriverWindow, DRIVER_WINDOW_AREA_ID)
        setUpWindowSeekBarListener(binding.seekPassengerWindow, PASSENGER_WINDOW_AREA_ID)
        setUpWindowSeekBarListener(binding.seekRearLeftWindow, REAR_LEFT_WINDOW_AREA_ID)
        setUpWindowSeekBarListener(binding.seekRearRightWindow, REAR_RIGHT_WINDOW_AREA_ID)

        setupDoorLockUnlockButtonListeners(binding.btnDriverLock, DRIVER_DOOR_AREA_ID, true)
        setupDoorLockUnlockButtonListeners(binding.btnPassengerLock, PASSENGER_DOOR_AREA_ID, true)
        setupDoorLockUnlockButtonListeners(binding.btnRearLeftLock, REAR_LEFT_DOOR_AREA_ID, true)
        setupDoorLockUnlockButtonListeners(binding.btnRearRightLock, REAR_RIGHT_DOOR_AREA_ID, true)

        setupDoorLockUnlockButtonListeners(binding.btnDriverUnlock, DRIVER_DOOR_AREA_ID, false)
        setupDoorLockUnlockButtonListeners(binding.btnPassengerUnlock, PASSENGER_DOOR_AREA_ID, false)
        setupDoorLockUnlockButtonListeners(binding.btnRearLeftUnlock, REAR_LEFT_DOOR_AREA_ID, false)
        setupDoorLockUnlockButtonListeners(binding.btnRearRightUnlock, REAR_RIGHT_DOOR_AREA_ID, false)

        binding.btnLockAll.setOnClickListener {
            try {
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, DRIVER_DOOR_AREA_ID, true)
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, PASSENGER_DOOR_AREA_ID, true)
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, REAR_LEFT_DOOR_AREA_ID, true)
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, REAR_RIGHT_DOOR_AREA_ID, true)
            } catch(e: Exception) {
                Log.e(tag, "Failed to Lock All Doors", e)
            }
        }

        binding.btnUnlockAll.setOnClickListener {
            try {
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, DRIVER_DOOR_AREA_ID, false)
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, PASSENGER_DOOR_AREA_ID, false)
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, REAR_LEFT_DOOR_AREA_ID, false)
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, REAR_RIGHT_DOOR_AREA_ID, false)
            } catch(e: Exception) {
                Log.e(tag, "Failed to Unlock All Doors", e)
            }
        }
    }

    private fun setUpWindowSeekBarListener(seekBar: SeekBar, areaId: Int) {
        try {
            val min = 0
            val max = 10
            seekBar.max = 10
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    // Notification that the progress level has changed
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Notification that the user has started a touch gesture.
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Notification that the user has finished a touch gesture.
                    val value = min + (seekBar?.progress ?: 0)
                    if (value in min..max) {
                        carPropertyManager?.setIntProperty(
                            VehiclePropertyIds.WINDOW_POS,
                            areaId,
                            value
                        )
                        updateWindowPositionInUi(areaId, value)
                    }
                }
            })
        } catch(e: Exception) {
            Log.e(tag, "Failed to setup window seekbar listener for areaId $areaId", e)
        }
    }

    private fun setupDoorLockUnlockButtonListeners(button: Button, areaId: Int, value: Boolean) {
        button.setOnClickListener {
            try {
                carPropertyManager?.setBooleanProperty(VehiclePropertyIds.DOOR_LOCK, areaId, value)
                updateDoorLockInUi(areaId, value)
            } catch(e: Exception) {
                Log.e(tag, "Failed to update door lock areaId=$areaId", e)
            }
        }
    }

    private fun updateWindowPositionInUi(areaId: Int, value: Int) {
        activity?.runOnUiThread {
            if(!isUpdatingWindowPositionFromCode) {
                isUpdatingWindowPositionFromCode = true
                when (areaId) {
                    DRIVER_WINDOW_AREA_ID -> {
                        binding.tvDriverWindowValue.text = "${value * 10}%"
                        binding.seekDriverWindow.progress = value
                    }

                    PASSENGER_WINDOW_AREA_ID -> {
                        binding.tvPassengerWindowValue.text = "${value * 10}%"
                        binding.seekPassengerWindow.progress = value
                    }

                    REAR_LEFT_WINDOW_AREA_ID -> {
                        binding.tvRearLeftWindowValue.text = "${value * 10}%"
                        binding.seekRearLeftWindow.progress = value
                    }

                    REAR_RIGHT_WINDOW_AREA_ID -> {
                        binding.tvRearRightWindowValue.text = "${value * 10}%"
                        binding.seekRearRightWindow.progress = value
                    }

                    else -> Log.e(tag, "Got invalid AreaId: $areaId to update window position in UI")
                }
                isUpdatingWindowPositionFromCode = false
            }
        }
    }

    private fun updateDoorLockInUi(areaId: Int, value: Boolean) {
        activity?.runOnUiThread {
            //if(!isUpdatingDoorLockFromCode) {
            //    isUpdatingDoorLockFromCode = true
            when (areaId) {
                PASSENGER_DOOR_AREA_ID -> {
                    binding.tvPassengerDoorStatus.text = if (value) "Locked" else "Unlocked"
                }

                DRIVER_DOOR_AREA_ID -> {
                    binding.tvDriverDoorStatus.text = if (value) "Locked" else "Unlocked"
                }

                REAR_LEFT_DOOR_AREA_ID -> {
                    binding.tvRearLeftDoorStatus.text = if (value) "Locked" else "Unlocked"
                }

                REAR_RIGHT_DOOR_AREA_ID -> {
                    binding.tvRearRightDoorStatus.text = if (value) "Locked" else "Unlocked"
                }

                else -> {
                    Log.e(tag, "Got invalid areaId to update door lock in UI")
                }
            }
                //isUpdatingDoorLockFromCode = false
            //}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carPropertyManager?.unsubscribePropertyEvents(bodyCallback)
        car?.disconnect()
        _binding = null
    }
}
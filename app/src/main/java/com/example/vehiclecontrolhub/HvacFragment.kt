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
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentHvacBinding

class HvacFragment : Fragment() {

    private val tag = "HvacFragment"

    private var _binding: FragmentHvacBinding? = null
    private val binding get() = _binding!!

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    private val DRIVER_AREA_ID = 1
    private val PASSENGER_AREA_ID = 4

    private val FRONT_DEFROSTER_AREA_ID = 1
    private val REAR_DEFROSTER_AREA_ID = 2

    private val acStates = mutableMapOf<Int, Boolean>()
    private val temperatureValues = mutableMapOf<Int, Float>()
    private val temperatureMinValues = mutableMapOf<Int, Float>()
    private val temperatureMaxValues = mutableMapOf<Int, Float>()

    private val fanSpeedValues = mutableMapOf<Int, Int>()
    private val fanSpeedMinValues = mutableMapOf<Int, Int>()
    private val fanSpeedMaxValues = mutableMapOf<Int, Int>()

    private val defrosterStates = mutableMapOf<Int, Boolean>()

    private var isUpdatingAcSwitchFromCode = false
    private var isUpdatingDefrosterSwitchFromCode = false
    private var isUpdatingFanSeekbarFromCode = false

    private val hvacCallback = object : CarPropertyManager.CarPropertyEventCallback {

        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val areaId = value.areaId

            when (value.propertyId) {

                VehiclePropertyIds.HVAC_AC_ON -> {
                    val acOn = value.value as? Boolean ?: return
                    acStates[areaId] = acOn
                    updateAcUi(areaId, acOn)
                    Log.i(tag, "HVAC_AC_ON update areaId=$areaId value=$acOn")
                }

                VehiclePropertyIds.HVAC_TEMPERATURE_SET -> {
                    val temp = value.value as? Float ?: return
                    temperatureValues[areaId] = temp
                    updateTemperatureUi(areaId, temp)
                    Log.i(tag, "HVAC_TEMPERATURE_SET update areaId=$areaId value=$temp")
                }

                VehiclePropertyIds.HVAC_FAN_SPEED -> {
                    val fanSpeed = value.value as? Int ?: return
                    fanSpeedValues[areaId] = fanSpeed
                    updateFanSpeedUi(areaId, fanSpeed)
                    Log.i(tag, "HVAC_FAN_SPEED update areaId=$areaId value=$fanSpeed")
                }

                VehiclePropertyIds.HVAC_DEFROSTER -> {
                    val defrosterOn = value.value as? Boolean ?: return
                    defrosterStates[areaId] = defrosterOn
                    updateDefrosterUi(areaId, defrosterOn)
                    Log.i(tag, "HVAC_DEFROSTER update areaId=$areaId value=$defrosterOn")
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(tag, "HVAC property error propertyId=$propertyId areaId=$areaId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHvacBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectToCarService()
    }

    private fun connectToCarService() {
        try {
            car = Car.createCar(requireContext())
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager


            //logHvacProperties()
            subscribeHvacProperties()
            loadHvacRanges()
            readInitialHvacValues()
            setupHvacControls()

            Log.i(tag, "HVAC initialized successfully")

        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Car Service", e)
        }
    }

    private fun logHvacProperties() {
        val properties = carPropertyManager?.getPropertyList() ?: emptyList()

        for(property in properties) {
            val propertyName = VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(property.propertyId)
            if(propertyName.contains("HVAC", ignoreCase = true)) {
                Log.i(tag, "HVAC property $propertyName areaIds ${property.areaIds.joinToString()}")
            }
        }
    }

    private fun subscribeHvacProperties() {
        subscribeToProperty(VehiclePropertyIds.HVAC_AC_ON)
        subscribeToProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET)
        subscribeToProperty(VehiclePropertyIds.HVAC_FAN_SPEED)
        subscribeToProperty(VehiclePropertyIds.HVAC_DEFROSTER)
    }

    private fun subscribeToProperty(propertyId: Int) {
        try {
            carPropertyManager?.subscribePropertyEvents(
                propertyId,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
                hvacCallback
            )

            Log.i(tag, "Subscribed to HVAC propertyId=$propertyId")

        } catch (e: Exception) {
            Log.e(tag, "Failed to subscribe HVAC propertyId=$propertyId", e)
        }
    }


    private fun loadHvacRanges() {
        loadFloatRange(
            VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            temperatureMinValues,
            temperatureMaxValues,
            16f,
            30f
        )

        loadIntRange(
            VehiclePropertyIds.HVAC_FAN_SPEED,
            fanSpeedMinValues,
            fanSpeedMaxValues,
            0,
            7
        )
    }

    private fun loadFloatRange(
        propertyId: Int,
        minMap: MutableMap<Int, Float>,
        maxMap: MutableMap<Int, Float>,
        defaultMin: Float,
        defaultMax: Float
    ) {
        try {
            val config = carPropertyManager?.getPropertyList()?.firstOrNull {
                it.propertyId == propertyId
            }

            if (config == null) {
                Log.e(tag, "Config not found for propertyId=$propertyId")
                return
            }

            Log.i(tag, "Config for propertyId=$propertyId $config")

            for (areaConfig in config.areaIdConfigs) {
                val areaId = areaConfig.areaId
                val min = areaConfig.minValue as? Float ?: defaultMin
                val max = areaConfig.maxValue as? Float ?: defaultMax

                minMap[areaId] = min
                maxMap[areaId] = max

                Log.i(tag, "Float range propertyId=$propertyId areaId=$areaId min=$min max=$max")
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to load float range for propertyId=$propertyId", e)
        }
    }

    private fun loadIntRange(
        propertyId: Int,
        minMap: MutableMap<Int, Int>,
        maxMap: MutableMap<Int, Int>,
        defaultMin: Int,
        defaultMax: Int
    ) {
        try {
            val config = carPropertyManager?.getPropertyList()?.firstOrNull {
                it.propertyId == propertyId
            }

            if (config == null) {
                Log.e(tag, "Config not found for propertyId=$propertyId")
                return
            }

            Log.i(tag, "Config for propertyId=$propertyId $config")

            for (areaConfig in config.areaIdConfigs) {
                val areaId = areaConfig.areaId
                val min = areaConfig.minValue as? Int ?: defaultMin
                val max = areaConfig.maxValue as? Int ?: defaultMax

                minMap[areaId] = min
                maxMap[areaId] = max

                Log.i(tag, "Int range propertyId=$propertyId areaId=$areaId min=$min max=$max")
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to load int range for propertyId=$propertyId", e)
        }
    }

    private fun readInitialHvacValues() {
        val acAreas = listOf(DRIVER_AREA_ID, PASSENGER_AREA_ID)
        val defrosterAreas = listOf(FRONT_DEFROSTER_AREA_ID, REAR_DEFROSTER_AREA_ID)

        for (areaId in acAreas) {
            try {
                val acOn = carPropertyManager?.getBooleanProperty(
                    VehiclePropertyIds.HVAC_AC_ON,
                    areaId
                ) ?: false
                acStates[areaId] = acOn

                val temp = carPropertyManager?.getFloatProperty(
                    VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                    areaId
                ) ?: 0f
                temperatureValues[areaId] = temp

                val fanSpeed = carPropertyManager?.getIntProperty(
                    VehiclePropertyIds.HVAC_FAN_SPEED,
                    areaId
                ) ?: 0
                fanSpeedValues[areaId] = fanSpeed

                Log.i(tag, "Initial HVAC areaId=$areaId ac=$acOn temp=$temp fan=$fanSpeed")

            } catch (e: Exception) {
                Log.e(tag, "Failed to read initial HVAC values for areaId=$areaId", e)
            }
        }

        for (areaId in defrosterAreas) {
            try {
                val defrosterOn = carPropertyManager?.getBooleanProperty(
                    VehiclePropertyIds.HVAC_DEFROSTER,
                    areaId
                ) ?: false
                defrosterStates[areaId] = defrosterOn

                Log.i(tag, "Initial defroster areaId=$areaId value=$defrosterOn")

            } catch (e: Exception) {
                Log.e(tag, "Failed to read initial defroster value for areaId=$areaId", e)
            }
        }

        refreshAllHvacUi()
    }

    private fun setupHvacControls() {
        setupAcControls()
        setupTemperatureControls()
        setupFanSpeedControls()
        setupDefrosterControls()
    }

    private fun setupAcControls() {

        // this method is creating listener for AC toggle switch
        // if AC toggle switch is changed from app running in emulator
        // or here in code if binding.switchDriverAC.isChecked is changed then immediately below listener will be triggered
        // _ means parameter is ignored

        binding.switchDriverAC.setOnCheckedChangeListener { _, isChecked ->

            // as mentioned above that if binding.switchDriverAC.isChecked is changed from this code then this method listener will be triggered
            // in updateAcUi() method, binding.switchDriverAC.isChecked is getting updated
            // so this listener will be triggered in that case. but before changing binding.switchDriverAC.isChecked inside that method
            // it is also making isUpdatingSwitchFromCode as true.
            // and if isUpdatingSwitchFromCode is true then below block if block will not allow to execute this listener completely
            // because it simply returns without executing anything
            // and thus it is ensured that this listener will be executed only when toggle switch is changed from app running in emulator.
            // in code if we want to update binding.switchAc.isChecked then either call updateHvacUi() or write below snippet
            /**
             * isUpdatingSwitchFromCode = true
             * binding.switchAc.isChecked = <whatever value you want>
             * isUpdatingSwitchFromCode = false
             */
            // after changing the value im making isUpdatingSwitchFromCode to false
            // so that below listener will be triggered if update comes from app running in emulator

            if (isUpdatingAcSwitchFromCode)
                return@setOnCheckedChangeListener
            setAcState(DRIVER_AREA_ID, isChecked)
        }

        binding.switchPassengerAC.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAcSwitchFromCode)
                return@setOnCheckedChangeListener
            setAcState(PASSENGER_AREA_ID, isChecked)
        }
    }

    private fun setupTemperatureControls() {
        binding.btnDriverTempDown.setOnClickListener {
            changeTemperature(DRIVER_AREA_ID, -1f)
        }

        binding.btnDriverTempUp.setOnClickListener {
            changeTemperature(DRIVER_AREA_ID, 1f)
        }

        binding.btnPassengerTempDown.setOnClickListener {
            changeTemperature(PASSENGER_AREA_ID, -1f)
        }

        binding.btnPassengerTempUp.setOnClickListener {
            changeTemperature(PASSENGER_AREA_ID, 1f)
        }
    }

    private fun setupFanSpeedControls() {
        setupFanSeekBar(DRIVER_AREA_ID, binding.seekDriverFan)
        setupFanSeekBar(PASSENGER_AREA_ID, binding.seekPassengerFan)
    }

    private fun setupFanSeekBar(areaId: Int, seekBar: SeekBar) {
        val min = fanSpeedMinValues[areaId] ?: 0
        val max = fanSpeedMaxValues[areaId] ?: 7

        seekBar.max = max - min
        seekBar.progress = (fanSpeedValues[areaId] ?: min) - min

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                // Notification that the progress level has changed
                // We write only when user stops sliding.
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Notification that the user has started a touch gesture.
                // No-op.
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Notification that the user has finished a touch gesture.
                if (isUpdatingFanSeekbarFromCode) return

                val value = min + (seekBar?.progress ?: 0)
                setFanSpeed(areaId, value)
            }
        })
    }

    private fun setupDefrosterControls() {
        binding.switchFrontDefroster.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingDefrosterSwitchFromCode) return@setOnCheckedChangeListener
            setDefrosterState(FRONT_DEFROSTER_AREA_ID, isChecked)
        }

        binding.switchRearDefroster.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingDefrosterSwitchFromCode) return@setOnCheckedChangeListener
            setDefrosterState(REAR_DEFROSTER_AREA_ID, isChecked)
        }
    }

    private fun setAcState(areaId: Int, enabled: Boolean) {
        try {
            Log.i(tag, "Setting HVAC_AC_ON areaId=$areaId value=$enabled")

            carPropertyManager?.setBooleanProperty(
                VehiclePropertyIds.HVAC_AC_ON,
                areaId,
                enabled
            )

            acStates[areaId] = enabled
            updateAcUi(areaId, enabled)

        } catch (e: Exception) {
            Log.e(tag, "Failed to set HVAC_AC_ON areaId=$areaId", e)
        }
    }

    private fun changeTemperature(areaId: Int, delta: Float) {
        try {
            val current = temperatureValues[areaId] ?: 0f
            val min = temperatureMinValues[areaId] ?: current
            val max = temperatureMaxValues[areaId] ?: current

            val next = (current + delta).coerceIn(min, max)

            Log.i(tag, "Setting HVAC_TEMPERATURE_SET areaId=$areaId value=$next")

            carPropertyManager?.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                areaId,
                next
            )

            temperatureValues[areaId] = next
            updateTemperatureUi(areaId, next)

        } catch (e: Exception) {
            Log.e(tag, "Failed to set HVAC_TEMPERATURE_SET areaId=$areaId", e)
        }
    }

    private fun setFanSpeed(areaId: Int, fanSpeed: Int) {
        try {
            val min = fanSpeedMinValues[areaId] ?: fanSpeed
            val max = fanSpeedMaxValues[areaId] ?: fanSpeed
            val value = fanSpeed.coerceIn(min, max)

            Log.i(tag, "Setting HVAC_FAN_SPEED areaId=$areaId value=$value")

            carPropertyManager?.setIntProperty(
                VehiclePropertyIds.HVAC_FAN_SPEED,
                areaId,
                value
            )

            fanSpeedValues[areaId] = value
            updateFanSpeedUi(areaId, value)

        } catch (e: Exception) {
            Log.e(tag, "Failed to set HVAC_FAN_SPEED areaId=$areaId", e)
        }
    }

    private fun setDefrosterState(areaId: Int, enabled: Boolean) {
        try {
            Log.i(tag, "Setting HVAC_DEFROSTER areaId=$areaId value=$enabled")

            carPropertyManager?.setBooleanProperty(
                VehiclePropertyIds.HVAC_DEFROSTER,
                areaId,
                enabled
            )

            defrosterStates[areaId] = enabled
            updateDefrosterUi(areaId, enabled)

        } catch (e: Exception) {
            Log.e(tag, "Failed to set HVAC_DEFROSTER areaId=$areaId", e)
        }
    }

    private fun refreshAllHvacUi() {
        updateAcUi(DRIVER_AREA_ID, acStates[DRIVER_AREA_ID] ?: false)
        updateAcUi(PASSENGER_AREA_ID, acStates[PASSENGER_AREA_ID] ?: false)

        updateTemperatureUi(DRIVER_AREA_ID, temperatureValues[DRIVER_AREA_ID] ?: 0f)
        updateTemperatureUi(PASSENGER_AREA_ID, temperatureValues[PASSENGER_AREA_ID] ?: 0f)

        updateFanSpeedUi(DRIVER_AREA_ID, fanSpeedValues[DRIVER_AREA_ID] ?: 0)
        updateFanSpeedUi(PASSENGER_AREA_ID, fanSpeedValues[PASSENGER_AREA_ID] ?: 0)

        updateDefrosterUi(FRONT_DEFROSTER_AREA_ID, defrosterStates[FRONT_DEFROSTER_AREA_ID] ?: false)
        updateDefrosterUi(REAR_DEFROSTER_AREA_ID, defrosterStates[REAR_DEFROSTER_AREA_ID] ?: false)
    }

    private fun updateAcUi(areaId: Int, enabled: Boolean) {
        activity?.runOnUiThread {
            isUpdatingAcSwitchFromCode = true

            when (areaId) {
                DRIVER_AREA_ID -> {
                    binding.switchDriverAC.isChecked = enabled
                    binding.tvDriverAcState.text = if(enabled) "ON" else "OFF"
                }
                PASSENGER_AREA_ID -> {
                    binding.switchPassengerAC.isChecked = enabled
                    binding.tvPassengerAcState.text = if(enabled) "ON" else "OFF"
                }
            }

            isUpdatingAcSwitchFromCode = false
        }
    }

    private fun updateTemperatureUi(areaId: Int, value: Float) {
        activity?.runOnUiThread {
            val text = "Temp:${value.toInt()}"

            when (areaId) {
                DRIVER_AREA_ID -> binding.tvDriverTemp.text = text
                PASSENGER_AREA_ID -> binding.tvPassengerTemp.text = text
            }
        }
    }

    private fun updateFanSpeedUi(areaId: Int, value: Int) {
        activity?.runOnUiThread {
            isUpdatingFanSeekbarFromCode = true

            val min = fanSpeedMinValues[areaId] ?: 0
            val progress = value - min

            when (areaId) {
                DRIVER_AREA_ID -> {
                    binding.seekDriverFan.progress = progress
                    binding.tvDriverFanValue.text = "Fan: $value"
                }
                PASSENGER_AREA_ID -> {
                    binding.seekPassengerFan.progress = progress
                    binding.tvPassengerFanValue.text = "Fan: $value"
                }
            }

            isUpdatingFanSeekbarFromCode = false
        }
    }

    private fun updateDefrosterUi(areaId: Int, enabled: Boolean) {
        activity?.runOnUiThread {
            isUpdatingDefrosterSwitchFromCode = true

            when (areaId) {
                FRONT_DEFROSTER_AREA_ID -> {
                    binding.switchFrontDefroster.isChecked = enabled
                    binding.tvFrontDefrosterState.text = if(enabled) "ON" else "OFF"
                }
                REAR_DEFROSTER_AREA_ID -> {
                    binding.switchRearDefroster.isChecked = enabled
                    binding.tvRearDefrosterState.text = if(enabled) "ON" else "OFF"
                }
            }

            isUpdatingDefrosterSwitchFromCode = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carPropertyManager?.unsubscribePropertyEvents(hvacCallback)
        car?.disconnect()
        _binding = null
    }
}
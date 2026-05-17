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
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentLightBinding

class LightFragment : Fragment() {

    private val tag = "LightFragment"

    private var _binding: FragmentLightBinding? = null
    private val binding get() = _binding!!

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    private val lightPropertyIdAreaIdMap = mutableMapOf<Int, Int>()

    private val LIGHT_OFF = 0
    private val LIGHT_ON = 256

    private val lightPropertyList = listOf(
        VehiclePropertyIds.HEADLIGHTS_SWITCH,
        VehiclePropertyIds.HIGH_BEAM_LIGHTS_SWITCH,
        VehiclePropertyIds.HAZARD_LIGHTS_SWITCH,
        VehiclePropertyIds.FRONT_FOG_LIGHTS_SWITCH,
        VehiclePropertyIds.REAR_FOG_LIGHTS_SWITCH
    )

    private var isUpdatingSwitchFromCode = false

    private val lightCallback = object: CarPropertyManager.CarPropertyEventCallback {

        override fun onChangeEvent(value: CarPropertyValue<*>?) {
            when(value?.propertyId) {
                VehiclePropertyIds.HEADLIGHTS_SWITCH -> {
                    Log.i(tag, "Headlight property callback")
                    val headLightOn = (value.value as? Int ?: return) == LIGHT_ON
                    updateInUi(binding.switchHeadlights, binding.tvHeadlightsState, headLightOn)
                }
                VehiclePropertyIds.HIGH_BEAM_LIGHTS_SWITCH -> {
                    Log.i(tag, "High beam property callback")
                    val highBeamOn = (value.value as? Int ?: return) == LIGHT_ON
                    updateInUi(binding.switchHighBeam, binding.tvHighBeamState, highBeamOn)
                }
                VehiclePropertyIds.HAZARD_LIGHTS_SWITCH -> {
                    Log.i(tag, "Hazard property callback")
                    val hazardLightOn = (value.value as? Int ?: return) == LIGHT_ON
                    updateInUi(binding.switchHazard, binding.tvHazardState, hazardLightOn)
                }
                VehiclePropertyIds.FRONT_FOG_LIGHTS_SWITCH -> {
                    Log.i(tag, "Front fog property callback")
                    val frontFogOn = (value.value as? Int ?: return) == LIGHT_ON
                    updateInUi(binding.switchFrontFog, binding.tvFrontFogState, frontFogOn)
                }
                VehiclePropertyIds.REAR_FOG_LIGHTS_SWITCH -> {
                    Log.i(tag, "Rear fog property callback")
                    val rearFogOn = (value.value as? Int ?: return) == LIGHT_ON
                    updateInUi(binding.switchRearFog, binding.tvRearFogState, rearFogOn)
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(tag, "Light Property Error. PropertyId ${VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(propertyId)} areaId $areaId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectToCarService()
    }

    private fun connectToCarService() {
        car = Car.createCar(requireContext())
        carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        //logLightProperties()
        //readLightAreaIds()
        setupLightControls()
        subscribeToLightProperties()
        readInitialLightValues()
        //setupLightControls()
    }

    private fun logLightProperties() {
        val properties = carPropertyManager?.getPropertyList() ?: emptyList()
        for (property in properties) {
            val propertyId = property.propertyId
            val propertyName = VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(propertyId)
            if(
                propertyName.contains("LIGHT", ignoreCase = true) ||
                propertyName.contains("BEAM", ignoreCase = true) ||
                propertyName.contains("HEAD", ignoreCase = true) ||
                propertyName.contains("CABIN", ignoreCase = true)
            ) {

                val areaId = property.areaIds.firstOrNull() ?: 0
                val valueString = try {
                    when (property.propertyType) {

                        Boolean::class.java -> {
                            val v = carPropertyManager?.getBooleanProperty(propertyId, areaId)
                            "Boolean value=$v"
                        }

                        Int::class.javaObjectType -> {
                            val v = carPropertyManager?.getIntProperty(propertyId, areaId)
                            "Int value=${v}"
                        }

                        Float::class.javaObjectType -> {
                            val v = carPropertyManager?.getFloatProperty(propertyId, areaId)
                            "Float value=$v"
                        }

                        else -> {
                            "Unsupported type=${property.propertyType}"
                        }
                    }
                } catch (e: Exception) {
                    "Error reading value: ${e.message}"
                }
                Log.i(
                    tag,
                    "Light property $propertyName type=${property.propertyType} areaId=$areaId -> $valueString"
                )

                /*
                val areaIds = property.areaIds
                val areaIdConfigs = property.areaIdConfigs
                val valueList = mutableListOf<String>()

                for(areaIdConfig in areaIdConfigs) {
                    val areaId = areaIdConfig.areaId
                    val minValue = areaIdConfig.minValue
                    val maxValue = areaIdConfig.maxValue
                    val valueString = "AreaId=$areaId minValue=$minValue maxValue=$maxValue"
                    valueList.add(valueString)
                }

                Log.i(
                    tag,
                    "Light property $propertyName type=${property.propertyType} areaIds=${areaIds.joinToString()} min_max_values=${valueList.joinToString()}"
                )*/
            }
        }
    }

    private fun readLightAreaIds() {
        for(propertyId in lightPropertyList) {
            val config = carPropertyManager?.getPropertyList()?.firstOrNull {
                it.propertyId == propertyId
            }

            if(config == null)
                return

            val areaIds = config.areaIds
            val areaId = areaIds.firstOrNull() ?: continue

            lightPropertyIdAreaIdMap[propertyId] = areaId
        }
    }

    private fun subscribeToLightProperties() {
        subscribeToLightProperty(VehiclePropertyIds.HEADLIGHTS_SWITCH)
        subscribeToLightProperty(VehiclePropertyIds.HIGH_BEAM_LIGHTS_SWITCH)
        subscribeToLightProperty(VehiclePropertyIds.HAZARD_LIGHTS_SWITCH)
        subscribeToLightProperty(VehiclePropertyIds.FRONT_FOG_LIGHTS_SWITCH)
        subscribeToLightProperty(VehiclePropertyIds.REAR_FOG_LIGHTS_SWITCH)
    }

    private fun subscribeToLightProperty(propertyId: Int) {
        carPropertyManager?.subscribePropertyEvents(propertyId, CarPropertyManager.SENSOR_RATE_ONCHANGE, lightCallback)
    }

    private fun readInitialLightValues() {
        var value = carPropertyManager?.getIntProperty(VehiclePropertyIds.HEADLIGHTS_SWITCH, 0)
        Log.i(tag, "HEADLIGHTS_SWITCH value $value")
        updateInUi(
            binding.switchHeadlights,
            binding.tvHeadlightsState,
            value != 0
        )

        value = carPropertyManager?.getIntProperty(VehiclePropertyIds.HIGH_BEAM_LIGHTS_SWITCH, 0)
        Log.i(tag, "HIGH_BEAM_LIGHTS_SWITCH value $value")
        updateInUi(
            binding.switchHighBeam,
            binding.tvHighBeamState,
            value != 0
        )

        value = carPropertyManager?.getIntProperty(VehiclePropertyIds.HAZARD_LIGHTS_SWITCH, 0)
        Log.i(tag, "HAZARD_LIGHTS_SWITCH value $value")
        updateInUi(
            binding.switchHazard,
            binding.tvHazardState,
            value != 0
        )

        value = carPropertyManager?.getIntProperty(VehiclePropertyIds.FRONT_FOG_LIGHTS_SWITCH, 0)
        Log.i(tag, "FRONT_FOG_LIGHTS_SWITCH value $value")
        updateInUi(
            binding.switchFrontFog,
            binding.tvFrontFogState,
            value != 0
        )

        value = carPropertyManager?.getIntProperty(VehiclePropertyIds.REAR_FOG_LIGHTS_SWITCH, 0)
        Log.i(tag, "REAR_FOG_LIGHTS_SWITCH value $value")
        updateInUi(
            binding.switchRearFog,
            binding.tvRearFogState,
            value != 0
        )
    }

    private fun setupLightControls() {

        // headlight switch
        binding.switchHeadlights.setOnCheckedChangeListener { _, isChecked ->
            Log.i(tag, "Headlight listener")
            if(!isUpdatingSwitchFromCode) {
                Log.i(tag, "inside Headlight listener")
                setLightProperty(VehiclePropertyIds.HEADLIGHTS_SWITCH, isChecked)
            }
        }

        // high beam switch
        binding.switchHighBeam.setOnCheckedChangeListener { _, isChecked ->
            Log.i(tag, "High beam listener")
            if(!isUpdatingSwitchFromCode) {
                Log.i(tag, "inside High beam listener")
                setLightProperty(VehiclePropertyIds.HIGH_BEAM_LIGHTS_SWITCH, isChecked)
            }
        }

        // hazard switch
        binding.switchHazard.setOnCheckedChangeListener { _, isChecked ->
            Log.i(tag, "Hazard listener")
            if(!isUpdatingSwitchFromCode) {
                Log.i(tag, "inside Hazard listener")
                setLightProperty(VehiclePropertyIds.HAZARD_LIGHTS_SWITCH, isChecked)
            }
        }

        // front fog switch
        binding.switchFrontFog.setOnCheckedChangeListener { _, isChecked ->
            Log.i(tag, "Front fog listener")
            if(!isUpdatingSwitchFromCode) {
                Log.i(tag, "inside Front fog listener")
                setLightProperty(VehiclePropertyIds.FRONT_FOG_LIGHTS_SWITCH, isChecked)
            }
        }

        // rear fog switch
        binding.switchRearFog.setOnCheckedChangeListener {_, isChecked ->
            Log.i(tag, "Rear fog listener")
            if(!isUpdatingSwitchFromCode) {
                Log.i(tag, "inside Rear fog listener")
                setLightProperty(VehiclePropertyIds.REAR_FOG_LIGHTS_SWITCH, isChecked)
            }
        }
    }

    private fun setLightProperty(propertyId: Int, isChecked: Boolean) {
        try {
            val value = if(isChecked) LIGHT_ON else LIGHT_OFF
            carPropertyManager?.setIntProperty(propertyId, 0, value)
        } catch (e: Exception) {
            Log.e(
                tag,
                "Failed to set ${VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(propertyId)} to $isChecked",
                e
            )
        }
    }

    private fun updateInUi(switch: Switch, textView: TextView, on: Boolean) {
        activity?.runOnUiThread {
            isUpdatingSwitchFromCode = true
            Log.i(tag, "updateInUi before switch")
            switch.isChecked = on
            /**
            switch.isChecked = value triggers listener only if:
                1. listener is attached
                2. value is different from current switch value (IMPORTANT NOTE)
            * */
            Log.i(tag, "updateInUi after switch")
            textView.text = if(on) "ON" else "OFF"
            isUpdatingSwitchFromCode = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carPropertyManager?.unsubscribePropertyEvents(lightCallback)
        car?.disconnect()
        _binding = null
    }

}
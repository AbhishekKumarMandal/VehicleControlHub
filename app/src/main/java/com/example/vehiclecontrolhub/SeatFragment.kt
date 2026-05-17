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
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.vehiclecontrolhub.databinding.FragmentSeatBinding

class SeatFragment: Fragment() {

    private val tag = "SeatFragment"
    private var _binding: FragmentSeatBinding? = null
    private val binding get() = _binding!!

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val DRIVER_AREA_ID = 1
    private val PASSENGER_AREA_ID = 4
    private val REAR_LEFT_AREA_ID = 16
    private val REAR_MID_AREA_ID = 32
    private val REAR_RIGHT_AREA_ID = 64

    private val seatCurrentTempLevels = mutableMapOf<Int, Int>() // areaId -> current temperature value
    private val seatMinTempLevels = mutableMapOf<Int, Int>() // areaId -> min allowed value
    private val seatMaxTempLevels = mutableMapOf<Int, Int>() // areaId -> max allowed value

    private val seatCurrentPositions = mutableMapOf<Int, Int>()
    private val seatMinPositions = mutableMapOf<Int, Int>()
    private val seatMaxPositions = mutableMapOf<Int, Int>()

    private val seatCallback = object: CarPropertyManager.CarPropertyEventCallback {

        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val seatPropertyIds = listOf(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, VehiclePropertyIds.SEAT_FORE_AFT_POS)

            if(value.propertyId in seatPropertyIds) {

                val tempOrPosValue = value.value as? Int ?: return
                val areaId = value.areaId
                val tvDriver: TextView
                val tvPassenger: TextView
                val tvRearLeft: TextView
                val tvRearMid: TextView
                val tvRearRight: TextView
                val isTemp: Boolean

                if(value.propertyId == VehiclePropertyIds.HVAC_SEAT_TEMPERATURE) {
                    seatCurrentTempLevels[areaId] = tempOrPosValue
                    tvDriver = binding.tvDriverLevel
                    tvPassenger = binding.tvPassengerLevel
                    tvRearLeft = binding.rearLeftSeat.tvRearLeftLevel
                    tvRearMid = binding.rearMidSeat.tvRearMidLevel
                    tvRearRight = binding.rearRightSeat.tvRearRightLevel
                    isTemp = true
                }
                else {
                    seatCurrentPositions[areaId] = tempOrPosValue
                    tvDriver = binding.tvDriverSeatPosition
                    tvPassenger = binding.tvPassengerSeatPosition
                    tvRearLeft = binding.rearLeftSeat.tvRearLeftSeatPosition
                    tvRearMid = binding.rearMidSeat.tvRearMidSeatPosition
                    tvRearRight = binding.rearRightSeat.tvRearRightSeatPosition
                    isTemp = false
                }

                when (areaId) {
                    DRIVER_AREA_ID -> updateSeatTemperatureOrPositionOnUI(isTemp, tvDriver, tempOrPosValue)
                    PASSENGER_AREA_ID -> updateSeatTemperatureOrPositionOnUI(isTemp, tvPassenger, tempOrPosValue)
                    REAR_LEFT_AREA_ID -> updateSeatTemperatureOrPositionOnUI(isTemp, tvRearLeft, tempOrPosValue)
                    REAR_MID_AREA_ID -> updateSeatTemperatureOrPositionOnUI(isTemp, tvRearMid, tempOrPosValue)
                    REAR_RIGHT_AREA_ID -> updateSeatTemperatureOrPositionOnUI(isTemp, tvRearRight, tempOrPosValue)
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.e(tag, "Seat property Error propertyId=$propertyId areaId=$areaId")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectCarService()
    }

    private fun connectCarService() {
        try {
            car = Car.createCar(requireContext())
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

            //logSeatProperties()
            subscribeSeatTemperatureAndPositionProperties()
            loadSeatTemperatureAndPositionRange()
            readInitialSeatTemperatureAndPositionValues()
            setupSeatTemperatureAndPositionControls()

        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Car Service", e)
        }
    }

    private fun logSeatProperties() {
        val properties = carPropertyManager?.getPropertyList() ?: emptyList()
        for(property in properties) {
            val propertyName = VehiclePropertyUtil.vehiclePropertyUtil.getPropertyName(property.propertyId)
            if(propertyName.contains("SEAT", ignoreCase = true)) {
                Log.i(tag, "Seat property name=$propertyName propertyId=${property.propertyId} areaIds=${property.areaIds.joinToString()}")
            }
        }
    }

    private fun subscribeSeatTemperatureAndPositionProperties() {
        try {
            carPropertyManager?.subscribePropertyEvents(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                CarPropertyManager.SENSOR_RATE_ONCHANGE, seatCallback)
            Log.i(tag, "Subscribed to seat temperature")

            carPropertyManager?.subscribePropertyEvents(VehiclePropertyIds.SEAT_FORE_AFT_POS,
                CarPropertyManager.SENSOR_RATE_ONCHANGE, seatCallback)
            Log.i(tag, "Subscribed to seat Position")

        } catch (e: Exception) {
            Log.e(tag, "Error occurred while subscribing to HVAC_SEAT_TEMPERATURE, SEAT_FORE_AFT_POS properties. Error: ", e)
        }
    }

    private fun loadSeatTemperatureAndPositionRange() {
        try {
            val propertyIdsList = listOf(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, VehiclePropertyIds.SEAT_FORE_AFT_POS)
            val defaultMinMaxValue = 0
            for(propertyId in propertyIdsList) {
                val config = carPropertyManager?.getPropertyList()?.firstOrNull {
                    it.propertyId == propertyId
                }

                if (config == null) {
                    if(propertyId == VehiclePropertyIds.HVAC_SEAT_TEMPERATURE)
                        Log.e(tag, "HVAC_SEAT_TEMPERATURE config not found")
                    else
                        Log.e(tag, "SEAT_FORE_AFT_POS config not found")
                    return
                }

                //Log.i(tag, "config for HVAC_SEAT_TEMPERATURE ${config.toString()}")

                for (areaConfig in config.areaIdConfigs) {
                    val areaId = areaConfig.areaId
                    val minVal = areaConfig.minValue as? Int ?: if(propertyId == VehiclePropertyIds.HVAC_SEAT_TEMPERATURE) -2 else defaultMinMaxValue
                    val maxVal = areaConfig.maxValue as? Int ?: if(propertyId == VehiclePropertyIds.HVAC_SEAT_TEMPERATURE) 2 else defaultMinMaxValue
                    if(propertyId == VehiclePropertyIds.HVAC_SEAT_TEMPERATURE) {
                        seatMinTempLevels[areaId] = minVal
                        seatMaxTempLevels[areaId] = maxVal
                    } else {
                        seatMinPositions[areaId] = minVal
                        seatMaxPositions[areaId] = maxVal
                    }
                    Log.i(tag, "Seat temp/position config areaId=$areaId min=${minVal} max=${maxVal}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load seat temperature range for HVAC_SEAT_TEMPERATURE property", e)
        }
    }

//    private fun loadSeatPositionRange() {
//        try {
//            val config = carPropertyManager?.getPropertyList()?.firstOrNull{
//                it.propertyId == VehiclePropertyIds.SEAT_FORE_AFT_POS
//            }
//
//            if(config == null) {
//                Log.e(tag, "SEAT_FORE_AFT_POS config not found")
//                return
//            }
//
//            for(config in config.areaIdConfigs) {
//                val areaId = config.areaId
//                val minPos = config.minValue as? Int ?: 0
//                val maxPos = config.maxValue as? Int ?: 0
//                seatMinPositions[areaId] = minPos
//                seatMaxPositions[areaId] = maxPos
//                Log.i(tag, "Seat position config areaId=$areaId min=${minPos} max=${maxPos}")
//            }
//        } catch(e: Exception) {
//            Log.e(tag, "Failed to load seat position range for SEAT_FORE_AFT_POS property", e)
//        }
//    }

    private fun readInitialSeatTemperatureAndPositionValues() {
        try {
            val areaIdsList = listOf(DRIVER_AREA_ID, PASSENGER_AREA_ID, REAR_LEFT_AREA_ID, REAR_MID_AREA_ID, REAR_RIGHT_AREA_ID)
            for(areaId in areaIdsList) {
                val currentTempValue = carPropertyManager?.getIntProperty(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, areaId) ?: 0
                seatCurrentTempLevels[areaId] = currentTempValue

                val currentPositionValue = carPropertyManager?.getIntProperty(VehiclePropertyIds.SEAT_FORE_AFT_POS, areaId) ?: 0
                seatCurrentPositions[areaId] = currentPositionValue
            }

            updateSeatTemperatureOrPositionOnUI(true, binding.tvDriverLevel, seatCurrentTempLevels[DRIVER_AREA_ID])
            updateSeatTemperatureOrPositionOnUI(false, binding.tvDriverSeatPosition, seatCurrentPositions[DRIVER_AREA_ID])

            updateSeatTemperatureOrPositionOnUI(true, binding.tvPassengerLevel, seatCurrentTempLevels[PASSENGER_AREA_ID])
            updateSeatTemperatureOrPositionOnUI(false, binding.tvPassengerSeatPosition, seatCurrentPositions[PASSENGER_AREA_ID])

            updateSeatTemperatureOrPositionOnUI(true, binding.rearLeftSeat.tvRearLeftLevel, seatCurrentTempLevels[REAR_LEFT_AREA_ID])
            updateSeatTemperatureOrPositionOnUI(false, binding.rearLeftSeat.tvRearLeftSeatPosition, seatCurrentPositions[REAR_LEFT_AREA_ID])

            updateSeatTemperatureOrPositionOnUI(true, binding.rearMidSeat.tvRearMidLevel, seatCurrentTempLevels[REAR_MID_AREA_ID])
            updateSeatTemperatureOrPositionOnUI(false, binding.rearMidSeat.tvRearMidSeatPosition, seatCurrentPositions[REAR_MID_AREA_ID])

            updateSeatTemperatureOrPositionOnUI(true, binding.rearRightSeat.tvRearRightLevel, seatCurrentTempLevels[REAR_RIGHT_AREA_ID])
            updateSeatTemperatureOrPositionOnUI(false, binding.rearRightSeat.tvRearRightSeatPosition, seatCurrentPositions[REAR_RIGHT_AREA_ID])

            Log.i(tag, "Set Initial temperature and position values for all seats")
        } catch (e: Exception) {
            Log.e(tag, "Error occurred while reading and setting initial temperature. error: ", e)
        }
    }

    private fun setupSeatTemperatureAndPositionControls() {
        setupSeatTemperatureAndPositionControl(true,DRIVER_AREA_ID, binding.btnDriverDecrease, binding.btnDriverIncrease)
        setupSeatTemperatureAndPositionControl(false, DRIVER_AREA_ID, binding.btnDriverSeatBackward, binding.btnDriverSeatForward)

        setupSeatTemperatureAndPositionControl(true, PASSENGER_AREA_ID, binding.btnPassengerDecrease, binding.btnPassengerIncrease)
        setupSeatTemperatureAndPositionControl(false, PASSENGER_AREA_ID, binding.btnPassengerSeatBackward, binding.btnPassengerSeatForward)

        setupSeatTemperatureAndPositionControl(true, REAR_LEFT_AREA_ID, binding.rearLeftSeat.btnRearLeftDecrease, binding.rearLeftSeat.btnRearLeftIncrease)
        setupSeatTemperatureAndPositionControl(false, REAR_LEFT_AREA_ID, binding.rearLeftSeat.btnRearLeftSeatBackward, binding.rearLeftSeat.btnRearLeftSeatForward)

        setupSeatTemperatureAndPositionControl(true, REAR_MID_AREA_ID, binding.rearMidSeat.btnRearMidDecrease, binding.rearMidSeat.btnRearMidIncrease)
        setupSeatTemperatureAndPositionControl(false, REAR_MID_AREA_ID, binding.rearMidSeat.btnRearMidSeatBackward, binding.rearMidSeat.btnRearMidSeatForward)

        setupSeatTemperatureAndPositionControl(true, REAR_RIGHT_AREA_ID, binding.rearRightSeat.btnRearRightDecrease, binding.rearRightSeat.btnRearRightIncrease)
        setupSeatTemperatureAndPositionControl(false, REAR_RIGHT_AREA_ID, binding.rearRightSeat.btnRearRightSeatBackward, binding.rearRightSeat.btnRearRightSeatForward)
    }

    private fun setupSeatTemperatureAndPositionControl(
        isTemp: Boolean,
        areaId: Int,
        decreaseButton: View,
        increaseButton: View) {

        decreaseButton.setOnClickListener {

            val currentValue: Int
            val minValue: Int

            if(isTemp) {
                currentValue = seatCurrentTempLevels[areaId] ?: 0
                minValue = seatMinTempLevels[areaId] ?: -2
            } else {
                currentValue = seatCurrentPositions[areaId] ?: 0
                minValue = seatMinPositions[areaId] ?: 0
            }
            if (currentValue > minValue) {
                updateSeatTemperatureAndPositionProperty(isTemp, areaId, currentValue - 1)
            }
        }

        increaseButton.setOnClickListener {
            val currentValue: Int
            val maxValue: Int

            if(isTemp) {
                currentValue = seatCurrentTempLevels[areaId] ?: 0
                maxValue = seatMaxTempLevels[areaId] ?: 2
            } else {
                currentValue = seatCurrentPositions[areaId] ?: 0
                maxValue = seatMaxPositions[areaId] ?: 0
            }

            if (currentValue < maxValue) {
                updateSeatTemperatureAndPositionProperty(isTemp, areaId, currentValue + 1)
            }
        }
    }

    private fun updateSeatTemperatureAndPositionProperty(isTemp: Boolean, areaId:Int, level: Int) {
        try {
            val propertyId = if (isTemp) {
                Log.i(tag, "Set seat temperature to $level for areaId $areaId")
                VehiclePropertyIds.HVAC_SEAT_TEMPERATURE
            } else {
                Log.i(tag, "Set seat position to $level for areaId $areaId")
                VehiclePropertyIds.SEAT_FORE_AFT_POS
            }

            carPropertyManager?.setIntProperty(
                propertyId,
                areaId,
                level
            )

            if(isTemp) { seatCurrentTempLevels[areaId] = level }
            else { seatCurrentPositions[areaId] = level }
        } catch (e: Exception) {
            Log.e(tag, "Exception occurred while setting temperature or position", e)
        }
    }

    private fun updateSeatTemperatureOrPositionOnUI(isTemp: Boolean, tvLevel: TextView, value: Int?) {
        try {
            if (value == null) {
                Log.e(tag, "Received value as null to update in UI.")
            }

            if(isTemp) {
                activity?.runOnUiThread {
                    tvLevel.text = "${getLabelForTemperature(value)}"
                }
                Log.i(tag, "Seat Temperature updated: $value")
            } else {
                activity?.runOnUiThread {
                    if(value == null) {
                        tvLevel.text = "Unknown"
                    } else {
                        tvLevel.text = "$value"
                    }
                }
                Log.i(tag, "Seat Position updated: $value")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error occurred while updating temperature UI. error: ", e)
        }
    }

    private fun getLabelForTemperature(temperature: Int?): String {
        return when(temperature) {
            -2 -> "Cool High"
            -1 -> "Cool Low"
            0 -> "Off"
            1 -> "Heat Low"
            2 -> "Heat High"
            else -> "Unknown"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carPropertyManager?.unsubscribePropertyEvents(seatCallback)
        car?.disconnect()
        _binding=null
    }
}
package Util

import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import java.lang.reflect.Modifier

class VehiclePropertyUtil {
    companion object {
        @JvmStatic // Optional: makes it visible as a static method in Java
        fun getVehiclePropertyNameFromId(vehiclePropertyId: Int) : String {
            return when(vehiclePropertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> "PERF_VEHICLE_SPEED"
                VehiclePropertyIds.EV_BATTERY_LEVEL -> "EV_BATTERY_LEVEL"
                VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> "INFO_EV_BATTERY_CAPACITY"
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> "EV_CHARGE_PORT_CONNECTED"
                VehiclePropertyIds.HVAC_AC_ON -> "HVAC_AC_ON"
                else -> "UNKNOWN"
            }
        }

        @JvmStatic
        fun getSupportedAreaIdsForHvac(carPropertyManager: CarPropertyManager?): Int? {
            return try {
                /*carPropertyManager?.getPropertyList()?.forEach {
                    Log.i(
                        "VehiclePropertyUtil",
                        "Visible property: ${it.toString()}"
                    )
                }*/
                val config = carPropertyManager?.getPropertyList()?.firstOrNull{
                    it.propertyId == VehiclePropertyIds.HVAC_AC_ON
                }
                if (config == null) {
                    Log.e(
                        "VehiclePropertyUtil",
                        "HVAC_AC_ON config not visible. Likely requires privileged permission."
                    )
                    return null
                }

                val areaIds=config.areaIds
                Log.i("VehiclePropertyUtil","HVAC_AC_ON supported areaIds=${areaIds?.joinToString()}")
                areaIds?.firstOrNull()
            } catch (e: Exception) {
                Log.e("VehiclePropertyUtil", "Failed to fetch HVAC_AC_ON config", e)
                null
            }
        }
    }

    object vehiclePropertyUtil {
        private val propertyIdToNameMap: Map<Int, String> by lazy {
            val map = mutableMapOf<Int, String>()

            for(field in VehiclePropertyIds::class.java.declaredFields) {
                try {
                    if(Modifier.isStatic(field.modifiers) && field.type == Int::class.javaPrimitiveType) {
                        val key = field.getInt(null)
                        val name = field.name
                        map[key] = name
                    }
                } catch (e: Exception) {
                    Log.e("VehiclePropertyUtil", "Failed to read VehiclePropertyIds field ${field.name}", e)
                }
            }
            map
        }

        fun getPropertyName(propertyId: Int): String {
            return propertyIdToNameMap[propertyId] ?: "UNKNOWN PROPERTY($propertyId)"
        }
    }
}
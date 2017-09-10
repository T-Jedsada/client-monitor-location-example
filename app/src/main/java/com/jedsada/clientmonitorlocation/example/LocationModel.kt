package com.jedsada.clientmonitorlocation.example

data class LocationModel(val deviceId: String? = null, val deviceLocation: DeviceLocationModel? = null)

data class DeviceLocationModel(val latitude: Double? = 0.0, val longitude: Double? = 0.0)
package com.qtwl.YitongAIzhuanzhan

import java.net.NetworkInterface

object IpHelper {
    fun getAllIps(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        addr.hostAddress?.let { ips.add(it) }
                    }
                }
            }
        } catch (_: Exception) {}
        return ips
    }
}
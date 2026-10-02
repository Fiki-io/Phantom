package com.phantom.tube.core.util

import java.util.Locale

data class RegionOption(
    val code: String,
    val name: String,
    val flag: String
)

object RegionHelper {

    val SUPPORTED_REGIONS = listOf(
        RegionOption("ID", "Indonesia", "🇮🇩"),
        RegionOption("GLOBAL", "Global / Internasional", "🌐"),
        RegionOption("US", "United States", "🇺🇸"),
        RegionOption("JP", "Japan", "🇯🇵"),
        RegionOption("KR", "South Korea", "🇰🇷"),
        RegionOption("GB", "United Kingdom", "🇬🇧"),
        RegionOption("IN", "India", "🇮🇳"),
        RegionOption("BR", "Brazil", "🇧🇷"),
        RegionOption("DE", "Germany", "🇩🇪"),
        RegionOption("FR", "France", "🇫🇷"),
        RegionOption("RU", "Russia", "🇷🇺"),
        RegionOption("ES", "Spain", "🇪🇸"),
        RegionOption("CA", "Canada", "🇨🇦"),
        RegionOption("AU", "Australia", "🇦🇺"),
        RegionOption("SG", "Singapore", "🇸🇬"),
        RegionOption("MY", "Malaysia", "🇲🇾")
    )

    val SUPPORTED_REGION_CODES = SUPPORTED_REGIONS.map { it.code }.toSet()

    /**
     * Otomatis deteksi wilayah / negara sistem Android user pada first install.
     * Jika negara pengguna tidak ada di daftar pilihan terdaftar, otomatis fallback ke "GLOBAL" (Internasional).
     */
    fun resolveDefaultCountry(): String {
        return try {
            val systemCountry = Locale.getDefault().country.uppercase()
            if (systemCountry.isNotBlank() && systemCountry in SUPPORTED_REGION_CODES) {
                systemCountry
            } else {
                "GLOBAL" // Otomatis fallback ke Global / Internasional bila tidak terdaftar
            }
        } catch (_: Exception) {
            "GLOBAL"
        }
    }
}

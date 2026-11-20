package com.example.data.local

import com.example.domain.model.EmergencyNumberEntry

object EmergencyNumbersDataset {

    val allEntries: List<EmergencyNumberEntry> = listOf(
        // Bangladesh
        EmergencyNumberEntry("BD", "Bangladesh", "Bangladesh Police / Fire / Ambulance", "National Emergency Service", "999", "National Emergency", "Bangladesh Government ICT Division", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Bangladesh Meteorological Department (BMD)", "Weather & Cyclone IVR Forecast", "1090", "Weather Service", "BMD / DDM Verified", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Department of Disaster Management", "Disaster Early Warning Hotline", "10941", "Disaster Service", "DDM Bangladesh", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Bangladesh Fire Service & Civil Defence", "Fire & Rescue Control Room", "16163", "Fire & Rescue", "FSCD Bangladesh", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Shastho Batayon (DGHS)", "24/7 Medical Helpline & Ambulance Info", "16263", "Medical", "Ministry of Health BD", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Bangladesh Coast Guard", "Maritime Search & Rescue", "16111", "Coast Guard", "Bangladesh Coast Guard", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "DESCO / DPDC Power Support", "Electricity Outage & Line Hazard", "16116", "Electricity", "Dhaka Power Utilities", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Titas Gas Transmission", "Gas Leak Emergency", "16496", "Gas Emergency", "Titas Gas", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Dhaka WASA", "Water Supply & Drainage", "16162", "Water Utility", "Dhaka WASA", "2025-10"),
        EmergencyNumberEntry("BD", "Bangladesh", "Bangladesh Government Info Service", "Government Citizen Helpline", "333", "Government", "a2i Bangladesh", "2025-10"),

        // India
        EmergencyNumberEntry("IN", "India", "ERSS India", "Single Emergency Number (Police/Fire/Medical)", "112", "National Emergency", "MHA Government of India", "2025-10"),
        EmergencyNumberEntry("IN", "India", "National Disaster Management Authority", "NDMA Disaster Helpline", "1078", "Disaster Service", "NDMA India", "2025-10"),
        EmergencyNumberEntry("IN", "India", "India Police", "Police Emergency", "100", "Police", "Government of India", "2025-10"),
        EmergencyNumberEntry("IN", "India", "Fire & Rescue India", "Fire Service", "101", "Fire & Rescue", "Government of India", "2025-10"),
        EmergencyNumberEntry("IN", "India", "Ambulance Service", "Medical Ambulance", "108", "Medical", "NHM India", "2025-10"),

        // Japan
        EmergencyNumberEntry("JP", "Japan", "National Police Agency", "Police Emergency", "110", "Police", "Government of Japan", "2025-10"),
        EmergencyNumberEntry("JP", "Japan", "Fire & Disaster Management Agency", "Fire, Ambulance & Rescue", "119", "Fire / Medical", "FDMA Japan", "2025-10"),
        EmergencyNumberEntry("JP", "Japan", "Japan Coast Guard", "Maritime & Coastal Emergency", "118", "Coast Guard", "Japan Coast Guard", "2025-10"),
        EmergencyNumberEntry("JP", "Japan", "NTT Disaster Voice Messaging", "Disaster Emergency Message Dial", "171", "Disaster Service", "NTT / Cabinet Office JP", "2025-10"),

        // United States & Canada
        EmergencyNumberEntry("US", "United States", "US Public Safety Answering Point", "Police, Fire & Ambulance", "911", "National Emergency", "FCC / US Gov", "2025-10"),
        EmergencyNumberEntry("US", "United States", "988 Lifeline", "Suicide & Crisis Lifeline", "988", "Crisis Helpline", "SAMHSA US Gov", "2025-10"),
        EmergencyNumberEntry("CA", "Canada", "Canada 9-1-1", "Police, Fire & Ambulance", "911", "National Emergency", "CRTC Canada", "2025-10"),

        // United Kingdom
        EmergencyNumberEntry("GB", "United Kingdom", "UK Emergency Services", "Police, Fire, Ambulance & Coastguard", "999", "National Emergency", "GOV.UK", "2025-10"),
        EmergencyNumberEntry("GB", "United Kingdom", "UK Power Networks / ENA", "National Power Cut Emergency", "105", "Electricity", "Energy Networks Assoc. UK", "2025-10"),
        EmergencyNumberEntry("GB", "United Kingdom", "Environment Agency", "Floodline Warnings & Advice", "03459881188", "Flood Service", "GOV.UK Floodline", "2025-10"),

        // Australia
        EmergencyNumberEntry("AU", "Australia", "Triple Zero", "Police, Fire & Ambulance", "000", "National Emergency", "Australian Government", "2025-10"),
        EmergencyNumberEntry("AU", "Australia", "State Emergency Service (SES)", "Storm & Flood Emergency Assistance", "132500", "Disaster Service", "Australian SES", "2025-10"),

        // Singapore, UAE, Saudi Arabia, Europe, Neighbors
        EmergencyNumberEntry("SG", "Singapore", "Singapore Police Force", "Police Emergency", "999", "Police", "Gov.sg", "2025-10"),
        EmergencyNumberEntry("SG", "Singapore", "SCDF", "Fire & Emergency Ambulance", "995", "Fire / Medical", "SCDF Singapore", "2025-10"),
        EmergencyNumberEntry("AE", "United Arab Emirates", "UAE Police", "Police Emergency", "999", "Police", "UAE Government", "2025-10"),
        EmergencyNumberEntry("AE", "United Arab Emirates", "UAE Civil Defence / Ambulance", "Ambulance & Rescue", "998", "Medical", "UAE Government", "2025-10"),
        EmergencyNumberEntry("SA", "Saudi Arabia", "Saudi Unified Emergency", "Security, Fire & Medical", "911", "National Emergency", "MOI Saudi Arabia", "2025-10"),
        EmergencyNumberEntry("DE", "Germany", "Notruf Deutschland", "Police (110) / Fire & EMS (112)", "112", "National Emergency", "BBK Germany", "2025-10"),
        EmergencyNumberEntry("FR", "France", "SAMU / Sécurité Civile", "Medical (15) & EU Emergency (112)", "112", "National Emergency", "Gouvernement.fr", "2025-10"),
        EmergencyNumberEntry("ES", "Spain", "112 Emergencias", "Police, Fire & Medical", "112", "National Emergency", "Gobierno de España", "2025-10"),
        EmergencyNumberEntry("NP", "Nepal", "Nepal Police / Rescue", "Police Emergency (100) / Fire (101)", "100", "Police", "Nepal Government", "2025-10"),
        EmergencyNumberEntry("PK", "Pakistan", "Rescue 1122", "Emergency Rescue, Fire & Medical", "1122", "National Emergency", "Rescue 1122 Pakistan", "2025-10"),
        EmergencyNumberEntry("MY", "Malaysia", "MERS 999", "Police, Fire & Ambulance", "999", "National Emergency", "Malaysia Gov", "2025-10"),
        EmergencyNumberEntry("ID", "Indonesia", "Layanan Darurat 112", "National Emergency Call", "112", "National Emergency", "Kominfo Indonesia", "2025-10"),
        EmergencyNumberEntry("KR", "South Korea", "Korea National Emergency", "Police (112) / Fire & EMS (119)", "119", "Fire / Medical", "MOIS Korea", "2025-10")
    )

    fun countries(): List<Pair<String, String>> =
        allEntries.map { it.countryCode to it.countryName }.distinct().sortedBy { it.second }

    fun forCountry(countryCode: String?): List<EmergencyNumberEntry> {
        val code = countryCode?.uppercase()?.takeIf { it.length == 2 } ?: return allEntries.filter { it.countryCode == "BD" }
        val exact = allEntries.filter { it.countryCode == code }
        return if (exact.isNotEmpty()) exact else allEntries.filter { it.countryCode in setOf("BD", "GB", "US") }
    }
}

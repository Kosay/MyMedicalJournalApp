package com.example.data

/** A single entry in the standard childhood vaccination schedule (reference data only). */
data class VaccineScheduleEntry(
    val vaccineName: String,
    val doseNumber: Int,
    val recommendedAgeMonths: Int
)

/**
 * Commonly recognized core childhood vaccines (WHO/CDC-aligned) with their typical
 * recommended age in months. This is general reference information to help a parent
 * see what's typically due next — not medical advice, and not region-specific; always
 * follow the schedule given by the child's own pediatrician/local health authority.
 */
object VaccineSchedule {
    val standardSchedule: List<VaccineScheduleEntry> = listOf(
        VaccineScheduleEntry("BCG", 1, 0),
        VaccineScheduleEntry("Hepatitis B", 1, 0),
        VaccineScheduleEntry("Hepatitis B", 2, 1),
        VaccineScheduleEntry("Hepatitis B", 3, 6),
        VaccineScheduleEntry("DTaP", 1, 2),
        VaccineScheduleEntry("DTaP", 2, 4),
        VaccineScheduleEntry("DTaP", 3, 6),
        VaccineScheduleEntry("Polio (IPV)", 1, 2),
        VaccineScheduleEntry("Polio (IPV)", 2, 4),
        VaccineScheduleEntry("Polio (IPV)", 3, 6),
        VaccineScheduleEntry("Hib", 1, 2),
        VaccineScheduleEntry("Hib", 2, 4),
        VaccineScheduleEntry("Hib", 3, 6),
        VaccineScheduleEntry("PCV (Pneumococcal)", 1, 2),
        VaccineScheduleEntry("PCV (Pneumococcal)", 2, 4),
        VaccineScheduleEntry("PCV (Pneumococcal)", 3, 6),
        VaccineScheduleEntry("Rotavirus", 1, 2),
        VaccineScheduleEntry("Rotavirus", 2, 4),
        VaccineScheduleEntry("MMR", 1, 12),
        VaccineScheduleEntry("Varicella", 1, 12),
        VaccineScheduleEntry("DTaP", 4, 18),
        VaccineScheduleEntry("MMR", 2, 48),
        VaccineScheduleEntry("DTaP", 5, 48)
    )
}

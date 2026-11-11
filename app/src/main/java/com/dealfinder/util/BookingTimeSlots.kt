package com.dealfinder.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generiert lokale Demo-Zeitfenster für einen gewählten Tag.
 *
 * WICHTIG (MVP-Hinweis): Es gibt noch keinen angebundenen Anbieter-Kalender.
 * Diese Fenster sind reine Demo-Daten und dürfen in der UI nicht als reale
 * Verfügbarkeiten dargestellt werden – die Screens kennzeichnen das entsprechend.
 *
 * Die Logik ist absichtlich reine Kotlin-Funktion (kein Android-Bezug), damit
 * sie direkt unit-getestet werden kann.
 */
object BookingTimeSlots {

    /** Öffnungszeiten der Demo-Anbieter (lokal, ohne Kalenderanbindung). */
    const val OPENING_HOUR_START = 9
    const val OPENING_HOUR_END = 18

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Erzeugt Zeitfenster von [openingHourStart] bis [openingHourEnd] Uhr,
     * aufgeteilt in Schritte von [durationMinutes].
     *
     * - Vergangene Tage liefern keine Fenster.
     * - Am heutigen Tag werden Fenster ausgelassen, die bereits begonnen haben.
     */
    fun generate(
        date: LocalDate,
        durationMinutes: Int = 60,
        now: LocalDateTime = LocalDateTime.now(),
        openingHourStart: Int = OPENING_HOUR_START,
        openingHourEnd: Int = OPENING_HOUR_END
    ): List<String> {
        if (date.isBefore(now.toLocalDate())) return emptyList()

        val step = durationMinutes.coerceIn(15, (openingHourEnd - openingHourStart) * 60)
        val slots = mutableListOf<String>()

        var start = date.atTime(openingHourStart, 0)
        val dayEnd = date.atTime(openingHourEnd, 0)

        while (!start.plusMinutes(step.toLong()).isAfter(dayEnd)) {
            val slotEnd = start.plusMinutes(step.toLong())
            val isFutureSlot = date.isAfter(now.toLocalDate()) || start.isAfter(now)
            if (isFutureSlot) {
                slots.add("${start.format(timeFormatter)} - ${slotEnd.format(timeFormatter)}")
            }
            start = slotEnd
        }
        return slots
    }
}

package com.example.giaodien.data.model

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Server uses restaurant-local dates, not the device timezone. */
object BookingAvailability {
    val restaurantZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    fun today(clock: Clock = Clock.systemUTC()): LocalDate = LocalDate.now(clock.withZone(restaurantZone))
    fun selectable(slot: BanSlot, clock: Clock = Clock.systemUTC()): Boolean = runCatching {
        val date = LocalDate.parse(slot.ngay.take(10))
        val start = LocalTime.parse(slot.khungGio.substringBefore('-'))
        slot.soBanConLai > 0 && !date.isBefore(today(clock)) && !date.isAfter(today(clock).plusDays(6)) &&
            date.atTime(start).atZone(restaurantZone).toInstant().isAfter(clock.instant())
    }.getOrDefault(false)
}

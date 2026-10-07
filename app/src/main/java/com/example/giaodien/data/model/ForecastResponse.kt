package com.example.giaodien.data.model
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.LocalDateTime

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long, // timestamp unix
    val main: Main,
    val weather: List<Weather>
)

fun ForecastItem.getDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(dt), ZoneId.systemDefault())
}

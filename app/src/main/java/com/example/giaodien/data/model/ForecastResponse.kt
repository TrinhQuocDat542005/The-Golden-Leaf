package com.example.giaodien.data.model
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
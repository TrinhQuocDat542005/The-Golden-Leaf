package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.BuildConfig
import com.example.giaodien.data.model.ForecastResponse
import com.example.giaodien.thotiet.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Weather is optional; no key/network must never block the booking journey. */
class WeatherViewModel(
    private val apiKey: String = BuildConfig.WEATHER_API_KEY,
    private val fetch: suspend (String, String) -> ForecastResponse? = { city, key ->
        val response = RetrofitClient.api.getForecast(city, key)
        if (response.isSuccessful) response.body() else throw java.io.IOException("Weather unavailable")
    }
) : ViewModel() {
    private val _forecast = MutableStateFlow<ForecastResponse?>(null)
    val forecast: StateFlow<ForecastResponse?> = _forecast
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun loadForecast(city: String = "Ho Chi Minh City") {
        if (apiKey.isBlank()) { _message.value = "Thời tiết chưa được cấu hình; bạn vẫn có thể đặt bàn."; return }
        viewModelScope.launch {
            try { _forecast.value = fetch(city, apiKey); _message.value = if (_forecast.value == null) "Không có dự báo cho thời điểm này." else null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _forecast.value = null; _message.value = "Không lấy được thời tiết; bạn vẫn có thể đặt bàn." }
        }
    }
}

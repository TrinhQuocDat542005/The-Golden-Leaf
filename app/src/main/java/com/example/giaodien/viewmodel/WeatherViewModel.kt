package com.example.giaodien.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giaodien.data.model.ForecastItem
import com.example.giaodien.data.model.WeatherResponse
import com.example.giaodien.thotiet.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.giaodien.data.model.ForecastResponse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


//class WeatherViewModel : ViewModel() {
//    private val _weather = MutableStateFlow<WeatherResponse?>(null)
//    val weather: StateFlow<WeatherResponse?> = _weather
//
//    private val apiKey = "5b9c46aa412ad72dedf807c4f01bb140" // 🔑 Thay bằng key của bạn
//
//    fun loadWeather(city: String) {
//        viewModelScope.launch {
//            val response = RetrofitClient.api.getWeather(city, apiKey)
//            if (response.isSuccessful) {
//                _weather.value = response.body()
//            }
//        }
//    }
//}

class WeatherViewModel : ViewModel() {
    private val _forecast = MutableStateFlow<ForecastResponse?>(null)
    val forecast: StateFlow<ForecastResponse?> = _forecast

    private val apiKey = "5b9c46aa412ad72dedf807c4f01bb140" // Thay bằng key của bạn

    fun loadForecast(city: String) {
        viewModelScope.launch {
            val response = RetrofitClient.api.getForecast(city, apiKey)
            if (response.isSuccessful) {
                _forecast.value = response.body()
            }
        }
    }

    // Hàm lấy dữ liệu theo ngày + khung giờ
    fun getForecastForSlot(selectedDate: LocalDate, khungStart: Int, khungEnd: Int): List<ForecastItem> {
        val list = _forecast.value?.list ?: return emptyList()
        return list.filter {
            val dateTime = Instant.ofEpochSecond(it.dt).atZone(ZoneId.systemDefault()).toLocalDate()
            val hour = Instant.ofEpochSecond(it.dt).atZone(ZoneId.systemDefault()).hour
            dateTime == selectedDate && hour in khungStart..khungEnd
        }
    }
}

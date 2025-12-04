    package com.example.giaodien.viewmodel
    import android.util.Log

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.giaodien.data.model.ThucDon
    import com.example.giaodien.data.repository.ThucDonRepository
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch

    class ThucDonViewModel : ViewModel() {

        private val repository = ThucDonRepository()

        private val _thucDonList = MutableStateFlow<List<ThucDon>>(emptyList())
        val thucDonList: StateFlow<List<ThucDon>> = _thucDonList

        fun loadThucDon() {
            viewModelScope.launch {
                try {
                    val list = repository.getAll()

                    // Đặt base URL theo môi trường bạn chạy
                    val baseUrl = "http://10.0.2.2:8080/uploads/"
                    // Nếu chạy điện thoại thật: đổi thành IP máy bạn
                    // val baseUrl = "http://192.168.1.5:8080/uploads/"

                    val updatedList = list.map { item ->
                        // Ghép URL ảnh đầy đủ
                        item.copy(anh = baseUrl + item.anh)
                    }

                    _thucDonList.value = updatedList
                    Log.d("ThucDonViewModel", "Loaded ${updatedList.size} món ăn (with image URLs)")
                } catch (e: Exception) {
                    Log.e("ThucDonViewModel", "Failed to load ThucDon", e)
                }
            }
        }


    }

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

                    val updatedList = list.map { item ->
                        item.copy(anh = com.example.giaodien.data.model.menuImageUrl(item.anh, com.example.giaodien.BuildConfig.API_BASE_URL))
                    }

                    _thucDonList.value = updatedList
                    Log.d("ThucDonViewModel", "Loaded ${updatedList.size} món ăn (with image URLs)")
                } catch (e: Exception) {
                    Log.e("ThucDonViewModel", "Failed to load ThucDon", e)
                }
            }
        }


    }

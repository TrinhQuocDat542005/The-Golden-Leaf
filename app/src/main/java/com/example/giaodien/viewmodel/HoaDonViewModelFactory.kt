import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.giaodien.data.repository.HoaDonRepository
import com.example.giaodien.viewmodel.HoaDonViewModel

class HoaDonViewModelFactory(private val repository: HoaDonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HoaDonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HoaDonViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

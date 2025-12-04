package com.example.giaodien.navigation
import HoaDonViewModelFactory
import com.example.giaodien.data.repository.NotificationRepository
import com.example.giaodien.data.network.RetrofitInstance
import com.example.giaodien.viewmodel.GioHangViewModelFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.giaodien.ui.screens.*
import com.example.giaodien.viewmodel.BanSlotViewModel
import com.example.giaodien.viewmodel.DatBanViewModel
import com.example.giaodien.data.model.DatBan
import com.example.giaodien.data.repository.HoaDonRepository
import com.example.giaodien.data.repository.TaiKhoanRepository
import com.example.giaodien.ui.viewmodel.NotificationViewModel
import com.example.giaodien.viewmodel.ChiTietHoaDonViewModel
import com.example.giaodien.viewmodel.ChiTietHoaDonViewModelFactory
// Thêm import này vào đầu file AppNavGraph.kt
import com.example.giaodien.viewmodel.ThucDonViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.giaodien.viewmodel.GioHangViewModel
import com.example.giaodien.viewmodel.HoaDonViewModel
import com.example.giaodien.viewmodel.NotificationViewModelFactory
import com.example.giaodien.viewmodel.WeatherViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")

    object Login : Screen("login")
    object TaiKhoan : Screen("TaiKhoan")
    object ChonMonAn : Screen("chon_mon_an")
    object NgayGio : Screen("ngay_gio")
    object TrangChu : Screen("trang_chu")
    object ThucDon : Screen("thuc_don") // màn hình thực đơn
    object YeuThich : Screen("yeu_thich")

    object HoaDon: Screen("hoa_don")
    object GioHang : Screen("gio_hang_screen")
    object Notification : Screen("notification") // thêm màn hình notification

    object SoDoBan : Screen("soDoBan/{ngayChon}/{khungGioChon}") {
        fun createRoute(ngayChon: String, khungGioChon: String) =
            "soDoBan/$ngayChon/$khungGioChon"
    }
    object ChiTietHoaDon : Screen("chi_tiet_hoa_don/{idDat}") {
        fun createRoute(idDat: Long) = "chi_tiet_hoa_don/$idDat"
    }



    object ViTriBan : Screen("vi_tri_ban/{ngayChon}/{khungGioChon}/{banConLai}") {
        fun createRoute(ngayChon: String, khungGioChon: String, banConLai: Int) =
            "vi_tri_ban/$ngayChon/$khungGioChon/$banConLai"
    }

    object NhapSoLuong :
        Screen("nhap_so_luong/{ngayChon}/{khungGioChon}/{viTriBan}/{banConLai}") {
        fun createRoute(
            ngayChon: String,
            khungGioChon: String,
            viTriBan: String,
            banConLai: Int
        ) = "nhap_so_luong/$ngayChon/$khungGioChon/$viTriBan/$banConLai"
    }
    object ThanhToan : Screen("thanh_toan/{method}/{idDat}/{tienBan}/{tienAn}") {
        fun createRoute(method: String, idDat: Long, tienBan: Double, tienAn: Double): String {
            return "thanh_toan/$method/$idDat/$tienBan/$tienAn"
        }
    }





}

@Composable
fun AppNavGraph(navController: NavHostController) {

    val notificationRepo = NotificationRepository(RetrofitInstance.api)
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(notificationRepo)
    )
    val banSlotViewModel: BanSlotViewModel = viewModel()
    val datBanViewModel: DatBanViewModel = viewModel()
    val slots by banSlotViewModel.slots.collectAsState()

    val apiService = RetrofitInstance.api
    val hoaDonRepository = HoaDonRepository(RetrofitInstance.api)
    val hoaDonViewModel: HoaDonViewModel = viewModel(
        factory = HoaDonViewModelFactory(hoaDonRepository)
    )



    val gioHangViewModel: GioHangViewModel = viewModel(
        factory = GioHangViewModelFactory(apiService)
    )
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        // Login
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.TrangChu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },

            )
        }
        composable(Screen.HoaDon.route) {
            HoaDonScreen(
                navController = navController,
                gioHangViewModel = gioHangViewModel,
                hoaDonViewModel = hoaDonViewModel // <-- truyền instance ở đây
            )
        }

        composable(Screen.TrangChu.route) {
            TrangChuScreen(
                navController = navController,
                notificationViewModel = notificationViewModel
            )
        }

        composable(Screen.Notification.route) {
            NotificationScreen(vm = notificationViewModel)
        }

        composable(Screen.ThucDon.route) {
            ThucDonScreen(navController = navController)
        }
        composable(Screen.YeuThich.route) {
            YeuThichScreen(
                navController = navController
            )
        }
        composable(
            route = "mo_ta/{idThucDon}",
            arguments = listOf(navArgument("idThucDon") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("idThucDon") ?: 0L

            // THAY THẾ DÒNG GỌI NÀY:
            // MoTaScreen(id = id)

            // BẰNG DÒNG GỌI CÓ THAM SỐ onBack SỬ DỤNG navController
            MoTaScreen(
                id = id,
                navController = navController,
                onBack = {
                    navController.navigate(Screen.ThucDon.route) {
                        popUpTo(Screen.ThucDon.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )


        }




        // Tài khoản
        composable(Screen.TaiKhoan.route) {
            TaiKhoanScreen(navController = navController)
        }
        composable(
            route = Screen.ChiTietHoaDon.route,
            arguments = listOf(navArgument("idDat") { type = NavType.LongType })
        ) { entry ->
            val idDat = entry.arguments?.getLong("idDat") ?: 0L

            // Tạo repository và viewModel ở đây
            val repository = TaiKhoanRepository(RetrofitInstance.api)
            val viewModel: ChiTietHoaDonViewModel = viewModel(
                factory = ChiTietHoaDonViewModelFactory(repository)
            )

            ChiTietHoaDonScreen(
                navController = navController,
                idDat = idDat,
                viewModel = viewModel
            )
        }


        // NgayGio
        composable(Screen.NgayGio.route) {
            NgayGioScreen(viewModel = banSlotViewModel, navController = navController)
        }

        // SoDoBan
        composable(
            route = Screen.SoDoBan.route,
            arguments = listOf(
                navArgument("ngayChon") { type = NavType.StringType },
                navArgument("khungGioChon") { type = NavType.StringType }
            )
        ) { entry ->
            val ngayChon = entry.arguments?.getString("ngayChon") ?: ""
            val khungGioChon = entry.arguments?.getString("khungGioChon") ?: ""

            SoDoBanScreen(
                navController = navController,
                ngayChon = ngayChon,
                khungGioChon = khungGioChon
            )
        }


// NavGraph.kt
        composable(
            route = Screen.ViTriBan.route,
            arguments = listOf(
                navArgument("ngayChon") { type = NavType.StringType },
                navArgument("khungGioChon") { type = NavType.StringType },
                navArgument("banConLai") { type = NavType.IntType }
            )
        ) { entry ->
            // Lấy argument từ route
            val ngayChon = entry.arguments?.getString("ngayChon") ?: ""
            val khungGioChon = entry.arguments?.getString("khungGioChon") ?: ""
            val banConLai = entry.arguments?.getInt("banConLai") ?: 0

            // ViewModel thời tiết
            val weatherViewModel: WeatherViewModel = viewModel()

            // Hiển thị màn hình vị trí bàn
            ViTriBanScreen(
                weatherViewModel = weatherViewModel,
                ngayChon = ngayChon,
                khungGioChon = khungGioChon,
//                banConLai = banConLai,
                onBack = { navController.popBackStack() },
                onNext = { viTriBan ->
                    // Chuyển sang màn hình nhập số lượng
                    navController.navigate(
                        Screen.NhapSoLuong.createRoute(
                            ngayChon = ngayChon,
                            khungGioChon = khungGioChon,
                            viTriBan = viTriBan,
                            banConLai = banConLai
                        )
                    )
                }
            )
        }


        // NhapSoLuong
        composable(
            route = Screen.NhapSoLuong.route,
            arguments = listOf(
                navArgument("ngayChon") { type = NavType.StringType },
                navArgument("khungGioChon") { type = NavType.StringType },
                navArgument("viTriBan") { type = NavType.StringType },
                navArgument("banConLai") { type = NavType.IntType }
            )
        ) { entry ->
            val ngayChon = entry.arguments?.getString("ngayChon") ?: ""
            val khungGioChon = entry.arguments?.getString("khungGioChon") ?: ""
            val viTriBan = entry.arguments?.getString("viTriBan") ?: ""
            val banConLai = entry.arguments?.getInt("banConLai") ?: 0

            NhapSoLuongScreen(
                navController = navController,
                ngayChon = ngayChon,
                khungGioChon = khungGioChon,
                viTriBan = viTriBan,
                banConLai = banConLai,
                onDatBan = { soLuong, ghiChu ->
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val datBan = DatBan(
                        idDat = null,
                        email = currentUser?.email ?: "",
                        ten = currentUser?.displayName ?: "Khách",
                        ngay = ngayChon,
                        khungGio = khungGioChon,
                        soLuong = soLuong,
                        ghiChu = ghiChu,
                        viTriBan = viTriBan
                    )

                    datBanViewModel.datBan(
                        datBan = datBan,
                        onSuccess = { savedDatBan ->
                            datBanViewModel.setDatBan(savedDatBan) // <-- QUAN TRỌNG
                            val datBanId = savedDatBan.idDat
                            if (datBanId != null) {
                                gioHangViewModel.setDatBanId(datBanId) // ⚡ cập nhật idDat cho giỏ hàng
                            }
                            navController.navigate(Screen.ChonMonAn.route)
                        },
                        onError = { msg ->
                            Log.e("DatBan", "Lỗi khi đặt bàn: $msg")
                        }
                    )
                }
            )
        }
        // ChonMonAn
// ChonMonAn
        composable(Screen.ChonMonAn.route) {
            // TRUYỀN INSTANCE CHUNG
            ChonMonAnScreen(
                navController = navController,
                gioHangViewModel = gioHangViewModel
            )
        }


        composable(Screen.GioHang.route) {
            GioHangScreen(
                navController = navController,
                gioHangViewModel = gioHangViewModel
            )
        }
        composable(
            route = Screen.ThanhToan.route,
            arguments = listOf(
                navArgument("method") { type = NavType.StringType },
                navArgument("idDat") { type = NavType.LongType },
                navArgument("tienBan") { type = NavType.FloatType },
                navArgument("tienAn") { type = NavType.FloatType }
            )
        ) { entry ->
            val method = entry.arguments?.getString("method") ?: ""
            val idDat = entry.arguments?.getLong("idDat") ?: 0L
            val tienBan = entry.arguments?.getFloat("tienBan")?.toDouble() ?: 0.0
            val tienAn = entry.arguments?.getFloat("tienAn")?.toDouble() ?: 0.0



            // Set thông tin thanh toán
            hoaDonViewModel.setThongTinThanhToan(idDat, tienBan, tienAn)

            ThanhToanScreen(
                navController = navController,
                method = method,
                viewModel = hoaDonViewModel
            )
        }














    }
}

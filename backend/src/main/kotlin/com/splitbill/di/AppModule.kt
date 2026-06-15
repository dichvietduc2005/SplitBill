package com.splitbill.di

import com.splitbill.auth.JwtConfig
import com.splitbill.data.BillRepository
import com.splitbill.data.GroupRepository
import com.splitbill.data.UserRepository
import com.splitbill.service.AuthService
import com.splitbill.service.BillService
import com.splitbill.service.GroupService
import com.splitbill.service.ProfileService
import org.koin.dsl.module

/**
 * Koin Dependency Injection Module.
 *
 * Đăng ký tất cả Repository và Service.
 * Koin sẽ tự động inject các dependency vào constructor.
 *
 * Lifecycle:
 * - Repository: single (1 instance duy nhất trong suốt vòng đời app)
 * - Service: single (1 instance, nhận Repository qua constructor)
 * - JwtConfig: single (nhận ApplicationEnvironment qua parameter)
 */
val appModule = module {
    // Repositories
    single { UserRepository() }
    single { GroupRepository() }
    single { BillRepository() }

    // JwtConfig — inject ApplicationEnvironment từ Koin's parameter
    single { JwtConfig(get()) }

    // Services
    single { AuthService(get(), get()) }
    single { GroupService(get(), get()) }
    single { BillService(get(), get(), get()) }
    single { ProfileService(get()) }
}

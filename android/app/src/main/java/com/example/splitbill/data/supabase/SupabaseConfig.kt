package com.example.splitbill.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    const val SUPABASE_URL = "https://qvqcvwepxluzatmzgiix.supabase.co"
    
    // Khóa anon public từ Supabase Dashboard (đọc từ local.properties qua BuildConfig)
    var supabaseAnonKey: String = com.example.splitbill.BuildConfig.SUPABASE_ANON_KEY

    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() {
            if (_client == null) {
                _client = createSupabaseClient(
                    supabaseUrl = SUPABASE_URL,
                    supabaseKey = supabaseAnonKey
                ) {
                    install(Auth)
                    install(Postgrest)
                    install(Storage)
                    install(Realtime)
                }
            }
            return _client!!
        }

    fun init(anonKey: String) {
        if (anonKey.isNotBlank()) {
            supabaseAnonKey = anonKey
            _client = null // Reset để khởi tạo lại với key mới
        }
    }
}

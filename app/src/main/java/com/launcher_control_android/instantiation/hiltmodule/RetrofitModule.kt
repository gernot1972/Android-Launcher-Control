package com.launcher_control_android.instantiation.hiltmodule

import com.launcher_control_android.BuildConfig
import com.launcher_control_android.api.HeaderHttpInterceptor
import com.launcher_control_android.api.service.EntryApiModule
import com.launcher_control_android.helper.util.PrefUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RetrofitModule {
    @Singleton
    @Provides
    fun getRetrofit(prefUtil: PrefUtil) = Retrofit.Builder().baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create()
        )
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().setLevel(
                        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                    )
                )
                .addInterceptor(HeaderHttpInterceptor(prefUtil))
                .build()
        ).build()

    @Singleton
    @Provides
    fun getEntryApiModule(retrofit: Retrofit): EntryApiModule =
        retrofit.create(EntryApiModule::class.java)
}
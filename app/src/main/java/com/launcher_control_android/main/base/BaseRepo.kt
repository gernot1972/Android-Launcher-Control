package com.launcher_control_android.main.base

import com.launcher_control_android.helper.util.NetworkUtil
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.CANCELLED
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.HTTP_ERROR
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.MISCELLANEOUS
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.NO_INTERNET
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.SUCCESS
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.TIME_OUT
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import javax.inject.Inject

open class BaseRepo {
    @Inject
    lateinit var nwUtil: NetworkUtil

    object ApiResultType {
        const val SUCCESS = 1
        const val NO_INTERNET = 2
        const val HTTP_ERROR = 3
        const val TIME_OUT = 4
        const val MISCELLANEOUS = 5
        const val CANCELLED = 6
    }

    internal suspend fun <T : Any> apiCall(executable: suspend () -> T): ApiResult<T> {
        return try {
            if (nwUtil.isNetworkAvailable())
                ApiResult(executable.invoke())
            else
                ApiResult(null, NO_INTERNET, "Internet not connected")
        } catch (e: HttpException) {
            //todo : parse e.response() using ApiErrorUtil.kt to get error message
            ApiResult(null, HTTP_ERROR, e.message ?: "Something went wrong", resCode = e.code())
        } catch (e: SocketTimeoutException) {
            ApiResult(null, TIME_OUT, "Time out")
        } catch (e: CancellationException) {
            ApiResult(null, CANCELLED, "")
        } catch (e: Throwable) {
            ApiResult(null, MISCELLANEOUS, e.message ?: "Something went wrong")
        }
    }
}

data class ApiResult<T>(
    val data: T?,
    val resultType: Int = SUCCESS,
    val error: String? = null,
    val reqCode: Int = -1,
    val resCode: Int = -1
)
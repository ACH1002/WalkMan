package inu.appcenter.walkman.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import inu.appcenter.walkman.BuildConfig
import inu.appcenter.walkman.domain.model.AuthResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SupabaseClient"
    }

    // 프로퍼티로 클라이언트 초기화를 지연시킴
    private val supabase: SupabaseClient by lazy {
        try {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_KEY
            ) {
                install(Auth) {
                    host = "login-callback"
                    scheme = "inu.appcenter.walkman"

                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase client initialization failed", e)
            throw e
        }
    }

    // 현재 세션 상태 확인
    fun getSessionStatus(): SessionStatus? {
        return try {
            val session = supabase.auth.currentSessionOrNull()
            Log.d(TAG, "Current session retrieved: ${session != null}")

            // 세션이 존재하고, 유저 정보도 있는지 확인
            if (session != null) {
                // 추가적인 세션 유효성 검사
                val user = session.user
                Log.d(TAG, "Session user details: $user")

                if (user != null) {
                    // 세션과 사용자 정보가 모두 있는 경우
                    SessionStatus.Authenticated(session)
                } else {
                    Log.d(TAG, "Session exists but user is null")
                    null
                }
            } else {
                Log.d(TAG, "No session found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting session status", e)
            null
        }
    }

    // 현재 사용자 ID 가져오기
    suspend fun getCurrentUserId(): String? {
        return try {
            withContext(Dispatchers.IO) {
                supabase.auth.currentUserOrNull()?.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user ID", e)
            null
        }
    }

    // 이메일로 회원가입
    fun signUpWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow {
        try {
            withContext(Dispatchers.IO) {
                supabase.auth.signUpWith(Email) {
                    email = emailValue
                    password = passwordValue
                }
            }
            emit(AuthResponse.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up with email", e)
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    // 이메일로 로그인
    fun signInWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow {
        try {
            withContext(Dispatchers.IO) {
                supabase.auth.signInWith(Email) {
                    email = emailValue
                    password = passwordValue
                }
            }
            emit(AuthResponse.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email", e)
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    // 로그아웃
    suspend fun signOut() {
        try {
            withContext(Dispatchers.IO) {
                supabase.auth.signOut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            throw e
        }
    }

    // nonce 생성
    fun createNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.fold("") { str, it ->
            str + "%02x".format(it)
        }
    }

    fun loginGoogleUser(): Flow<AuthResponse> = flow {
        val hashedNonce = createNonce()

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .setNonce(hashedNonce)
                .setFilterByAuthorizedAccounts(false) // 모든 계정 표시
                .setAutoSelectEnabled(false) // 자동 선택 비활성화
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)

            try {
                val result = withContext(Dispatchers.IO) {
                    credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                }

                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)

                val googleIdToken = googleIdTokenCredential.idToken

                withContext(Dispatchers.IO) {
                    supabase.auth.signInWith(IDToken) {
                        idToken = googleIdToken
                        provider = Google
                    }
                }

                emit(AuthResponse.Success)
            } catch (e: NoCredentialException) {
                // 사용자가 취소하거나 자격 증명을 선택하지 않은 경우
                Log.d(TAG, "Google 로그인 취소됨: ${e.message}")
                emit(AuthResponse.Error("로그인이 취소되었습니다"))
            } catch (e: GetCredentialException) {
                // 그 외 오류
                Log.e(TAG, "Google 로그인 실패: ${e.message}")
                emit(AuthResponse.Error("Google 로그인에 실패했습니다: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google 로그인 실패", e)
            emit(AuthResponse.Error(e.localizedMessage ?: "알 수 없는 오류가 발생했습니다"))
        }
    }
}
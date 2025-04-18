package inu.appcenter.walkman.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import inu.appcenter.walkman.data.datasource.DriveServiceHelper
import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.data.repository.AppUsageRepositoryImpl
import inu.appcenter.walkman.data.repository.AuthRepositoryImpl
import inu.appcenter.walkman.data.repository.NotificationRepositoryImpl
import inu.appcenter.walkman.data.repository.SensorRepositoryImpl
import inu.appcenter.walkman.data.repository.StorageRepositoryImpl
import inu.appcenter.walkman.data.repository.UserRepositoryImpl
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.domain.repository.AuthRepository
import inu.appcenter.walkman.domain.repository.NotificationRepository
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StorageRepository
import inu.appcenter.walkman.domain.repository.UserRepository
import inu.appcenter.walkman.local.database.WalkManDatabase
import inu.appcenter.walkman.local.repository.GaitAnalysisRepository
import inu.appcenter.walkman.local.repository.GaitAnalysisRepositoryImpl
import inu.appcenter.walkman.utils.SessionManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDriveServiceHelper(
        @ApplicationContext context: Context
    ): DriveServiceHelper {
        return DriveServiceHelper(
            context = context,
            folderId = "13Lc6GyE_TquVX8E0tCStm8I8Fp9XWX_J" // 구글 드라이브 폴더 ID
        )
    }

    /**
     * 센서 저장소 제공 - 걷기 감지와 센서 데이터 수집 기능을 모두 제공하는 하이브리드 버전
     */
    @Provides
    @Singleton
    fun provideSensorRepository(
        @ApplicationContext context: Context
    ): SensorRepository {
        return SensorRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context
    ): UserRepository {
        return UserRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideStorageRepository(
        @ApplicationContext context: Context,
        driveServiceHelper: DriveServiceHelper
    ): StorageRepository {
        return StorageRepositoryImpl(context, driveServiceHelper)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        @ApplicationContext context: Context
    ): NotificationRepository {
        return NotificationRepositoryImpl(context)
    }

    /**
     * 앱 사용 저장소 제공 - 소셜 미디어 앱 감지를 위한 간소화된 버전
     */
    @Provides
    @Singleton
    fun provideAppUsageRepository(
        @ApplicationContext context: Context
    ): AppUsageRepository {
        return AppUsageRepositoryImpl(context)
    }


    @Provides
    @Singleton
    fun provideWalkManDatabase(@ApplicationContext context: Context): WalkManDatabase {
        return WalkManDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideGaitAnalysisRepository(database: WalkManDatabase): GaitAnalysisRepository {
        return GaitAnalysisRepositoryImpl(database)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        return SupabaseClient(context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(supabaseClient: SupabaseClient, sessionManager: SessionManager): AuthRepository {
        return AuthRepositoryImpl(supabaseClient, sessionManager =  sessionManager)
    }


}
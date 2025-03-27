package inu.appcenter.walkman.di


import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import inu.appcenter.walkman.data.datasource.DriveServiceHelper
import inu.appcenter.walkman.data.repository.AppUsageRepositoryImpl
import inu.appcenter.walkman.data.repository.NotificationRepositoryImpl
import inu.appcenter.walkman.data.repository.SensorRepositoryImpl
import inu.appcenter.walkman.data.repository.StepCountRepositoryImpl
import inu.appcenter.walkman.data.repository.StorageRepositoryImpl
import inu.appcenter.walkman.data.repository.UserRepositoryImpl
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.domain.repository.NotificationRepository
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StepCountRepository
import inu.appcenter.walkman.domain.repository.StorageRepository
import inu.appcenter.walkman.domain.repository.UserRepository
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

    @Provides
    @Singleton
    fun provideSensorRepository(
        @ApplicationContext context: Context,
        stepCountRepository: StepCountRepository
    ): SensorRepository {
        return SensorRepositoryImpl(context, stepCountRepository)
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
    fun provideStepCountRepository(
        @ApplicationContext context: Context
    ): StepCountRepository {
        return StepCountRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        @ApplicationContext context: Context
    ): NotificationRepository {
        return NotificationRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideAppUsageRepository(
        @ApplicationContext context: Context
    ): AppUsageRepository {
        return AppUsageRepositoryImpl(context)
    }
}
package app.tisimai.mektep.di

import android.content.Context
import androidx.room.Room
import app.tisimai.mektep.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MektepDatabase =
        Room.databaseBuilder(context, MektepDatabase::class.java, "mektep.db")
            .addMigrations(MektepDatabase.MIGRATION_1_2, MektepDatabase.MIGRATION_2_3, MektepDatabase.MIGRATION_3_4, MektepDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: MektepDatabase): UserDao = db.userDao()
    @Provides fun provideChildProfileDao(db: MektepDatabase): ChildProfileDao = db.childProfileDao()
    @Provides fun provideProgressDao(db: MektepDatabase): ProgressDao = db.progressDao()
    @Provides fun provideScreenTimeDao(db: MektepDatabase): ScreenTimeDao = db.screenTimeDao()
    @Provides fun provideParentalConfigDao(db: MektepDatabase): ParentalConfigDao = db.parentalConfigDao()
    @Provides fun provideAllowedAppDao(db: MektepDatabase): AllowedAppDao = db.allowedAppDao()
    @Provides fun provideChildSessionDao(db: MektepDatabase): ChildSessionDao = db.childSessionDao()
    @Provides fun provideQuestDao(db: MektepDatabase): QuestDao = db.questDao()

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore = TokenStore(context)

    @Provides
    @Singleton
    fun provideParentalPrefsStore(@ApplicationContext context: Context): ParentalPrefsStore = ParentalPrefsStore(context)

    @Provides
    @Singleton
    fun provideLessonLoader(@ApplicationContext context: Context): LessonLoader = LessonLoader(context)
}

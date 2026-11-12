package pl.zse.dziennik.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock
import pl.zse.dziennik.data.*
import pl.zse.dziennik.database.*
import pl.zse.dziennik.model.*

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun clock(): Clock = Clock.system(SchoolZone)
    @Provides @Singleton fun database(@ApplicationContext context: Context): SchoolDatabase =
        Room.databaseBuilder(context, SchoolDatabase::class.java, "zse-demo.db").build()
    @Provides fun dao(database: SchoolDatabase): SchoolDao = database.schoolDao()
    @Provides @Singleton fun source(clock: Clock): SnapshotSource = MockDataRepository(clock)
    @Provides @Singleton fun cachedRepository(dao: SchoolDao, source: SnapshotSource): CachedSchoolRepository = CachedSchoolRepository(dao, source)
    @Provides fun repository(cached: CachedSchoolRepository): SchoolRepository = cached
    @Provides @Singleton fun preferences(@ApplicationContext context: Context): ThemePreferences = ThemePreferences(context)
    @Provides @Singleton fun accounts(@ApplicationContext context: Context, cached: CachedSchoolRepository): AccountRepository =
        LocalAccountRepository(context, listOf(DemoAuthenticationProvider()), cached::clearStudent)
}

package pl.zse.dziennik.dashboard

import androidx.lifecycle.ViewModelStore
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pl.zse.dziennik.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val clock = MutableClock(Instant.parse("2026-11-12T07:15:00Z"))
    private val cached = SchoolSnapshot(Student("demo-zse-3ti", "Demo", "3TI", "ZSE"), emptyList(), emptyList(), emptyList(), emptyList(), clock.instant())

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel(repository: SchoolRepository) = DashboardViewModel(repository, clock).also { store.put("dashboard", it) }

    @Test fun `failed refresh keeps cache and repeated taps start a single retry`() = runTest(dispatcher) {
        var refreshes = 0
        var failing = true
        val repository = object : SchoolRepository {
            override fun observe(studentId: String) = MutableStateFlow(cached)
            override suspend fun refresh(studentId: String) {
                refreshes++
                if (failing) throw IOException()
            }
        }
        val vm = viewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(cached, vm.state.value.snapshot)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.refreshing)
        failing = false
        vm.refresh()
        vm.refresh()
        runCurrent()
        assertEquals(2, refreshes)
        assertEquals(cached, vm.state.value.snapshot)
        assertNull(vm.state.value.error)
    }

    @Test fun `read failure retains last cache and resumes observation`() = runTest(dispatcher) {
        var subscriptions = 0
        val updated = cached.copy(syncedAt = cached.syncedAt.plusSeconds(60))
        val repository = object : SchoolRepository {
            override fun observe(studentId: String): Flow<SchoolSnapshot?> = flow {
                subscriptions++
                emit(if (subscriptions == 1) cached else updated)
                if (subscriptions == 1) throw IOException()
                awaitCancellation()
            }
            override suspend fun refresh(studentId: String) = Unit
        }
        val vm = viewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(cached, vm.state.value.snapshot)
        assertNotNull(vm.state.value.error)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, subscriptions)
        assertEquals(updated, vm.state.value.snapshot)
        assertNull(vm.state.value.error)
    }

    @Test fun `first read error is visible and recovers without restarting the screen`() = runTest(dispatcher) {
        var subscriptions = 0
        val repository = object : SchoolRepository {
            override fun observe(studentId: String): Flow<SchoolSnapshot?> = flow {
                if (++subscriptions == 1) throw IOException()
                emit(cached)
                awaitCancellation()
            }
            override suspend fun refresh(studentId: String) = Unit
        }
        val vm = viewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertNull(vm.state.value.snapshot)
        assertNotNull(vm.state.value.error)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(cached, vm.state.value.snapshot)
        assertNull(vm.state.value.error)
    }

    @Test fun `countdown updates do not emit new repository screen state`() = runTest(dispatcher) {
        val repository = object : SchoolRepository {
            override fun observe(studentId: String) = MutableStateFlow(cached)
            override suspend fun refresh(studentId: String) = Unit
        }
        val vm = viewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.now.collect {} }
        runCurrent()
        val previous = vm.state.value
        clock.current = clock.current.plusSeconds(1)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(clock.instant(), vm.now.value)
        assertSame(previous, vm.state.value)
    }

    private class MutableClock(var current: Instant) : Clock() {
        override fun instant() = current
        override fun getZone() = SchoolZone
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)
    }
}

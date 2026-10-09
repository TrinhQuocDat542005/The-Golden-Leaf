package com.example.giaodien

import com.example.giaodien.data.model.formatVnd
import com.example.giaodien.data.network.SessionAuthInterceptor
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

/** Real OkHttp exchanges on disposable loopback, no Firebase or production server. */
class Week10NetworkTest {
    private class Session(val token: String)
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    @Volatile private var current: Session? = null
    private val pending = mutableListOf<() -> Unit>()
    @Before fun setup() {
        server = MockWebServer(); server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
        current = Session("synthetic-a")
        client = OkHttpClient.Builder().retryOnConnectionFailure(false)
            .connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS)
            .addInterceptor(SessionAuthInterceptor({ current }, { it.token }) { expected ->
                pending.add { if (current === expected) current = null }
            }).build()
    }
    @After fun cleanup() { server.shutdown(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
    // Pin the fixture host too: localhost IPv4/IPv6 route fallback differs across OSes.
    private fun request() = client.newCall(Request.Builder().url(server.url("/private").newBuilder().host("127.0.0.1").build())
        .header("Authorization", "must-be-replaced").build()).execute()
    private fun dispatchLogout() { pending.toList().forEach { it() }; pending.clear() }

    @Test fun requestUsesSingleCurrentBearerHeader() {
        server.enqueue(MockResponse().setBody("ok")); request().use { assertEquals(200, it.code) }
        val headers = server.takeRequest().headers.values("Authorization")
        assertEquals(listOf("Bearer synthetic-a"), headers)
    }
    @Test fun current401LogsOutWithoutReplay() {
        server.enqueue(MockResponse().setResponseCode(401)); request().use { assertEquals(401, it.code) }
        assertNotNull(current); dispatchLogout(); assertNull(current); assertEquals(1,server.requestCount)
    }
    @Test fun forbiddenDoesNotExpireSession() {
        val original=current; server.enqueue(MockResponse().setResponseCode(403))
        request().close(); dispatchLogout(); assertSame(original,current)
    }
    @Test fun unavailableBackendKeepsSessionAndSuccessfulRetryWorks() {
        val original=current; server.enqueue(MockResponse().setResponseCode(503)); request().close()
        dispatchLogout(); assertSame(original,current)
        server.enqueue(MockResponse().setBody("recovered")); request().use { assertEquals("recovered",it.body!!.string()) }
        assertSame(original,current)
    }
    @Test fun connectionLossKeepsSessionAndExplicitRetryWorks() {
        val original=current; server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertThrows(IOException::class.java) { request().close() }
        dispatchLogout(); assertSame(original,current)
        server.enqueue(MockResponse().setBody("recovered")); request().use { assertEquals(200,it.code) }
    }
    @Test fun timeoutDoesNotLogout() {
        val original=current; server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertThrows(IOException::class.java) { request().close() }
        dispatchLogout(); assertSame(original,current)
    }
    @Test fun old401AfterAccountSwitchDoesNotLogoutNewSession() {
        val next=Session("synthetic-b")
        server.dispatcher=object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                current=next; return MockResponse().setResponseCode(401)
            }
        }
        request().close(); dispatchLogout(); assertSame(next,current)
    }
    @Test fun queuedOldLogoutCannotClearReauthenticatedSession() {
        server.enqueue(MockResponse().setResponseCode(401)); request().close()
        val next=Session("synthetic-b"); current=next
        dispatchLogout(); assertSame(next,current)
    }
    @Test fun restartExpiredTokenRequiresNewSessionThenWorks() {
        server.enqueue(MockResponse().setResponseCode(401)); request().close(); dispatchLogout(); assertNull(current)
        current=Session("synthetic-new-process")
        server.enqueue(MockResponse().setBody("ok")); request().close()
        server.takeRequest(); assertEquals("Bearer synthetic-new-process",server.takeRequest().getHeader("Authorization"))
    }
    @Test fun vietnameseMoneyIsStableAcrossDeviceLocales() {
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("70.000 VND",formatVnd(70000.0)); assertEquals("340.000 VND",formatVnd(340000.0))
            assertEquals("0 VND",formatVnd(0.0)); assertEquals("1.234,5 VND",formatVnd(1234.5))
            assertEquals("— VND",formatVnd(Double.NaN)); assertEquals("— VND",formatVnd(Double.POSITIVE_INFINITY))
        } finally { Locale.setDefault(previous) }
    }
}

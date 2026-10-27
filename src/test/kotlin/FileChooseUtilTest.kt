import io.github.jixiaoyong.utils.FileChooseUtil
import java.awt.EventQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class FileChooseUtilTest {
    @org.junit.Test(timeout = 5000) fun `event thread failure resumes caller with original exception`() = runTest {
        val failure = IllegalStateException("dialog failed")
        val caught = assertFailsWith<IllegalStateException> {
            FileChooseUtil.onEventDispatchThread<String> { throw failure }
        }
        // Coroutine stack-trace recovery can copy an exception across dispatchers.
        assertEquals(failure.message, caught.message)
    }

    @org.junit.Test(timeout = 5000) fun `successful selection runs on event thread`() = runTest {
        val value = FileChooseUtil.onEventDispatchThread {
            assertTrue(EventQueue.isDispatchThread())
            "selected directory"
        }
        assertEquals("selected directory", value)
    }

    @org.junit.Test(timeout = 5000) fun `cancellation before dispatch does not open dialog`() = runTest {
        val release = CountDownLatch(1)
        EventQueue.invokeLater { release.await(3, TimeUnit.SECONDS) }
        var called = false
        val selection = async(start = CoroutineStart.UNDISPATCHED) {
            FileChooseUtil.onEventDispatchThread { called = true }
        }
        try {
            selection.cancel()
        } finally {
            release.countDown()
        }
        EventQueue.invokeAndWait { }
        assertFalse(called)
    }
}

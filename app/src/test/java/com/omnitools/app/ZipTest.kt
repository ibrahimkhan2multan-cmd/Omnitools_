package com.omnitools.app

import com.omnitools.app.tools.zip.ZipEngine
import com.omnitools.app.tools.zip.ZipSafety
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun makeZip(vararg entries: Pair<String, ByteArray>): File {
        val f = tmp.newFile()
        ZipOutputStream(f.outputStream()).use { z ->
            for ((name, data) in entries) {
                z.putNextEntry(ZipEntry(name))
                z.write(data)
                z.closeEntry()
            }
        }
        return f
    }

    /** Writes into a real folder, but only inside it: the same promise the Android sink makes. */
    private inner class DirSink(val root: File) : ZipEngine.Sink {
        override fun directory(path: List<String>) { File(root, path.joinToString("/")).mkdirs() }
        override fun file(path: List<String>): OutputStream {
            val target = File(root, path.joinToString("/"))
            assertTrue("escaped the folder: $target", target.canonicalPath.startsWith(root.canonicalPath + File.separator))
            target.parentFile!!.mkdirs()
            return target.outputStream()
        }
    }

    @Test fun safePathAcceptsNormalNames() {
        assertEquals(listOf("a", "b", "c.txt"), ZipSafety.safePath("a/b/c.txt"))
        assertEquals(listOf("a", "b"), ZipSafety.safePath("./a//b/"))
        assertEquals(listOf("a", "b.txt"), ZipSafety.safePath("a\\b.txt"))
    }

    @Test fun safePathRejectsTraversalAndAbsolute() {
        for (bad in listOf("../evil.txt", "a/../../evil", "a/..", "/etc/passwd", "\\windows\\x", "C:/x.txt", "c:\\x", "", "..", "a\u0000b", "x/\n/y")) {
            assertNull("accepted: ${bad.replace("\u0000", "\\0")}", ZipSafety.safePath(bad))
        }
        assertNull(ZipSafety.safePath("a".repeat(300)))
    }

    @Test fun listShowsSizesAndSafety() {
        val zip = makeZip("ok.txt" to "hello".toByteArray(), "../bad.txt" to "x".toByteArray(), "dir/" to ByteArray(0))
        val list = ZipEngine.list(zip)
        assertEquals(3, list.size)
        val ok = list.first { it.name == "ok.txt" }
        assertEquals(5L, ok.size)
        assertTrue(ok.safe)
        assertFalse(list.first { it.name == "../bad.txt" }.safe)
        assertTrue(list.first { it.name == "dir/" }.isDirectory)
    }

    @Test fun extractWritesSafeEntriesAndSkipsUnsafe() {
        val zip = makeZip(
            "readme.txt" to "hi".toByteArray(),
            "docs/guide/a.txt" to "AAA".toByteArray(),
            "../escape.txt" to "bad".toByteArray(),
            "/abs.txt" to "bad".toByteArray(),
            "empty/" to ByteArray(0),
        )
        val out = tmp.newFolder("out")
        val result = ZipEngine.extract(zip, DirSink(out))
        assertEquals(2, result.files)
        assertEquals(1, result.folders)
        assertEquals(2, result.skippedUnsafe)
        assertEquals("hi", File(out, "readme.txt").readText())
        assertEquals("AAA", File(out, "docs/guide/a.txt").readText())
        assertTrue(File(out, "empty").isDirectory)
        assertFalse(File(out.parentFile, "escape.txt").exists())
    }

    @Test fun extractStopsAtTheByteLimit() {
        val zip = makeZip("big.bin" to ByteArray(100_000))
        val out = tmp.newFolder("limit")
        try {
            ZipEngine.extract(zip, DirSink(out), maxBytes = 10_000)
            fail("limit not enforced")
        } catch (e: ZipEngine.ZipLimitException) {
            assertTrue(e.message!!.contains("limit"))
        }
    }

    @Test fun extractReportsProgressAndCanBeCancelled() {
        val zip = makeZip("a" to ByteArray(1), "b" to ByteArray(1), "c" to ByteArray(1))
        var last = 0 to 0
        ZipEngine.extract(zip, DirSink(tmp.newFolder("p")), onProgress = { d, t -> last = d to t })
        assertEquals(3 to 3, last)
        try {
            ZipEngine.extract(zip, DirSink(tmp.newFolder("c")), isCancelled = { true })
            fail("not cancelled")
        } catch (e: InterruptedException) { }
    }

    @Test fun uniqueNamesAvoidClashesAndFolders() {
        assertEquals(listOf("a.txt", "a (1).txt", "a (2).txt", "b"), ZipEngine.uniqueNames(listOf("a.txt", "a.txt", "A.TXT", "b")))
        assertEquals(listOf("x_y.txt", "file"), ZipEngine.uniqueNames(listOf("x/y.txt", " ")))
        assertEquals(listOf("noext", "noext (1)"), ZipEngine.uniqueNames(listOf("noext", "noext")))
    }

    @Test fun createThenExtractRoundTrip() {
        val data1 = ByteArray(5000) { (it % 256).toByte() }
        val data2 = "text file".toByteArray()
        val target = tmp.newFile()
        val count = ZipEngine.create(
            listOf(
                ZipEngine.Input("one.bin") { ByteArrayInputStream(data1) },
                ZipEngine.Input("one.bin") { ByteArrayInputStream(data2) },
            ),
            target.outputStream(),
        )
        assertEquals(2, count)
        val out = tmp.newFolder("rt")
        val result = ZipEngine.extract(target, DirSink(out))
        assertEquals(2, result.files)
        assertArrayEquals(data1, File(out, "one.bin").readBytes())
        assertArrayEquals(data2, File(out, "one (1).bin").readBytes())
    }
}

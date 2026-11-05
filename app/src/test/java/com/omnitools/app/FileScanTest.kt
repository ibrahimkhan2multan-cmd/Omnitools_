package com.omnitools.app

import com.omnitools.app.tools.files.Duplicates
import com.omnitools.app.tools.files.FileEntry
import com.omnitools.app.tools.files.FileStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileScanTest {
    private fun f(path: String, size: Long, id: String = path) = FileEntry(path, path.substringAfterLast('/'), size, id)

    @Test fun extensionsAndGroups() {
        assertEquals("jpg", FileStats.extension("A.JPG"))
        assertEquals("", FileStats.extension(".hidden"))
        assertEquals("", FileStats.extension("noext"))
        assertEquals("Images", FileStats.typeGroup("x.PNG"))
        assertEquals("Other", FileStats.typeGroup("x.weird"))
        assertEquals("Archives", FileStats.typeGroup("a.zip"))
    }

    @Test fun summaryTotalsAndOrdering() {
        val s = FileStats.summarize(listOf(f("a/p.jpg", 100), f("b.mp4", 5000), f("c.txt", 10), f("d.png", 50)), topFiles = 2)
        assertEquals(4, s.files); assertEquals(5160L, s.bytes)
        assertEquals("Videos", s.byType.first().label)
        assertEquals(listOf("b.mp4", "p.jpg"), s.largest.map { it.name })
        assertEquals(150L, s.byType.first { it.label == "Images" }.bytes)
    }

    // Fake content: files with the same "content" string are identical.
    private fun finder(files: List<Pair<FileEntry, String>>): List<Duplicates.Group> {
        val content = files.associate { it.first.id to it.second }
        return Duplicates.find(files.map { it.first }, hash = { e, limit -> content.getValue(e.id).let { c -> if (limit == null) c else c.take(limit.toInt()) } })
    }

    @Test fun findsIdenticalNotJustSameSize() {
        val groups = finder(listOf(
            f("a.txt", 3) to "abc", f("b.txt", 3) to "abc", f("c.txt", 3) to "xyz", // same size, different content
            f("d.bin", 9) to "123456789", f("e.bin", 9) to "123456789", f("f.bin", 9) to "123456780",
            f("solo", 77) to "q",
        ))
        assertEquals(2, groups.size)
        assertEquals(listOf("a.txt", "b.txt"), groups.first { it.size == 3L }.files.map { it.path })
        assertEquals(listOf("d.bin", "e.bin"), groups.first { it.size == 9L }.files.map { it.path })
        assertEquals(9L, groups.first().wasted)
    }

    @Test fun ignoresEmptyFilesAndSortsByWaste() {
        val groups = finder(listOf(f("z1", 0) to "", f("z2", 0) to "", f("a", 2) to "ab", f("b", 2) to "ab", f("c", 2) to "ab"))
        assertEquals(1, groups.size)
        assertEquals(4L, groups[0].wasted)
    }

    @Test fun neverDeletesEveryCopy() {
        val g = Duplicates.Group(5, listOf(f("a", 5), f("b", 5), f("c", 5)))
        assertEquals(setOf("b", "c"), Duplicates.suggestKeepFirst(listOf(g)))
        val all = Duplicates.safeToDelete(listOf(g), setOf("a", "b", "c"))
        assertEquals(2, all.size); assertFalse("a" in all)
        assertEquals(setOf("b"), Duplicates.safeToDelete(listOf(g), setOf("b")))
        assertTrue(Duplicates.safeToDelete(listOf(g), emptySet()).isEmpty())
    }
}

package com.omnitools.app

import com.omnitools.app.tools.files.BatchRename
import com.omnitools.app.tools.files.BatchRename.Status
import com.omnitools.app.tools.files.CaseMode
import com.omnitools.app.tools.files.RenameRules
import org.junit.Assert.assertEquals
import org.junit.Test

class BatchRenameTest {
    @Test fun findReplaceKeepsExtensionAndIgnoresCase() {
        assertEquals("Trip_1.JPG", BatchRename.newName("IMG_1.JPG", 0, RenameRules(find = "img", replaceWith = "Trip")))
        assertEquals("a-b.txt", BatchRename.newName("a b.txt", 0, RenameRules(find = " ", replaceWith = "-")))
    }

    @Test fun prefixSuffixAndNumbering() {
        assertEquals("pre_x_post.png", BatchRename.newName("x.png", 0, RenameRules(prefix = "pre_", suffix = "_post")))
        assertEquals("Holiday 007.jpg", BatchRename.newName("a.jpg", 6, RenameRules(numbering = true, numberName = "Holiday", start = 1, digits = 3)))
        assertEquals("a 02.jpg", BatchRename.newName("a.jpg", 1, RenameRules(numbering = true)))
    }

    @Test fun caseModes() {
        assertEquals("my file.TXT", BatchRename.newName("My FILE.TXT", 0, RenameRules(caseMode = CaseMode.LOWER)))
        assertEquals("Hello World.txt", BatchRename.newName("hELLO wORLD.txt", 0, RenameRules(caseMode = CaseMode.TITLE)))
        assertEquals("ABC.txt", BatchRename.newName("abc.txt", 0, RenameRules(caseMode = CaseMode.UPPER)))
    }

    @Test fun illegalCharactersAreReplaced() {
        assertEquals("a_b.txt", BatchRename.newName("ab.txt", 0, RenameRules(find = "ab", replaceWith = "a/b")))
    }

    @Test fun unchangedAndOk() {
        val p = BatchRename.plan(listOf("a1.txt", "a2.txt", "b.txt"), RenameRules(find = "a1", replaceWith = "x"))
        assertEquals(listOf(Status.OK, Status.UNCHANGED, Status.UNCHANGED), p.map { it.status })
        assertEquals(Status.OK, BatchRename.plan(listOf("abc.txt"), RenameRules(caseMode = CaseMode.UPPER))[0].status)
    }

    @Test fun clashWithExistingFile() {
        val p = BatchRename.plan(listOf("a.txt", "b.txt"), RenameRules(find = "b", replaceWith = "a"))
        assertEquals(Status.UNCHANGED, p[0].status)
        assertEquals(Status.DUPLICATE, p[1].status) // a.txt stays a.txt, so b.txt would become a second a.txt
        val other = BatchRename.plan(listOf("a1.txt", "b1.txt"), RenameRules(find = "a", replaceWith = "z"), others = setOf("z1.txt"))
        assertEquals(Status.EXISTS, other[0].status)
    }

    @Test fun twoFilesGettingTheSameName() {
        val p = BatchRename.plan(listOf("k.txt", "K.txt"), RenameRules(caseMode = CaseMode.LOWER))
        assertEquals(Status.UNCHANGED, p[0].status)
        assertEquals(Status.DUPLICATE, p[1].status)
    }

    @Test fun numberingMakesUniqueNames() {
        val p = BatchRename.plan(listOf("a.txt", "b.txt"), RenameRules(numbering = true, numberName = "same", digits = 1))
        assertEquals(listOf("same 1.txt", "same 2.txt"), p.map { it.new })
        assertEquals(listOf(Status.OK, Status.OK), p.map { it.status })
    }

    @Test fun emptyOrHiddenResultsAreInvalid() {
        assertEquals(Status.INVALID, BatchRename.plan(listOf("a"), RenameRules(find = "a", replaceWith = ""))[0].status)
        assertEquals(Status.INVALID, BatchRename.plan(listOf("a.txt"), RenameRules(find = "a", replaceWith = ""))[0].status)
    }

    @Test fun naturalOrder() {
        assertEquals(listOf("a2", "a10", "b1"), BatchRename.naturalOrder(listOf("a10", "b1", "a2")))
    }
}

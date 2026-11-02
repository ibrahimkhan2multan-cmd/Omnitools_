package com.omnitools.app

import com.omnitools.app.data.ToolRegistry
import com.omnitools.app.tools.hash.HashTool
import com.omnitools.app.tools.image.ImageMath
import com.omnitools.app.tools.pdf.PdfLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch2Test {

    @Test fun hashKnownVectors() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", HashTool.digestText(HashTool.Algo.MD5, "abc"))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", HashTool.digestText(HashTool.Algo.SHA1, "abc"))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            HashTool.digestText(HashTool.Algo.SHA256, "abc"),
        )
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", HashTool.digestText(HashTool.Algo.MD5, ""))
    }

    @Test fun hashStreamMatchesTextAndReportsProgress() {
        val data = ByteArray(200_000) { (it % 251).toByte() }
        var last = 0L
        val all = HashTool.digestAll(data.inputStream(), onProgress = { last = it })
        assertEquals(200_000L, last)
        val direct = java.security.MessageDigest.getInstance("SHA-256").digest(data)
        assertEquals(HashTool.hex(direct), all[HashTool.Algo.SHA256])
    }

    @Test fun hashCompareIgnoresCaseAndSpaces() {
        assertTrue(HashTool.matches(" BA7816BF 8F01CFEA ", "ba7816bf8f01cfea"))
        assertFalse(HashTool.matches("abc", "abd"))
    }

    @Test fun cropBoxClampsAndNeverCrosses() {
        val b = ImageMath.cropBox(1000, 500, 10f, 20f, 10f, 20f)
        assertEquals(ImageMath.CropBox(100, 100, 800, 300), b)
        val extreme = ImageMath.cropBox(100, 100, 90f, 90f, 90f, 90f)
        assertTrue(extreme.width >= 1 && extreme.height >= 1)
        assertTrue(extreme.x + extreme.width <= 100 && extreme.y + extreme.height <= 100)
    }

    @Test fun scaledSizeAndNames() {
        assertEquals(50 to 25, ImageMath.scaledSize(100, 50, 50))
        assertEquals(1 to 1, ImageMath.scaledSize(1, 1, 1))
        assertEquals("photo-edited.png", ImageMath.outputName("photo.jpg", "png"))
        assertEquals("image-edited.jpg", ImageMath.outputName(null, "jpg"))
    }

    @Test fun pdfPlacementFitsInsideMargins() {
        val dims = PdfLayout.pageDims(PdfLayout.PageSize.A4, PdfLayout.Orientation.AUTO, PdfLayout.Margin.NORMAL, 2000, 1000)
        assertEquals(PdfLayout.PageDims(842, 595), dims)
        val p = PdfLayout.placement(dims, PdfLayout.Margin.NORMAL, 2000, 1000)
        assertTrue(p.left >= 36f && p.top >= 36f)
        assertTrue(p.left + p.width <= 842f - 36f + 0.01f)
        assertTrue(p.top + p.height <= 595f - 36f + 0.01f)
        assertEquals(2f, p.width / p.height, 0.01f)
    }

    @Test fun pdfFitSizeFollowsPicture() {
        val d = PdfLayout.pageDims(PdfLayout.PageSize.FIT, PdfLayout.Orientation.AUTO, PdfLayout.Margin.NONE, 1000, 500)
        assertEquals(842, d.width)
        assertEquals(421, d.height)
    }

    @Test fun newToolsAreRegisteredOnce() {
        listOf("hash_tool", "image_tools", "image_pdf", "pdf_reader").forEach { assertNotNull(ToolRegistry.find(it)) }
        assertEquals(ToolRegistry.all.size, ToolRegistry.all.map { it.id }.toSet().size)
    }
}

package com.omnitools.app

import com.omnitools.app.tools.advanced.Json
import com.omnitools.app.tools.gaming.AddonManifest
import com.omnitools.app.tools.gaming.McCoords
import com.omnitools.app.tools.gaming.McFormat
import com.omnitools.app.tools.gaming.PackZip
import com.omnitools.app.tools.gaming.Uuids
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GamingToolsTest {
    @Test fun uuidFormats() {
        val u = "123e4567-e89b-12d3-a456-426614174000"
        assertTrue(Uuids.isValid(u)); assertFalse(Uuids.isValid("nope")); assertFalse(Uuids.isValid(u.replace("-", "")))
        assertEquals("123E4567-E89B-12D3-A456-426614174000", Uuids.format(u, true, true, false))
        assertEquals("123e4567e89b12d3a456426614174000", Uuids.format(u, false, false, false))
        assertEquals("{$u}", Uuids.format(u, false, true, true))
        try { Uuids.format("bad", false, true, false); fail() } catch (e: IllegalArgumentException) { }
    }
    @Test fun uuidManyAreUniqueAndValid() {
        val l = Uuids.many(50, false, true, false)
        assertEquals(50, l.toSet().size); assertTrue(l.all { Uuids.isValid(it) })
        assertEquals('4', l[0][14])
        try { Uuids.many(0, false, true, false); fail() } catch (e: IllegalArgumentException) { }
        try { Uuids.many(51, false, true, false); fail() } catch (e: IllegalArgumentException) { }
    }
    private fun input(kind: AddonManifest.Kind = AddonManifest.Kind.BEHAVIOR, dep: String? = null, name: String = "Test \"Pack\"") = AddonManifest.Input(
        name, "desc", kind, listOf(1, 0, 0), listOf(1, 20, 0), Uuids.random(), Uuids.random(), dep,
    )
    @Test fun manifestIsValidJsonWithExpectedShape() {
        val text = AddonManifest.build(input())
        val root = Json.parse(text) as Json.Node.Obj
        fun get(o: Json.Node.Obj, k: String) = o.entries.first { it.first == k }.second
        assertEquals(Json.Node.Num("2"), get(root, "format_version"))
        val header = get(root, "header") as Json.Node.Obj
        assertEquals(Json.Node.Str("Test \"Pack\""), get(header, "name"))
        val mod = ((get(root, "modules") as Json.Node.Arr).items[0]) as Json.Node.Obj
        assertEquals(Json.Node.Str("data"), get(mod, "type"))
        assertTrue(root.entries.none { it.first == "dependencies" })
        val res = Json.parse(AddonManifest.build(input(AddonManifest.Kind.RESOURCE))) as Json.Node.Obj
        assertEquals(Json.Node.Str("resources"), get(((get(res, "modules") as Json.Node.Arr).items[0]) as Json.Node.Obj, "type"))
    }
    @Test fun manifestDependencyAndRules() {
        val dep = Uuids.random()
        assertTrue(AddonManifest.build(input(dep = dep)).contains("\"dependencies\""))
        val same = Uuids.random()
        val bad = listOf(
            AddonManifest.Input("", "d", AddonManifest.Kind.BEHAVIOR, listOf(1, 0, 0), listOf(1, 20, 0), Uuids.random(), Uuids.random()),
            AddonManifest.Input("n", "d", AddonManifest.Kind.BEHAVIOR, listOf(1, 0, 0), listOf(1, 20, 0), same, same),
            AddonManifest.Input("n", "d", AddonManifest.Kind.BEHAVIOR, listOf(1, 0, 0), listOf(1, 20, 0), "x", Uuids.random()),
            AddonManifest.Input("n", "d", AddonManifest.Kind.BEHAVIOR, listOf(1, 0, 0), listOf(1, 20, 0), same, Uuids.random(), same),
            AddonManifest.Input("n", "d", AddonManifest.Kind.BEHAVIOR, listOf(1, 0, 0), listOf(1, 20, 0), Uuids.random(), Uuids.random(), "bad"),
        )
        for (b in bad) try { AddonManifest.build(b); fail() } catch (e: IllegalArgumentException) { }
    }
    @Test fun versionParsing() {
        assertEquals(listOf(1, 20, 80), AddonManifest.parseVersion("1.20.80"))
        for (bad in listOf("1.0", "1.0.0.0", "a.b.c", "1..0", "", "12345.0.0")) try { AddonManifest.parseVersion(bad); fail(bad) } catch (e: IllegalArgumentException) { }
    }
    @Test fun colourCodes() {
        val s = McFormat.parse("&6&lGold &rplain &a&oGreen", '&')
        assertEquals(listOf("Gold ", "plain ", "Green"), s.map { it.text })
        assertEquals(0xFFAA00, s[0].rgb); assertTrue(s[0].bold)
        assertNull(s[1].rgb); assertFalse(s[1].bold)
        assertEquals(0x55FF55, s[2].rgb); assertTrue(s[2].italic)
    }
    @Test fun colourResetsStyles() {
        val s = McFormat.parse("&l&nBold&cRed", '&')
        assertTrue(s[0].bold && s[0].underline)
        assertFalse(s[1].bold); assertFalse(s[1].underline)
    }
    @Test fun invalidCodesStayAsText() {
        assertEquals("100% & more &z end&", McFormat.strip("100% & more &z end&", '&'))
        assertEquals("Hi", McFormat.strip("&aH&li", '&'))
        assertEquals("§aHello §lWorld & more", McFormat.toSection("&aHello &lWorld & more"))
        assertEquals("Hi", McFormat.strip("§aHi", '§'))
    }
    @Test fun coordinates() {
        val r = McCoords.compute(100, -250)
        assertEquals(12L to -32L, r.nether)
        assertEquals(6L to -16L, r.chunk)
        assertEquals(4L to 6L, r.inChunk)
        assertEquals("r.0.-1.mca", r.regionFile)
        val n = McCoords.compute(-1, -1)
        assertEquals(-1L to -1L, n.nether); assertEquals(-1L to -1L, n.chunk); assertEquals(15L to 15L, n.inChunk); assertEquals("r.-1.-1.mca", n.regionFile)
        assertEquals(-16L to -16L, n.chunkOrigin)
        assertEquals(5.0, McCoords.distance2d(0, 0, 3, 4), 0.0001)
        assertEquals(13.0, McCoords.distance3d(0, 0, 0, 3, 4, 12), 0.0001)
    }
    @Test fun coordinateParsing() {
        assertEquals(-5L, McCoords.parse(" -5 ", "X"))
        for (bad in listOf("", "1.5", "abc", "30000001")) try { McCoords.parse(bad, "X"); fail(bad) } catch (e: IllegalArgumentException) { }
    }
    @Test fun zipNamesAreSafe() {
        assertEquals("a/b.txt", PackZip.safeEntryName("a/b.txt"))
        assertEquals("a/b.txt", PackZip.safeEntryName("\\a\\b.txt"))
        for (bad in listOf("../x", "a/../../x", "", "a//b", "dir/", "./x")) assertNull(bad, PackZip.safeEntryName(bad))
    }
    @Test fun manifestWarnings() {
        assertNull(PackZip.manifestWarning(listOf("manifest.json", "textures/a.png")))
        assertTrue(PackZip.manifestWarning(listOf("MyPack/manifest.json"))!!.contains("MyPack"))
        assertTrue(PackZip.manifestWarning(listOf("a.png"))!!.contains("No manifest.json"))
    }
    @Test fun suggestedNames() {
        assertEquals("My_Pack.mcpack", PackZip.suggestedName("My:Pack", "mcpack"))
        assertEquals("pack.zip", PackZip.suggestedName("  ", "zip"))
        assertNotEquals("", PackZip.suggestedName("x".repeat(200), "zip"))
    }
}

package dev.duefor.cornered.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {

    @Test
    fun `tag prefix is ignored`() {
        assertEquals(listOf(1, 2, 3), Version.segments("v1.2.3"))
        assertEquals(listOf(1, 2, 3), Version.segments("1.2.3"))
        assertEquals(listOf(2, 0, 0), Version.segments("V2.0.0"))
    }

    @Test
    fun `version code matches build script encoding`() {
        assertEquals(10_000, Version.encodeCode("1.0.0"))
        assertEquals(10_001, Version.encodeCode("v1.0.1"))
        assertEquals(10_200, Version.encodeCode("1.2.0"))
        assertEquals(20_000, Version.encodeCode("2.0.0"))
    }

    @Test
    fun `newer versions are detected per segment not lexically`() {
        assertTrue(Version.isNewer("1.10.0", "1.9.0"))
        assertTrue(Version.isNewer("1.0.10", "1.0.9"))
        assertTrue(Version.isNewer("2.0.0", "1.99.99"))
        assertFalse(Version.isNewer("1.0.0", "1.0.0"))
        assertFalse(Version.isNewer("1.0.9", "1.0.10"))
    }

    @Test
    fun `debug suffix does not look like an upgrade`() {
        assertFalse(Version.isNewer("1.0.0", "1.0.0-debug"))
    }

    @Test
    fun `suffixed tags still compare by their numeric part`() {
        assertTrue(Version.isNewer("1.1.0-beta", "1.0.0"))
    }
}

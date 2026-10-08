package com.naeem.streamnest

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    @Test fun acceptsSharedTextAndRemovesTrackingFragment() {
        assertEquals("https://youtu.be/abc123", SourceUrl.normalize("Watch this https://youtu.be/abc123#t=5"))
    }
    @Test fun acceptsSchemeLessLink() {
        assertEquals("https://www.youtube.com/watch?v=abc123", SourceUrl.normalize("www.youtube.com/watch?v=abc123"))
    }
    @Test fun retainsPlaylistChoiceAndEncodedQuery() {
        assertEquals("https://www.youtube.com/watch?v=abc&list=PL123&x=a%20b", SourceUrl.normalize("https://www.youtube.com/watch?v=abc&list=PL123&x=a%20b"))
    }
    @Test fun preservesUnicodeChannelHandle() {
        assertTrue(SourceUrl.normalize("https://youtube.com/@تعلیم").startsWith("https://youtube.com/@"))
    }
    @Test fun acceptsSocialMediaPlatforms() {
        assertEquals("https://www.instagram.com/reel/C123456", SourceUrl.normalize("https://www.instagram.com/reel/C123456"))
        assertEquals("https://www.facebook.com/watch/?v=12345678", SourceUrl.normalize("https://www.facebook.com/watch/?v=12345678"))
        assertEquals("https://www.tiktok.com/@user/video/7123456789", SourceUrl.normalize("https://www.tiktok.com/@user/video/7123456789"))
        assertEquals("https://x.com/user/status/1234567890", SourceUrl.normalize("https://x.com/user/status/1234567890"))
    }
    @Test fun rejectsCredentials() { rejected("https://secret@youtube.com/watch?v=abc") }
    @Test fun rejectsNonstandardPort() { rejected("https://youtube.com:8443/watch?v=abc") }
    @Test fun rejectsLocalFile() { rejected("file:///sdcard/video.mp4") }
    @Test fun rejectsEmptyAndHomepage() { rejected(""); rejected("https://youtube.com/"); rejected("https://instagram.com") }
    @Test fun rejectsInvalidUri() { rejected("not a url"); rejected("https://localhost/video") }
    @Test fun safeNamesRemovePathAndControlCharacters() {
        assertEquals("a_b_c_d_", safeFileStem("a/b\\c:d\n"))
        assertEquals("Video", safeFileStem(" "))
        assertEquals(100, safeFileStem("x".repeat(200)).length)
    }
    @Test fun durationIncludesHoursAndUnknownIsEmpty() {
        assertEquals("1:01:01", durationLabel(3661)); assertEquals("2:05", durationLabel(125)); assertEquals("", durationLabel(0))
    }
    @Test fun duplicateDetectionRespectsFormatAndCompletion() {
        val item = MediaItem("abc", "https://youtu.be/abc", "Title")
        val choice = DownloadChoice(720)
        val existing = DownloadJob(media = item, choice = choice)
        assertTrue(shouldDeduplicate(existing, item, choice))
        assertTrue(shouldDeduplicate(existing.copy(status = JobStatus.COMPLETED), item, choice))
        assertFalse(shouldDeduplicate(existing.copy(status = JobStatus.FAILED), item, choice))
        assertFalse(shouldDeduplicate(existing, item, DownloadChoice(1080)))
        assertFalse(shouldDeduplicate(existing, item.copy(id = "different"), choice))
    }
    @Test fun terminalStatesAreNotActive() {
        assertTrue(JobStatus.MERGING.active); assertTrue(JobStatus.SAVING.active)
        assertFalse(JobStatus.COMPLETED.active); assertFalse(JobStatus.CANCELLED.active)
    }
    private fun rejected(value: String) {
        try { SourceUrl.normalize(value); fail("Accepted invalid URL: $value") }
        catch (_: IllegalArgumentException) { }
    }
}

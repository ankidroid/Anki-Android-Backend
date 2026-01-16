package net.ankiweb

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackendSyncTest{
    @Test
    fun verifyMediaFileSizeSync(){
        val file = File("../anki/rslib/src/sync/media/mod.rs")

        assertTrue(
            "File does not exist at ${file.absolutePath}",
            file.exists()
        )

        val content = file.readText()

        val expectedContent = "pub static MAX_INDIVIDUAL_MEDIA_FILE_SIZE: usize = 100 * 1024 * 1024"

        assertTrue(
            "MAX_INDIVIDUAL_MEDIA_FILE_SIZE in Backend.kt is out of sync with anki",
            content.contains(expectedContent)
        )
    }
}
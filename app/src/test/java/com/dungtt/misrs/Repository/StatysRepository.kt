import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainViewModelTest {

    @Test
    fun testFormatTimestamp() {
        // Arrange: Prepare various test cases with different Epoch milliseconds
        val testCases = mapOf(
            "1725155927000" to "2024-08-04 21:59:48.000000",
            "1625155927000" to "2021-07-01 10:12:07.000000",
            "1525155927000" to "2018-05-01 22:12:07.000000"
        )

        // Act & Assert: Test each case
        for ((epochMillis, expectedTimestamp) in testCases) {
            val formattedTimestamp = formatTimestamp(epochMillis)
            assertEquals("Failed for epochMillis: $epochMillis", expectedTimestamp, formattedTimestamp)
        }
    }

    // This is the formatTimestamp function being tested
    private fun formatTimestamp(timestamp: String): String {
        val epochMillis = timestamp.toLong()
        val instant = Instant.ofEpochMilli(epochMillis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}

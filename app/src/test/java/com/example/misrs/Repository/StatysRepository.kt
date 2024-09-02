import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainViewModelTest {

    @Test
    fun testFormatTimestamp() {
        // Arrange: Chuẩn bị giá trị Epoch millisecond (ví dụ: "1725155927000" tương đương với "2024-08-04 21:59:48.000000")
        val epochMillis = "1725155927000"

        // Expected: Chuỗi thời gian mong muốn
        val expectedTimestamp = "2024-08-04 21:59:48.000000"

        // Act: Gọi hàm formatTimestamp với giá trị Epoch millisecond
        val formattedTimestamp = formatTimestamp(epochMillis)

        // Assert: Kiểm tra kết quả có giống với giá trị mong muốn không
        assertEquals(expectedTimestamp, formattedTimestamp)
    }

    // Đây là hàm formatTimestamp được test
    private fun formatTimestamp(timestamp: String): String {
        val epochMillis = timestamp.toLong()
        val instant = Instant.ofEpochMilli(epochMillis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}

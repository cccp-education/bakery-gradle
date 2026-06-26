package bakery.tree

import bakery.LayoutType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodeMetadataTest {

    @Test
    fun `empty metadata has all null fields`() {
        val meta = NodeMetadata()
        assertNull(meta.title)
        assertNull(meta.description)
        assertNull(meta.tags)
        assertNull(meta.layout)
    }

    @Test
    fun `metadata with title only`() {
        val meta = NodeMetadata(title = "Formations")
        assertEquals("Formations", meta.title)
        assertNull(meta.description)
        assertNull(meta.tags)
        assertNull(meta.layout)
    }

    @Test
    fun `metadata with all fields`() {
        val meta = NodeMetadata(
            title = "AB Partition",
            description = "Apprendre la technique AB Partition",
            tags = listOf("formation", "partition", "docker"),
            layout = LayoutType.SIDEBAR_RIGHT
        )
        assertEquals("AB Partition", meta.title)
        assertEquals("Apprendre la technique AB Partition", meta.description)
        assertEquals(listOf("formation", "partition", "docker"), meta.tags)
        assertEquals(LayoutType.SIDEBAR_RIGHT, meta.layout)
    }

    @Test
    fun `structural equality holds for same fields`() {
        val a = NodeMetadata(title = "Formations", tags = listOf("dev"))
        val b = NodeMetadata(title = "Formations", tags = listOf("dev"))
        assertEquals(a, b)
    }

    @Test
    fun `copy produces new instance with overridden field`() {
        val original = NodeMetadata(title = "A", description = "Desc A")
        val copy = original.copy(title = "B")
        assertEquals("Desc A", copy.description)
        assertEquals("B", copy.title)
    }
}

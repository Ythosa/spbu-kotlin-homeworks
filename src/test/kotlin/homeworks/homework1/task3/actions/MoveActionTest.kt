package homeworks.homework1.task3.actions

import homeworks.homework1.task3.commandStorage.actions.ActionExecutionException
import homeworks.homework1.task3.commandStorage.actions.MoveAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.LinkedList

internal class MoveActionTest {
    @Test
    fun apply() {
        val elements = mutableListOf(1, 2, 3)
        moveAction.apply(elements)
        assertEquals(mutableListOf(2, 3, 1), elements)
    }

    @Test
    fun `apply with invalid first index`() {
        val elements = LinkedList(emptyList<Int>())
        assertThrows<ActionExecutionException> { moveAction.apply(elements) }
    }

    @Test
    fun `apply with invalid second index`() {
        val elements = mutableListOf(1, 2)
        assertThrows<ActionExecutionException> { moveAction.apply(elements) }
    }

    @Test
    fun cancel() {
        val elements = mutableListOf(1, 3, 2)
        moveAction.cancel(elements)
        assertEquals(elements, mutableListOf(2, 1, 3))
    }

    companion object {
        private const val firstIndex = 0
        private const val secondIndex = 2
        private val moveAction = MoveAction(firstIndex, secondIndex)
    }
}

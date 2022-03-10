package homeworks.homework1.task3.ui.handlers

import homeworks.homework1.task3.commandStorage.CommandStorage
import homeworks.homework1.task3.commandStorage.actions.PushAction

class PushHandler(private val commandStorage: CommandStorage) : Handler() {
    override fun handle(arguments: List<String>) {
        try {
            val (number) = this.extractArguments(arguments, arrayOf(intCast))
            this.commandStorage.apply(PushAction(number as Int))
        } catch (e: InvalidArgumentsException) {
            println("Oops... ${e.localizedMessage}")
        }
    }
}
package world.selene.server.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.suggestion.Suggestions
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import org.slf4j.Logger
import java.util.concurrent.ExecutionException

class TerminalCompleter(
    private val dispatcher: CommandDispatcher<CommandSource>,
    private val logger: Logger
) : Completer {

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val input = line.line()
        val stringReader = StringReader(input)
        try {
            val commandSource = null
            val results: ParseResults<CommandSource> = dispatcher.parse(stringReader, commandSource)
            val suggestions: Suggestions = dispatcher.getCompletionSuggestions(results).get()
            for (suggestion in suggestions.list) {
                val completion = suggestion.text
                if (completion.isNotEmpty()) {
                    candidates.add(Candidate(completion))
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: ExecutionException) {
            logger.error("Completer error", e)
        }
    }
}
import org.gradle.api.Project
import java.io.ByteArrayOutputStream

fun Project.latestCommitHash(): String {
    return runGitCommand(listOf("rev-parse", "--short", "HEAD"))
}

fun Project.runGitCommand(args: List<String>): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git") + args
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

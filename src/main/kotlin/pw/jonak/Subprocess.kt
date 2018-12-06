package pw.jonak

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * Launches the given [executableName] with the given [arguments],
 * in the optionally-given [workingDirectory]. Captures stdin/stdout.
 */
class Subprocess(executableName: String, private vararg val arguments: String, workingDirectory: String? = null) {

    private val executable: File
    private val process: Process

    /** Direct access to the process's stdout. */
    public val stdout: BufferedReader
    /** Direct access to the process's stdin. */
    public val stdin: BufferedWriter

    init {
        executable = searchForExecutableInPath(executableName)
                ?: throw FileNotFoundException("Could not find $executableName")

        val builder = ProcessBuilder(executable.absolutePath, *arguments)
        if (workingDirectory != null) {
            builder.directory(File(workingDirectory))
        }

        process = builder.start()
        stdout = process.inputStream.bufferedReader()
        stdin = process.outputStream.bufferedWriter()
    }

    /** Exposes the process' exit value as a property. */
    public val exitCode: Int
        get() = process.exitValue()

    /** Blocks until the process finishes. */
    public fun waitForCompletion() {
        process.waitFor()
    }

    /* (static members) */
    companion object {

        /** Maps executable names to the full path found, so that we don't need to search again. */
        private val cache = ConcurrentHashMap<String, String>()

        /** On windows especially, executable names are resolved with an implicit extension. */
        private val EXECUTABLE_EXTENSIONS = listOf(".exe", ".bat", ".cmd", "", ".sh")

        /** Locates the given [executable] in the current directory or PATH, and returns a representative File object. */
        private fun searchForExecutableInPath(executable: String): File? {

            if(cache.containsKey(executable)) return File(cache[executable])

            val searchPath = (System.getenv("PATH")?.split(File.pathSeparator) ?: return null) + "."

            for (path in searchPath) {
                for (extension in EXECUTABLE_EXTENSIONS) {
                    val file = File(path, executable + extension)
                    if (file.isFile && file.canExecute()) {
                        cache[executable] = file.absolutePath
                        return file
                    }
                }
            }

            return null
        }
    }
}
import java.io.File

fun getNames(folder: String): List<String> {
    val f = File(folder)

    return f.listFiles()!!.mapNotNull {
        getName(it.readText())
    }
}

fun getName(data: String): String? {
    return data.split("\n").find { it.startsWith("Name=") }?.drop(5)
}
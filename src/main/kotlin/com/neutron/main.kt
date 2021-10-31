import com.neutron.BookName
import com.neutron.Chapter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess

val metadata: MutableList<Pair<String, Int>> = mutableListOf()

enum class Translation { KJV, Vulgate, Karoli }
fun Translation.toStr() = when(this) {
    Translation.KJV ->
        "kjv"
    Translation.Vulgate ->
        "vulgate"
    Translation.Karoli ->
        "karoli"
}

lateinit var book: String
lateinit var translation: Translation
var chapt by Delegates.notNull<Int>()
var vers by Delegates.notNull<Int>()
var isDebug: Boolean = false
var isChapt = false

class Main { companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            parse(args)
            run {
                var i = 0
                Json.decodeFromString<MutableList<BookName>>(read("books.json")).forEach { it ->
                    i++
                    metadata.add(Pair(it.name, i))
                }
            }
            if(!isChapt)
                println(Json.decodeFromString<Chapter>(read("bibles/${translation.toStr()}/$book/$chapt.json")).verses[vers-1].text)
            else
                Json.decodeFromString<Chapter>(read("bibles/${translation.toStr()}/$book/$chapt.json")).verses.forEach { it -> println(it.text) }
            /*val reader = FileReader("new.csv")
            val json = Json { prettyPrint = true }
            val records:Iterable<CSVRecord> = CSVFormat.DEFAULT.withHeader().parse(reader)
            for(record in records) {
                println("${record["BookID"]} ${record["Chapter"]}")
                //if(record["BookID"].toInt() == 3) break
                //for(i in metadata) { File("karoli/${i.first}/").mkdirs() }
                val obj = Json.decodeFromString<NewV>(
                    URL("https://getbible.net/v2/karoli/${record["BookID"]}/${record["Chapter"]}.json")
                        .readText()
                )
                Writer.bufferedWriter(json.encodeToString(obj), "karoli/${obj.book_name}/${obj.chapter}.json")
            }*/
        }
}}

fun parse(args: Array<String>) {
    run {
        val verse: String
        val options = Options()
        val verseOpt = Option("v", "verse", true, "-v <Book> <Chapter(number)> <Verse(number)>")
        val transOpt = Option("t", "translation", true,
            "kjv / en (King James Version)\nkaroli / hu (Károli - Hungarian)\nvulgate / vul / lat (Vulgate - Latin)")
        val debugOpt = Option("d", "debug", false, "Set debug flag on")
        verseOpt.isRequired = true
        debugOpt.isRequired = false
        transOpt.isRequired = false
        options.addOption(verseOpt)
        options.addOption(transOpt)
        options.addOption(debugOpt)
        val parser: CommandLineParser = DefaultParser()
        val formatter = HelpFormatter()
        val cmd: CommandLine //not a good practice, it serves it purpose
        try {
            cmd = parser.parse(options, args)
        }catch(e: ParseException){
            e.printStackTrace()
            formatter.printHelp("foo", options)
            exitProcess(1)
        }
        verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")
        if(cmd.hasOption("debug")) isDebug = true
        val foo = verse.split(" ")
        if(foo.size < 2) {
            if(isDebug)
                println("debug: argument count: ${foo.size}")
            error("Not enough arguments")
        }
        book = foo[0]
        chapt = foo[1].toInt()
        try {
            vers = foo[2].toInt()
        } catch(e: Exception) {
            isChapt = true
        }
        if(isDebug)
            println("debug: $book $chapt $vers")
        val value = cmd.getOptionValue("translation")
        translation = when(value) {
            "en", "kjv" ->
                Translation.KJV
            "lat", "val", "vulgate" ->
                Translation.Vulgate
            "karoli", "hu" ->
                Translation.Karoli
            null -> // Default
                Translation.Karoli
            else -> {
                System.err.println("Invalid translation setting")
                exitProcess(1)
            }
        }
        if(isDebug)
            println("debug: ${translation.toStr()} $value")
    }
}

fun read(name: String): String {
    try {
        val file = File(name)
        val sc = Scanner(file)
        var buff = ""
        while (sc.hasNextLine()) {
            val data = sc.nextLine()
            buff += data
        }
        sc.close()
        return buff
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }
    return ""
}
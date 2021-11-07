import com.neutron.BookName
import com.neutron.Chapter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.StringBuilder
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess


enum class Translation { KJV, Vulgate, Karoli }
fun Translation.toStr() = when(this) {
    Translation.KJV ->
        "kjv"
    Translation.Vulgate ->
        "vulgate"
    Translation.Karoli ->
        "karoli"
}

val metadata: MutableList<Pair<String, Int>> = mutableListOf()
lateinit var book: String
lateinit var translation: Translation
var chapt by Delegates.notNull<Int>()
var vers by Delegates.notNull<Int>()
var isDebug: Boolean = false
var isChapt = false
var isNum = false
const val progName = "GetVerse"
const val VERSION = 1.2

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
            val obj: Chapter?
            try {
                obj = Json.decodeFromString(read("bibles/${translation.toStr()}/$book/$chapt.json"))
            } catch (e: Exception) {
                if(isDebug) e.printStackTrace()
                System.err.println("No such chapter")
                exitProcess(1)
            }
            requireNotNull(obj)
            if(!isChapt) {
                if(isNum) print("$vers. ")
                println(obj.verses[vers - 1].text)
            } else {
                if(isNum)
                    obj.verses.forEach { println("${it.verse}. ${it.text}") }
                else
                    obj.verses.forEach { println(it.text) }
            }
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
        if("-V" in args || "--version" in args) {
            println("Version: $VERSION")
            exitProcess(0)
        }
        val verse: String
        val options = Options()
        val verseOpt = Option("v", "verse", true, "-v \"<Book> <Chapter(number)> <Verse(number, not required)>\"")
        val transOpt = Option("t", "translation", true,
            "kjv / en (King James Version)\nkaroli / hu (Károli - Hungarian)\nvulgate / vul / lat (Vulgate - Latin)")
        val debugOpt = Option("d", "debug", false, "Set debug flag on")
        val numOpt = Option("n", "number", false, "Print verse number")
        val helpOpt = Option("h", "help", false, "Print out help")
        verseOpt.isRequired = true
        options.addOption(verseOpt)
        options.addOption(transOpt)
        options.addOption(debugOpt)
        options.addOption(numOpt)
        options.addOption(helpOpt)
        val parser: CommandLineParser = DefaultParser()
        val formatter = HelpFormatter()
        val cmd: CommandLine
        try {
            cmd = parser.parse(options, args)
        }catch(e: ParseException){
            System.err.println(e.localizedMessage)
            formatter.printHelp(progName, options)
            exitProcess(1)
        }
        verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")
        if(cmd.hasOption("help")) {
            formatter.printHelp(progName, options)
            exitProcess(0)
        }
        if(cmd.hasOption("debug")) isDebug = true
        if(cmd.hasOption("number")) isNum = true
        val foo = verse.split(" ")
        if(foo.size < 2) {
            if(isDebug)
                println("debug: argument count: ${foo.size}")
            error("Not enough arguments")
        }
        parseVerse(verse)
        if(isDebug)
            println("debug: $book $chapt")
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
fun parseVerse(raw: String) {
    /*if(!raw.matches("(\\d*)\\s*([a-zA-Z]+)\\s*(\\d+)(?:(:|,)(\\d+))?".toRegex())) {
        System.err.println("Invalid verse")
        exitProcess(1)
    }*/
    var stringBuilder = ""
    try {
        val tmp = raw.split(' ') // "Psalms 9, 1"
        try {
            stringBuilder += "${tmp[0].toInt()} "
            stringBuilder += tmp[1]
            chapt = tmp[2].toInt()
            try {
                vers  = tmp[3].toInt()
            } catch (e: IndexOutOfBoundsException) {
                isChapt = true
            }
        } catch (e: NumberFormatException) {
            if(isDebug)
                e.printStackTrace()
            stringBuilder += tmp[0]
            chapt = tmp[1].toInt()
            try {
                vers = tmp[2].toInt()
            } catch (e: IndexOutOfBoundsException) {
                isChapt = true
            }
        }
        //stringBuilder += tmp[1].lowercase()

    } catch (e: Exception) {
        if(isDebug)
            e.printStackTrace()
        System.err.println("Invalid verse/chapter")
    }
    if(isDebug)
        println("debug: chapt:$chapt vers:$vers str: $stringBuilder")
    book = stringBuilder
}
fun read(name: String): String {
    val file = File(name)
    val sc = Scanner(file)
    var buff = ""
    while (sc.hasNextLine()) {
        val data = sc.nextLine()
        buff += data
    }
    sc.close()
    return buff
}
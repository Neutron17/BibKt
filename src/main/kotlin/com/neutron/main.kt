import com.neutron.BookName
import com.neutron.Chapter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import java.io.File
import java.util.*
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

class Args {
    var book: String = "John"
    var translation: Translation = Translation.KJV
    var chapt: Int = 3
    var vers: Int  = 16
    var isChapt = false
    var isNum   = false

    constructor(book: String, translation: Translation, chapt: Int, vers: Int, isChapt: Boolean, isNum: Boolean) {
        this.book = book
        this.translation = translation
        this.chapt = chapt
        this.vers = vers
        this.isChapt = isChapt
        this.isNum = isNum
    }
    constructor()
}

var isDebug: Boolean = false
val metadata: MutableList<Pair<String, Int>> = mutableListOf()
const val progName = "GetVerse"
const val VERSION = 1.2

class Main { companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val arg = parse(args)
            run {
                var i = 0
                Json.decodeFromString<MutableList<BookName>>(read("books.json")).forEach {
                    i++
                    metadata.add(Pair(it.name, i))
                }
            }
            val obj: Chapter?
            try {
                obj = Json.decodeFromString(read("bibles/${arg.translation.toStr()}/${arg.book}/${arg.chapt}.json"))
            } catch (e: Exception) {
                if(isDebug) e.printStackTrace()
                System.err.println("No such chapter")
                exitProcess(1)
            }
            requireNotNull(obj)
            if(!arg.isChapt) {
                if(arg.isNum) print("${arg.vers}. ")
                println(obj.verses[arg.vers - 1].text)
            } else {
                if(arg.isNum)
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

fun parse(args: Array<String>): Args {
    if ("-V" in args || "--version" in args) {
        println("Version: $VERSION")
        exitProcess(0)
    }
    val verse: String
    val options = Options()
    val verseOpt = Option("v", "verse", true, "-v \"<Book> <Chapter(number)> <Verse(number, not required)>\"")
    val transOpt = Option(
        "t", "translation", true,
        "kjv / en (King James Version)\nkaroli / hu (Károli - Hungarian)\nvulgate / vul / lat (Vulgate - Latin)"
    )
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
    } catch (e: ParseException) {
        System.err.println(e.localizedMessage)
        formatter.printHelp(progName, options)
        exitProcess(1)
    }
    verse = cmd.getOptionValue("verse").replace(",", " ").replace(":", " ")
    if (cmd.hasOption("help")) {
        formatter.printHelp(progName, options)
        exitProcess(0)
    }
    val arg = Args()
    if (cmd.hasOption("debug")) isDebug = true
    if (cmd.hasOption("number")) arg.isNum = true
    val foo = verse.split(" ")
    if (foo.size < 2) {
        if (isDebug)
            println("debug: argument count: ${foo.size}")
        error("Not enough arguments")
    }
    parseVerse(verse)
    if (isDebug)
        println("debug: ${arg.book} ${arg.chapt}")
    val value = cmd.getOptionValue("translation")
    arg.translation = when (value) {
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
    if (isDebug)
        println("debug: ${arg.translation.toStr()} $value")
    return arg
}

fun parseVerse(raw: String): Args {
    /*if(!raw.matches("(\\d*)\\s*([a-zA-Z]+)\\s*(\\d+)(?:(:|,)(\\d+))?".toRegex())) {
        System.err.println("Invalid verse")
        exitProcess(1)
    }*/
    var stringBuilder = ""
    val args = Args()
    try {
        val tmp = raw.split(' ') // "Psalms 9, 1"
        try {
            stringBuilder += "${tmp[0].toInt()} "
            stringBuilder += tmp[1]
            args.chapt = tmp[2].toInt()
            try {
                args.vers  = tmp[3].toInt()
            } catch (e: IndexOutOfBoundsException) {
                args.isChapt = true
            }
        } catch (e: NumberFormatException) {
            if(isDebug)
                e.printStackTrace()
            stringBuilder += tmp[0]
            args.chapt = tmp[1].toInt()
            try {
                args.vers = tmp[2].toInt()
            } catch (e: IndexOutOfBoundsException) {
                args.isChapt = true
            }
        }
    } catch (e: Exception) {
        if(isDebug)
            e.printStackTrace()
        System.err.println("Invalid verse/chapter")
    }
    if(isDebug)
        println("debug: chapt:${args.chapt} vers:${args.vers} str: $stringBuilder")
    args.book = stringBuilder
    return args
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
import java.io.*

class SaveList{
    private lateinit var savePath: String

    fun set (path: String) {
        this.savePath = path
    }

    fun save (saveList: MutableList<String>) : Double{
        val savePathFile = File(this.savePath)
        try {
            val mesuretime = measureExecutionTime {
                val outstream: FileOutputStream = FileOutputStream(savePathFile, true)
                val writer: OutputStreamWriter = OutputStreamWriter(outstream)
                for (data in saveList) {
                    writer.write(data)
                    writer.write("\r")
                }
                writer.flush()
                writer.close()
            }
            return mesuretime

        } catch (e: FileNotFoundException){
            e.printStackTrace()
            return 0.0

        } catch (e: IOException) {
            e.printStackTrace()
            return 0.0
        }
    }

    fun measureExecutionTime(action: () -> Unit): Double {
        val startTime = System.currentTimeMillis()
        action()
        val endTime = System.currentTimeMillis()
        return (endTime - startTime).toDouble() // ミリ秒
    }
}
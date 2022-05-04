import kotlin.reflect.*
import kotlin.reflect.full.*

// know if a KClassifier is an enumerated
    fun KClassifier?.isEnum() = this is KClass<*> && this.isSubclassOf(Enum::class)

    // get a list of constants of an enumerated type
    fun <T : Any> KClass<T>.enumConstants(): List<T> {
        require(isEnum()) { "class must be enum" }
        return this.java.enumConstants.toList()
    }




     /*fun mapType(c: KProperty<*>): String =
        (if (c.returnType.classifier.isEnum())
            c.name + (c.returnType.classifier as KClass<*>).enumConstants().joinToString { "'$it'" } + ")"
        else if (c.returnType.classifier == String::class && c.hasAnnotation<Length>())
            "VARCHAR(${c.findAnnotation<Length>()!!.size})"
        else
            when (c.returnType.classifier) {
                String::class -> "CHAR"
                Int::class -> "INT"
                Double::class -> "DOUBLE"
                Boolean::class -> "BIT"
                else -> fail("not supported")
            }) +
                if (!c.returnType.isMarkedNullable && !c.hasAnnotation<XmlName>())
                    " NOT NULL"
                else if (c.hasAnnotation<XmlName>())
                    " PRIMARY KEY"
                else
                    ""

*/

class XMLGenerator() {

    //returns the list of properties in declaration order
    private fun fields(c: KClass<*>): List<KProperty1<*, *>> {
        require(c.isData)
        val consParams = c.primaryConstructor!!.parameters
        return c.declaredMemberProperties.sortedWith { a, b ->
            consParams.indexOfFirst { it.name == a.name } - consParams.indexOfFirst { it.name == b.name }
        }
    }

    /*
        given an object o, generate a TextEntity representation of it
    */

    private fun createTextEntity(o:Any,root:Entity?){
        var eName = ""
        var eText = ""
        fields(o::class).forEach {
            if (fieldName(it) == "entityID") {
                eName = it.call(o).toString()
            }  else if (fieldText(it)) {
                eText = it.call(o).toString()
            }
        }
        TextEntity(eName, eText, root)
    }

    //given an object o, generate an xml representation of it
     fun createXMLElement(o:Any,root:Entity?):Entity? {
         var firstElement:Entity?=null
         var ent:Entity?=null
         var eName:String
         var eAttribute:String?


         if (isTextEntity(o)) {
            createTextEntity(o,root)
         }
         else {
             for (f in fields(o::class)) {
                 if (fieldToIgnore(f)) continue
                 if (fieldName(f) == "entityID") {
                     eName = f.call(o).toString()
                     ent = Entity(eName, root)
                     if (root == null)
                         firstElement = ent
                 } else if (fieldName(f) == "attribute") {
                     eAttribute = if (f.call(o) != null) f.call(o).toString() else null
                     if (eAttribute != null)
                         ent?.addAttribute(Attribute(f.name, eAttribute!!))

                 } else if (fieldName(f) == "children") {
                     val l = f.call(o) as Collection<*>
                     if (l.isNotEmpty() && l != null)
                         l.forEach {
                             if (it != null)
                                 createXMLElement(it, ent)
                         }
                 }
             }
         }
         return firstElement
     }

    //checks if an object should be represented as a TextEntity or not
     private fun isTextEntity(o:Any):Boolean{
        fields(o::class).forEach {
            if(it.hasAnnotation<XmlTagContent>()){
                return it.call(o)!=null
            }
        }
        return false
    }

    //check if a kProperty has annotation XmlTagContent
    private fun fieldText(p: KProperty<*>):Boolean =
        p.hasAnnotation<XmlTagContent>()

    //check if a kProperty has annotation XmlName
    private fun fieldName(p: KProperty<*>):String? =
        if(p.hasAnnotation<XmlName>()) p.findAnnotation<XmlName>()!!.name
        else null

    //check if a kProperty has annotation XmlIgnore
    private fun fieldToIgnore(p: KProperty<*>):Boolean =
        p.hasAnnotation<XmlIgnore>()



}
fun main() {

    val s3 = Book("golden ball award","After many years...",BookPart.Middle,null )
    val li:List<Book>?=listOf(s3)

    val s2 = Book("first chapter",null,BookPart.Start,li )
    val l:List<Book>?=listOf(s2)

    val s1 = Book("Messi", null, BookPart.Start,l)


    val xmlGen = XMLGenerator()
    xmlGen.createXMLElement(s1,null)?.printVisitor()

}


@Target(AnnotationTarget.PROPERTY)
annotation class XmlName(val name:String)

@Target(AnnotationTarget.PROPERTY)
annotation class XmlTagContent()

@Target(AnnotationTarget.PROPERTY)
annotation class XmlIgnore()


data class Book(
    @XmlName("entityID")
    val name:String,
    @XmlTagContent()
    val text:String?,
    @XmlName("attribute")
    val position:BookPart?,
    @XmlName("children")
    val chapters:List<Book>?
)

enum class BookPart {
    Start, Middle, End
}
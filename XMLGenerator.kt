import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * The instances of this class are objects that represent generator of XMLElement objects, specifical
 */
class XMLGenerator() {

    /**
     * @param c : the class whose properties have to be extracted.
     * effect: returns the list of properties in declaration order
     */
    private fun fields(c: KClass<*>): List<KProperty1<*, *>> {
        require(c.isData)
        val consParams = c.primaryConstructor!!.parameters
        return c.declaredMemberProperties.sortedWith { a, b ->
            consParams.indexOfFirst { it.name == a.name } - consParams.indexOfFirst { it.name == b.name }
        }
    }

    /*


    */
    /**
     * @param o: the object used to create a TextEntity representation
     * @param root: the parent of the TextEntity object that will be created
     * effect: given an object o, generate a TextEntity representation of it
     */
    private fun createTextEntity(o:Any,root:Entity?){
        var eName = ""
        var eText = ""
        var eAttribute:Attribute?=null
        fields(o::class).forEach {
            if (fieldName(it) == "entityID") {
                eName = it.call(o).toString()
            }  else if (fieldText(it)) {
                eText = it.call(o).toString()
            } else if (fieldName(it) == "attribute") {
                eAttribute = if (it.call(o) != null) Attribute(it.name,it.call(o).toString()) else null
            }
        }
        val te=TextEntity(eName, eText, root)
            if (eAttribute != null)
                te.addAttribute(eAttribute!!.key, eAttribute!!.value)
    }

    /**
     * @param o: the object used to create a TextEntity representation
     * @param root: the parent of the TextEntity object that will be created
     * effect: given an object o, generate an xml representation of it
     */
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
                         ent?.addAttribute(f.name, eAttribute!!)

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

    //
    /**
     * @param o : the object which is required to check on.
     * effect: checks if an object should be represented as a TextEntity or not
     */
     private fun isTextEntity(o:Any):Boolean{
        fields(o::class).forEach {
            if(it.hasAnnotation<XmlTagContent>()){
                return it.call(o)!=null
            }
        }
        return false
    }


    /**
     * @param p: the kProperty which has to be checked.
     * effect: check if a kProperty has annotation XmlTagContent
     */
    private fun fieldText(p: KProperty<*>):Boolean =
        p.hasAnnotation<XmlTagContent>()


    /**
     *  @param p: the kProperty which has to be checked.
     *  check if a kProperty has annotation XmlName
     */
    private fun fieldName(p: KProperty<*>):String? =
        if(p.hasAnnotation<XmlName>()) p.findAnnotation<XmlName>()!!.name
        else null

    /**
     * @param p: the kProperty which has to be checked.
     * check if a kProperty has annotation XmlName
     */
    private fun fieldToIgnore(p: KProperty<*>):Boolean =
        p.hasAnnotation<XmlIgnore>()



}

fun main() {

    /*val s3 = Book("golden ball award","After many years...",BookPart.Middle,null )
    val li:List<Book>?=listOf(s3)

    val s2 = Book("first chapter",null,BookPart.Start,li )
    val l:List<Book>?=listOf(s2)

    val s1 = Book("Messi", null, BookPart.Start,l)*/
    val b1=Book("paragraph 1","Once upon a time...",BookPart.Start,null )
    val listParagraph=listOf(b1)
    val b=Book("chapter 1",null,BookPart.Start,listParagraph )
    val c=Book("chapter 2","first classes...",BookPart.Start,null )
    val listChapters= listOf(b,c)
    val a = Book("harry potter",null,BookPart.Start,listChapters)

    val xmlGen = XMLGenerator()
    println(xmlGen.createXMLElement(a,null)?.serialize())

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

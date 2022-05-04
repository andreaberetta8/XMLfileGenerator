abstract class XMLElement(val name: String, val parent: Entity? = null) {

    init {
        parent?.children?.add(this)
    }

    abstract fun accept(v: XMLVisitor)

    //print the element
    abstract fun print()
}

class TextEntity(name: String,val text:String, parent: Entity? = null) : XMLElement(name, parent) {

    override fun print(){
        println("<${this.name}> ${this.text} </${this.name}>")
    }
    override fun accept(v: XMLVisitor) {
        v.visit(this)
    }
}

class Entity(name: String, parent: Entity? = null) : XMLElement(name, parent) {
    var atb = mutableListOf<Attribute>()

    constructor(name:String,parent: Entity? ,attributes:List<Attribute>) : this(name,parent) {
        atb=attributes as MutableList<Attribute>
    }

    var children = mutableListOf<XMLElement>()

    //add an attribute to the list of attributes of the entity
    fun addAttribute(attribute:Attribute){
        atb?.add(attribute)
    }

    //check if an entity has attributes
    fun hasAttribute():Boolean=atb.isNotEmpty()

    override fun print(){
        if (this.atb!!.isNotEmpty()) {//if attribute!=null
            var s="<${this.name} "
            for (att in this.atb)
                s+=att.toString()
            s+="> <${this.name} >"
            println(s)
        }else
            println("<${this.name} >")
    }

    override fun accept(v: XMLVisitor) {
        if(v.visit(this)) {
            children.forEach {
                it.accept(v)
            }
        }
        v.endVisit(this)
    }
}


interface XMLVisitor {
    fun visit(e: TextEntity) {}
    fun visit(e: Entity) = true
    fun endVisit(e: Entity) {}
}


//check if an XMLElement is inside the structure using its name,if not it returns null, otherwise it returns the found element
fun Entity.findVisitor(name: String): XMLElement? {
    var el: XMLElement? = null
    val v = object : XMLVisitor {
        override fun visit(e: TextEntity) {
            if(e.name == name)
                el = e
        }

        override fun visit(e: Entity): Boolean {
            if (el!=null )return false //if el!= null it means the entity has already been found
            if(e.name == name) {
                el = e
                return false //if i found the entity i can stop the visit
            }
            return true
        }
    }
    accept(v)
    return el
}

//filter the entities visited and produce an XMLELement structure
fun XMLElement.filter(lmbd: (XMLElement) -> Boolean ):XMLElement? {
    var firstElement:Entity?=null
    var tmpParent:Entity?=null   //represent the Entity that will be the parent of the next Entity

        val v = object : XMLVisitor {

            override fun visit(e: TextEntity){
                if (lmbd(e)) {
                    TextEntity(e.name,e.text,tmpParent)
                }
            }
            override fun visit(e: Entity):Boolean{
                if (lmbd(e)) {
                    if (tmpParent == null) { //if i m visiting the first entity, its parent is null
                        if (e.hasAttribute())
                            tmpParent= Entity(e.name, null,e.atb)
                        else
                            tmpParent=Entity(e.name,null)

                        firstElement = tmpParent //the tmpParent became the first entity
                        return true
                    }
                    val ent = Entity(e.name, tmpParent) //create a copy of the current entity with parent=tmpParent
                    if (e.children.isNotEmpty())
                        tmpParent = ent //if ent has children i have to add those children to ent

                }
                return true
            }
        }
        accept(v)
        return firstElement
    }

//print all the subentity of the current object
fun Entity.printVisitor() {
    accept(object : XMLVisitor {
        var depth = 0
        override fun visit(e: TextEntity) {
            println("\t".repeat(depth) + "<${e.name}> ${e.text} </${e.name}>")
        }

        override fun visit(e: Entity): Boolean {
            if (e.atb!!.isNotEmpty()) {//if attribute!=null
                var s="\t".repeat(depth) + "<${e.name} "
                for (att in e.atb)
                    s+=att.toString()
                s+=">"
                println(s)
            }else
                println("\t".repeat(depth) + "<${e.name}>")//print name
            depth++
            return true
        }

        override fun endVisit(e: Entity) {
            depth--
            println("\t".repeat(depth) + "</${e.name}>")

        }
    })
}


//represent an attribute in xml which is composed by a key and a value
class Attribute(val key:String, val value:String){

      override fun toString():String= "$key='$value' "

}

class XMLDocument(val name:String, val e:Entity){
    private val header="<?xml version='1.0' encoding='UTF-8'?>"

    //returns an XMLDocument instance from this ,after filtering based on f
     fun XMlfilter(f :(XMLElement)->Boolean) :XMLDocument{
         return XMLDocument("filteredDoc", e.filter(f) as Entity)
     }
    //returns null if there is no element with such a name in the document
     fun findEntity(name:String):XMLElement?{
        return e.findVisitor(name)
    }
     fun printDocument(){
        println(header)
        e.printVisitor()
    }
}
fun main() {


    val att1=Attribute("genre ","action")
    val att2=Attribute("year ","2008")
    val e1=Entity("filter1",null)
    e1.addAttribute(att1)
    e1.addAttribute(att2)
    val t1=TextEntity("1film","mission impossible",e1)
    val t2=TextEntity("2film","joker",e1)
    val e2=Entity("filter2",e1)
    e2.addAttribute(att2)
    val t3=TextEntity("1film","mission impossible",e2)
    val e3=Entity("select",e2)
    val t4=TextEntity("press button","film selected",e3)



    val d=XMLDocument("filmSelection",e1)
    //d.printDocument()
    //print(d.findEntity("1film")?.name)
    d.XMlfilter { e-> e.name!="filter2" }.printDocument()


}

class Controller() {

    val listOfCOmmand=UndoStack()

    fun addEntity(e:XMLElement,name:String, parent:Entity?){
        val ent= e as Entity
        listOfCOmmand.execute(AddEntityCommand(ent,name,parent))
    }
    fun renameEntity(e:XMLElement,newName:String){
        e.name=newName
    }
    fun addNewAttribute(e:XMLElement,key:String,value:String){
        val entity= e as Entity
        listOfCOmmand.execute(AddAttributeCommand(entity,key,value))
    }

    fun modifyAttribute(e:XMLElement,key:String,newValue:String){
        val entity= e as Entity
        entity.editAttribute(key,newValue)
    }

    fun deleteAttribute(e:XMLElement,key:String){
        val entity= e as Entity
        entity.removeAttribute(key)
    }

    fun addTextEntity(e:XMLElement,name:String,text:String){
        val ent= e as Entity
        ent.addTextEntityChild(name,text)
    }

    fun deleteEntity(e:XMLElement){
        e.removeEntity()
    }

    fun goBack(){
        if (!listOfCOmmand.stack.empty())
            listOfCOmmand.undo()
    }
}
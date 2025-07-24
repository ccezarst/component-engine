package EngineCore.Actions;


public class ActionDataContainer <T> {
    private T data;
    public ActionDataContainer(T defaultVal){
        this.data = defaultVal;
    }

    public T getValue(){
        return this.data;
    }

    public void setValue(T newv){
        this.data = newv;
    }
}

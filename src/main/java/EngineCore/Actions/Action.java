package EngineCore.Actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class Action<T> {
    private ActionDataContainer<T> data;

    private ArrayList<Consumer<ActionDataContainer>> subscribers;

    public final String name;

    public Action(String name, T defaultValue, Consumer<ActionDataContainer>... defaultCallbacks){
        this.name = name;
        this.data = new ActionDataContainer<>(defaultValue);
        this.subscribers = new ArrayList<>();
        this.subscribers.addAll(Arrays.asList(defaultCallbacks));
    }

    public void trigger(){
        for(Consumer<ActionDataContainer> callback: this.subscribers){
            callback.accept(this.data);
        }
    }

    public void trigger(ActionDataContainer<T> data){
        this.data = data;
        this.trigger();
    }

    public void subscribe(Consumer<ActionDataContainer> callback){
        this.subscribers.add(callback);
    }

    public ActionDataContainer getContainer(){
        return  this.data;
    }
}

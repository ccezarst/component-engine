package EngineCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import EngineCore.Actions.Action;
import EngineCore.Actions.ActionDataContainer;
import EngineCore.DefaultComponents.ComponentType;
import EngineCore.DefaultComponents.CoreComponent;
import EngineCore.DefaultComponents.CoreComponent.CoreComponentBackingThread;
import EngineCore.DefaultComponents.GlobalVariableContainer;
import EngineCore.DefaultComponents.Extra.UserInterface.ConsoleInterface;
import EngineCore.DefaultComponents.Extra.UserInterface.GeneralCommandsHandler;

public class EngineCore {
	// TODO: add exception throwing to the code
    private ArrayList<CoreComponent> components = new ArrayList<CoreComponent>();

    private Map<String, GlobalVariableContainer> globalVariables;

    private ArrayList<Action> actions = new ArrayList<>();

    public int threads = 5;
    public ArrayList<CoreComponent.CoreComponentBackingThread> threadsList = new ArrayList<>();

    private boolean logInteractions = false;

    public EngineCore(boolean useInstrumentation, Class<?> instrumentationClass){
        this.superSecretFunc();
		this.addComponent(new GeneralCommandsHandler(true, this));
		this.addComponent(new ConsoleInterface(true, this));
		this.useInstrumentation = useInstrumentation;
		this.instrumentation = instrumentationClass;
		if(this.useInstrumentation) {
        	InstrumentationBootstrap.install(instrumentation);
        	// this allows stuff like seeing loaded classes, etc..
        }
    }
    
    public EngineCore(){
        this(true, InstrumentationInterface.class);
    }

    public final void superSecretFunc(){
        this.globalVariables = new HashMap<>();
    }


    //                                          -- ACTIONS


    public final ArrayList<Action> getAllActions(){
        return this.actions;
    }

    public final <T> void addAction(String actionName, T actionDataType, Consumer<ActionDataContainer>... defaultCallbacks){
        if(this.getActionFromName(actionName) == null){
            this.actions.add(new Action<T>(actionName, actionDataType, defaultCallbacks));
        }
    }
    public final Action getActionFromName(String name){
        for(Action pl: this.actions){
            if(Objects.equals(pl.name, name)){
                return pl;
            }
        }
        return null;
    }

    private ArrayList<String> actionWaitingList = new ArrayList<>();
    private ArrayList<Consumer<ActionDataContainer>> callbackWaitingList = new ArrayList<>();

    public final void subscribeToAction(String actionName, Consumer<ActionDataContainer> callback){
        Action res = this.getActionFromName(actionName);
        if(res != null){
            res.subscribe(callback);
        }else{
            // action might have not been created yet
            this.actionWaitingList.add(actionName);
            this.callbackWaitingList.add(callback);
        }
    }

    public final void connectActions(String actionA, String actionB){
        Action firstAction = this.getActionFromName(actionA);
        Action secondAction = this.getActionFromName(actionB);
        if(firstAction.getContainer().getValue().getClass() == secondAction.getContainer().getValue().getClass()){
            firstAction.subscribe((ActionDataContainer) -> {
                secondAction.trigger((ActionDataContainer) ActionDataContainer);
            });
        }else{
            firstAction.subscribe((ActionDataContainer) -> {
                secondAction.trigger();
            });
        }
    }


    //                                              -- GLOBAL VARIABLES


    public final <T> void setGlobalVariable(String name, T instance){
        if(this.globalVariables.containsKey(name)){
            this.globalVariables.get(name).value = instance;
        }else{
            this.globalVariables.put(name, new GlobalVariableContainer(instance));
        }
    }

    public final <T> T getGlobalVariable(String name, Class<T> caster){
        if(this.globalVariables.containsKey(name)){
            return caster.cast(Objects.requireNonNull(this.globalVariables.get(name)).value);
        }else{
            //throw new RuntimeException("Failed to find variable");
            return null;
        }
    }

    public final void activateInteractionLogging(){
        this.logInteractions = true;
    }


    public final void disableInteractionLogging(){
        this.logInteractions = false;
    }

    public void logInteraction(String message){}


    //                                              -- COMPONENTS


    private String compsToString(ArrayList<CoreComponent> in){
        String res = "";
        for(CoreComponent comp:  in){
            res += comp.name;
        }
        return res;
    }
    public final void reorderComponents(){
        ArrayList<String> history = new ArrayList<>();
        while(true){
            for(CoreComponent comp : this.components){
                for(CoreComponent mata : this.components){
                    if(this.components.indexOf(comp) < this.components.indexOf(mata)){
                        for(ComponentType tip : comp.dependencies){
                            for(ComponentType mataType: mata.types){
                                if(mataType.name() == tip.name()){
                                    this.components.remove(mata);
                                    this.components.add(this.components.indexOf(comp), mata);
                                    if(history.contains(this.components)){
                                        throw new IllegalArgumentException("CoreComponents circular dependency, check dependencies");
                                    }else{
                                        history.add(this.compsToString(this.components));
                                    }
                                    continue; // DO NOT REMOVE.
                                }
                            }
                        }
                    }
                }
            }
            break;
        }
    }

    public final void removeComponent(String name){
        if(this.logInteractions){
            this.logInteraction("Removed component: " + name);
        }
        this.components.remove(this.getComponentFromName(name));
        this.reorderComponents();
    }

    public final void addComponent(CoreComponent comp){
        if(this.getComponentFromName(comp.name) == null){
            if(this.logInteractions){
                this.logInteraction("Added component: " + comp.name);
            }
            this.components.add(comp);
            this.reorderComponents();
        }else{
            throw new IllegalArgumentException("CoreComponent already exists with the name " + comp.name);
        }
    }
    

    public final CoreComponent getComponentFromName(String name){
        for(int i = 0; i < this.components.size(); i++){
            if(Objects.equals(this.components.get(i).name, name)){
                if(this.logInteractions){
                    this.logInteraction("Returned component from name: " + name);
                }
                return this.components.get(i);
            }
        }
        if(this.logInteractions){
            this.logInteraction("Failed to return component from name(name not found): " + name);
        }
        return null;
    }
    

    public final <T> T getComponentFromName(String name, Class<? extends T> caster){
        for(int i = 0; i < this.components.size(); i++){
            if(Objects.equals(this.components.get(i).name, name)){
                if(this.logInteractions){
                    this.logInteraction("Returned component from name: " + name);
                }
                return caster.cast(this.components.get(i));
            }
        }
        if(this.logInteractions){
            this.logInteraction("Failed to return component from name(name not found): " + name);
        }
        return null;
    }

    public final ArrayList<CoreComponent> getAllComponents(){
        if(this.logInteractions){
            this.logInteraction("Returned all components");
        }
        return this.components;
    }

    public final void wipeComponents(){
        this.components.clear();
    }

    private final boolean containsCompType(ComponentType[] list, ComponentType type){
        for(int i = 0; i < list.length; i++){
            if(list[i].name().equals(type.name())){
                return true;
            }
        }
        return false;
    }

    public final ArrayList<CoreComponent> getComponentsOfType(ComponentType type){
        ArrayList<CoreComponent> toReturn = new ArrayList<CoreComponent>();

        for(int i =0; i< this.components.size(); i++){
            if(this.containsCompType(this.components.get(i).types, type)){
                toReturn.add(this.components.get(i));
            }
        }
        if(this.logInteractions){
            this.logInteraction("Gotten components of type: " + type.name());
        }
        return toReturn;
    }
    public final <T> ArrayList<T> getComponentsOfType(ComponentType type, Class<? extends T> caster){
        ArrayList<T> toReturn = new ArrayList<T>();

        for(int i =0; i< this.components.size(); i++){
            if(this.containsCompType(this.components.get(i).types, type)){
                toReturn.add(caster.cast(this.components.get(i)));
            }
        }
        if(this.logInteractions){
            this.logInteraction("Gotten components of type: " + type.name());
        }
        return toReturn;
    }


    //                      -- INTERFACES

    /*
    public final ArrayList<Interface> getInterfacesOfType(InterfaceType type){
        ArrayList<CoreComponent> interfs = this.getComponentsOfType(ComponentType.INTERFACE);
        ArrayList<Interface> toReturn = new ArrayList<>();
        for(int i = 0; i < interfs.size(); i++){
            if(((Interface)(interfs.get(i))).interfaceType == type){
                toReturn.add((Interface) interfs.get(i));
            }
        }
        if(this.logInteractions){
            this.logInteraction("Gotten interfaces of type: " + type.name());
        }
        return toReturn;
    }

    public final <T> ArrayList<T> getInterfacesOfType(InterfaceType type, Class<? extends T> caster){
        ArrayList<CoreComponent> interfs = this.getComponentsOfType(ComponentType.INTERFACE);
        ArrayList<T> toReturn = new ArrayList<>();
        for(int i = 0; i < interfs.size(); i++){
            if(((Interface)(interfs.get(i))).interfaceType == type){
                toReturn.add(caster.cast(interfs.get(i)));
            }
        }
        if(this.logInteractions){
            this.logInteraction("Gotten interfaces of type: " + type.name());
        }
        return toReturn;
    }
	*/

    //                      -- THREADING


    protected Map<String, Double> threadLoopTimes = new HashMap<>();
    public void reportThreadLoopTime(String threadID, double ms){
        this.threadLoopTimes.put(threadID, ms);
    }

    public CoreComponent.CoreComponentBackingThread createNewThread(){
        CoreComponent.CoreComponentBackingThread toReturn = new CoreComponent.CoreComponentBackingThread(this);
        this.threadsList.add(toReturn);
        return toReturn;
    }

    public void addComponentToThread(String componentName, String threadName){
        for(CoreComponent.CoreComponentBackingThread thread : this.threadsList){
            if(thread.getName() == threadName){
                thread.attachComponent(this.getComponentFromName(componentName));
            }
        }
    }

    public CoreComponent.CoreComponentBackingThread getComponentBackingThread(String componentName){
        for(CoreComponent.CoreComponentBackingThread th: this.threadsList){
            if(th.isComponentAttached(componentName)){
                return th;
            }
        }
        return null;
    }

    public void pauseComponentExecution(String componentName){
        this.getComponentBackingThread(componentName).pauseComponentExecution(componentName);
    }

    public void resumeComponentExecution(String componentName){
        this.getComponentBackingThread(componentName).resumeComponentExecution(componentName);
    }

    public void moveComponentToThread(CoreComponent comp, String newThreadName){
        this.getComponentBackingThread(comp.name).deattachComponent(comp);
        this.addComponentToThread(comp.name, newThreadName);
    }



    //                      -- EXTRA

    public boolean useInstrumentation = true;
    public Class<?> instrumentation = InstrumentationInterface.class;
    
    public boolean debugMode = false;

    public void enableDebugging(){
        this.debugMode = true;
        this.logInteractions = true;
    }

    public void init(){this.update();} // the same
    
    public boolean oneThreadPerComponent = false;
    
    
    public void update(){
        this.reorderComponents(); // just to be safe 
        if(this.oneThreadPerComponent) {
        	this.threads = this.components.size();
        }
        this.threadsList.clear();
        for(int i = 0; i < this.threads; i++){
            this.createNewThread();
        }
        int threadNr = 0;
        for(CoreComponent comp: this.components){
            this.threadsList.get(threadNr).attachComponent(comp);
            if(threadNr >= this.threads-1){
                threadNr = 0;
            }else{
                threadNr += 1;
            };
        }
        if(this.debugMode){
            String toPrint = "";
            for(CoreComponent.CoreComponentBackingThread th: this.threadsList){
                toPrint += th.getName() + " - " + th.attachedComponentStepIndex.keySet().toString() + "\n";
            }
        }
        if(this.logInteractions){
            this.logInteraction("Core updated");
        }

    }

    public void start(){
        for(CoreComponent.CoreComponentBackingThread th : this.threadsList){
            th.startRunning();
            th.setUncaughtExceptionHandler((t, e) -> {
                this.handleThreadException((CoreComponentBackingThread)t, e);
            });
        }
    }
    
    protected void handleThreadException(CoreComponentBackingThread t, Throwable e) {
    	e.printStackTrace();
    	// TODO: add a error/logging system to properly log the thread's exception
    	this.threadsList.remove(t);
    	String[] attachedComponents = t.getAttachedComponents();
    	String[] pausedComponents = t.getPausedComponents();
    	CoreComponentBackingThread newThread = this.createNewThread();
    	for(String c: attachedComponents) { // re-attach the components to the new thread
    		newThread.attachComponent(this.getComponentFromName(c));
    	}
    	for(String c: pausedComponents) { // re-pause the components to the new thread
    		newThread.pauseComponentExecution(c);
    	}
    	newThread.startRunning();
    	newThread.setUncaughtExceptionHandler((tt, ee) -> {
            this.handleThreadException((CoreComponentBackingThread)tt, ee);
        });
    	this.update();
    	System.out.println("Thread restarted ");
    }
    
    public void step(){
    	// should not use anymore bcauze the components are automatically stepped by their backing thread
    	// if you want to manually step the components by the main thread, you have to either
    	// 1. make threading optional on the core
    	// 2. pause the execution of all the threads then step the components manually
        if(this.logInteractions){
            this.logInteraction("Core stepped");
        }
        for(int i = 0; i < this.components.size(); i++){
            // primitive step to help not accidentally run components
            // that don't check if they should be active or not
            this.components.get(i).primitiveStep(this);
        }
        for(Map.Entry<String, Double> pair: this.threadLoopTimes.entrySet()){
        //  ----------- this.getGlobalVariable("Telemetry", Telemetry.class).addData(pair.getKey(),pair.getValue() + " ms");
        }
        //((UI_Manager)this.getComponentFromName("UI_Manager")).refresh();
        if(!this.actionWaitingList.isEmpty()){
            this.subscribeToAction(this.actionWaitingList.remove(0), this.callbackWaitingList.remove(0));
        }

    }

    public String getStatus(){
        String toReturn = "";
        if(this.logInteractions){
            this.logInteraction("Core status requested");
        }
        for(int i = 0; i < this.components.size(); i ++){
            ArrayList<String> status = this.components.get(i).primitiveGetStatus();
            String temp = "-";
            if(status != null){
                for(int b = 0; b < status.size(); b++) {
                    temp += Arrays.toString(this.components.get(i).types) + "-" + this.components.get(i).name + ": " + status.get(b) + "\n";
                }
            }

            toReturn += temp;
        }
        return toReturn;
    }

    public void exit(){
    	System.out.print("Running threads: ");
    	for(Thread thread: Thread.getAllStackTraces().keySet()) {
    		System.out.print(thread.getName() + ", ");
    	}
    	System.out.println();
        for(CoreComponent.CoreComponentBackingThread thread: this.threadsList){
            thread.stopRunning();
            System.out.println("Stopped thread: " + thread.getName());
        }
        for(CoreComponent comp : this.components){
            comp.primitiveExit();
            System.out.println("Component exited: " + comp.name);
        }
        Thread.currentThread().interrupt();
    }

    public ArrayList<String> testComponents(){
        // make new TestingEnviroment
        TestingEnviromentCore env = new TestingEnviromentCore();
        env.init();
        for(CoreComponent comp : this.components){
            env.setCurrentComponentName(comp.name);
            env.logInteraction("Component returned: " + comp.primitiveTest(env));
            env.reset();
        }
        return env.logs;
    }
}

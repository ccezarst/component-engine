package EngineCore.DefaultComponents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import EngineCore.EngineCore;
import EngineCore.TestingEnviromentCore;

public abstract class CoreComponent {
    public boolean active = false;
    public final String name;
    public final ComponentType[] types;
    public final EngineCore core;
    public final ArrayList<CoreComponentSettings> settings;
    public final ArrayList<ComponentType> dependencies;

    protected CoreComponentBackingThread runningThread;
    public int threadId = 0;
    public static class CoreComponentBackingThread extends Thread{

        public ArrayList<Consumer<Integer>> stepFuncs = new ArrayList<>();
        public Map<String, Integer> attachedComponentStepIndex = new HashMap<>();
        public EngineCore core;
        public Boolean stepNotifier = false;

        private ArrayList<Integer> pausedComponents = new ArrayList<>();

        public boolean isComponentAttached(String compName){
            return this.attachedComponentStepIndex.containsKey(compName);
        }

        public void attachComponent(CoreComponent comp){
            synchronized (this.stepFuncs){
                synchronized (this.attachedComponentStepIndex){
                    boolean wasRunning = run;
                    if(this.run){
                        this.stopRunning();
                    }
                    // TODO: prob should verify if component is already in list(2 lazy to do)
                    this.stepFuncs.add((Integer o) -> {comp.step(this.core);});
                    this.attachedComponentStepIndex.put(comp.name, this.stepFuncs.size()-1);
                    if(wasRunning){
                        this.startRunning();
                    }
                }
            }
        }

        public void deattachComponent(CoreComponent comp){
            synchronized (this.stepFuncs){
                synchronized (this.attachedComponentStepIndex){
                    boolean wasRunning = run;
                    if(this.run){
                        this.stopRunning();
                    }
                    int index = -1;
                    for(Map.Entry<String, Integer> cEntry: this.attachedComponentStepIndex.entrySet()){
                        if(cEntry.getKey() == comp.name){
                            index = cEntry.getValue();
                        }
                    }
                    if(index == -1){
                        throw new IllegalArgumentException("Component was not attached to thread. CoreComponentBackingThread.deattachComponent, component name: " + comp.name + ", threadName: " + this.getName());
                    }
                    this.stepFuncs.remove(index);
                    this.attachedComponentStepIndex.remove(comp.name);
                    if(wasRunning){
                        this.startRunning();
                    }
                }
            }
        }

        public void pauseComponentExecution(String name){
            int index = -1;
            for(Map.Entry<String, Integer> cEntry: this.attachedComponentStepIndex.entrySet()){
                if(cEntry.getKey() == name){
                    index = cEntry.getValue();
                }
            }
            this.pausedComponents.add(index);
        }

        public void resumeComponentExecution(String name){
            int index = -1;
            for(Map.Entry<String, Integer> cEntry: this.attachedComponentStepIndex.entrySet()){
                if(cEntry.getKey() == name){
                    index = cEntry.getValue();
                }
            }
            this.pausedComponents.remove(index);
        }

        public CoreComponentBackingThread(EngineCore core){
            this.core = core;
        }

        public void startRunning(){
            this.run = true;
            this.stoppedRunning = false;
            this.start();
        }
        private Boolean run = false;
        private Boolean stoppedRunning = true;
        public void run(){
            synchronized (this.attachedComponentStepIndex){
                synchronized (this.pausedComponents){
                	for(Map.Entry<String, Integer> e: this.attachedComponentStepIndex.entrySet()) { // weird bug where if an update needs a step function to run the whole thing halts, so the update runs on the specific thread
                		core.getComponentFromName(e.getKey()).primitiveUpdate(core);
                	}
                }
            }
            while(true){
                if(run){
                    synchronized (this.attachedComponentStepIndex){
                        synchronized (this.pausedComponents){
                            double start = System.nanoTime();
                            int index = 0;
                            for(Consumer<Integer> cons: this.stepFuncs){
                                cons.accept(0);
                                /*
                                if(!this.pausedComponents.contains(index)){

                                }
                                index += 1;
                                 */
                            }
                            this.core.reportThreadLoopTime(Thread.currentThread().getName(), (System.nanoTime()-start)/1000000);
                        }
                    }
                }else{
                    this.stoppedRunning = true;
                    break;
                }
                synchronized(this.stepNotifier) {
                	this.stepNotifier.notifyAll();	
                }
            }
        }

        public void stopRunning(){
            this.run = false;
            // basically this func runs on the calling thread, which is usually the core.
            // however, if this runs on the same thread as the this thread, it's gonna get caught forever.
            // ex: current thread is T1(contains a comps named compA,compB). T1.run -> compA.step -> core.moveComponentToThread(compB, T2)-runs on T1 -> T1.deattachComponent(compB)-runs on T1 -> T1.stopRunning() -> hogs all execution time because it's still T1, and thus does not allow T1.run to finish the while loop and recheck the condition
            // because the func runs on the calling func's thread, which in this example is the current thread.

            long loopOut = 0;
            while(this.stoppedRunning != true && loopOut < 2000){loopOut += 1;} // fail safe in case the ^ex happens
        }
    }


    public CoreComponent(String name, Boolean active, EngineCore core, ComponentType... type){
        this(name, active, core, new ArrayList<>(), type);
    }
    public CoreComponent(String name, Boolean active, EngineCore core, ArrayList<ComponentType> dependencies,ComponentType... type){
        if(name != null && !name.isEmpty()){
            this.name = name;
            this.active = active;
            /// is fixed now :)
            this.types = type;
            this.core = core;
            this.settings = new ArrayList<CoreComponentSettings>();
            this.dependencies = dependencies;
        }else{
            throw new IllegalArgumentException("Name cannot be empty (CoreComponent constructor)");
        }
    }


    public final ArrayList<String> getAllSettings(){
        ArrayList<String> res = new ArrayList<>();
        for(CoreComponentSettings caca : this.settings){
            for(String setting : caca.getSettings()){
                if(!res.contains(setting)){
                    res.add(setting);
                }
            }
        }
        return res;
    }
    public final ArrayList<String> getSettingOptions(String settingName){
        for(CoreComponentSettings caca: this.settings){
            if(caca.getSettings().contains(settingName)){
                return caca.getSettingOptions(settingName);
            }
        }
        return new ArrayList<>();
    }

    public final boolean changeSetting(String settingName, String option){
        synchronized (this.settings){
            // first check if the option is still valid, as it can happen that after a code push the old config is applied and the component might not work.
            if(this.getSettingOptions(settingName).contains(option)){
                for(CoreComponentSettings caca: this.settings){
                    if(caca.getSettings().contains(settingName)){
                        caca.changeSetting(settingName, option);
                    }
                }
                return true;
            }
        }
        return false;
    }
    public final ArrayList<String> primitiveGetStatus(){
        if(this.active){
            return this.getStatus();
        }else{
            ArrayList<String> toReturn = new ArrayList<String>();
            toReturn.add("Component not active");
            return toReturn;
        }
    }
    protected ArrayList<String> getStatus(){
        ArrayList<String> caca = new ArrayList<>();
        caca.add("active");
        return caca;
    }

    public final void primitiveStep(EngineCore core){
        if(this.active){
            this.step(core);
        }
    }

    protected abstract void step(EngineCore core);

    public final void primitiveUpdate(EngineCore core){
        if(this.active){
            this.update(core);
        }
    }
    protected abstract void update(EngineCore core); // update function should contain all init code
    // to allow real-time updating of components and hot testing of components

    public final int primitiveTest(TestingEnviromentCore core){
        if(this.active){
            return this.test(core);
        }else{
            return -404; // hopefully users remember this means component not active
        }
    }
    protected abstract int test(TestingEnviromentCore core);

    public final void primitiveExit(){
        if(this.active){
            this.exit();
        }
    }
    protected void exit(){}
    
    public static ComponentType[] mergeComponentTypes(ComponentType single, ComponentType[] array) {
	    if (array == null) {
	        return new ComponentType[]{ single };
	    }
	    ComponentType[] merged = Arrays.copyOf(array, array.length + 1);
	    merged[array.length] = single;
	    return merged;
	}
	
}

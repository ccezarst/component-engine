package EngineCore.DefaultComponents;

import java.util.ArrayList;

public interface CoreComponentSettings{
    public ArrayList<String> getSettings();
    public ArrayList<String> getSettingOptions(String settingName);
    public void changeSetting(String settingName, String option);
}
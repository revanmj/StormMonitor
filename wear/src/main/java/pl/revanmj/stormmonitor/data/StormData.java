package pl.revanmj.stormmonitor.data;

import java.io.Serializable;

/**
 * Created by revanmj on 10.07.2013.
 */

public class StormData implements Serializable {
    private int cityId;
    private String cityName;
    private int stormChance;
    private int stormTime;
    private int stormAlert;
    private int rainChance;
    private int rainTime;
    private int rainAlert;
    private boolean error;

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public int getStormChance() {
        return stormChance;
    }

    public void setStormChance(int stormChance) {
        this.stormChance = stormChance;
    }

    public int getStormTime() {
        return stormTime;
    }

    public void setStormTime(int stormTime) {
        this.stormTime = stormTime;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public int getRainTime() {
        return rainTime;
    }

    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
    }

    public int getRainChance() {
        return rainChance;
    }

    public void setRainChance(int rainChance) {
        this.rainChance = rainChance;
    }

    public int getRainAlert() {
        return rainAlert;
    }

    public void setRainAlert(int rainAlert) {
        this.rainAlert = rainAlert;
    }

    public int getStormAlert() {
        return stormAlert;
    }

    public void setStormAlert(int stormAlert) {
        this.stormAlert = stormAlert;
    }

    @Override
    public String toString() {
        return cityName + ": id[" + cityId + "] p_b[" + stormChance + "] t_b[" + stormTime + "] p_o[" + rainChance + "] t_o[" + rainTime + "]";
    }
}


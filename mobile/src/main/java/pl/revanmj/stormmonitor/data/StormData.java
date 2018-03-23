package pl.revanmj.stormmonitor.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by revanmj on 10.07.2013.
 */

public class StormData {
    private int cityId;

    @Expose
    @SerializedName("m")
    private String cityName;

    @Expose
    @SerializedName("p_b")
    private int stormChance;

    @Expose
    @SerializedName("t_b")
    private int stormTime;

    @Expose
    @SerializedName("a_b")
    private int stormAlert;

    @Expose
    @SerializedName("p_o")
    private int rainChance;

    @Expose
    @SerializedName("t_o")
    private int rainTime;

    @Expose
    @SerializedName("a_o")
    private int rainAlert;

    public StormData(int cityId, String cityName, int stormChance, int stormTime, int stormAlert,
                     int rainChance, int rainTime, int rainAlert) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.stormChance = stormChance;
        this.stormTime = stormTime;
        this.stormAlert = stormAlert;
        this.rainChance = rainChance;
        this.rainTime = rainTime;
        this.rainAlert = rainAlert;
    }

    public String getCityName() {
        return cityName;
    }

    public int getStormChance() {
        return stormChance;
    }

    public int getStormTime() {
        return stormTime;
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

    public int getRainChance() {
        return rainChance;
    }

    public int getRainAlert() {
        return rainAlert;
    }

    public int getStormAlert() {
        return stormAlert;
    }
}


package pl.revanmj.stormmonitor.model;

import java.io.Serializable;

/**
 * Created by Student on 10.07.13.
 */
public class StormData implements Serializable {
    private int miasto_id;
    private String miasto;
    private int p_burzy;
    private int t_burzy;
    private int p_opadow;
    private int t_opadow;
    private boolean error;

    public String getMiasto() {
        return miasto;
    }

    public void setMiasto(String miasto) {
        this.miasto = miasto;
    }

    public int getP_burzy() {
        return p_burzy;
    }

    public void setP_burzy(int p_burzy) {
        this.p_burzy = p_burzy;
    }

    public int getT_burzy() {
        return t_burzy;
    }

    public void setT_burzy(int t_burzy) {
        this.t_burzy = t_burzy;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public int getMiasto_id() {
        return miasto_id;
    }

    public void setMiasto_id(int miasto_id) {
        this.miasto_id = miasto_id;
    }

    public int getT_opadow() {
        return t_opadow;
    }

    public void setT_opadow(int t_opadow) {
        this.t_opadow = t_opadow;
    }

    public int getP_opadow() {
        return p_opadow;
    }

    public void setP_opadow(int p_opadow) {
        this.p_opadow = p_opadow;
    }
}


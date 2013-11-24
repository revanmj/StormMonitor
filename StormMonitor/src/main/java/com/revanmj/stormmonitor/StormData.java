package com.revanmj.stormmonitor;

import java.io.Serializable;

/**
 * Created by Student on 10.07.13.
 */
public class StormData implements Serializable {
    private int miasto_id;
    private String miasto;
    private int p_burzy;
    private int t_burzy;
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
}


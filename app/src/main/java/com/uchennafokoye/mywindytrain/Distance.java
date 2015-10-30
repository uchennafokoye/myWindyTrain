package com.uchennafokoye.mywindytrain;

/**
 * Created by faithfulokoye on 10/29/15.
 */
public class Distance {
    private double value;
    private String unit;

    public Distance(double value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public String toString() {
        return Double.toString(value) + " " + unit;
    }
}

package com.uchennafokoye.mywindytrain;

/**
 * Created by faithfulokoye on 10/29/15.
 */
public class TrainColor {


    private static final String BLUE = "#0072bc";
    private static final String RED = "#ff0000";
    private static final String YELLOW = "#fff200";
    private static final String GREEN = "#00a651";
    private static final String PURPLE = "#522398";
    private static final String BROWN = "#603913";
    private static final String ORANGE = "#F9461C";
    private static final String PINK = "#f49ac1";

    public String color;
    public String colorName;


    public TrainColor(String colorName) {
        setColor(colorName);
    }


    public String toString() {
        return "Color: " + this.colorName + " Hex: " + this.color;
    }


    public boolean isValid() {
        return (!color.isEmpty());
    }

    public void setColor(String colorName) {

        colorName = colorName.toLowerCase();
        this.colorName = colorName;

        switch (colorName){
            case "red":
                color = RED;
                break;
            case "blue":
                color = BLUE;
                break;
            case "brown":
                color = BROWN;
                break;
            case "orange":
                color = ORANGE;
                break;
            case "green":
                color = GREEN;
                break;
            case "yellow":
                color = YELLOW;
            case "purple":
                color = PURPLE;
                break;
            case "pink":
                color = PINK;
                break;
            default:
                color = "";
                break;

        }

    }
}

package com.uchennafokoye.mywindytrain;

/**
 * Created by faithfulokoye on 10/29/15.
 */
public class TrainColor {


    static final String BLUE = "#0072bc";
    static final String RED = "#ff0000";
    static final String YELLOW = "#fff200";
    static final String GREEN = "#00a651";
    static final String PURPLE = "#522398";
    static final String BROWN = "#603913";
    static final String ORANGE = "#F9461C";
    static final String PINK = "#f49ac1";

    static final String ANY = "#ffffff";


    static final String[] TRAIN_COLORS_ARRAY = {"Blue", "Red", "Yellow", "Green", "Purple", "Brown", "Orange", "Pink", "Any"};

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


    public static String getColor(String colorName) {

        colorName = colorName.toLowerCase();
        String color;

        switch (colorName){
            case "red":
            case "#ff0000":
                color = RED;
                break;
            case "blue":
            case "#0072bc":
                color = BLUE;
                break;
            case "brown":
            case "#603913":
                color = BROWN;
                break;
            case "orange":
            case "#f9461c":
                color = ORANGE;
                break;
            case "green":
            case "#00a651":
                color = GREEN;
                break;
            case "yellow":
            case "#fff200":
                color = YELLOW;
                break;
            case "purple":
            case "#522398":
                color = PURPLE;
                break;
            case "pink":
            case "#f49ac1":
                color = PINK;
                break;
            default:
                color = ANY;
                break;

        }

        return color;
    }

    public void setColor(String colorName) {

        colorName = colorName.toLowerCase();
        this.colorName = colorName;


        switch (colorName){
            case "red":
            case "#ff0000":
                color = RED;
                break;
            case "blue":
            case "#0072bc":
                color = BLUE;
                break;
            case "brown":
            case "#603913":
                color = BROWN;
                break;
            case "orange":
            case "#f9461c":
                color = ORANGE;
                break;
            case "green":
            case "#00a651":
                color = GREEN;
                break;
            case "yellow":
            case "#fff200":
                color = YELLOW;
                break;
            case "purple":
            case "#522398":
                color = PURPLE;
                break;
            case "pink":
            case "#f49ac1":
                color = PINK;
                break;
            default:
                color = ANY;
                break;

        }

    }
}

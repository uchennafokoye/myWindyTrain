package com.uchennafokoye.mywindytrain;

import java.sql.Time;
import java.util.Date;

/**
 * Created by faithfulokoye on 10/29/15.
 */
public class Train {

    private TrainColor color;
    private Geolocation geolocation;
    private Distance distance;
    private Date next_arrival;


    public Train(TrainColor color, Geolocation geolocation, Distance distance, Date next_arrival){
        this.color = color;
        this.geolocation = geolocation;
        this.distance = distance;
        this.next_arrival = next_arrival;
    }



    public Geolocation getGeolocation() {
        return this.geolocation;
    }

    public Distance getDistance() {
        return this.distance;
    }

    public Date getNextArrival(){
        return this.next_arrival;
    }


    public static String getTimeString(Date next_arrival) {
        return new Time(next_arrival.getTime()).toString();
    }

    public String getTimeString() {
        return new Time(next_arrival.getTime()).toString();
    }


    public static void main(String[] args){

        Geolocation g = new Geolocation(16.205753, 5.835114);
        Distance d = new Distance(3.5, "miles");
        Date n_a = new Date(1445959800);
        TrainColor t_c = new TrainColor("green");

        Train t = new Train(t_c, g, d, n_a);

        if (t.color.isValid()) {
            System.out.println(t.color.toString());
        }
        System.out.println("Geolocation: " + t.geolocation.toString());
        System.out.println("Distance: " + t.distance.toString());
        System.out.println("Next Arrival: " + getTimeString(t.next_arrival));



    }




}

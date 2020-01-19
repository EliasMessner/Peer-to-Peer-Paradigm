import java.util.Date;

/**
 * Klasse für verschiedene Wetterverhältnisse an verschiedenen Standorten.
 * @author Elias Messner, Marius Gerull
 */
public class WeatherInfo {

    private String location;
    private int temperature;
    private int humidity;
    private Date timestamp;

    /**
     * Konstruktor für eine Wetterauskunft.
     * @param location  Standort
     * @param temperature   Temperatur
     * @param humidity  Luftfeuchtigkeit
     * @param timestamp Zeitstempel
     */
    public WeatherInfo(String location, int temperature, int humidity, Date timestamp) {
        this.location = location;
        this.humidity = humidity;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

/**
 * Konstruktor für eine Wetterauskunft.
 * @param location Standort
 * @param temperature Temperatur
 * @param humidity Luftfeuchtigkeit
 * **/
    public WeatherInfo(String location, int temperature, int humidity) {
        this.location = location;
        this.humidity = humidity;
        this.temperature = temperature;
        this.timestamp = new Date(); //now
    }

    /**
     * Konstruktor für eine Wetterauskunft.
     * @param location Standort.
     */
    public WeatherInfo(String location) {
        this.location = location;
        this.humidity = (int)(Math.random()*100+1);     //between 0-100 %
        this.temperature = (int)(Math.random()*45-19);  //between -20 and 45 deg
        this.timestamp = new Date(); //now
    }

    public String getLocation() {
        return location;
    }

    /**
     * Simpler String-Builder für alle relevanten Informationen über das Wetter an einem bestimmten Standort.
     * @return String
     */
    @Override
    public String toString() {
        return new String("Wheather Info for "+this.location+" at "+this.timestamp.toString()+":\n" +
                "Temperature: "+temperature+" degrees Celsius\n" +
                "Humidity: "+humidity+"%");
    }
}

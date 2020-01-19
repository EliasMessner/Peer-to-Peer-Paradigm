import java.util.Date;

public class WeatherInfo {

    private String location;
    private int temperature;
    private int humidity;
    private Date timestamp;

    public WeatherInfo(String location, int temperature, int humidity, Date timestamp) {
        this.location = location;
        this.humidity = humidity;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

    public WeatherInfo(String location, int temperature, int humidity) {
        this.location = location;
        this.humidity = humidity;
        this.temperature = temperature;
        this.timestamp = new Date(); //now
    }

    public WeatherInfo(String location) {
        this.location = location;
        this.humidity = (int)(Math.random()*100+1);     //between 0-100 %
        this.temperature = (int)(Math.random()*45-19);  //between -20 and 45 deg
        this.timestamp = new Date(); //now
    }

    public String getLocation() {
        return location;
    }
}

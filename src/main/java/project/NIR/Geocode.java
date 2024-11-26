package project.NIR;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.IOException;

public class Geocode {
    public static GeoPosition getCoordinates(String address) throws IOException {
        address = address.replace(' ', '+');
        final Content getResult = Request.Get("https://geocode.maps.co/search?q="+ address +"&api_key=663f71fe9ff45300043066hsv4f01d5")
                .execute().returnContent();
        System.out.println(getResult);

        JSONArray jsonArray = new JSONArray(getResult.toString());
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        double lat = jsonObject.getDouble("lat");
        double lon = jsonObject.getDouble("lon");

        System.out.println("lat: " + lat);
        System.out.println("lon: " + lon);

        return new GeoPosition(lat, lon);
    }
}

package ca.ubc.cs.cpsc210.meetup.Parsers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.model.EatingPlace;
import ca.ubc.cs.cpsc210.meetup.model.Place;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;

/**
 * Created by Ha on 3/22/2015.
 */
public class GetPlacesParser {

    public GetPlacesParser(){

    }

    public ArrayList<Place> parse(String jSONOfPlaces) {
        ArrayList<Place> places = new ArrayList<Place>();
        JSONTokener token = new JSONTokener(jSONOfPlaces);
        try {
            JSONObject obj = new JSONObject(token);

            JSONArray venues = obj.getJSONObject("response").getJSONArray("groups").getJSONObject(0).getJSONArray("items");
            for (int x = 0; x < venues.length();x++) {
                JSONObject a = venues.getJSONObject(x).getJSONObject("venue");
                String name = a.getString("name");
                Double lat = a.getJSONObject("location").getDouble("lat");
                Double lon = a.getJSONObject("location").getDouble("lng");
                LatLon latlon = new LatLon(lat,lon);
                String address = a.getJSONObject("location").getString("address");
                String price = a.getJSONObject("price").getString("tier");
                Place eatingPlace = new Place(name, latlon);
                eatingPlace.setAddressAndPrice("Address: "+ address
                        + "\n" + "Price Rating: " + price);
                places.add(eatingPlace);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return places;
    }
}

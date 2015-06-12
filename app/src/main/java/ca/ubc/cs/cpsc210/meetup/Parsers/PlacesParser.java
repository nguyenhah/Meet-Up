package ca.ubc.cs.cpsc210.meetup.Parsers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

/**
 * Created by Ha on 3/18/2015.
 */
public class PlacesParser {
    public PlacesParser(){
    }

    public ArrayList<GeoPoint> parser(String s){
        ArrayList<GeoPoint> geoList = new ArrayList<>();
        JSONTokener token = new JSONTokener(s);
        token.skipTo('{');
        //token.skipPast("renderAdvancedNarrative(");
        try {
            JSONObject top = new JSONObject(token);
            JSONArray geoPoints = top.getJSONObject("route").getJSONObject("shape").getJSONArray("shapePoints");
            for (int x = 0; x < geoPoints.length(); x += 2) {
                double lat = geoPoints.getDouble(x);
                double lon = geoPoints.getDouble(x + 1);
                GeoPoint geo = new GeoPoint(lat, lon);
                geoList.add(geo);
            }
        }catch(JSONException e){
            e.printStackTrace();
        }

        return geoList;
    }
}

package ca.ubc.cs.cpsc210.meetup.model;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.cpsc210.meetup.util.LatLon;

/*
 * A place represents a location at which people can meet
 */
public class Place extends Location {

    // The name of the place
    private String name;

    // Tags describing place
    private Set<String> tags;

    private String addressAndPrice;
    /**
     * Constructor
     * @param placeName tThe name of the place
     */
    public Place(String placeName) {
        this(placeName, new LatLon());
    }

    /**
     * Constructor
     * @param placeName The name of the place
     * @param latLon The latitude and longitude of the place
     */
    public Place(String placeName, LatLon latLon) {
        super(latLon);
        name = placeName;
        displayText = placeName;
        tags = new HashSet<String>();
    }


    /**
     * Add a tag describing what can be done in the place
     * @param tag The tag to add, non-null
     */
    public void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * Determine if this place has the specified tag
     * @param tag The tag to look for, non-null
     * @return true if found, false otherwise
     */
    public boolean containsTag(String tag) {
        return tags.contains(tag);
    }

    public String getAddressAndPrice() {
        return addressAndPrice;
    }

    public void setAddressAndPrice(String addressAndPrice) {
        this.addressAndPrice = addressAndPrice;
    }

// ************** Getters ****************

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Place)) return false;
        if (!super.equals(o)) return false;

        Place place = (Place) o;

        if (!name.equals(place.name)) return false;
        if (!tags.equals(place.tags)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + tags.hashCode();
        return result;
    }
}

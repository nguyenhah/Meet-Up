package ca.ubc.cs.cpsc210.meetup.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.PathOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.Parsers.GetPlacesParser;
import ca.ubc.cs.cpsc210.meetup.Parsers.PlacesParser;
import ca.ubc.cs.cpsc210.meetup.Parsers.RandomStudentScheduleParser;
import ca.ubc.cs.cpsc210.meetup.R;
import ca.ubc.cs.cpsc210.meetup.model.BreakTime;
import ca.ubc.cs.cpsc210.meetup.model.Building;
import ca.ubc.cs.cpsc210.meetup.model.Course;
import ca.ubc.cs.cpsc210.meetup.model.CourseFactory;
import ca.ubc.cs.cpsc210.meetup.model.EatingPlace;
import ca.ubc.cs.cpsc210.meetup.model.Place;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.model.Schedule;
import ca.ubc.cs.cpsc210.meetup.model.Section;
import ca.ubc.cs.cpsc210.meetup.model.Student;
import ca.ubc.cs.cpsc210.meetup.model.StudentManager;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;
import ca.ubc.cs.cpsc210.meetup.util.SchedulePlot;

/**
 * Fragment holding the map in the UI.
 */
public class MapDisplayFragment extends Fragment {

    /**
     * Log tag for LogCat messages
     */
    private final static String LOG_TAG = "MapDisplayFragment";

    /**
     * Preference manager to access user preferences
     */
    private SharedPreferences sharedPreferences;

    /**
     * String to know whether we are dealing with MWF or TR schedule.
     * You will need to update this string based on the settings dialog at appropriate
     * points in time. See the project page for details on how to access
     * the value of a setting.
     */
    private String activeDay = "MWF";
    private String activeTime = "10";
    private String activeDistance = "250";
    private String activeLocation = "query=food";
    private GeoPoint testPlace = new GeoPoint(49.26566225, -123.2500577);
    private GeoPoint myGeoPoint = new GeoPoint(49.264865, -123.252782);
    /**
     * A central location in campus that might be handy.
     */
    private final static GeoPoint UBC_MARTHA_PIPER_FOUNTAIN = new GeoPoint(49.264865,
            -123.252782);

    /**
     * Meetup Service URL
     * CPSC 210 Students: Complete the string.
     */
    private final String getStudentURL = "http://kramer.nss.cs.ubc.ca:8081/getStudent";

    /**
     * FourSquare URLs. You must complete the client_id and client_secret with values
     * you sign up for.
     */
    private static String FOUR_SQUARE_URL = "https://api.foursquare.com/v2/venues/explore";
    private static String FOUR_SQUARE_CLIENT_ID = "BPKIFJQC1JBXO2NGVROY5E30MTTGLBBSRORZFMYTTWCI2WHB";
    private static String FOUR_SQUARE_CLIENT_SECRET = "YQEOI4125F5KFVCIYWKABWATWZXFD25UL0VEN0LLIQWNPA1N";


    /**
     * Overlays for displaying my schedules, buildings, etc.
     */
    private List<PathOverlay> scheduleOverlay;
    private ItemizedIconOverlay<OverlayItem> buildingOverlay;
    private OverlayItem selectedBuildingOnMap;

    /**
     * View that shows the map
     */
    private MapView mapView;

    /**
     * Access to domain model objects. Only store "me" in the studentManager for
     * the base project (i.e., unless you are doing bonus work).
     */
    private StudentManager studentManager;
    private Student randomStudent = null;
    private Student me = null;
    private static int ME_ID = 999999;
    private Set<Student> allStudents = new TreeSet<Student>();

    /**
     * Map controller for zooming in/out, centering
     */
    private IMapController mapController;

    // ******************** Android methods for starting, resuming, ...

    // You should not need to touch this method
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        scheduleOverlay = new ArrayList<PathOverlay>();

        // You need to setup the courses for the app to know about. Ideally
        // we would access a web service like the UBC student information system
        // but that is not currently possible
        initializeCourses();

        // Initialize the data for the "me" schedule. Note that this will be
        // hard-coded for now
        initializeMySchedule();
        getCurrentLocation();
        // You are going to need an overlay to draw buildings and locations on the map
        buildingOverlay = createBuildingOverlay();
    }

    // You should not need to touch this method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
    }

    // You should not need to touch this method
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mapView == null) {
            mapView = new MapView(getActivity(), null);

            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setClickable(true);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);

            mapController = mapView.getController();
            mapController.setZoom(mapView.getMaxZoomLevel() - 2);
            mapController.setCenter(UBC_MARTHA_PIPER_FOUNTAIN);
        }

        return mapView;
    }

    // You should not need to touch this method
    @Override
    public void onDestroyView() {
        Log.d(LOG_TAG, "onDestroyView");
        ((ViewGroup) mapView.getParent()).removeView(mapView);
        super.onDestroyView();
    }

    // You should not need to touch this method
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    // You should not need to touch this method
    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
    }

    // You should not need to touch this method
    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
    }

    /**
     * Save map's zoom level and centre. You should not need to
     * touch this method
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            outState.putInt("zoomLevel", mapView.getZoomLevel());
            IGeoPoint cntr = mapView.getMapCenter();
            outState.putInt("latE6", cntr.getLatitudeE6());
            outState.putInt("lonE6", cntr.getLongitudeE6());
            Log.i("MapSave", "Zoom: " + mapView.getZoomLevel());
        }
    }

    // ****************** App Functionality

    /**
     * Show my schedule on the map. Every time "me"'s schedule shows, the map
     * should be cleared of all existing schedules, buildings, meetup locations, etc.
     */
    public void showMySchedule() {
        // CPSC 210 Students: You must complete the implementation of this method.
        // The very last part of the method should call the asynchronous
        // task (which you will also write the code for) to plot the route
        // for "me"'s schedule for the day of the week set in the Settings

        // Asynchronous tasks are a bit onerous to deal with. In order to provide
        // all information needed in one object to plot "me"'s route, we
        // create a SchedulePlot object and pass it to the asynchrous task.
        // See the project page for more details.


        // Get a routing between these points. This line of code creates and calls
        // an asynchronous task to do the calls to MapQuest to determine a route
        // and plots the route.
        // Assumes mySchedulePlot is a create and initialized SchedulePlot object

        // UNCOMMENT NEXT LINE ONCE YOU HAVE INSTANTIATED mySchedulePlot
        //TODO
        activeDay = sharedPreferences.getString("dayOfWeek", "MWF");
        SortedSet<Section> sections = me.getSchedule().getSections(activeDay);
        SchedulePlot mySchedulePlot = new SchedulePlot(sections, "Ha", "#0000FF", R.drawable.ic_action_place);
        new GetRoutingForSchedule().execute(mySchedulePlot);
    }


    /**
     * Retrieve a random student's schedule from the Meetup web service and
     * plot a route for the schedule on the map. The plot should be for
     * the given day of the week as determined when "me"'s schedule
     * was plotted.
     */
    public void showRandomStudentsSchedule() {
        // To get a random student's schedule, we have to call the MeetUp web service.
        // Calling this web service requires a network access to we have to
        // do this in an asynchronous task. See below in this class for where
        // you need to implement methods for performing the network access
        // and plotting.
        activeDay = sharedPreferences.getString("dayOfWeek", "MWF");
        new GetRandomSchedule().execute();
    }

    /**
     * Clear all schedules on the map
     */
    public void clearSchedules() {
        randomStudent = null;
        allStudents.clear();
        allStudents.add(me);
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        scheduleOverlay.clear();
        buildingOverlay.removeAllItems();
        om.addAll(scheduleOverlay);
        om.add(buildingOverlay);
        mapView.invalidate();
    }

    public void clearOverlay() {
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        scheduleOverlay.clear();
        buildingOverlay.removeAllItems();
        om.addAll(scheduleOverlay);
        om.add(buildingOverlay);
        mapView.invalidate();
    }

    /**
     * Find all possible locations at which "me" and random student could meet
     * up for the set day of the week and the set time to meet and the set
     * distance either "me" or random is willing to travel to meet.
     * A meetup is only possible if both "me" and random are free at the
     * time specified in the settings and each of us must have at least an hour
     * (>= 60 minutes) free. You should display dialog boxes if there are
     * conditions under which no meetup can happen (e.g., me or random is
     * in class at the specified time)
     */
    public void findMeetupPlace() {
        //TODO
        // CPSC 210 students: you must complete this method
        PlaceFactory placeFactory = PlaceFactory.getInstance();
        activeDay = sharedPreferences.getString("dayOfWeek", "MWF");
        activeTime = sharedPreferences.getString("timeOfDay", "10");
        activeDistance = sharedPreferences.getString("placeDistance", "250");
        if (randomStudent == null) {
            AlertDialog aDialog = createSimpleDialog("No random student picked");
            aDialog.show();
            return;
        }
        ArrayList<BreakTime> myBreaks = me.getSchedule().getStartTimesOfBreaksMod(activeDay);
        ArrayList<BreakTime> randomBreaks = randomStudent.getSchedule().getStartTimesOfBreaksMod(activeDay);
        int meetTime = Integer.parseInt(activeTime) * 60;
        int meetDistance = Integer.parseInt(activeDistance);
        boolean letsMeet = canWeMeet(myBreaks, randomBreaks, meetTime);
        if (letsMeet == true) {
            if (calculateMinutesIntoDayMod(me.getSchedule().startTime(activeDay).getStartTime()) <= meetTime
                    && calculateMinutesIntoDayMod(randomStudent.getSchedule().startTime(activeDay).getStartTime()) <= meetTime) {
                LatLon myLastBuilding = me.getSchedule().whereAmI(activeDay, activeTime).getLatLon();
                LatLon theirLastBuilding = randomStudent.getSchedule().whereAmI(activeDay, activeTime).getLatLon();
                Set<Place> placesInMyRange = placeFactory.findPlacesWithinDistance(myLastBuilding, meetDistance);
                Set<Place> placesInTheirRange = placeFactory.findPlacesWithinDistance(theirLastBuilding, meetDistance);
                Set<Place> commonPlaces = getCommonPlaces(placesInMyRange, placesInTheirRange);
                plotPlaces(commonPlaces);
                AlertDialog aDialog = createSimpleDialog("We can meet in " + commonPlaces.size() + " places!");
                aDialog.show();
//                Log.i("MapDisplayFragment", testPlace.toString());
//                Log.i("MapDisplayFragment", myGeoPoint.toString());
            } else {
                AlertDialog aDialog = createSimpleDialog("One of you are not a school yet");
                aDialog.show();
            }
        } else {
            AlertDialog aDialog = createSimpleDialog("Cannot meet up at this time");
            aDialog.show();
        }
    }

    public void getMyLocation() {
        getCurrentLocation();
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        scheduleOverlay.clear();
        om.addAll(scheduleOverlay);
        om.add(buildingOverlay);
        mapView.invalidate();
        new MeetRoute().execute();
    }


    private void getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                GeoPoint whereIAM = new GeoPoint(lat, lon);
                myGeoPoint = whereIAM;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void plotPlaces(Set<Place> places) {
        clearOverlay();
        int icon = R.drawable.ic_action_toggle_star_outline;
        for (Place p : places) {
            String name = p.getName();
            LatLon latLon = p.getLatLon();
            String msg = p.getAddressAndPrice();
            plotAPlace(latLon, name, msg, icon);
        }
        OverlayManager om = mapView.getOverlayManager();
        om.add(buildingOverlay);

    }

    public void plotAPlace(LatLon building, String title, String msg, int drawableToUse) {
        OverlayItem buildingItem = new OverlayItem(title, msg,
                new GeoPoint(building.getLatitude(), building.getLongitude()));
        Drawable icon = this.getResources().getDrawable(drawableToUse);
        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);
        buildingItem.setMarker(icon);
        buildingOverlay.addItem(buildingItem);
    }

    public Set<Place> getCommonPlaces(Set<Place> mine, Set<Place> theirs) {
        Set<Place> common = new HashSet<>();
        Iterator<Place> it = mine.iterator();
        while (it.hasNext()) {
            Place place = it.next();
            if (theirs.contains(place)) {
                common.add(place);
            }
        }
        return common;
    }

    public boolean canWeMeet(ArrayList<BreakTime> myBreaks, ArrayList<BreakTime> randomBreaks, int meetTime) {
        boolean canWeMeet = false;
        if ((calculateMinutesIntoDayMod(me.getSchedule().startTime(activeDay).getStartTime()) - 60 >= meetTime) ||
                (calculateMinutesIntoDayMod(me.getSchedule().endTime(activeDay).getEndTime()) <= meetTime) ||
                isThereABreak(myBreaks, meetTime)) {
            if ((calculateMinutesIntoDayMod(randomStudent.getSchedule().startTime(activeDay).getStartTime()) - 60 <= meetTime) ||
                    (calculateMinutesIntoDayMod(randomStudent.getSchedule().endTime(activeDay).getEndTime()) >= meetTime) ||
                    isThereABreak(randomBreaks, meetTime)) {
                canWeMeet = true;
            }
        }

        return canWeMeet;
    }

    public boolean isThereABreak(ArrayList<BreakTime> breaks, int meetUpTime) {
        boolean thereIsABreak = false;
        for (BreakTime b : breaks) {
            if ((calculateMinutesIntoDayMod(b.getStartBreak()) <= meetUpTime) &&
                    (calculateMinutesIntoDayMod(b.getLengthOfBreak()) - 60) >= meetUpTime) {
                thereIsABreak = true;
            }
        }
        return thereIsABreak;
    }

    public int calculateMinutesIntoDayMod(String aTime) {
        int colonIndex = aTime.indexOf(":");
        int hours = Integer.parseInt(aTime.substring(0, colonIndex));
        int minutes = Integer.parseInt(aTime.substring(colonIndex + 1,
                aTime.length()));
        return (hours * 60) + minutes;
    }

    /**
     * Initialize the PlaceFactory with information from FourSquare
     */
    public void initializePlaces() {
        // CPSC 210 Students: You should not need to touch this method, but
        // you will have to implement GetPlaces below.
        new GetPlaces().execute();
    }


    /**
     * Plot all buildings referred to in the given information about plotting
     * a schedule.
     *
     * @param schedulePlot All information about the schedule and route to plot.
     */
    private void plotBuildings(SchedulePlot schedulePlot) {
        //TODO
        // CPSC 210 Students: Complete this method by plotting each building in the
        // schedulePlot with an appropriate message displayed
        int icon = schedulePlot.getIcon();
        String name = schedulePlot.getName();
        SortedSet<Section> section = new TreeSet<Section>(schedulePlot.getSections());
        for (Section s : section) {
            String msg = s.getCourse().getCode() + " " +
                    s.getCourse().getNumber() + " Section " + s.getName() + " " + s.getCourseTime().getStartTime()
                    + " to " + s.getCourseTime().getEndTime() + "\n" + "Building: "+ s.getBuilding().getName();
            plotABuilding(s.getBuilding(), name, msg, icon);
        }

        // CPSC 210 Students: You will need to ensure the buildingOverlay is in
        // the overlayManager. The following code achieves this. You should not likely
        // need to touch it
        OverlayManager om = mapView.getOverlayManager();
        om.add(buildingOverlay);

    }

    /**
     * Plot a building onto the map
     *
     * @param building      The building to put on the map
     * @param title         The title to put in the dialog box when the building is tapped on the map
     * @param msg           The message to display when the building is tapped
     * @param drawableToUse The icon to use. Can be R.drawable.ic_action_place (or any icon in the res/drawable directory)
     */
    private void plotABuilding(Building building, String title, String msg, int drawableToUse) {
        // CPSC 210 Students: You should not need to touch this method
        Random rand = new Random();
        double start = building.getLatLon().getLongitude();
        double end = building.getLatLon().getLongitude() + .001;
        double random = new Random().nextDouble();
        double result = start + (random * (end - start));
        OverlayItem buildingItem = new OverlayItem(title, msg,
                new GeoPoint(building.getLatLon().getLatitude(), result ));

        //Create new marker
        Drawable icon = this.getResources().getDrawable(drawableToUse);

        //Set the bounding for the drawable
        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);

        //Set the new marker to the overlay
        buildingItem.setMarker(icon);
        buildingOverlay.addItem(buildingItem);
    }


    /**
     * Initialize your schedule by coding it directly in. This is the schedule
     * that will appear on the map when you select "Show My Schedule".
     */
    private void initializeMySchedule() {
        // CPSC 210 Students; Implement this method
        //TODO
        studentManager = new StudentManager();
        studentManager.addStudent("Nguyen", "Ha", ME_ID);
        studentManager.addSectionToSchedule(ME_ID, "CPSC", 210, "202");
        studentManager.addSectionToSchedule(ME_ID, "ENGL", 222, "007");
        studentManager.addSectionToSchedule(ME_ID, "MATH", 221, "202");
        studentManager.addSectionToSchedule(ME_ID, "PHYS", 203, "201");
        studentManager.addSectionToSchedule(ME_ID, "FNH", 330, "002");
        me = studentManager.get(ME_ID);
        allStudents.add(me);

    }

    /**
     * Helper to create simple alert dialog to display message
     *
     * @param msg message to display in alert dialog
     * @return the alert dialog
     */
    private AlertDialog createSimpleDialog(String msg) {
        // CPSC 210 Students; You should not need to modify this method
        AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
        dialogBldr.setMessage(msg);
        dialogBldr.setNeutralButton(R.string.ok, null);

        return dialogBldr.create();
    }

    /**
     * Create the overlay used for buildings. CPSC 210 students, you should not need to
     * touch this method.
     *
     * @return An overlay
     */
    private ItemizedIconOverlay<OverlayItem> createBuildingOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

        ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gestureListener =
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

                    /**
                     * Display building description in dialog box when user taps stop.
                     *
                     * @param index
                     *            index of item tapped
                     * @param oi
                     *            the OverlayItem that was tapped
                     * @return true to indicate that tap event has been handled
                     */
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem oi) {

                        new AlertDialog.Builder(getActivity())
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        if (selectedBuildingOnMap != null) {
                                            mapView.invalidate();
                                        }
                                    }
                                }).setTitle(oi.getTitle()).setMessage(oi.getSnippet())
                                .show();

                        selectedBuildingOnMap = oi;
                        mapView.invalidate();
                        testPlace = oi.getPoint();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem oi) {
                        // do nothing
                        return false;
                    }
                };

        return new ItemizedIconOverlay<OverlayItem>(
                new ArrayList<OverlayItem>(), getResources().getDrawable(
                R.drawable.ic_action_place), gestureListener, rp);
    }


    /**
     * Create overlay with a specific color
     *
     * @param colour A string with a hex colour value
     */
    private PathOverlay createPathOverlay(String colour) {
        // CPSC 210 Students, you should not need to touch this method
        PathOverlay po = new PathOverlay(Color.parseColor(colour),
                getActivity());
        Paint pathPaint = new Paint();
        pathPaint.setColor(Color.parseColor(colour));
        pathPaint.setStrokeWidth(4.0f);
        pathPaint.setStyle(Paint.Style.STROKE);
        po.setPaint(pathPaint);
        return po;
    }

    // *********************** Asynchronous tasks

    /**
     * This asynchronous task is responsible for contacting the Meetup web service
     * for the schedule of a random student. The task must plot the retrieved
     * student's route for the schedule on the map in a different colour than the "me" schedule
     * or must display a dialog box that a schedule was not retrieved.
     */
    private class GetRandomSchedule extends AsyncTask<Void, Void, SchedulePlot> {

        // Some overview explanation of asynchronous tasks is on the project web page.

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected SchedulePlot doInBackground(Void... params) {
            //TODO
            // CPSC 210 Students: You must complete this method. It needs to
            // contact the Meetup web service to get a random student's schedule.
            // If it is successful in retrieving a student and their schedule,
            // it needs to remember the student in the randomStudent field
            // and it needs to create and return a schedulePlot object with
            // all relevant information for being ready to retrieve the route
            // and plot the route for the schedule. If no random student is
            // retrieved, return null.
            //
            // Note, leave all determination of routing and plotting until
            // the onPostExecute method below.
            SchedulePlot random = null;
            RandomStudentScheduleParser parser = new RandomStudentScheduleParser();
            try {
                TreeSet<Section> sections = parser.parse(makeRoutingCall(getStudentURL));
                studentManager.addStudent(parser.getLastName(), parser.getFirstName(), parser.getStudentID());
                for (Section s : sections) {
                    studentManager.addSectionToSchedule(parser.getStudentID(), s.getCourse().getCode(), s.getCourse().getNumber(), s.getName());
                }
                randomStudent = studentManager.get(parser.getStudentID());

                SortedSet<Section> ranSections = randomStudent.getSchedule().getSections(activeDay);
                if (ranSections.size() == 0 || allStudents.contains(randomStudent)) {
                    return null;
                }
                random = new SchedulePlot(ranSections, parser.getFirstName(), "#2ecc71", R.drawable.ic_action_place);
            } catch (MalformedURLException e) {

            } catch (IOException e) {

            } catch (JSONException e) {

            }


            random = addGeoPoints(random);

            return random;
        }

        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {
            // CPSC 210 students: When this method is called, it will be passed
            // whatever schedulePlot object you created (if any) in doBackground
            // above. Use it to plot the route.
            if (schedulePlot == null) {
                AlertDialog aDialog = createSimpleDialog("Random Student schedule on different day!");
                aDialog.show();
            } else {
                plotBuildings(schedulePlot);
                PathOverlay po = createPathOverlay(schedulePlot.getColourOfLine());
                List<GeoPoint> route = new ArrayList<>(schedulePlot.getRoute());
                for (GeoPoint g : route) {
                    po.addPoint(g);
                }
                scheduleOverlay.add(po);
                OverlayManager om = mapView.getOverlayManager();
                om.addAll(scheduleOverlay);
                mapView.invalidate();
            }
        }

        private SchedulePlot addGeoPoints(SchedulePlot plot) {
            SchedulePlot randomStudent = plot;
            SortedSet<Section> listOfSections = randomStudent.getSections();
            ArrayList<String> listOfLatLon = new ArrayList<>();
            List<GeoPoint> geoP = new ArrayList<GeoPoint>();
            for (Section s : listOfSections) {
                double lat = s.getBuilding().getLatLon().getLatitude();
                double lon = s.getBuilding().getLatLon().getLongitude();
                String latLon = lat + "," + lon;
                listOfLatLon.add(latLon);
            }
            ArrayList<String> listOfGeoPoints = new ArrayList<String>();
            for (int x = 0; x < listOfLatLon.size() - 1; x++) {
                String from = listOfLatLon.get(x);
                String to = listOfLatLon.get(x + 1);
                try {
                    listOfGeoPoints.add(makeRoutingCall("http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu82lutll%2Cb5%3Do5-948aqr&callback=renderAdvancedNarrative&outFormat=json&routeType=pedestrian&timeType=0&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=m&from=" + from + "&to=" + to + "&drivingStyle=2&highwayEfficiency=21.0"));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (int sA = 0; sA < listOfGeoPoints.size(); sA++) {
                PlacesParser parse = new PlacesParser();
                geoP.addAll(parse.parser(listOfGeoPoints.get(sA)));
            }
            randomStudent.setRoute(geoP);

            return randomStudent;
        }

    }

    /**
     * This asynchronous task is responsible for contacting the MapQuest web service
     * to retrieve a route between the buildings on the schedule and for plotting any
     * determined route on the map.
     */
    private class GetRoutingForSchedule extends AsyncTask<SchedulePlot, Void, SchedulePlot> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected SchedulePlot doInBackground(SchedulePlot... params) {
            //TODO
            // The params[0] element contains the schedulePlot object
            SchedulePlot scheduleToPlot = params[0];
            SortedSet<Section> listOfSections = scheduleToPlot.getSections();
            ArrayList<String> listOfLatLon = new ArrayList<>();
            List<GeoPoint> geoP = new ArrayList<GeoPoint>();
            for (Section s : listOfSections) {
                double lat = s.getBuilding().getLatLon().getLatitude();
                double lon = s.getBuilding().getLatLon().getLongitude();
                String latLon = lat + "," + lon;
                listOfLatLon.add(latLon);
            }
            ArrayList<String> listOfGeoPoints = new ArrayList<String>();
            for (int x = 0; x < listOfLatLon.size() - 1; x++) {
                String from = listOfLatLon.get(x);
                String to = listOfLatLon.get(x + 1);
                try {
                    listOfGeoPoints.add(makeRoutingCall("http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu82lutll%2Cb5%3Do5-948aqr&callback=renderAdvancedNarrative&outFormat=json&routeType=pedestrian&timeType=0&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=m&from=" + from + "&to=" + to + "&drivingStyle=2&highwayEfficiency=21.0"));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (int sA = 0; sA < listOfGeoPoints.size(); sA++) {
                PlacesParser parse = new PlacesParser();
                geoP.addAll(parse.parser(listOfGeoPoints.get(sA)));
            }
            scheduleToPlot.setRoute(geoP);

            /*
            CPSC 210 Students: Complete this method. This method should
            call the MapQuest webservice to retrieve a List<GeoPoint>
            that forms the routing between the buildings on the
            schedule. The List<GeoPoint> should be put into
            scheduleToPlot object.
            */

            return scheduleToPlot;
        }


        /**
         * An example helper method to call a web service
         */
        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {

            // CPSC 210 Students: This method should plot the route onto the map
            // with the given line colour specified in schedulePlot. If there is
            // no route to plot, a dialog box should be displayed.

            // To actually make something show on the map, you can use overlays.
            // For instance, the following code should show a line on a map
//            GeoPoint point1 = new GeoPoint(49.269258, -123.254784);
//            GeoPoint point2 = new GeoPoint(49.262786, -123.255044);
//            PathOverlay po = createPathOverlay("#FFFFFF");
//            po.addPoint(point1); // one end of line
//            po.addPoint(point2); // second end of line
//            scheduleOverlay.add(po);
//            OverlayManager om = mapView.getOverlayManager();
//            om.addAll(scheduleOverlay);
//            mapView.invalidate(); // cause map to redraw
            plotBuildings(schedulePlot);
            PathOverlay po = createPathOverlay(schedulePlot.getColourOfLine());
            List<GeoPoint> route = new ArrayList<>(schedulePlot.getRoute());
            for (GeoPoint g : route) {
                po.addPoint(g);
            }
            scheduleOverlay.add(po);
            OverlayManager om = mapView.getOverlayManager();
            om.addAll(scheduleOverlay);
            mapView.invalidate();
        }

    }

    /**
     * This asynchronous task is responsible for contacting the FourSquare web service
     * to retrieve all places around UBC that have to do with food. It should load
     * any determined places into PlaceFactory and then display a dialog box of how it did
     */
    private class GetPlaces extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... params) {

            // CPSC 210 Students: Complete this method to retrieve a string
            // of JSON from FourSquare. Return the string from this method
            PlaceFactory placeFactory = PlaceFactory.getInstance();
            placeFactory.getPlaces().clear();
            String listOfPlaces = "";
            activeLocation = sharedPreferences.getString("typePlace", "query=food");
            try {
                listOfPlaces = makeRoutingCall("https://api.foursquare.com/v2/venues/explore?ll=49.264865,-123.252782&client_id=BPKIFJQC1JBXO2NGVROY5E30MTTGLBBSRORZFMYTTWCI2WHB&client_secret=YQEOI4125F5KFVCIYWKABWATWZXFD25UL0VEN0LLIQWNPA1N&v=20150322&radius=2500&photos=1&" + activeLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }


            return listOfPlaces;
        }

        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        protected void onPostExecute(String jSONOfPlaces) {

            // CPSC 210 Students: Given JSON from FourQuest, parse it and load
            // PlaceFactory
            PlaceFactory placeFactory = PlaceFactory.getInstance();
            ArrayList<Place> listOfPlaces;
            GetPlacesParser places = new GetPlacesParser();
            listOfPlaces = places.parse(jSONOfPlaces);
            for (Place p : listOfPlaces) {
                placeFactory.add(p);
            }
            AlertDialog aDialog = createSimpleDialog(listOfPlaces.size() + " Places loaded");
            aDialog.show();
        }
    }

    private class MeetRoute extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... params) {
            String from = myGeoPoint.getLatitude() + "," + myGeoPoint.getLongitude();
            String to = testPlace.getLatitude() + "," + testPlace.getLongitude();
            String routeToMeetup = "";
            try {
                routeToMeetup = makeRoutingCall("http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu82lutll%2Cb5%3Do5-948aqr&callback=renderAdvancedNarrative&outFormat=json&routeType=pedestrian&timeType=0&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=m&from=" + from + "&to=" + to + "&drivingStyle=2&highwayEfficiency=21.0");
            }  catch (IOException e) {
                e.printStackTrace();
            }
            return routeToMeetup;
        }

        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        protected void onPostExecute(String jSONOfPlaces) {
            PlacesParser meet = new PlacesParser();
            List<GeoPoint> meetUpRoute = meet.parser(jSONOfPlaces);
            PathOverlay po = createPathOverlay("#0000FF");
            for (GeoPoint g : meetUpRoute) {
                po.addPoint(g);
            }
            scheduleOverlay.add(po);
            OverlayManager om = mapView.getOverlayManager();
            om.addAll(scheduleOverlay);
            mapView.invalidate();
        }
    }

    /**
     * Initialize the CourseFactory with some courses.
     */
    private void initializeCourses() {
        // CPSC 210 Students: You can change this data if you desire.
        CourseFactory courseFactory = CourseFactory.getInstance();

        Building dmpBuilding = new Building("DMP", new LatLon(49.261474, -123.248060));

        Course cpsc210 = courseFactory.getCourse("CPSC", 210);
        Section aSection = new Section("202", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("201", "MWF", "16:00", "16:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("BCS", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);

        Course engl222 = courseFactory.getCourse("ENGL", 222);
        aSection = new Section("007", "MWF", "14:00", "14:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        engl222.addSection(aSection);
        aSection.setCourse(engl222);

        Course scie220 = courseFactory.getCourse("SCIE", 220);
        aSection = new Section("200", "MWF", "15:00", "15:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie220.addSection(aSection);
        aSection.setCourse(scie220);

        Course math200 = courseFactory.getCourse("MATH", 200);
        aSection = new Section("201", "MWF", "09:00", "09:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        math200.addSection(aSection);
        aSection.setCourse(math200);

        Course fren102 = courseFactory.getCourse("FREN", 102);
        aSection = new Section("202", "MWF", "11:00", "11:50", new Building("Barber", new LatLon(49.267442, -123.252471)));
        fren102.addSection(aSection);
        aSection.setCourse(fren102);

        Course japn103 = courseFactory.getCourse("JAPN", 103);
        aSection = new Section("002", "MWF", "10:00", "11:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        japn103.addSection(aSection);
        aSection.setCourse(japn103);

        Course scie113 = courseFactory.getCourse("SCIE", 113);
        aSection = new Section("213", "MWF", "13:00", "13:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie113.addSection(aSection);
        aSection.setCourse(scie113);

        Course micb308 = courseFactory.getCourse("MICB", 308);
        aSection = new Section("201", "MWF", "12:00", "12:50", new Building("Woodward", new LatLon(49.264704, -123.247536)));
        micb308.addSection(aSection);
        aSection.setCourse(micb308);

        Course math221 = courseFactory.getCourse("MATH", 221);
        aSection = new Section("202", "TR", "11:00", "12:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        math221.addSection(aSection);
        aSection.setCourse(math221);

        Course phys203 = courseFactory.getCourse("PHYS", 203);
        aSection = new Section("201", "TR", "09:30", "10:50", new Building("Hennings", new LatLon(49.266400, -123.252047)));
        phys203.addSection(aSection);
        aSection.setCourse(phys203);

        Course crwr209 = courseFactory.getCourse("CRWR", 209);
        aSection = new Section("002", "TR", "12:30", "13:50", new Building("Geography", new LatLon(49.266039, -123.256129)));
        crwr209.addSection(aSection);
        aSection.setCourse(crwr209);

        Course fnh330 = courseFactory.getCourse("FNH", 330);
        aSection = new Section("002", "TR", "15:00", "16:20", new Building("MacMillian", new LatLon(49.261167, -123.251157)));
        fnh330.addSection(aSection);
        aSection.setCourse(fnh330);

        Course cpsc499 = courseFactory.getCourse("CPSC", 430);
        aSection = new Section("201", "TR", "16:20", "17:50", new Building("Liu", new LatLon(49.267632, -123.259334)));
        cpsc499.addSection(aSection);
        aSection.setCourse(cpsc499);

        Course chem250 = courseFactory.getCourse("CHEM", 250);
        aSection = new Section("203", "TR", "10:00", "11:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        chem250.addSection(aSection);
        aSection.setCourse(chem250);

        Course eosc222 = courseFactory.getCourse("EOSC", 222);
        aSection = new Section("200", "TR", "11:00", "12:20", new Building("ESB", new LatLon(49.262866, -123.25323)));
        eosc222.addSection(aSection);
        aSection.setCourse(eosc222);

        Course biol201 = courseFactory.getCourse("BIOL", 201);
        aSection = new Section("201", "TR", "14:00", "15:20", new Building("BioSci", new LatLon(49.263920, -123.251552)));
        biol201.addSection(aSection);
        aSection.setCourse(biol201);
    }
}
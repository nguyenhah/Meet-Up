package ca.ubc.cs.cpsc210.meetup.Parsers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.model.CourseFactory;
import ca.ubc.cs.cpsc210.meetup.model.Section;

/**
 * Created by Ha on 3/19/2015.
 */
public class RandomStudentScheduleParser {

    private String firstName;
    private String lastName;
    private int studentID;
    private TreeSet<Section> sec;




    public RandomStudentScheduleParser(){

    }

    public TreeSet<Section> parse(String url) throws JSONException{
        CourseFactory courseFactory = CourseFactory.getInstance();
        TreeSet<Section> sec = new TreeSet<>();
        JSONTokener token = new JSONTokener(url);
        JSONObject obj = new JSONObject(token);
        firstName = obj.getString("FirstName");
        lastName = obj.getString("LastName");
        studentID = obj.getInt("Id");
        JSONArray array = obj.getJSONArray("Sections");
        for(int x = 0; x < array.length();x++){
            String courseName = array.getJSONObject(x).getString("CourseName");
            int courseNum = array.getJSONObject(x).getInt("CourseNumber");
            String sectionName = array.getJSONObject(x).getString("SectionName");
            sec.add(courseFactory.getCourse(courseName,courseNum).getSection(sectionName));
        }
        return sec;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getStudentID() {
        return studentID;
    }

    public TreeSet<Section> getSec() {
        return sec;
    }

}

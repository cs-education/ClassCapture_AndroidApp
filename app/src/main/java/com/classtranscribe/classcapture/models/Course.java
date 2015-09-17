package com.classtranscribe.classcapture.models;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class Course extends RealmObject {

    @PrimaryKey
    public long id; // Will be created on backend

    public String department; // In a course like CS 225, it would be "CS"
    public long number; // In a course like CS 225, it would be 225
    public RealmList<Section> sections; // A course like CS 225 would have Lecture Sections, Lab, Discussion, etc.

    public Course() {
        // Does nothing
    }

    public Course(String department, long number, Section...sections) {
        this.department = department;
        this.number = number;
        this.sections = new RealmList<Section>(sections); // Initialize to empty
    }

    @Override
    public String toString() {
        return this.department + " " + this.number;
    }

}

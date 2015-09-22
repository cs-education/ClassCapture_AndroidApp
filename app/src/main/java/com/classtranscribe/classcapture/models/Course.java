package com.classtranscribe.classcapture.models;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class Course extends RealmObject {

    @PrimaryKey
    private long id; // Will be created on backend

    private String department; // In a course like CS 225, it would be "CS"
    private long number; // In a course like CS 225, it would be 225
    private RealmList<Section> sections = new RealmList<Section>(); // A course like CS 225 would have Lecture Sections, Lab, Discussion, etc.

    public Course() {
        // Does nothing
    }

    public Course(String department, long number, Section...sections) {
        this.department = department;
        this.number = number;
        this.sections = new RealmList<Section>(sections); // Initialize to empty
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public void setSections(RealmList<Section> sections) {
        this.sections = sections;
    }

    public long getId() {
        return id;
    }

    public String getDepartment() {
        return department;
    }

    public long getNumber() {
        return number;
    }

    public RealmList<Section> getSections() {
        return sections;
    }

}

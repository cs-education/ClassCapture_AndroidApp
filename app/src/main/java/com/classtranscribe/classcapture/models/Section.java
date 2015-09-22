package com.classtranscribe.classcapture.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class Section extends RealmObject {

    @PrimaryKey
    private long id; // Will be created on backend

    private Date startTime;

    private Date endTime;
    private String name; // CS 225's lecture section would have a name like AL1
    private Course course;

    public Section() {
        //does nothing...
    }

    public Section(Date startTime, Date endTime, String name, Course course) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.name = name;
        this.course = course;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public long getId() {
        return id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getName() {
        return name;
    }

    public Course getCourse() {
        return course;
    }

}

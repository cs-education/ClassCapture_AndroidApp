package com.classtranscribe.classcapture.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class Section extends RealmObject {

    @PrimaryKey
    public long id; // Will be created on backend

    public Date startTime;
    public Date endTime;
    public String name; // CS 225's lecture section would have a name like AL1
    public Course course;

    public Section() {
        //does nothing...
    }

    public Section(Date startTime, Date endTime, String name, Course course) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.name = name;
        this.course = course;
    }

    @Override
    public String toString() {
        return this.course.toString() + ": " + this.name;
    }
}

package com.classtranscribe.classcapture.models;

import android.content.Context;

import com.classtranscribe.classcapture.services.CourseServiceProvider;
import com.classtranscribe.classcapture.services.InstanceRetriever;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.annotations.PrimaryKey;
import retrofit.Call;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class Course extends RealmObject {

    @PrimaryKey
    private long id; // Will be created on backend

    private String department; // In a course like CS 225, it would be "CS"
    private long number; // In a course like CS 225, it would be 225
//    private RealmList<Section> sections; // A course like CS 225 would have Lecture Sections, Lab, Discussion, etc.

    public Course() {
        // Does nothing
    }

    public Course(String department, long number, Section...sections) {
        this.department = department;
        this.number = number;
//        this.sections = new RealmList<Section>(sections);
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

//    public void setSections(RealmList<Section> sections) {
//        this.sections = sections;
//    }

    public long getId() {
        return id;
    }

    public String getDepartment() {
        return department;
    }

    public long getNumber() {
        return number;
    }

//    public RealmList<Section> getSections() {
//        return sections;
//    }

    public static class CourseRetriever extends InstanceRetriever<Course> {

        public CourseRetriever(Context context) {
            super(context, Course.class);
        }

        @Override
        protected Course getLocalCopy(Context context, long id) {
            Realm realm = Realm.getDefaultInstance();
            try {
                RealmQuery<Course> query = realm.where(this.paramClass).equalTo("id", id);
                return query.findFirst();
            } finally {
                realm.close();
            }
        }

        @Override
        public Call<Course> getInstanceFromAPI(Context context, long id) {
            return CourseServiceProvider.getInstance(context).getCourse(id);
        }

        @Override
        public void saveLocalCopy(Course instance) {
            Realm realm = Realm.getDefaultInstance();
            try {
                realm.beginTransaction();
                realm.copyToRealmOrUpdate(instance);
                realm.commitTransaction();
            } finally {
                realm.close();
            }
        }

        @Override
        public long getInstanceID(Course instance) {
            return instance.getId();
        }
    }

}

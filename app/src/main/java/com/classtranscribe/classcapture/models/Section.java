package com.classtranscribe.classcapture.models;

import android.content.Context;
import android.util.Log;

import com.classtranscribe.classcapture.services.CourseServiceProvider;
import com.classtranscribe.classcapture.services.GSONUtils;
import com.classtranscribe.classcapture.services.InstanceRetriever;
import com.classtranscribe.classcapture.services.SectionServiceProvider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.Date;

import io.gsonfire.PostProcessor;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import retrofit.Call;
import retrofit.Response;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class Section extends RealmObject {

    @PrimaryKey
    private long id; // Will be created on backend
    private String name; // CS 225's lecture section would have a name like AL1
    private long course;

    public Section() {
        //does nothing...
    }

    public Section(Date startTime, Date endTime, String name, long course) {
        this.name = name;
        this.course = course;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCourse(long course) {
        this.course = course;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCourse() {
        return course;
    }

    public static class SectionRetriever extends InstanceRetriever<Section> {

        public SectionRetriever(Context context) {
            super(context, Section.class);
        }

        @Override
        protected Section getLocalCopy(Context context, long id) {
            Realm realm = Realm.getDefaultInstance();
            try {
                Section section = realm.where(Section.class)
                        .equalTo("id", id)
                        .findFirst();
                return section;
            } finally {
                realm.close();
            }
        }

        @Override
        public Call<Section> getInstanceFromAPI(Context context, long id) throws NoSuchMethodException {
            return SectionServiceProvider.getInstance(context).getSection(id);
        }

        @Override
        public void saveLocalCopy(Section instance) {
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
        public long getInstanceID(Section instance) {
            return instance.getId();
        }
    }

    public static class SectionPostProcessor implements PostProcessor<Section> {

        private final Context context;

        public SectionPostProcessor(Context context) {
            this.context = context;
        }

        @Override
        public void postDeserialize(Section result, JsonElement src, Gson gson) {
            // First check whether Realm contains the course that is linked to this section
            Realm realm = Realm.getDefaultInstance();
            try {
                long courseID = result.getCourse();
                Course course = realm.where(Course.class).equalTo("id", courseID).findFirst();
                if (course == null) {
                    // Fetch the course for the section and save it locally
                    Response<Course> response = CourseServiceProvider.getInstance(this.context).getCourse(courseID).execute();
                    if (response.isSuccess()) {
                        Course linkedCourse = response.body();
                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(linkedCourse);
                        realm.commitTransaction();
                    } else {
                        Log.e("SectionPostProcessor", "Received following status error while trying to retrieve course: " + response.code());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                realm.close();
            }


        }

        @Override
        public void postSerialize(JsonElement result, Section src, Gson gson) {

        }
    }

}

package com.classtranscribe.classcapture.services;

import android.content.Context;
import android.util.Log;

import com.classtranscribe.classcapture.models.Course;
import com.classtranscribe.classcapture.models.Section;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import retrofit.Response;

/**
 * Created by sourabhdesai on 1/17/16.
 */
public class RealmQueryUtils {

    private static final String TAG = RealmQueryUtils.class.toString();

    /**
     * Utility method for getting correlation between a section and the course it's under
     * Section & Courses are denoted by their IDs
     * @return
     */
    public static Map<Long, Long> getSectionCourseMap(Realm realm, Context context, List<Section> sections) throws IOException {
        Set<Long> courseIDs = new HashSet<>(sections.size());

        // Populate the courseIDs set with all the courses that need to be fetched (using set to resolve overlap)
        for (Section section : sections) {
            courseIDs.add(section.getCourse());
        }

        Map<Long, Course> idCourseMap = new HashMap<>(courseIDs.size());

        RealmResults<Course> results = getAllByID(realm, Course.class, courseIDs);
        for (Course result : results) {
            idCourseMap.put(result.getId(), result);
        }

        // Make a set of course ids that need weren't found from the realm query
        Set<Long> courseIDsToRequest = new HashSet<>(courseIDs);
        courseIDsToRequest.removeAll(idCourseMap.keySet()); // remove the courses that were found by realm
        List<Course> newCourses = new ArrayList<>(courseIDsToRequest.size());

        // Download courses from API
        CourseService courseService = CourseServiceProvider.getInstance(context);
        for (Long courseID : courseIDsToRequest) {
            Response<Course> response = courseService.getCourse(courseID).execute();
            if (response.isSuccess()) {
                Course course = response.body();
                newCourses.add(course);
                idCourseMap.put(courseID, course); // add to the id-course map
            } else {
                Log.e("getSectionCourseMap", "Got response with status: " + response.code());
            }
        }
        // Put all the newly downloaded courses into the realm DB
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(newCourses);
        realm.commitTransaction();

        Map<Long, Long> sectionCourseMap = new HashMap<>(courseIDs.size());
        for (Section section : sections) {
            Course linkedCourse = idCourseMap.get(section.getCourse());

            sectionCourseMap.put(section.getId(), linkedCourse.getId());
        }

        return sectionCourseMap;
    }

    public static void downloadLinkedCourses(Context context, List<Long> sections) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<Section> sectionObjs = getAllByID(realm, Section.class, sections);
            getSectionCourseMap(realm, context, sectionObjs);
        } finally {
            realm.close();
        }
    }

    public static <T extends RealmObject> RealmResults<T> getAllByID(Realm realm, Class<T> clazz, Collection<Long> ids) {
        RealmQuery<T> query = realm.where(clazz).beginGroup();

        boolean firstIterDone = false;
        for (Long id : ids) {
            query = (firstIterDone ? query.or() : query).equalTo("id", id);
            firstIterDone = true;
        }

        return query.endGroup().findAll();
    }

    public static <T> List<Long> getObjectIDs(List<T> objects, IdGetter<T> idGetter) {
        List<Long> ids = new ArrayList<>(objects.size());
        for (T object : objects) {
            long id = idGetter.getInstanceID(object);
            ids.add(id);
        }

        return ids;
    }

    public interface IdGetter<T> {
        public long getInstanceID(T instance);
    }

}

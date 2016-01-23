package com.classtranscribe.classcapture.models;

import android.content.Context;

import com.classtranscribe.classcapture.services.InstanceRetriever;
import com.classtranscribe.classcapture.services.UserServiceProvider;

import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import retrofit.Call;

/**
 * Created by sourabhdesai on 1/13/16.
 */
public class User extends RealmObject {

    @PrimaryKey
    private long id;
    private String email;
    private String firstName;
    private String lastName;
    private String password; // will rarely ever be populated
    private RealmList<Section> sections;

    // required empty constructor
    public User() {}

    public User(String email, String password) {
        this.email    = email;
        this.password = password;
    }

    /**
     * Vanilla Getters/Setters
     */

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RealmList<Section> getSections() {
        return sections;
    }

    public void setSections(RealmList<Section> sections) {
        this.sections = sections;
    }

    /**
     * Convenience method that returns a preopulated map that maps between
     * "email" & "password" values and their corresponding values
     * @return
     */
    public static HashMap<String, String> getLoginCreds(User user) {
        if (user.email == null || user.password == null) {
            throw new IllegalStateException("Email & Password fields must both be populated");
        }

        HashMap<String, String> credsMap = new HashMap<String, String>(2);
        credsMap.put("email", user.email);
        credsMap.put("password", user.password);

        return credsMap;
    }

    public static class UserRetriever extends InstanceRetriever<User> {
        protected UserRetriever(Context context) {
            super(context, User.class);
        }

        @Override
        protected User getLocalCopy(Context context, long id) {
            Realm realm = Realm.getDefaultInstance();
            try {
                return realm.where(User.class).equalTo("id", id).findFirst();
            } finally {
                realm.close();
            }
        }

        @Override
        public Call<User> getInstanceFromAPI(Context context, long id) throws NoSuchMethodException {
            return UserServiceProvider.getInstance(context).getUser(id);
        }

        @Override
        public void saveLocalCopy(User instance) {
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
        public long getInstanceID(User instance) {
            return instance.getId();
        }
    }


}

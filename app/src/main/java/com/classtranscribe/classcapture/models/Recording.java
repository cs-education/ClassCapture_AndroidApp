package com.classtranscribe.classcapture.models;

import hirondelle.date4j.DateTime;

/**
 * Created by sourabhdesai on 6/19/15.
 */

/**
 * Example JSON Recording representation:
     {
         startTime: "2015-05-28T01:31:37.073Z",
         endTime: "2016-05-28T01:31:37.073Z",
         filename: "1432776697073_1464399097073_1432776697179_752.mp4",
         id: 4,
         createdAt: "2015-05-28T01:31:37.182Z",
         updatedAt: "2015-05-28T01:31:37.182Z"
     }
 */
public class Recording {

    public DateTime startTime;
    public DateTime endTime;
    public String filename;
    public long id;
    public DateTime createdAt;
    public DateTime updatedAt;

}

/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.whistlepunk.metadata;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A label which represents a note made by the application.
 * This can be used for recording start/stop events or any background
 * events controlled by the app.
 */
public class ApplicationLabel extends Label {

    // Types of ApplicationLabel. Add more types here as needed.
    @IntDef({TYPE_RECORDING_START, TYPE_RECORDING_STOP, TYPE_CROP_START, TYPE_CROP_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    public static final int TYPE_RECORDING_START = 1;
    public static final int TYPE_RECORDING_STOP = 2;
    public static final int TYPE_CROP_START = 3;
    public static final int TYPE_CROP_END = 4;

    public static final String TAG = "application";
    public static final String VALUE_PREFIX = "application_type_";

    private static final int NUM_FIELDS = 1;
    private static final int INDEX_VALUE_TYPE = 0;
    private static final String KEY_VALUE_TYPE = "value_type";

    public ApplicationLabel(String id, String startLabelId, long timestamp,
            GoosciLabelValue.LabelValue value) {
        super(id, startLabelId, timestamp, value);
    }

    // TODO: use a clock to build labels.
    public ApplicationLabel(@Type int type, String labelId, String startLabelId,
            long timestampMillis) {
        this(labelId, startLabelId, timestampMillis, createStorageValue(type));
    }

    ApplicationLabel(String value, String labelId, String startLabelId, long timestampMillis) {
        this(valueToType(value), labelId, startLabelId, timestampMillis);
    }

    private ApplicationLabel() {
        super();
    }

    private static GoosciLabelValue.LabelValue createStorageValue(@Type int type) {
        GoosciLabelValue.LabelValue value = new GoosciLabelValue.LabelValue();
        value.data = new GoosciLabelValue.LabelValue.DataEntry[NUM_FIELDS];
        value.data[INDEX_VALUE_TYPE] = new GoosciLabelValue.LabelValue.DataEntry();
        value.data[INDEX_VALUE_TYPE].key = KEY_VALUE_TYPE;
        value.data[INDEX_VALUE_TYPE].value = String.valueOf(type);
        return value;
    }

    public @Type int getType() {
        return Integer.parseInt(getValue().data[INDEX_VALUE_TYPE].value);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public boolean canEditTimestamp() {
        // Autogenerated label has uneditable timestamp.
        return false;
    }

    // This function is only used when parsing a deprecated version of ApplicationLabel,
    // for users who created labels on a database version earlier than 15.
    static public @Type int valueToType(String value) {
        String suffix = value.substring(VALUE_PREFIX.length(), value.length());
        @Type int result = Integer.parseInt(suffix);
        return result;
    }

    static boolean isTag(String tag) {
        return TAG.equalsIgnoreCase(tag);
    }

    public static final Parcelable.Creator<ApplicationLabel> CREATOR =
            new Parcelable.Creator<ApplicationLabel>() {
                public ApplicationLabel createFromParcel(Parcel in) {
                    ApplicationLabel label = new ApplicationLabel();
                    label.populateFromParcel(in);
                    return label;
                }

                @Override
                public ApplicationLabel[] newArray(int size) {
                    return new ApplicationLabel[size];
                }
    };
}

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

package com.google.android.apps.forscience.whistlepunk.devicemanager;

import android.test.AndroidTestCase;

import com.google.android.apps.forscience.javalib.Success;
import com.google.android.apps.forscience.whistlepunk.DataController;
import com.google.android.apps.forscience.whistlepunk.TestConsumers;
import com.google.android.apps.forscience.whistlepunk.metadata.BleSensorSpec;
import com.google.android.apps.forscience.whistlepunk.metadata.ExperimentSensors;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;
import com.google.android.apps.forscience.whistlepunk.sensordb.DataControllerTest;
import com.google.android.apps.forscience.whistlepunk.sensordb.InMemorySensorDatabase;
import com.google.android.apps.forscience.whistlepunk.sensordb.MemoryMetadataManager;
import com.google.android.apps.forscience.whistlepunk.sensordb.StoringConsumer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DeviceOptionsViewControllerTest extends AndroidTestCase {
    public void testCommit() {
        DataController dc = new InMemorySensorDatabase().makeSimpleController(
                new MemoryMetadataManager(), DataControllerTest.bleProviderMap(getContext()));

        BleSensorSpec oldSpec = new BleSensorSpec("address", "name");
        StoringConsumer<String> cOldSensorId = new StoringConsumer<>();
        dc.addOrGetExternalSensor(oldSpec, cOldSensorId);
        String oldSensorId = cOldSensorId.getValue();
        assertEquals(ExternalSensorSpec.getSensorId(oldSpec, 0), oldSensorId);
        String experimentId = "experimentId";
        dc.addSensorToExperiment(experimentId, ExternalSensorSpec.getSensorId(oldSpec, 0),
                TestConsumers.<Success>expectingSuccess());

        final BleSensorSpec newSpec = new BleSensorSpec("address", "name");
        newSpec.setSensorType(SensorTypeProvider.TYPE_ROTATION);

        DeviceOptionsViewController c = new TestController(dc, newSpec, experimentId);
        c.setSensor(ExternalSensorSpec.getSensorId(oldSpec, 0), oldSpec, null);

        RecordingDeviceOptionsListener listener = new RecordingDeviceOptionsListener();

        c.commit(listener);
        assertEquals(ExternalSensorSpec.getSensorId(oldSpec, 0), listener.mostRecentOldSensorId);
        assertEquals(ExternalSensorSpec.getSensorId(oldSpec, 1), listener.mostRecentNewSensorId);

        dc.getExternalSensorsByExperiment(experimentId,
                TestConsumers.<ExperimentSensors>expecting(new ExperimentSensors(Lists.newArrayList(
                        ConnectableSensor.connected(newSpec,
                                ExternalSensorSpec.getSensorId(oldSpec, 1))),
                        Sets.<String>newHashSet(oldSensorId))));

        ExternalSensorSpec options = c.getOptions();
        ((BleSensorSpec) options).setSensorType(SensorTypeProvider.TYPE_CUSTOM);
        c.commit(listener);
        assertEquals(ExternalSensorSpec.getSensorId(oldSpec, 1), listener.mostRecentOldSensorId);
        assertEquals(ExternalSensorSpec.getSensorId(oldSpec, 2), listener.mostRecentNewSensorId);

        // And does nothing if new value is the same
        RecordingDeviceOptionsListener newListener = new RecordingDeviceOptionsListener();
        c.commit(newListener);
        assertNull(newListener.mostRecentNewSensorId);
    }

    private static class RecordingDeviceOptionsListener implements DeviceOptionsDialog
            .DeviceOptionsListener {
        public String mostRecentOldSensorId;
        public String mostRecentNewSensorId;

        @Override
        public void onExperimentSensorReplaced(String oldSensorId, String newSensorId) {
            mostRecentOldSensorId = oldSensorId;
            mostRecentNewSensorId = newSensorId;
        }

        @Override
        public void onRemoveSensorFromExperiment(String experimentId, String address) {

        }
    }

    private class TestController extends DeviceOptionsViewController {
        private final BleSensorSpec mNewSpec;

        public TestController(DataController dc, BleSensorSpec newSpec, String experimentId) {
            super(DeviceOptionsViewControllerTest.this.getContext(), dc, experimentId);
            mNewSpec = newSpec;
        }

        @Override
        public BleSensorSpec getOptions() {
            return mNewSpec;
        }
    }
}
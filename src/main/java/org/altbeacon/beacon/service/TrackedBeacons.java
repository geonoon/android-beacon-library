package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Keeps track of beacons that have ever been seen and
 * merges them together in the case of Gatt beacons
 * Created by dyoung on 5/5/15.
 */
public class TrackedBeacons {
    // We use a HashMap keyed by hash here so we can update the value
    private HashMap<Integer,Beacon> mBeacons = new HashMap<Integer,Beacon>();
    // This is a lookup table to find tracked beacons by a combo of service UUID and mac address
    private HashMap<String,HashMap<Integer,Beacon>> mBeaconsByServiceUuidAndMac = new HashMap<String,HashMap<Integer,Beacon>>();

    /**
     * Tracks a beacon.  For Gatt-based beacons, returns a merged copy of fields from multiple
     * frames.  Returns null when passed a Gatt-based beacon that has is only extra beacon data.
     *
     * @param beacon
     * @return
     */
    public synchronized Beacon track(Beacon beacon) {
        Beacon trackedBeacon = null;
        if (beacon.getServiceUuid() == -1) {
            trackedBeacon = mBeacons.put(beacon.hashCode(), beacon);
        }
        else {
            trackedBeacon = trackGattBeacon(beacon);
        }
        return trackedBeacon;
    }

    // The following code is for dealing with merging data fields in GATT-based beacons

    private Beacon trackGattBeacon(Beacon beacon) {
        // If this is a service UUID based beacon, we may need to merge fields, as
        // service UUID based beacons can have multiple frames
        Beacon trackedBeacon = null;
        HashMap<Integer,Beacon> matchingTrackedBeacons = mBeaconsByServiceUuidAndMac.get(serviceUuidAndMac(beacon));
        for (Beacon matchingTrackedBeacon: matchingTrackedBeacons.values()) {
            if (beacon.isExtraBeaconData()) {
                matchingTrackedBeacon.setRssi(beacon.getRssi());
                matchingTrackedBeacon.setExtraDataFields(beacon.getDataFields());
            }
            else {
                beacon.setExtraDataFields(matchingTrackedBeacon.getExtraDataFields());
                // replace the tracked beacon instance with this one so it has updated values
                updateTrackingHashes(beacon, matchingTrackedBeacons);
                trackedBeacon = beacon;
            }
        }
        if (trackedBeacon == null && !beacon.isExtraBeaconData()) {
            trackedBeacon = beacon;
        }
        return trackedBeacon;
    }

    private void updateTrackingHashes(Beacon trackedBeacon, HashMap<Integer,Beacon> matchingTrackedBeacons) {
        mBeacons.put(trackedBeacon.hashCode(), trackedBeacon);
        matchingTrackedBeacons.put(trackedBeacon.hashCode(), trackedBeacon);
    }

    private String serviceUuidAndMac(Beacon beacon) {
        return beacon.getBluetoothAddress()+beacon.getServiceUuid();
    }
}

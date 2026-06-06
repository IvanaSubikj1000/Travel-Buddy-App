package com.travelbuddy.sync;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.travelbuddy.data.local.AppDatabase;
import com.travelbuddy.data.local.ChecklistDao;
import com.travelbuddy.data.local.ChecklistItem;
import com.travelbuddy.data.local.Place;
import com.travelbuddy.data.local.PlaceDao;
import com.travelbuddy.data.local.Trip;
import com.travelbuddy.data.local.TripDao;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages bidirectional sync between Room (local source of truth) and Firestore.
 *
 * Write-through: every local Room write also pushes to Firestore (fire-and-forget).
 * Sync-down: snapshot listeners upsert remote changes into Room using last-write-wins
 * on the updatedAt field.
 *
 * Firestore structure:
 *   users/{uid}/trips/{tripId}
 *   users/{uid}/places/{placeId}
 *   users/{uid}/checklistItems/{itemId}
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String COL_TRIPS = "trips";
    private static final String COL_PLACES = "places";
    private static final String COL_CHECKLIST = "checklistItems";

    private static volatile SyncManager instance;

    private final FirebaseFirestore db;
    private final TripDao tripDao;
    private final PlaceDao placeDao;
    private final ChecklistDao checklistDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ListenerRegistration tripsListener;
    private ListenerRegistration placesListener;
    private ListenerRegistration checklistListener;
    private String currentUid;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private SyncManager(Application application) {
        db = FirebaseFirestore.getInstance();
        AppDatabase database = AppDatabase.getInstance(application);
        tripDao = database.tripDao();
        placeDao = database.placeDao();
        checklistDao = database.checklistDao();
    }

    public static SyncManager getInstance(Application application) {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager(application);
                }
            }
        }
        return instance;
    }

    // ── Listener lifecycle ────────────────────────────────────────────────────

    public synchronized void startListening(String uid) {
        if (uid.equals(currentUid)) return; // already attached for this user
        stopListening();                     // detach any stale listeners first
        currentUid = uid;

        tripsListener = db.collection("users").document(uid)
                .collection(COL_TRIPS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Trips listener error", error);
                        return;
                    }
                    if (snapshots == null) return;
                    executor.execute(() -> {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            handleTripChange(change);
                        }
                    });
                });

        placesListener = db.collection("users").document(uid)
                .collection(COL_PLACES)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Places listener error", error);
                        return;
                    }
                    if (snapshots == null) return;
                    executor.execute(() -> {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            handlePlaceChange(change);
                        }
                    });
                });

        checklistListener = db.collection("users").document(uid)
                .collection(COL_CHECKLIST)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Checklist listener error", error);
                        return;
                    }
                    if (snapshots == null) return;
                    executor.execute(() -> {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            handleChecklistChange(change);
                        }
                    });
                });
    }

    public synchronized void stopListening() {
        if (tripsListener != null)    { tripsListener.remove();    tripsListener = null; }
        if (placesListener != null)   { placesListener.remove();   placesListener = null; }
        if (checklistListener != null){ checklistListener.remove(); checklistListener = null; }
        currentUid = null;
    }

    // ── Sync-down handlers (run on background executor) ──────────────────────

    private void handleTripChange(DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            Trip local = tripDao.getTripByIdSync(doc.getId());
            if (local != null) tripDao.delete(local);
        } else {
            Trip remote = docToTrip(doc);
            Trip local = tripDao.getTripByIdSync(doc.getId());
            if (local == null || remote.getUpdatedAt() > local.getUpdatedAt()) {
                tripDao.insert(remote);
            }
        }
    }

    private void handlePlaceChange(DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            Place local = placeDao.getPlaceByIdSync(doc.getId());
            if (local != null) placeDao.delete(local);
        } else {
            Place remote = docToPlace(doc);
            // Guard: skip orphaned places whose parent trip was deleted locally
            if (remote.getTripId() != null
                    && tripDao.getTripByIdSync(remote.getTripId()) == null) {
                return;
            }
            Place local = placeDao.getPlaceByIdSync(doc.getId());
            if (local == null || remote.getUpdatedAt() > local.getUpdatedAt()) {
                placeDao.insert(remote);
            }
        }
    }

    private void handleChecklistChange(DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            ChecklistItem local = checklistDao.getChecklistItemByIdSync(doc.getId());
            if (local != null) checklistDao.delete(local);
        } else {
            ChecklistItem remote = docToChecklistItem(doc);
            // Guard: skip orphaned items whose parent trip was deleted locally
            if (remote.getTripId() != null
                    && tripDao.getTripByIdSync(remote.getTripId()) == null) {
                return;
            }
            ChecklistItem local = checklistDao.getChecklistItemByIdSync(doc.getId());
            if (local == null || remote.getUpdatedAt() > local.getUpdatedAt()) {
                checklistDao.insert(remote);
            }
        }
    }

    // ── Write-through: trips ──────────────────────────────────────────────────

    public void syncTrip(Trip trip) {
        String uid = trip.getUserId();
        if (uid == null || uid.isEmpty()) return;
        tripRef(uid, trip.getId())
                .set(tripToMap(trip))
                .addOnFailureListener(e -> Log.w(TAG, "syncTrip failed: " + trip.getId(), e));
    }

    /**
     * Deletes the trip document and cascades to its places and checklist items in Firestore.
     * Room already cascade-deleted them locally via the FK constraint.
     */
    public void deleteTrip(String tripId, String uid) {
        if (uid == null || uid.isEmpty()) return;
        tripRef(uid, tripId).delete()
                .addOnFailureListener(e -> Log.w(TAG, "deleteTrip failed: " + tripId, e));
        deletePlacesByTripId(tripId, uid);
        deleteChecklistItemsByTripId(tripId, uid);
    }

    // ── Write-through: places ─────────────────────────────────────────────────

    public void syncPlace(Place place) {
        String uid = place.getUserId();
        if (uid == null || uid.isEmpty()) return;
        placeRef(uid, place.getId())
                .set(placeToMap(place))
                .addOnFailureListener(e -> Log.w(TAG, "syncPlace failed: " + place.getId(), e));
    }

    public void deletePlace(String placeId, String uid) {
        if (uid == null || uid.isEmpty()) return;
        placeRef(uid, placeId).delete()
                .addOnFailureListener(e -> Log.w(TAG, "deletePlace failed: " + placeId, e));
    }

    // ── Write-through: checklist items ────────────────────────────────────────

    public void syncChecklistItem(ChecklistItem item) {
        String uid = item.getUserId();
        if (uid == null || uid.isEmpty()) return;
        checklistRef(uid, item.getId())
                .set(checklistItemToMap(item))
                .addOnFailureListener(e -> Log.w(TAG, "syncChecklistItem failed: " + item.getId(), e));
    }

    public void deleteChecklistItem(String itemId, String uid) {
        if (uid == null || uid.isEmpty()) return;
        checklistRef(uid, itemId).delete()
                .addOnFailureListener(e -> Log.w(TAG, "deleteChecklistItem failed: " + itemId, e));
    }

    // ── Firestore cascade helpers ─────────────────────────────────────────────

    private void deletePlacesByTripId(String tripId, String uid) {
        db.collection("users").document(uid).collection(COL_PLACES)
                .whereEqualTo("tripId", tripId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots) batch.delete(doc.getReference());
                    batch.commit().addOnFailureListener(
                            e -> Log.w(TAG, "deletePlacesByTripId batch failed", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "deletePlacesByTripId query failed", e));
    }

    private void deleteChecklistItemsByTripId(String tripId, String uid) {
        db.collection("users").document(uid).collection(COL_CHECKLIST)
                .whereEqualTo("tripId", tripId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots) batch.delete(doc.getReference());
                    batch.commit().addOnFailureListener(
                            e -> Log.w(TAG, "deleteChecklistItemsByTripId batch failed", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "deleteChecklistItemsByTripId query failed", e));
    }

    // ── Document references ───────────────────────────────────────────────────

    private DocumentReference tripRef(String uid, String id) {
        return db.collection("users").document(uid).collection(COL_TRIPS).document(id);
    }

    private DocumentReference placeRef(String uid, String id) {
        return db.collection("users").document(uid).collection(COL_PLACES).document(id);
    }

    private DocumentReference checklistRef(String uid, String id) {
        return db.collection("users").document(uid).collection(COL_CHECKLIST).document(id);
    }

    // ── Entity → Firestore map ────────────────────────────────────────────────

    private Map<String, Object> tripToMap(Trip trip) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", trip.getUserId());
        m.put("title", trip.getTitle());
        m.put("destination", trip.getDestination());
        m.put("notes", trip.getNotes());
        m.put("startDateMillis", trip.getStartDateMillis());
        m.put("endDateMillis", trip.getEndDateMillis());
        m.put("updatedAt", trip.getUpdatedAt());
        return m;
    }

    private Map<String, Object> placeToMap(Place place) {
        Map<String, Object> m = new HashMap<>();
        m.put("tripId", place.getTripId());
        m.put("userId", place.getUserId());
        m.put("name", place.getName());
        m.put("category", place.getCategory());
        m.put("address", place.getAddress());
        m.put("notes", place.getNotes());
        m.put("latitude", place.getLatitude());
        m.put("longitude", place.getLongitude());
        m.put("visited", place.isVisited());
        m.put("updatedAt", place.getUpdatedAt());
        return m;
    }

    private Map<String, Object> checklistItemToMap(ChecklistItem item) {
        Map<String, Object> m = new HashMap<>();
        m.put("tripId", item.getTripId());
        m.put("userId", item.getUserId());
        m.put("label", item.getLabel());
        m.put("checked", item.isChecked());
        m.put("updatedAt", item.getUpdatedAt());
        return m;
    }

    // ── Firestore document → entity ───────────────────────────────────────────

    private Trip docToTrip(DocumentSnapshot doc) {
        Trip trip = new Trip();
        trip.setId(doc.getId());
        trip.setUserId(str(doc, "userId"));
        trip.setTitle(str(doc, "title"));
        trip.setDestination(str(doc, "destination"));
        trip.setNotes(str(doc, "notes"));
        trip.setStartDateMillis(lng(doc, "startDateMillis"));
        trip.setEndDateMillis(lng(doc, "endDateMillis"));
        trip.setUpdatedAt(lng(doc, "updatedAt"));
        return trip;
    }

    private Place docToPlace(DocumentSnapshot doc) {
        Place place = new Place();
        place.setId(doc.getId());
        place.setTripId(str(doc, "tripId"));
        place.setUserId(str(doc, "userId"));
        place.setName(str(doc, "name"));
        place.setCategory(str(doc, "category"));
        place.setAddress(str(doc, "address"));
        place.setNotes(str(doc, "notes"));
        place.setLatitude(dbl(doc, "latitude"));
        place.setLongitude(dbl(doc, "longitude"));
        place.setVisited(bool(doc, "visited"));
        place.setUpdatedAt(lng(doc, "updatedAt"));
        return place;
    }

    private ChecklistItem docToChecklistItem(DocumentSnapshot doc) {
        ChecklistItem item = new ChecklistItem();
        item.setId(doc.getId());
        item.setTripId(str(doc, "tripId"));
        item.setUserId(str(doc, "userId"));
        item.setLabel(str(doc, "label"));
        item.setChecked(bool(doc, "checked"));
        item.setUpdatedAt(lng(doc, "updatedAt"));
        return item;
    }

    // ── Field extraction helpers ──────────────────────────────────────────────

    private String str(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        return v instanceof String ? (String) v : null;
    }

    private long lng(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        if (v instanceof Long)   return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        return 0L;
    }

    private boolean bool(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        return v instanceof Boolean && (Boolean) v;
    }

    private Double dbl(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        if (v instanceof Double) return (Double) v;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }
}

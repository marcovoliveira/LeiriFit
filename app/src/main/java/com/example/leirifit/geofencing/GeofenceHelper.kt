package com.example.leirifit.geofencing

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.GeofenceStatusCodes

class GeofenceHelper(base: Context?) : ContextWrapper(base) {

    private var TAG = "GeofenceHelper"
    private var pendingIntent: PendingIntent? = null

    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()
    }

    fun getGeofence(id: String, latLng: LatLng, radius: Float, transitionTypes: Int): Geofence {
        return Geofence
            .Builder()
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setRequestId(id)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(5000)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }

    fun getPendingIntent(): PendingIntent? {
        if (pendingIntent != null) {
            return pendingIntent
        }

        var intent = Intent(this, GeofenceBroadcastReceiver::class.java)

        pendingIntent =
            PendingIntent.getBroadcast(this, 2607, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent
    }

    fun getErrorString(e: Exception): String{
        if(e is ApiException) {
            var apiException: ApiException = e;

            if(apiException.statusCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                return "GEOFENCE_NOT_AVAILABLE"
            } else if(apiException.statusCode == GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES) {
                return "GEOFENCE_TOO_MANY_GEOFENCES"
            } else if(apiException.statusCode == GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS) {
                return "GEOFENCE_TOO_MANY_PENDING_INTENTS"
            }
        }

        return e.localizedMessage
    }
}
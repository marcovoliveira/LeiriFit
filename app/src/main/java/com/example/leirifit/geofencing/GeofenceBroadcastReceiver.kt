package com.example.leirifit.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.

        Toast.makeText(
            context,
            "Geofence triggered",
            Toast.LENGTH_LONG
        ).show()

        var geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            Toast.makeText(
                context,
                "Geofence error: on receive",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // var geofenceList = geofencingEvent.triggeringGeofences;

        // var location: Location = geofencingEvent.triggeringLocation;

        var transitionType = geofencingEvent.geofenceTransition

        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Toast.makeText(
                context,
                "Geofence_transition_enter",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent("X") //FILTER is a string to identify this intent

            intent.putExtra("Activate", true)
            context.sendBroadcast(intent)

        }
    }
}

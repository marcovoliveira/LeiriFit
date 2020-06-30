package com.example.leirifit.datamodel

import com.google.android.gms.maps.model.LatLng

data class CheckpointModel(private val cpName: String, private val cpCoords: LatLng, private val cpNextCp: String) {

    var name: String? = null
    var coords: LatLng? = null
    var nextCp: String? = null

    init {
        name = cpName
        coords = cpCoords
        nextCp = cpNextCp
    }
}
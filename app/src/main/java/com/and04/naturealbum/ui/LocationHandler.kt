package com.and04.naturealbum.ui

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

class LocationHandler(
    private val context: Context,
    private val takePicture: () -> Unit,
) {

    private val client by lazy { LocationServices.getSettingsClient(context) }
    private val builder by lazy {
        LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                Long.MAX_VALUE,
            ).build()
        )
    }
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            context
        )
    }

    fun checkLocationSettings(showGPSActivationDialog: (IntentSenderRequest) -> Unit) {
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                takePicture()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    resolveLocationSettings(exception, showGPSActivationDialog)
                }
            }
    }

    private fun resolveLocationSettings(
        resolvable: ResolvableApiException,
        showGPSActivationDialog: (IntentSenderRequest) -> Unit,
    ) {
        val intentSenderRequest = IntentSenderRequest.Builder(resolvable.resolution).build()
        try {
            showGPSActivationDialog(intentSenderRequest)
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    fun getLocation(onSuccess: (Location) -> Unit) {
        if (!checkPermission()) return
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            // TODO: location이 null로 오면?
            Log.d("FFFF", "${location.latitude}, ${location.longitude}")
            onSuccess(location)
        }
    }

    private fun checkPermission(): Boolean {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).all {
            ActivityCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
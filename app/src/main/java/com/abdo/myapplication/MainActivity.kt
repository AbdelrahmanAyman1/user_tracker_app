package com.abdo.myapplication

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task


class MainActivity : BaseActivity(), OnMapReadyCallback {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var googleMap: GoogleMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.abdo.myapplication.R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(com.abdo.myapplication.R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (isGPSPermissionAllowed()) {
            getUserLocation()
        } else {
            requestPermission()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        drawUserMarkerOnMap()
    }

    var userLocation: Location? = null
    var userMarker: Marker? = null
    private fun drawUserMarkerOnMap() {
        if (userLocation == null) return
        if (googleMap == null) return

        val latLang = LatLng(userLocation?.latitude ?: 0.0, userLocation?.longitude ?: 0.0)

        if (userMarker == null) {
            val markerOptions = MarkerOptions()
            markerOptions.position(latLang)
            markerOptions.title("current Location")
            googleMap?.addMarker(markerOptions)
        } else {
            userMarker?.position = latLang
        }

        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLang, 16.0f
            )
        )
    }

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in you r
                // app.
            } else {
                showDialog("we can't get the nearest drivers to you," + "to use this feature allow Location Permission")
            }
        }

    fun requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showDialog(
                message = "Please Enable Location " + " ,to get you the nearest drivers",
                posActionName = "yes",
                posAction = DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                negActionName = "no",
                negAction = { dialogInterface, i ->
                    dialogInterface.dismiss()
                }
            )

        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun isGPSPermissionAllowed(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result ?: return
            for (location in result.locations) {
                Log.e("location updated", "" + location.latitude + "" + location.longitude)
                userLocation = location
                drawUserMarkerOnMap()
            }
        }
    }


    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(5000)
        .setMaxUpdateDelayMillis(10)
        .build()


//        interval = 10000,
//        fastestInterval = 5000,
//        priority = LocationRequest.QUALITY_HIGH_ACCURACY
//    )

    val REQUEST_CHECK_SETTINGS = 102
    fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
//        fusedLocationClient.getCurrentLocation().addOnSuccessListener { location: Location? ->
//            if (location==null){
//                Log.e("location","null")
//                return@addOnSuccessListener
//            }
        Log.e("lat", "" + locationRequest)

//        }
//        Toast.makeText(this,"we can access user Location",Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            getUserLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}




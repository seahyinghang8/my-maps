package edu.stanford.seahyinghang8.mymaps

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import edu.stanford.seahyinghang8.mymaps.models.Place
import edu.stanford.seahyinghang8.mymaps.models.UserMap

private const val TAG = "DisplayMapActivity"
private const val LOCATION_REQUEST_CODE = 1323


class DisplayMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var userMap: UserMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var markers: MutableList<Marker> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_map)
        userMap = intent.getSerializableExtra(EXTRA_USER_MAP) as UserMap
        supportActionBar?.title = userMap.title
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val showInstructions = intent.getSerializableExtra(EXTRA_SHOW_INS) as Boolean
        if (showInstructions) {
            mapFragment.view?.let {
                Snackbar.make(
                    it,
                    "Long press to add a marker.\nClick on marker info window to edit.",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("OK", {})
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_save_map, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // if save button is clicked
        if (item.itemId == R.id.miSave) {
            Log.i(TAG, "Saving data")
            if (markers.isEmpty()) {
                Toast.makeText(
                    this,
                    "There must be at least one marker on the map",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
            val position = intent.getSerializableExtra(EXTRA_POSITION) as Int
            val places: List<Place> = markers.map { marker ->
                Place(
                    marker.title,
                    marker.position.latitude,
                    marker.position.longitude
                )
            }
            val data = Intent()
            data.putExtra(EXTRA_MAP_RESULT, Pair(position, places))
            setResult(Activity.RESULT_OK, data)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                zoomToUserLocation()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // User can add new places by long clicking on the map
        mMap.setOnMapLongClickListener { latLng ->
            val marker =
                mMap.addMarker(MarkerOptions().position(latLng).title(""))
            markers.add(marker)
            showEditPlaceDialog(marker, true)
        }

        // User can edit existing places by long clicking the info window
        mMap.setOnInfoWindowClickListener { clickedMarker ->
            showEditPlaceDialog(clickedMarker, false)
        }


        if (userMap.places.isEmpty()) {
            zoomToUserLocation()
        } else {
            val boundsBuilder = LatLngBounds.Builder()
            for (place in userMap.places) {
                val latLng = LatLng(place.latitude, place.longitude)
                boundsBuilder.include(latLng)
                markers.add(
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(place.title)
                    )
                )
            }
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    1000,
                    1000,
                    0
                )
            )
        }
    }

    private fun showEditPlaceDialog(marker: Marker, isNew: Boolean) {
        val placeFormView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_place, null)
        val negativeText = if (isNew) "Cancel" else "Delete"
        val positiveText = if (isNew) "Create" else "Save"
        val dialog =
            AlertDialog.Builder(this)
                .setView(placeFormView)
                .setNegativeButton(negativeText, null)
                .setPositiveButton(positiveText, null)
                .show()
        val editTitle = placeFormView.findViewById<EditText>(R.id.etPlaceTitle)
        editTitle.setText(marker.title)

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = editTitle.text.toString()
            if (title.trim().isEmpty()) {
                Toast.makeText(this, "Place must have non-empty name", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            marker.title = title
            marker.hideInfoWindow()
            marker.showInfoWindow()
            dialog.dismiss()
        }

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
            markers.remove(marker)
            marker.remove()
            dialog.dismiss()
        }
    }

    private fun zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(userLocation, 10f)
                    )
                }
            }
        }
    }
}
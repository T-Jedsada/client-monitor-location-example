package com.jedsada.clientmonitorlocation.example

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, ChildEventListener, ValueEventListener {

    private var mapFragment: SupportMapFragment? = null
    private val dbRefDashboard: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference.child("dashboard")
    }

    private var hashMapMarker = mutableMapOf<String?, Marker?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

    }

    override fun onStart() {
        super.onStart()
        Dexter.withActivity(this)
                .withPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(permissionsListener)
                .withErrorListener(errorListener)
                .check()
    }

    override fun onStop() {
        super.onStop()
        dbRefDashboard.removeEventListener(this as ChildEventListener)
    }

    private val permissionsListener: MultiplePermissionsListener = object : MultiplePermissionsListener {
        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
            when (hasDeniedPermission(report)) {
                false -> {
                    if (isLocationEnable()) mapFragment?.getMapAsync(this@MainActivity)
                    else createLocationNotification()
                }
                else -> Snackbar.make(container, report.toString(), Snackbar.LENGTH_SHORT).show()
            }
        }

        override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
            token?.continuePermissionRequest()
        }
    }

    private val errorListener = { _: DexterError ->
        // nothings
    }

    private fun hasDeniedPermission(report: MultiplePermissionsReport?): Boolean =
            report?.deniedPermissionResponses != null && !report.deniedPermissionResponses.isEmpty()

    private var googleMap: GoogleMap? = null

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap
        configGoogleMap()
    }

    private fun configGoogleMap() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        googleMap?.run {
            isTrafficEnabled = true
            isMyLocationEnabled = true
            uiSettings.isMyLocationButtonEnabled = false
        }
        dbRefDashboard.run {
            addListenerForSingleValueEvent(this@MainActivity)
            addChildEventListener(this@MainActivity)
        }
    }

    private fun isLocationEnable(): Boolean =
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)

    private fun createLocationNotification() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(this, 1001, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val mBuilder = NotificationCompat.Builder(this, "1")
        mBuilder.setSmallIcon(R.drawable.ic_settings_white)
                .setSound(alarmSound)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(longArrayOf(0, 400))
                .setColor(ContextCompat.getColor(this, R.color.notification_color))
                .setLights(ContextCompat.getColor(this, R.color.colorPrimaryDark), 1000, 1000)
                .setContentTitle(getString(R.string.enable_location_service))
                .setContentText(getString(R.string.open_location_settings))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        val mNotifyMgr = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        mNotifyMgr?.notify(1, mBuilder.build())
    }

    override fun onDataChange(dataSnapshot: DataSnapshot?) {
        loading.visibility = View.GONE
    }

    override fun onCancelled(error: DatabaseError?) {
        Snackbar.make(container, error?.message.toString(), Snackbar.LENGTH_SHORT).show()
    }

    override fun onChildMoved(dataSnapshot: DataSnapshot?, str: String?) {

    }

    override fun onChildChanged(dataSnapshot: DataSnapshot?, str: String?) {
        dataSnapshot?.run {
            val model = dataSnapshot.getValue(LocationModel::class.java)
            val marker = hashMapMarker[model?.deviceId]
            marker?.remove()
            hashMapMarker[model?.deviceId]?.remove()
            insertPinToGoogleMap(model)
        }
    }

    override fun onChildAdded(dataSnapshot: DataSnapshot?, str: String?) {
        dataSnapshot?.run {
            val model = dataSnapshot.getValue(LocationModel::class.java)
            insertPinToGoogleMap(model)
        }
    }

    override fun onChildRemoved(dataSnapshot: DataSnapshot?) {

    }

    private fun insertPinToGoogleMap(model: LocationModel?) {
        model?.deviceLocation?.run {
            val markerOptions = MarkerOptions()
            markerOptions.position(LatLng(model.deviceLocation.latitude!!, model.deviceLocation.longitude!!))
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker())
            hashMapMarker.put(model.deviceId, googleMap?.addMarker(markerOptions))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(model.deviceLocation.latitude, model.deviceLocation.longitude), 16.0f))
        }
    }
}
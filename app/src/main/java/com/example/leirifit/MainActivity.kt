package com.example.leirifit

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.leirifit.databinding.ActivityMainBinding

/**
 * Main activity binding, nav and fragments setup
 */
class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {


    private val requiredPermissions: Array<String> by lazy {
        try {
            this.packageManager.getPackageInfo(
                this.packageName,
                PackageManager.GET_PERMISSIONS
            ).requestedPermissions ?: arrayOf()
        } catch (e: PackageManager.NameNotFoundException) {
            arrayOf<String>()
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.none { !isPermissionGranted(it) }

    private fun requestRuntimePermissions() {
        val allNeededPermissions = requiredPermissions.filter { !isPermissionGranted(it) }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                allNeededPermissions.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return when (ContextCompat.checkSelfPermission(this, permission)) {
            PackageManager.PERMISSION_GRANTED -> {
                true
            }
            else -> {
                false
            }
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        drawerLayout = binding.drawerLayout
        val navController = this.findNavController(R.id.myNavHostFragment)

        NavigationUI.setupWithNavController(binding.navView, navController)
        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)

        if (!allPermissionsGranted()) {
            requestRuntimePermissions()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    companion object {

        private const val PERMISSION_REQUESTS = 1

    }
}
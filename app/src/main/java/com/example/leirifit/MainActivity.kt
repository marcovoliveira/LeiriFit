package com.example.leirifit

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.leirifit.database.Run
import com.example.leirifit.database.RunDatabase
import com.example.leirifit.databinding.ActivityMainBinding
import com.example.leirifit.viewmodel.RunViewModel
import com.example.leirifit.viewmodel.RunViewModelFactory

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

        val application = requireNotNull(this).application
        val dataSource = RunDatabase.getInstance(application).runDatabaseDao
        val viewModelFactory = RunViewModelFactory(dataSource, application)
        val runViewModel =
            ViewModelProviders.of(
                this, viewModelFactory
            ).get(RunViewModel::class.java)


        createRuns(runViewModel)
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    companion object {

        private const val PERMISSION_REQUESTS = 1

    }


    private fun createRuns(runViewModel: RunViewModel) {
        var run1 = Run()

        run1.duration = 8868000
        run1.distance = 13.11F
        run1.name = "Joana"
        run1.age = "36"
        run1.sexo = "Feminino"
        runViewModel.insertRun(run1)

    }
}
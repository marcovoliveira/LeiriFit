package com.example.leirifit.viewmodel

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import android.util.Log
import androidx.lifecycle.*
import com.example.leirifit.database.Run
import com.example.leirifit.database.RunDAO
import com.example.leirifit.utils.formatRuns
import kotlinx.coroutines.*

class RunViewModel(
    val database: RunDAO,
    application: Application
) : AndroidViewModel(application) {


    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var run = MutableLiveData<Run?>()

    private val runs = database.getAllRuns()

    init {
        initializeRun()
    }

    private fun initializeRun() {
        uiScope.launch {
            run.value = getRunFromDatabase()
        }
    }

    private suspend fun getRunFromDatabase(): Run? {
        return withContext(Dispatchers.IO) {
            var run = database.getRun()
            if (run?.endTimeMilli != run?.startTimeMilli) {
                run = null
            }
            run
        }
    }

    fun insertRun(run: Run) {
        uiScope.launch {
            insert(run)
        }
    }

    fun onStartTracking() {
        uiScope.launch {
            val newRun = Run()
            insert(newRun)
            run.value = getRunFromDatabase()

        }
    }

    fun getAllRuns(): LiveData<List<Run>> {
        return runs;
    }

    fun onStopTracking(distance: Float) {
        uiScope.launch {
            val updateRun = run.value ?: return@launch
            updateRun.endTimeMilli = System.currentTimeMillis()
            updateRun.distance = distance
            update(updateRun)
        }
    }

    private suspend fun insert(run: Run) {
        withContext(Dispatchers.IO) {
            database.insert(run)
        }
    }

    private suspend fun update(run: Run) {
        withContext(Dispatchers.IO) {
            database.update(run)
        }
    }


    /**
     * Converted nights to Spanned for displaying.
     */
    val runString = Transformations.map(runs) { runs ->
        formatRuns(runs, application.resources)
    }




    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}
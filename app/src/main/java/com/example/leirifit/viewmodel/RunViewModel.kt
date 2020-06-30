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



    fun insertRun(run: Run) {
        uiScope.launch {
            insert(run)
        }
    }

    fun getAllRuns(): LiveData<List<Run>> {
        return runs;
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
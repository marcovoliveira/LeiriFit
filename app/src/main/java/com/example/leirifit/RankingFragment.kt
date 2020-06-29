package com.example.leirifit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.example.leirifit.database.RunDatabase
import com.example.leirifit.databinding.FragmentRankingBinding
import com.example.leirifit.viewmodel.RunViewModel
import com.example.leirifit.viewmodel.RunViewModelFactory

class RankingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentRankingBinding>(
            inflater,
            R.layout.fragment_ranking, container, false
        )

        // viewModel
        val application = requireNotNull(this.activity).application
        val dataSource = RunDatabase.getInstance(application).runDatabaseDao
        val viewModelFactory = RunViewModelFactory(dataSource, application)
        val runViewModel =
            ViewModelProviders.of(
                this, viewModelFactory).get(RunViewModel::class.java)

        binding.setLifecycleOwner(this)
        binding.runViewModel = runViewModel

        return binding.root
    }
}
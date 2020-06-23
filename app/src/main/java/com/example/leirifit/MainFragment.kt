package com.example.leirifit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.leirifit.databinding.FragmentMainPageBinding

class MainFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentMainPageBinding>(inflater,
            R.layout.fragment_main_page,container,false)
        val args = MainFragmentArgs.fromBundle(arguments!!)
        Toast.makeText(context, "Name: ${args.participantName}, Idade: ${args.age}, Sexo: ${args.sex}", Toast.LENGTH_LONG).show()
        return binding.root
    }

}
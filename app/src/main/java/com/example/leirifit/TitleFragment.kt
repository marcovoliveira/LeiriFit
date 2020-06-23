package com.example.leirifit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.example.leirifit.databinding.FragmentTitleBinding


/**
 * A simple [Fragment] subclass.
 * Use the [TitleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TitleFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentTitleBinding>(inflater,
            R.layout.fragment_title,container,false)


        binding.start.setOnClickListener { view: View ->

            var ageString = binding.personAge.text.toString()
            val age = ageString.toInt()
            val name = binding.personName.text.toString()
            var sex = 0
            if (  binding.radioFemale.isChecked ) {
                sex = 1
            }
            // TODO validações
            view.findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToMainPageFragment(name, age, sex))
        }


        // TODO click listener para o butão do fundo
        return binding.root
    }



    companion object {

    }
}
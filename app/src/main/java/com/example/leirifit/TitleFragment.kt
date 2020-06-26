package com.example.leirifit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.example.leirifit.databinding.FragmentTitleBinding


/**
 * First page fragment
 */
class TitleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentTitleBinding>(
            inflater,
            R.layout.fragment_title, container, false
        )


        binding.start.setOnClickListener {
            val ageString = binding.personAge.text.toString()
            val name = binding.personName.text.toString()
            var sex = 0
            if (binding.radioFemale.isChecked) {
                sex = 1
            }
            if (name.isEmpty()) {
                Toast.makeText(activity, "Introduza o seu nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ageString.isEmpty()) {
                Toast.makeText(activity, "Inroduza a sua didade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageString.toInt()

            view?.findNavController()?.navigate(
                TitleFragmentDirections.actionTitleFragmentToMainPageFragment(
                    name,
                    age,
                    sex
                )
            )
        }

        binding.startNoData.setOnClickListener {
            view?.findNavController()?.navigate(
                TitleFragmentDirections.actionTitleFragmentToMainPageFragment(
                    "user",
                    0,
                    -1
                )
            )
        }

        return binding.root
    }


    companion object {

    }
}
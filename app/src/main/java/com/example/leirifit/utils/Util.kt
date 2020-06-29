package com.example.leirifit.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import com.example.leirifit.R
import com.example.leirifit.database.Run
import java.text.SimpleDateFormat



@SuppressLint("SimpleDateFormat")
fun convertLongToDateString(systemTime: Long): String {
    return SimpleDateFormat("EEEE dd-MMM-yyyy' Horas: 'HH:mm")
        .format(systemTime).toString()
}

fun formatRuns(runs: List<Run>, resources: Resources): Spanned {
    val sb = StringBuilder()
    sb.apply {
        runs.forEach {
            append("<br>")
            append(resources.getString(R.string.name_list))
            append("\t${it.name}<br>")
            append(resources.getString(R.string.age_list))
            append("\t${it.age}<br>")
            append(resources.getString(R.string.sex_list))
            append("\t${it.sexo}<br>")
            append(resources.getString(R.string.start_time))
            append("\t${convertLongToDateString(it.startTimeMilli)}<br>")
            if (it.endTimeMilli != it.startTimeMilli) {
                append(resources.getString(R.string.end_time))
                append("\t${convertLongToDateString(it.endTimeMilli)}<br>")
                append(resources.getString(R.string.distance))
                append("\t${it.distance} Km. <br>")
                append(resources.getString(R.string.hours_run))
                // Hours
                append("\t ${it.endTimeMilli.minus(it.startTimeMilli) / 1000 / 60 / 60}:")
                // Minutes
                append("${(it.endTimeMilli.minus(it.startTimeMilli) / 1000) / 60 % 60}:")
                // Seconds
                append("${it.endTimeMilli.minus(it.startTimeMilli) / 1000 % 60}<br><br>")
            }
        }
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
    } else {
        HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
package com.example.aquasegv2.ui.about

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.text.bold
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AboutViewModel : ViewModel() {
    private val _text = MutableLiveData<Spanned>().apply {
        value = buildSpannedText()
    }

    val text: LiveData<Spanned> = _text

    private fun buildSpannedText(): Spanned {
        return SpannableStringBuilder().apply {
            append("This app is a powerful offline tool built for fishers, conservationists, and anyone passionate about protecting marine life. It helps you identify egg-bearing lobsters instantly — even without an internet connection.\n\n")

            bold { append("Why Egg-Bearing Lobsters Matter\n") }
            append("Egg-bearing (or “berried”) lobsters carry thousands of eggs under their tails. These females are vital for maintaining lobster populations and marine biodiversity. Harvesting them is illegal in many regions to protect future generations of lobsters.\n\n")

            bold { append("How the App Works\n") }
            append("The app uses a custom-trained YOLOv11 (You Only Look Once) model to detect and classify lobsters in real time. It can distinguish between regular lobsters and those carrying eggs based on visual features — fast, reliably, and offline.\n\n")

            bold { append("Fully Offline\n") }
            append("Whether you're out at sea or in a remote harbor, the app runs completely offline by processing images locally on your device. No internet? No problem.\n\n")

            bold { append("Built for Conservation & Compliance\n") }
            append("Use this tool to help you follow local regulations, reduce accidental harvesting, and contribute to sustainable fishing practices.\n\n")

            append("Thank you for doing your part to protect our oceans!\n\n")
            }
        }
}
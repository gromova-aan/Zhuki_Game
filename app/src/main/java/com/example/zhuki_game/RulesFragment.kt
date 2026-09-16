package com.example.zhuki_game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment

class RulesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_rules, container, false)
        val webRules = view.findViewById<WebView>(R.id.webRules)
        webRules.loadUrl("file:///android_asset/rules.html")
        return view
    }
}

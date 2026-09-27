package com.example.zhuki_game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordsFragment : Fragment() {

    private lateinit var repository: UserRepository
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = UserRepository(requireContext())
        listView = view.findViewById(R.id.lvRecords)
        emptyView = view.findViewById(R.id.tvRecordsEmpty)
        listView.emptyView = emptyView
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) { repository.getRecords() }
            if (!isAdded) return@launch
            listView.adapter = RecordAdapter(requireContext(), records)
        }
    }
}

package com.joshtalks.joshskills.ui.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentExecuteDatabaseQueryBinding

class ExecuteDatabaseQueryFragment : Fragment() {
    private lateinit var binding: FragmentExecuteDatabaseQueryBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[DatabaseViewModel::class.java]
    }
    private val args: ExecuteDatabaseQueryFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_execute_database_query, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etQuery.setText(
            when (args.dbAction) {
                DatabaseOperation.INSERT -> "INSERT INTO "
                DatabaseOperation.UPDATE -> "UPDATE "
                DatabaseOperation.DELETE -> "DELETE FROM "
                DatabaseOperation.QUERY -> "SELECT * FROM "
            }
        )
        binding.etQuery.requestFocus()
        binding.etQuery.doOnTextChanged { text, _, _, _ ->
            if (text != null && text.isNotEmpty()) {
                try {
                    binding.ltQuery.error = null
                    viewModel.validateQuery(text.toString())
                } catch (e: Exception) {
                    binding.ltQuery.error = e.message
                }
            }
        }
        binding.btnExecute.setOnClickListener {
            try {
                viewModel.validateQuery(binding.etQuery.text.toString())
                binding.ltQuery.isErrorEnabled = false
                displayData(viewModel.executeQuery(binding.etQuery.text.toString()))
            } catch (e: Exception) {
                binding.ltQuery.isErrorEnabled = true
                binding.ltQuery.error = e.message
            }
        }
        binding.btnTableList.setOnClickListener {
            val tableListFragment = TableListFragment.newInstance()
            tableListFragment.setOnTableSelectedListener {
                binding.etQuery.setText(binding.etQuery.text.toString().plus(it))
            }
            tableListFragment.show(childFragmentManager, "TableListFragment")
        }
    }

    private fun displayData(list: List<Map<Int, String?>>) {
        binding.tableLayout.removeAllViews()
        binding.tvRowCount.text = list.size.minus(1).toString() + " row(s) found"
        for (i in list) {
            binding.tableLayout.addView(getRow(i))
        }
    }

    private fun getRow(map: Map<Int, String?>): TableRow {
        val row = TableRow(requireContext())
        for (i in 0 until map.size) {
            row.addView(getCell(map[i]))
        }
        return row
    }

    private fun getCell(text: String?): MaterialTextView {
        val cell = MaterialTextView(requireContext())
        cell.text = text.toString()
        cell.layoutParams =
            TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT)
        cell.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_table_cell)
        return cell
    }
}
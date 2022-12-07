package com.joshtalks.joshskills.ui.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentViewTableDataBinding

class ViewTableDataFragment : Fragment() {

    private lateinit var binding: FragmentViewTableDataBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[DatabaseViewModel::class.java]
    }
    private val args: ViewTableDataFragmentArgs by navArgs()
    val selectedColumns = mutableListOf<String>()
    val selectedColumnIndices = mutableListOf<Boolean>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_view_table_data, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initUI()
    }

    private fun FragmentViewTableDataBinding.initUI() {
        lifecycleOwner = this@ViewTableDataFragment
        tableName.text = args.tableName
        displayData(viewModel.getTableData(args.tableName))
        val columns: Array<String> = viewModel.getColumnsInTable(args.tableName).toTypedArray()
        selectedColumns.addAll(columns)
        for (i in columns.indices) {
            selectedColumnIndices.add(true)
        }
        btnColumn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Columns")
                .setMultiChoiceItems(columns, selectedColumnIndices.toBooleanArray()) { _, which, isChecked ->
                    if (isChecked) {
                        selectedColumns.add(columns[which])
                        selectedColumnIndices[which] = true
                    } else {
                        selectedColumns.remove(columns[which])
                        selectedColumnIndices[which] = false
                    }
                }
                .setPositiveButton("OK") { d, _ ->
                    displayData(viewModel.getTableData(args.tableName, selectedColumns))
                    d.dismiss()
                }
                .show()
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
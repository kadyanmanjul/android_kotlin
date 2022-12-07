package com.joshtalks.joshskills.ui.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentDatabaseBinding
import com.joshtalks.joshskills.ui.BottomAlertDialog

class DatabaseFragment : Fragment() {
    private lateinit var binding: FragmentDatabaseBinding
    private val viewModel: DatabaseViewModel by lazy {
        ViewModelProvider(this)[DatabaseViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_database, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.handler = this
    }

    fun openExecuteQueryFragment(v: View) {
        findNavController().navigate(
            DatabaseFragmentDirections.actionDatabaseFragmentToExecuteDatabaseQueryFragment(
                DatabaseOperation.QUERY
            )
        )
    }

    fun openDeleteQueryFragment(v: View) {
        findNavController().navigate(
            DatabaseFragmentDirections.actionDatabaseFragmentToExecuteDatabaseQueryFragment(
                DatabaseOperation.DELETE
            )
        )
    }

    fun openUpdateQueryFragment(v: View) {
        findNavController().navigate(
            DatabaseFragmentDirections.actionDatabaseFragmentToExecuteDatabaseQueryFragment(
                DatabaseOperation.UPDATE
            )
        )
    }

    fun openViewTableDataFragment(v: View) {
        val tableListFragment = TableListFragment.newInstance()
        tableListFragment.setOnTableSelectedListener { tableName ->
            findNavController().navigate(
                DatabaseFragmentDirections.actionDatabaseFragmentToViewTableDataFragment(
                    tableName
                )
            )
        }
        tableListFragment.show(childFragmentManager, "tableListFragment")
    }

    fun clearDatabase(v: View) {
        BottomAlertDialog()
            .setTitle("Clear Database")
            .setMessage("Are you sure you want to clear the database?")
            .setPositiveButton("Yes") { d ->
                viewModel.clearDatabase()
                d.dismiss()
            }
            .setNegativeButton("No") { d ->
                d.dismiss()
            }
            .show(childFragmentManager)
    }
}
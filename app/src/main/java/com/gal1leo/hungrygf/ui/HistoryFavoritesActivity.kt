package com.gal1leo.hungrygf.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.gal1leo.hungrygf.HungryGFApplication
import com.gal1leo.hungrygf.PlacesViewModel
import com.gal1leo.hungrygf.PlacesViewModelFactory
import com.gal1leo.hungrygf.R
import kotlinx.coroutines.launch

class HistoryFavoritesActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: HistoryFavoritesAdapter
    
    private val viewModel: PlacesViewModel by viewModels {
        val app = application as HungryGFApplication
        PlacesViewModelFactory(app.dependencyContainer.placesRepository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_favorites)
        
        initializeViews()
        setupToolbar()
        setupTabs()
        observeData()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        
        // Initialize adapter
        adapter = HistoryFavoritesAdapter { _ ->
            // Handle item click - for now just show a toast
            // TODO: Implement proper item click handling
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupToolbar() {
        // Set the toolbar as the action bar
        setSupportActionBar(toolbar)
        
        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Handle navigation click
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("History"))
        tabLayout.addTab(tabLayout.newTab().setText("Favorites"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showHistory()
                    1 -> showFavorites()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            viewModel.getSearchHistory().collect { history ->
                if (tabLayout.selectedTabPosition == 0) {
                    updateRecyclerView(history.map { "${it.location} - ${it.timestamp}" })
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.getFavorites().collect { favorites ->
                if (tabLayout.selectedTabPosition == 1) {
                    updateRecyclerView(favorites.map { "${it.name} (${it.location})" })
                }
            }
        }
    }
    
    private fun showHistory() {
        lifecycleScope.launch {
            viewModel.getSearchHistory().collect { history ->
                updateRecyclerView(history.map { "${it.location} - ${it.timestamp}" })
            }
        }
    }
    
    private fun showFavorites() {
        lifecycleScope.launch {
            viewModel.getFavorites().collect { favorites ->
                updateRecyclerView(favorites.map { "${it.name} (${it.location})" })
            }
        }
    }
    
    private fun updateRecyclerView(items: List<String>) {
        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.updateItems(items)
        }
    }
}

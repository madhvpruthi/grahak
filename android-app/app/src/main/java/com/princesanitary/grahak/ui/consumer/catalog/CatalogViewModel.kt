package com.princesanitary.grahak.ui.consumer.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.model.ProductCategory
import com.princesanitary.grahak.data.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class CatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    // Master list of all published products - ALWAYS kept fresh from repository.
    // Dashboard / Home screen observes this so it is NEVER wiped or filtered by catalog searches.
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ProductCategory?>(null)
    val selectedCategory: StateFlow<ProductCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Master job listens to all published products indefinitely so dashboard shelves
     * stay reactive without interference.
     */
    private var masterJob: Job? = null

    /**
     * The catalog is served by a *never-completing* snapshot listener. Each new
     * request must cancel the previous one, otherwise every filter tap leaves
     * another live listener writing to [_uiState] and they fight over it.
     */
    private var loadJob: Job? = null

    init {
        startObservingMasterProducts()
        loadAllProducts()
    }

    private fun startObservingMasterProducts() {
        masterJob?.cancel()
        masterJob = viewModelScope.launch {
            productRepository.getPublishedProducts().collect { result ->
                result.onSuccess { list ->
                    _allProducts.value = list
                }
            }
        }
    }

    fun refreshMasterProducts() {
        startObservingMasterProducts()
        loadAllProducts()
    }

    fun loadAllProducts() {
        _selectedCategory.value = null
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Keep whatever is already on screen while refreshing; only show the
            // skeleton when we genuinely have nothing yet.
            if (_uiState.value !is CatalogUiState.Success) {
                _uiState.value = CatalogUiState.Loading
            }
            productRepository.getPublishedProducts().collect { result ->
                result.fold(
                    onSuccess = { 
                        _allProducts.value = it
                        _uiState.value = CatalogUiState.Success(it) 
                    },
                    onFailure = {
                        _uiState.value = CatalogUiState.Error(it.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    fun loadProductsByCategory(category: ProductCategory) {
        _selectedCategory.value = category
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_uiState.value !is CatalogUiState.Success) {
                _uiState.value = CatalogUiState.Loading
            }
            productRepository.getProductsByCategory(category.id).collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = CatalogUiState.Success(it) },
                    onFailure = {
                        _uiState.value = CatalogUiState.Error(it.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    fun searchProducts(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            loadAllProducts()
            return
        }

        // Split query into tokens for order-independent matching
        // e.g. "pipe upvc" should match "UPVC Pipe" by matching all tokens
        val tokens = query.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Use master product list if already available (avoids extra Firestore trip)
            val masterList = _allProducts.value
            if (masterList.isNotEmpty()) {
                val filtered = masterList.filter { product ->
                    val searchable = listOf(
                        product.name,
                        product.brand,
                        product.itemType,
                        product.categoryId,
                        product.size
                    ).joinToString(" ").lowercase()
                    tokens.all { token -> searchable.contains(token) }
                }
                _uiState.value = CatalogUiState.Success(filtered)
            } else {
                // Fall back to live Firestore stream and filter client-side
                productRepository.getPublishedProducts().collect { result ->
                    result.fold(
                        onSuccess = { allProducts ->
                            val filtered = allProducts.filter { product ->
                                val searchable = listOf(
                                    product.name,
                                    product.brand,
                                    product.itemType,
                                    product.categoryId,
                                    product.size
                                ).joinToString(" ").lowercase()
                                tokens.all { token -> searchable.contains(token) }
                            }
                            _uiState.value = CatalogUiState.Success(filtered)
                        },
                        onFailure = {
                            _uiState.value = CatalogUiState.Error(it.message ?: "Search failed")
                        }
                    )
                }
            }
        }
    }

    fun clearCategoryFilter() = loadAllProducts()
}

sealed class CatalogUiState {
    data object Loading : CatalogUiState()
    data class Success(val products: List<Product>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

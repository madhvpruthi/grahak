package com.princesanitary.grahak.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.princesanitary.grahak.ui.RootViewModel
import com.princesanitary.grahak.ui.auth.AuthViewModel
import com.princesanitary.grahak.ui.consumer.cart.CartViewModel
import com.princesanitary.grahak.ui.consumer.catalog.CatalogViewModel
import com.princesanitary.grahak.ui.consumer.checkout.CheckoutViewModel
import com.princesanitary.grahak.ui.consumer.estimate.EstimateApprovalViewModel
import com.princesanitary.grahak.ui.consumer.orders.OrderDetailViewModel
import com.princesanitary.grahak.ui.consumer.profile.ConsumerProfileViewModel
import com.princesanitary.grahak.ui.consumer.orders.OrderHistoryViewModel
import com.princesanitary.grahak.ui.consumer.services.BookPlumberViewModel
import com.princesanitary.grahak.ui.consumer.services.MyRequestsViewModel
import com.princesanitary.grahak.ui.plumber.jobs.PlumberJobsHistoryViewModel
import com.princesanitary.grahak.ui.consumer.services.ChoosePlumberViewModel
import com.princesanitary.grahak.ui.plumber.PlumberGateViewModel
import com.princesanitary.grahak.ui.plumber.estimate.CreateEstimateViewModel
import com.princesanitary.grahak.ui.plumber.estimate.EstimateListViewModel
import com.princesanitary.grahak.ui.plumber.jobs.ActiveJobViewModel
import com.princesanitary.grahak.ui.plumber.jobs.JobRequestsViewModel
import com.princesanitary.grahak.ui.plumber.onboarding.PlumberRegistrationViewModel
import com.princesanitary.grahak.ui.plumber.profile.PlumberProfileViewModel
import com.princesanitary.grahak.ui.plumber.wallet.PlumberWalletViewModel

/**
 * Replaces Hilt's generated ViewModel factories with one explicit table.
 *
 * Adding a ViewModel means adding one line here. In exchange the project has no
 * annotation processor, which is what made Gradle sync and every incremental
 * build slow.
 */
object AppViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val sl = ServiceLocator

        val vm: ViewModel = when {
            // Root / auth
            modelClass.isAssignableFrom(RootViewModel::class.java) ->
                RootViewModel(sl.auth, sl.firestore, sl.application)

            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(sl.auth, sl.firestore, sl.userRepository)

            // Consumer
            modelClass.isAssignableFrom(CatalogViewModel::class.java) ->
                CatalogViewModel(sl.productRepository)

            modelClass.isAssignableFrom(CartViewModel::class.java) ->
                CartViewModel(sl.cartRepository)

            modelClass.isAssignableFrom(CheckoutViewModel::class.java) ->
                CheckoutViewModel(sl.cartRepository, sl.orderRepository)

            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) ->
                OrderHistoryViewModel(sl.orderRepository)

            modelClass.isAssignableFrom(OrderDetailViewModel::class.java) ->
                OrderDetailViewModel(sl.orderRepository)

            modelClass.isAssignableFrom(ConsumerProfileViewModel::class.java) ->
                ConsumerProfileViewModel(sl.userRepository)

            modelClass.isAssignableFrom(EstimateApprovalViewModel::class.java) ->
                EstimateApprovalViewModel(sl.estimateRepository, sl.orderRepository)

            modelClass.isAssignableFrom(BookPlumberViewModel::class.java) ->
                BookPlumberViewModel(sl.serviceJobRepository)

            modelClass.isAssignableFrom(MyRequestsViewModel::class.java) ->
                MyRequestsViewModel(
                    sl.serviceJobRepository,
                    sl.plumberRepository,
                    sl.reviewRepository
                )

            modelClass.isAssignableFrom(PlumberJobsHistoryViewModel::class.java) ->
                PlumberJobsHistoryViewModel(sl.serviceJobRepository, sl.orderRepository)

            // Plumber
            modelClass.isAssignableFrom(JobRequestsViewModel::class.java) ->
                JobRequestsViewModel(sl.serviceJobRepository, sl.plumberRepository)

            modelClass.isAssignableFrom(ActiveJobViewModel::class.java) ->
                ActiveJobViewModel(sl.serviceJobRepository, sl.orderRepository)

            modelClass.isAssignableFrom(CreateEstimateViewModel::class.java) ->
                CreateEstimateViewModel(sl.estimateRepository)

            modelClass.isAssignableFrom(EstimateListViewModel::class.java) ->
                EstimateListViewModel(sl.estimateRepository)

            modelClass.isAssignableFrom(PlumberRegistrationViewModel::class.java) ->
                PlumberRegistrationViewModel(sl.plumberRepository)

            modelClass.isAssignableFrom(PlumberGateViewModel::class.java) ->
                PlumberGateViewModel(sl.plumberRepository)

            modelClass.isAssignableFrom(ChoosePlumberViewModel::class.java) ->
                ChoosePlumberViewModel(sl.plumberRepository, sl.serviceJobRepository)

            modelClass.isAssignableFrom(PlumberProfileViewModel::class.java) ->
                PlumberProfileViewModel(sl.plumberRepository)

            modelClass.isAssignableFrom(PlumberWalletViewModel::class.java) ->
                PlumberWalletViewModel(sl.walletRepository)

            modelClass.isAssignableFrom(com.princesanitary.grahak.ui.owner.OwnerViewModel::class.java) ->
                com.princesanitary.grahak.ui.owner.OwnerViewModel(sl.adminRepository)

            else -> throw IllegalArgumentException(
                "Unknown ViewModel ${modelClass.name}. Register it in AppViewModelFactory."
            )
        }

        return vm as T
    }
}

/** Drop-in replacement for the Hilt Compose entry point. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(): VM =
    viewModel(factory = AppViewModelFactory)

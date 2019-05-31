package me.ibrahimsn.wallet.ui.wallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.ibrahimsn.wallet.entity.EtherPriceResponse
import me.ibrahimsn.wallet.entity.EtherScanResponse
import me.ibrahimsn.wallet.entity.Transaction
import me.ibrahimsn.wallet.entity.Wallet
import me.ibrahimsn.wallet.repository.EtherScanRepository
import me.ibrahimsn.wallet.repository.EthereumNetworkRepository
import me.ibrahimsn.wallet.repository.WalletRepository
import me.ibrahimsn.wallet.util.Constants
import me.ibrahimsn.wallet.util.RxBus
import javax.inject.Inject

class WalletViewModel @Inject constructor(private val networkRepository: EthereumNetworkRepository,
                                          private val etherScanRepository: EtherScanRepository,
                                          walletRepository: WalletRepository) : ViewModel() {

    private val disposable = CompositeDisposable()

    val currentWallet: MutableLiveData<Wallet?> = MutableLiveData()
    val transactions: MutableLiveData<List<Transaction>> = MutableLiveData()
    val walletBalance: MutableLiveData<Double> = MutableLiveData()
    val walletBalanceReal: MutableLiveData<Double> = MutableLiveData()

    /**
     * Asynchronously get current wallet on initialization
     */
    init {
        disposable.add(walletRepository.getCurrentWallet()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::onGetCurrentWallet)
                .doOnSuccess(this::loadWalletBalance)
                .doOnSuccess(this::fetchTransactions)
                .doOnError(this::onRxError)
                .subscribe())

        // Listen wallet changes
        disposable.add(RxBus.listen(RxBus.RxEvent.OnChangeCurrentWallet::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { Observable.just(it.wallet) }
                .doOnNext(this::onGetCurrentWallet)
                .doOnNext(this::loadWalletBalance)
                .doOnNext(this::fetchTransactions)
                .doOnError(this::onRxError)
                .subscribe())
    }

    /**
     * Asynchronously fetch wallet transactions via EtherScan API.
     */
    private fun fetchTransactions(wallet: Wallet?) {
        if (wallet != null)
            disposable.add(etherScanRepository.fetchTransaction(wallet.address, 1, 4)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onFetchTransactions, this::onRxError))
    }

    /**
     * Asynchronously load current wallet balance from ETH network.
     */
    private fun loadWalletBalance(wallet: Wallet?) {
        if (wallet != null)
            disposable.add(networkRepository.getWalletBalance(wallet)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(this::loadEthPrice)
                    .subscribe(this::onLoadWalletBalance, this::onRxError))
    }

    /**
     * Asynchronously get ETH price in USD
     */
    private fun loadEthPrice(balance: Double) {
        disposable.add(etherScanRepository.fetchEthPrice()
                .flatMap<Pair<EtherPriceResponse, Double>> {
                    Single.just(Pair(it, balance))
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::onLoadEthPrice)
                .subscribe())
    }

    private fun onGetCurrentWallet(wallet: Wallet?) {
        currentWallet.postValue(wallet)
    }

    private fun onLoadWalletBalance(balance: Double) {
        walletBalance.postValue(balance)
    }

    private fun onFetchTransactions(response: EtherScanResponse) {
        transactions.postValue(response.result)
    }

    private fun onLoadEthPrice(response: Pair<EtherPriceResponse, Double>) {
        val price = response.first.result.ethusd.toDouble()
        walletBalanceReal.postValue(price * response.second)
    }

    private fun onRxError(e: Throwable) {
        Log.d(Constants.TAG, "RxError:", e)
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}
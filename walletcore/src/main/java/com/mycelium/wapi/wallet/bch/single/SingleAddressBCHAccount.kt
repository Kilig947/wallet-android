package com.mycelium.wapi.wallet.bch.single

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.btc.Reference
import com.mycelium.wapi.wallet.SingleAddressAccountBacking
import com.mycelium.wapi.wallet.SpvBalanceFetcher
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.btc.ChangeAddressMode
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance

import java.util.UUID

class SingleAddressBCHAccount(context: SingleAddressAccountContext,
                              keyStore: PublicPrivateKeyStore, network: NetworkParameters,
                              backing: SingleAddressAccountBacking, wapi: Wapi,
                              private val spvBalanceFetcher: SpvBalanceFetcher)
    : SingleAddressAccount(context, keyStore, network, backing, wapi, Reference(ChangeAddressMode.NONE)) {


    private var visible: Boolean = false

    override fun getCurrencyBasedBalance(): CurrencyBasedBalance {
        return spvBalanceFetcher.retrieveByUnrelatedAccountId(id.toString())
    }

    override fun calculateMaxSpendableAmount(minerFeePerKbToUse: Long): Value {
        //TODO Refactor the code and make the proper usage of minerFeePerKbToUse parameter
        val txFee = "NORMAL"
        val txFeeFactor = 1.0f
        return Value.valueOf( if(_network.isProdnet())  BitcoinMain.get() else BitcoinTest.get(),
                spvBalanceFetcher.calculateMaxSpendableAmountUnrelatedAccount(id.toString(), txFee, txFeeFactor))
    }

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long, destinationAddress: Address?) =
            calculateMaxSpendableAmount(minerFeeToUse)

    override fun getId(): UUID {
        return UUID.nameUUIDFromBytes(("BCH" + super.getId().toString()).toByteArray())
    }

    override fun getTransactionHistory(offset: Int, limit: Int): List<TransactionSummary> {
        return spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString(), offset, limit)
                .filter { it.height >= forkBlock }
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<BtcTransaction> {
        TODO("not implemented") //spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString(), receivingSince!!)
    }

    override fun getTransactionSummary(txid: Sha256Hash): TransactionSummary? {
        val transactions = spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString())
        return transactions.firstOrNull { it.txid == txid }
    }

    override fun dropCachedData() {
        //BCH account have no separate context, so no cashed data, nothing to drop here
    }

    override fun isVisible(): Boolean {
        if (!visible && (spvBalanceFetcher.syncProgressPercents == 100f || spvBalanceFetcher.isAccountSynced(this))) {
            visible = spvBalanceFetcher.isAccountVisible(this) ||
                    !spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString()).isEmpty()
            if (visible) {
                spvBalanceFetcher.setVisible(this)
            }
        }
        return visible
    }

    companion object {
        private const val forkBlock = 478559

        fun calculateId(address: Address): UUID {
            return UUID.nameUUIDFromBytes(("BCH" + SingleAddressAccount.calculateId(address).toString()).toByteArray())
        }
    }

    override fun getTransactions(offset: Int, limit: Int): MutableList<BtcTransaction> {
        return ArrayList<BtcTransaction>()
    }
}
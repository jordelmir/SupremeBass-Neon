package com.supreme.truth

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * Money — type-safe monetary value using BigDecimal.
 * Every monetary value MUST carry an ISO 4217 currency code.
 */
data class Money(
    val amount: BigDecimal,
    val currencyCode: String
) : Comparable<Money> {

    init {
        require(currencyCode.length == 3) { "Currency must be ISO 4217 (3 chars): $currencyCode" }
    }

    constructor(amount: Double, currencyCode: String) : this(
        BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP),
        currencyCode
    )

    constructor(amount: String, currencyCode: String) : this(
        BigDecimal(amount).setScale(2, RoundingMode.HALF_UP),
        currencyCode
    )

    val currency: Currency get() = Currency.getInstance(currencyCode)

    fun add(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add different currencies: $currencyCode + ${other.currencyCode}" }
        return Money(amount.add(other.amount), currencyCode)
    }

    fun subtract(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract different currencies" }
        return Money(amount.subtract(other.amount), currencyCode)
    }

    fun multiply(factor: Int): Money = Money(amount.multiply(BigDecimal(factor)), currencyCode)
    fun multiply(factor: Double): Money = Money(amount.multiply(BigDecimal.valueOf(factor)), currencyCode)

    fun isPositive(): Boolean = amount > BigDecimal.ZERO
    fun isNegative(): Boolean = amount < BigDecimal.ZERO
    fun isZero(): Boolean = amount == BigDecimal.ZERO

    fun abs(): Money = if (isNegative()) Money(amount.abs(), currencyCode) else this

    override fun compareTo(other: Money): Int {
        require(currencyCode == other.currencyCode) { "Cannot compare different currencies" }
        return amount.compareTo(other.amount)
    }

    override fun toString(): String = "${currency.symbol}${amount.toPlainString()} $currencyCode"
}

package mx.odelant.printorders.entities

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import java.util.*

@Dao
interface CartDao {
    @Insert
    fun insert(cart: Cart)

    @Update
    fun updateCart(cart: Cart)

    @Delete
    fun deleteCart(cart: Cart)

    @Query("DELETE FROM cart")
    fun deleteAllCart()

    @Query("SELECT * FROM Cart WHERE isPending = 1 ORDER BY cart_id DESC LIMIT 1")
    fun getPendingCart(): Cart?

    @Query("SELECT * FROM Cart WHERE cart_id = :cartId LIMIT 1")
    fun getById(cartId: Int): Cart?

    @Query("SELECT * FROM Cart")
    fun getAllCart(): Array<Cart>

    @Query("UPDATE Cart SET folio = :Folio WHERE cart_id = :cartId")
    fun updateFolio(cartId: Int, Folio: Int)

    @Query("SELECT count(*) FROM Cart")
    fun getOrderQuantity(): Int

    @Query("SELECT folio FROM Cart WHERE cart_id = :cartId")
    fun getFolio(cartId: Int): Int

    @Query(
        """
        SELECT Cart.*, Client.* 
        FROM Cart 
        LEFT JOIN Client ON Cart.clientId = Client.id
        WHERE Cart.dateCreated BETWEEN :start AND :end
            AND Cart.isPending = 0
        ORDER BY Cart.dateCreated DESC
    """
    )
    fun getCartsAndClientsByDate(start: Date, end: Date): List<CartAndClient>

    @Query(
        """
        SELECT Cart.*, Client.* 
        FROM Cart 
        LEFT JOIN Client ON Cart.clientId = Client.id
        WHERE Cart.dateCreated BETWEEN :start AND :end
            AND Client.id = :clientId
            AND Cart.isPending = 0
        ORDER BY Cart.dateCreated DESC
    """
    )
    fun getCartsAndClientsByDateAndClient(
        start: Date,
        end: Date,
        clientId: Int
    ): List<CartAndClient>

    @RawQuery
    fun execIntPairListQuery(query: SupportSQLiteQuery): List<IntPairForChart>

    @RawQuery
    fun execAnalyticsTotalsQuery(query: SupportSQLiteQuery): AnalyticsTotals

    @RawQuery
    fun execIntQuery(query: SupportSQLiteQuery): Int

    data class CartAndClient(
        @Embedded
        val cart: Cart,
        @Embedded
        val client: Client?
    )

    data class IntPairForChart(
        val amount: Int,
        val selectGranularity: Int,
        val date: Date
    )

    data class AnalyticsTotals(
        val totalRevenue: Int,
        val totalAmount: Int,
        val totalOrders: Int
    )
}
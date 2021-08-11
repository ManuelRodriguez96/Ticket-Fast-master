package mx.odelant.printorders.entities

import androidx.room.*

@Dao
interface CartReturnItemDao {
    @Insert
    fun insert(cartReturnItem: CartReturnItem)

    @Update
    fun update(cartReturnItem: CartReturnItem)

    @Delete
    fun deleteCartReturnItem(cartReturnItem: CartReturnItem)

    @Query("DELETE FROM cartReturnItem")
    fun deleteAllCartReturnItem()

    @Query("SELECT * FROM cartreturnitem WHERE cartId = :cartId")
    fun getCartReturnItems(cartId: Int): List<CartReturnItem>

    @Query("SELECT * FROM cartreturnitem WHERE productId = :productId AND cartId = :cartId")
    fun getCartReturnItemByProductIdAndCartId(productId: Int, cartId: Int): CartReturnItem?

    @Query(
        """
        SELECT CartReturnItem.*, Product.* 
        FROM CartReturnItem 
        JOIN Product ON CartReturnItem.productId = Product.product_id
        WHERE CartReturnItem.cartId = :cartId
    """
    )
    fun getCartReturnItemsAndProductsFromCartId(cartId: Int): List<CartReturnItemAndProduct>

    data class CartReturnItemAndProduct(
        @Embedded
        val cartReturnItem: CartReturnItem,
        @Embedded
        val product: Product
    )
}
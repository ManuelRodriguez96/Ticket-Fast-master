package mx.odelant.printorders.entities

import androidx.room.*

@Dao
interface CartItemDao {
    @Insert
    fun insert(cartItem: CartItem)

    @Update
    fun update(cartItem: CartItem)

    @Delete
    fun deleteCartItem(cartItem: CartItem)

    @Query("DELETE FROM cartItem")
    fun deleteAllCartItem()

    @Query("SELECT * FROM cartitem WHERE cartId = :cartId")
    fun getCartItems(cartId: Int): List<CartItem>

    @Query("SELECT * FROM cartitem WHERE productId = :productId AND cartId = :cartId")
    fun getCartItemByProductIdAndCartId(productId: Int, cartId: Int): CartItem?

    @Query(
        """
        SELECT CartItem.*, Product.* 
        FROM CartItem 
        JOIN Product ON CartItem.productId = Product.product_id
        WHERE CartItem.cartId = :cartId
    """
    )
    fun getCartItemsAndProductsFromCartId(cartId: Int): List<CartItemAndProduct>

    data class CartItemAndProduct(
        @Embedded
        val cartItem: CartItem,
        @Embedded
        val product: Product
    )
}
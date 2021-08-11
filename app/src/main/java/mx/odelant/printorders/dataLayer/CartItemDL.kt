package mx.odelant.printorders.dataLayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.odelant.printorders.entities.Cart
import mx.odelant.printorders.entities.CartItem
import mx.odelant.printorders.entities.CartItemDao
import mx.odelant.printorders.entities.Product

class CartItemDL {

    companion object {
        suspend fun addCartItem(db: AppDatabase, cartItem: CartItem, product: Product, cart: Cart) {
            withContext(Dispatchers.IO) {

                val cartItemFromDb =
                    db.cartItemDao().getCartItemByProductIdAndCartId(product.id, cart.cart_id)
                if (cartItemFromDb == null) {
                    db.cartItemDao().insert(cartItem)
                } else {
                    val updateCartItem = cartItem.copy(
                        id = cartItemFromDb.id,
                        quantityInHundredths = cartItem.quantityInHundredths + cartItemFromDb.quantityInHundredths
                    )

                    db.cartItemDao().update(updateCartItem)
                }
            }
        }

        suspend fun getCartItemAndProductByCartId(
            db: AppDatabase,
            cartId: Int
        ): List<CartItemDao.CartItemAndProduct> {
            return withContext(Dispatchers.IO) {
                db.cartItemDao().getCartItemsAndProductsFromCartId(cartId)
            }
        }

        suspend fun update(db: AppDatabase, cartItem: CartItem) {
            withContext(Dispatchers.IO) {
                db.cartItemDao().update(cartItem)
            }
        }

        suspend fun delete(db: AppDatabase, cartItem: CartItem) {
            withContext(Dispatchers.IO) {
                db.cartItemDao().deleteCartItem(cartItem)
            }
        }

        suspend fun deleteAll(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                db.cartItemDao().deleteAllCartItem()
            }
        }
    }
}
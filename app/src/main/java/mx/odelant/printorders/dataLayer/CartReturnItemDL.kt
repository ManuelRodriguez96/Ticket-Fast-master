package mx.odelant.printorders.dataLayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.odelant.printorders.entities.Cart
import mx.odelant.printorders.entities.CartReturnItem
import mx.odelant.printorders.entities.CartReturnItemDao
import mx.odelant.printorders.entities.Product

class CartReturnItemDL {

    companion object {
        suspend fun addCartReturnItem(
            db: AppDatabase,
            cartReturnItem: CartReturnItem,
            product: Product,
            cart: Cart
        ) {
            withContext(Dispatchers.IO) {
                val cartReturnItemFromDb = db.cartReturnItemDao()
                    .getCartReturnItemByProductIdAndCartId(product.id, cart.cart_id)
                if (cartReturnItemFromDb == null) {
                    db.cartReturnItemDao().insert(cartReturnItem)
                } else {
                    val updateCartItem = cartReturnItem.copy(
                        id = cartReturnItemFromDb.id,
                        quantityInHundredths = cartReturnItem.quantityInHundredths + cartReturnItemFromDb.quantityInHundredths
                    )
                    db.cartReturnItemDao().update(updateCartItem)
                }
            }
        }

        suspend fun getCartReturnItemAndProductByCartId(
            db: AppDatabase,
            cartId: Int
        ): List<CartReturnItemDao.CartReturnItemAndProduct> {
            return withContext(Dispatchers.IO) {
                db.cartReturnItemDao().getCartReturnItemsAndProductsFromCartId(cartId)
            }
        }

        suspend fun update(db: AppDatabase, cartReturnItem: CartReturnItem) {
            withContext(Dispatchers.IO) {
                db.cartReturnItemDao().update(cartReturnItem)
            }
        }

        suspend fun delete(db: AppDatabase, cartReturnItem: CartReturnItem) {
            withContext(Dispatchers.IO) {
                db.cartReturnItemDao().deleteCartReturnItem(cartReturnItem)
            }
        }

        suspend fun deleteAll(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                db.cartReturnItemDao().deleteAllCartReturnItem()
            }
        }
    }
}
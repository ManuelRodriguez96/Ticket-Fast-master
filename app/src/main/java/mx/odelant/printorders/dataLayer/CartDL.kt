package mx.odelant.printorders.dataLayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.odelant.printorders.entities.Cart
import mx.odelant.printorders.entities.CartDao
import mx.odelant.printorders.entities.Client
import java.util.*

class CartDL {

    companion object {
        suspend fun getOrCreatePendingCart(db: AppDatabase): Cart {
            return withContext(Dispatchers.IO) {
                var cart = db.cartDao().getPendingCart()
                if (cart == null) {
                    var folio = db.cartDao().getOrderQuantity()
                    db.cartDao().insert(Cart(0 ,null, Date(), 0, true, (folio + 1)))
                    cart = db.cartDao().getPendingCart()
                }
                cart!!
            }
        }

        suspend fun update(db: AppDatabase, cart: Cart) {
            withContext(Dispatchers.IO) {
                db.cartDao().updateCart(cart)
            }
        }

        suspend fun deleteAll(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                db.cartDao().deleteAllCart()
            }
        }

        suspend fun clearPendingCart(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                val cart = getOrCreatePendingCart(db)

                val cartItemAndProducts =
                    CartItemDL.getCartItemAndProductByCartId(db, cart.cart_id)
                cartItemAndProducts.map {
                    CartItemDL.delete(db, it.cartItem)
                }

                val cartReturnItemAndProducts =
                    CartReturnItemDL.getCartReturnItemAndProductByCartId(db, cart.cart_id)
                cartReturnItemAndProducts.map {
                    CartReturnItemDL.delete(db, it.cartReturnItem)
                }
            }
        }

        suspend fun finalizePendingCart(db: AppDatabase): Cart {
            return withContext(Dispatchers.IO) {
                var cart = getOrCreatePendingCart(db)
                val updatedCart = cart.copy(
                    isPending = false,
                    dateCreated = Date()
                )

                cart = updatedCart
                update(db, cart)

                val cartItemAndProducts =
                    CartItemDL.getCartItemAndProductByCartId(db, cart.cart_id)
                cartItemAndProducts.map {
                    val updatedProduct = it.product.copy(
                        stockInHundredths = it.product.stockInHundredths - it.cartItem.quantityInHundredths
                    )
                    ProductDL.tryUpdateProduct(db, updatedProduct)
                }

                val cartReturnItemAndProducts =
                    CartReturnItemDL.getCartReturnItemAndProductByCartId(db, cart.cart_id)
                cartReturnItemAndProducts.map {
                    val updatedProduct = it.product.copy(
                        stockInHundredths = it.product.stockInHundredths - it.cartReturnItem.quantityInHundredths
                    )
                    ProductDL.tryUpdateProduct(db, updatedProduct)
                }

                cart
            }
        }

        suspend fun getById(db: AppDatabase, cartId: Int): Cart? {
            return withContext(Dispatchers.IO) {
                db.cartDao().getById(cartId)
            }
        }

        suspend fun getCartsWithClient(
            db: AppDatabase,
            client: Client?,
            dateStart: Calendar,
            dateEnd: Calendar
        ): List<CartDao.CartAndClient> {
            return withContext(Dispatchers.IO) {
                if (client != null) {
                    db.cartDao()
                        .getCartsAndClientsByDateAndClient(dateStart.time, dateEnd.time, client.id)
                } else {
                    db.cartDao().getCartsAndClientsByDate(dateStart.time, dateEnd.time)
                }
            }
        }
    }
}

package mx.odelant.printorders.dataLayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.entities.ClientPrice
import mx.odelant.printorders.entities.Product

class ClientPriceDL {

    enum class ClientPriceDLError {
        ERROR_DUPLICATE_ENTRY, ERROR_NO_ENTRY, ERROR_INVALID_REFERENCE, NONE
    }

    companion object {

        fun getAllClientPricesLikeProductName(
            db: AppDatabase,
            mClientId: Int,
            searchString: String
        ): List<ClientPrice> {
            return db.clientPriceDao()
                .getAllClientPricesLikeFormattedProductName(mClientId, "%$searchString%")
        }

        suspend fun tryInsertClientPrice(
            db: AppDatabase,
            clientPrice: ClientPrice
        ): ClientPriceDLError {

            return withContext(Dispatchers.IO) {

                val client = ClientDL.getById(db, clientPrice.clientId)
                val product = ProductDL.getProductById(db, clientPrice.productId)

                if (client == null || product == null) {
                    return@withContext ClientPriceDLError.ERROR_INVALID_REFERENCE
                }

                val conflictingClientPrice =
                    db.clientPriceDao()
                        .getByClientAndProduct(clientPrice.clientId, clientPrice.productId)

                if (conflictingClientPrice != null) {
                    return@withContext ClientPriceDLError.ERROR_DUPLICATE_ENTRY
                }
                db.clientPriceDao().insertClientPrice(clientPrice)
                return@withContext ClientPriceDLError.NONE
            }
        }

        suspend fun tryUpdateClientPrice(
            db: AppDatabase,
            clientPrice: ClientPrice
        ): ClientPriceDLError {

            return withContext(Dispatchers.IO) {

                val client = ClientDL.getById(db, clientPrice.clientId)
                val product = ProductDL.getProductById(db, clientPrice.productId)

                if (client == null || product == null) {
                    return@withContext ClientPriceDLError.ERROR_INVALID_REFERENCE
                }

                val conflictingClientPrice =
                    db.clientPriceDao()
                        .getByClientAndProduct(clientPrice.clientId, clientPrice.productId)

                if (conflictingClientPrice == null) {
                    return@withContext ClientPriceDLError.ERROR_NO_ENTRY
                }
                db.clientPriceDao().updateClientPrice(clientPrice)
                return@withContext ClientPriceDLError.NONE
            }
        }

        suspend fun deleteClientPrice(db: AppDatabase, clientPrice: ClientPrice) {
            withContext(Dispatchers.IO) {
                db.clientPriceDao().deleteClientPrice(clientPrice)
            }
        }


        suspend fun deleteAll(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                db.clientPriceDao().deleteAllClientPrice()
            }
        }

        suspend fun getByClientAndProduct(
            db: AppDatabase,
            client: Client,
            product: Product
        ): ClientPrice? {
            return withContext(Dispatchers.IO) {
                db.clientPriceDao().getByClientAndProduct(client.id, product.id)
            }
        }
    }
}
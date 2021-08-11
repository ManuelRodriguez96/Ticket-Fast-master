package mx.odelant.printorders.dataLayer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.odelant.printorders.entities.Product

class ProductDL {
    enum class ProductDLError {
        ERROR_DUPLICATE_NAME, ERROR_EMPTY_NAME, NONE
    }

    companion object {

        suspend fun getAllProductsLikeName(db: AppDatabase, searchString: String): List<Product> {
            return withContext(Dispatchers.IO) {
                db.productDao().getAllProductsLikeName("%$searchString%")
            }
        }

        suspend fun tryInsertProduct(db: AppDatabase, product: Product): ProductDLError {
            if (product.name.isBlank()) {
                return ProductDLError.ERROR_EMPTY_NAME
            }

            return withContext(Dispatchers.IO) {
                val repeatedProduct = db.productDao().getProductByName(product.name)
                if (repeatedProduct == null) {
                    db.productDao().insertProduct(product)
                    return@withContext ProductDLError.NONE
                }

                if (repeatedProduct.isDeleted) {
                    val updatedProduct = product.copy(id = repeatedProduct.id)
                    db.productDao().updateProduct(updatedProduct)
                    return@withContext ProductDLError.NONE
                } else {
                    return@withContext ProductDLError.ERROR_DUPLICATE_NAME
                }

            }
        }

        suspend fun tryUpdateProduct(db: AppDatabase, product: Product): ProductDLError {
            if (product.name.isBlank()) {
                return ProductDLError.ERROR_EMPTY_NAME
            }

            return withContext(Dispatchers.IO) {
                val nameAlreadyExists = db.productDao().getProductCountByName(product.name) > 0
                val isNameChanging =
                    db.productDao().getProductCountByNameAndId(product.name, product.id) == 0

                if (isNameChanging && nameAlreadyExists) {
                    return@withContext ProductDLError.ERROR_DUPLICATE_NAME
                }
                db.productDao().updateProduct(product)
                return@withContext ProductDLError.NONE
            }
        }

        suspend fun deleteProductById(db: AppDatabase, product: Product) {
            withContext(Dispatchers.IO) {
                db.productDao().deleteProduct(product.id)
            }
        }

        suspend fun deleteAll(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                db.productDao().deleteAllProduct()
            }
        }

        suspend fun getProductById(db: AppDatabase, productId: Int): Product? {
            return withContext(Dispatchers.IO) {
                db.productDao().getProduct(productId)
            }
        }

        suspend fun getAll(db: AppDatabase): List<Product> {
            return withContext(Dispatchers.IO) {
                db.productDao().getAllProducts()
            }
        }
    }
}

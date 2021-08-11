package mx.odelant.printorders.dataLayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.odelant.printorders.entities.Client

class ClientDL {
    enum class ClientDLError {
        ERROR_DUPLICATE_NAME, ERROR_EMPTY_NAME, NONE
    }

    companion object {

        suspend fun getAllClientsLikeName(db: AppDatabase, searchString: String): List<Client> {
            return withContext(Dispatchers.IO) {
                db.clientDao().getAllClientsLikeName("%$searchString%")
            }
        }

        suspend fun tryInsertClient(db: AppDatabase, client: Client): ClientDLError {
            var error = ClientDLError.NONE

            if (client.name.isBlank()) {
                return ClientDLError.ERROR_EMPTY_NAME
            }

            withContext(Dispatchers.IO) {
                val nameConflicts = db.clientDao().getClientCountByName(client.name)
                if (nameConflicts > 0) {
                    error = ClientDLError.ERROR_DUPLICATE_NAME
                }
                if (error == ClientDLError.NONE) {
                    db.clientDao().insertClient(client)
                }
            }
            return error
        }

        suspend fun tryUpdateClient(db: AppDatabase, client: Client): ClientDLError {
            var error = ClientDLError.NONE

            if (client.name.isBlank()) {
                return ClientDLError.ERROR_EMPTY_NAME
            }

            withContext(Dispatchers.IO) {
                val nameAlreadyExists = db.clientDao().getClientCountByName(client.name) > 0
                val isNameChanging =
                    db.clientDao().getClientCountByNameAndId(client.name, client.id) == 0

                if (isNameChanging && nameAlreadyExists) {
                    error = ClientDLError.ERROR_DUPLICATE_NAME
                }
                if (error == ClientDLError.NONE) {
                    db.clientDao().updateClient(client)
                }
            }

            return error
        }

        // usage pending, see ClientDetailDialog
        suspend fun deleteClientById(db: AppDatabase, client: Client) {
            withContext(Dispatchers.IO) {
                db.clientDao().deleteClient(client.id)
            }
        }

        suspend fun deleteAll(db: AppDatabase) {
            withContext(Dispatchers.IO) {
                db.clientDao().deleteAllClient()
            }
        }

        suspend fun getById(db: AppDatabase, clientId: Int): Client? {
            return withContext(Dispatchers.IO) {
                db.clientDao().getClient(clientId)
            }
        }

        suspend fun getAll(db: AppDatabase): List<Client> {
            return withContext(Dispatchers.IO) {
                db.clientDao().getAllClients()
            }
        }
    }
}
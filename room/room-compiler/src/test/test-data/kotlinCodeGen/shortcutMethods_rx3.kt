import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.lang.Void
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>

  private val __deleteAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

  private val __updateAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

  private val __upsertAdapterOfMyEntity: EntityUpsertionAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`other`) VALUES (?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
      }
    }
    this.__deleteAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }
    this.__updateAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `MyEntity` SET `pk` = ?,`other` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
        statement.bindLong(3, entity.pk.toLong())
      }
    }
    this.__upsertAdapterOfMyEntity = EntityUpsertionAdapter<MyEntity>(object :
        EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `MyEntity` (`pk`,`other`) VALUES (?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
      }
    }, object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `MyEntity` SET `pk` = ?,`other` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
        statement.bindLong(3, entity.pk.toLong())
      }
    })
  }

  public override fun insertSingle(vararg entities: MyEntity): Single<List<Long>> =
      Single.fromCallable(object : Callable<List<Long>?> {
    public override fun call(): List<Long>? {
      __db.beginTransaction()
      try {
        val _result: List<Long>? = __insertAdapterOfMyEntity.insertAndReturnIdsList(entities)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun insertCompletable(vararg entities: MyEntity): Completable =
      Completable.fromCallable(object : Callable<Void?> {
    public override fun call(): Void? {
      __db.beginTransaction()
      try {
        __insertAdapterOfMyEntity.insert(entities)
        __db.setTransactionSuccessful()
        return null
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun deleteSingle(entity: MyEntity): Single<Int> = Single.fromCallable(object :
      Callable<Int?> {
    public override fun call(): Int? {
      var _total: Int = 0
      __db.beginTransaction()
      try {
        _total += __deleteAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return _total
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun deleteCompletable(entity: MyEntity): Completable =
      Completable.fromCallable(object : Callable<Void?> {
    public override fun call(): Void? {
      __db.beginTransaction()
      try {
        __deleteAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return null
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun updateSingle(entity: MyEntity): Single<Int> = Single.fromCallable(object :
      Callable<Int?> {
    public override fun call(): Int? {
      var _total: Int = 0
      __db.beginTransaction()
      try {
        _total += __updateAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return _total
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun updateCompletable(entity: MyEntity): Completable =
      Completable.fromCallable(object : Callable<Void?> {
    public override fun call(): Void? {
      __db.beginTransaction()
      try {
        __updateAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return null
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun upsertSingle(vararg entities: MyEntity): Single<List<Long>> =
      Single.fromCallable(object : Callable<List<Long>?> {
    public override fun call(): List<Long>? {
      __db.beginTransaction()
      try {
        val _result: List<Long>? = __upsertAdapterOfMyEntity.upsertAndReturnIdsList(entities)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun upsertCompletable(vararg entities: MyEntity): Completable =
      Completable.fromCallable(object : Callable<Void?> {
    public override fun call(): Void? {
      __db.beginTransaction()
      try {
        __upsertAdapterOfMyEntity.upsert(entities)
        __db.setTransactionSuccessful()
        return null
      } finally {
        __db.endTransaction()
      }
    }
  })

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

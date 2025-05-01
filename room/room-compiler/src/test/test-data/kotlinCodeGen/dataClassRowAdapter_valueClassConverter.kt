import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.convertByteToUUID
import androidx.room.util.convertUUIDToByte
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import java.util.UUID
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuidData`,`nullableUuidData`,`nullableLongData`,`doubleNullableLongData`,`genericData`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        val _data: Long = checkNotNull(entity.pk.data) { "Cannot bind NULLABLE value 'data' of inline class 'LongValueClass' to a NOT NULL column." }
        statement.bindLong(1, _data)
        val _data_1: UUID = checkNotNull(entity.uuidData.data) { "Cannot bind NULLABLE value 'data' of inline class 'UUIDValueClass' to a NOT NULL column." }
        statement.bindBlob(2, convertUUIDToByte(_data_1))
        val _tmpNullableUuidData: UUIDValueClass? = entity.nullableUuidData
        val _data_2: UUID? = _tmpNullableUuidData?.data
        if (_data_2 == null) {
          statement.bindNull(3)
        } else {
          statement.bindBlob(3, convertUUIDToByte(_data_2))
        }
        val _data_3: Long = checkNotNull(entity.nullableLongData.data) { "Cannot bind NULLABLE value 'data' of inline class 'NullableLongValueClass' to a NOT NULL column." }
        statement.bindLong(4, _data_3)
        val _tmpDoubleNullableLongData: NullableLongValueClass? = entity.doubleNullableLongData
        val _data_4: Long? = _tmpDoubleNullableLongData?.data
        if (_data_4 == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _data_4)
        }
        val _password: String = checkNotNull(entity.genericData.password) { "Cannot bind NULLABLE value 'password' of inline class 'GenericValueClass<String>' to a NOT NULL column." }
        statement.bindText(6, _password)
      }
    }
  }

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) { _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _columnIndexOfUuidData: Int = getColumnIndexOrThrow(_stmt, "uuidData")
        val _columnIndexOfNullableUuidData: Int = getColumnIndexOrThrow(_stmt, "nullableUuidData")
        val _columnIndexOfNullableLongData: Int = getColumnIndexOrThrow(_stmt, "nullableLongData")
        val _columnIndexOfDoubleNullableLongData: Int = getColumnIndexOrThrow(_stmt, "doubleNullableLongData")
        val _columnIndexOfGenericData: Int = getColumnIndexOrThrow(_stmt, "genericData")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: LongValueClass
          val _data: Long
          _data = _stmt.getLong(_columnIndexOfPk)
          _tmpPk = LongValueClass(_data)
          val _tmpUuidData: UUIDValueClass
          val _data_1: UUID
          _data_1 = convertByteToUUID(_stmt.getBlob(_columnIndexOfUuidData))
          _tmpUuidData = UUIDValueClass(_data_1)
          val _tmpNullableUuidData: UUIDValueClass?
          if (_stmt.isNull(_columnIndexOfNullableUuidData)) {
            _tmpNullableUuidData = null
          } else {
            val _data_2: UUID
            _data_2 = convertByteToUUID(_stmt.getBlob(_columnIndexOfNullableUuidData))
            _tmpNullableUuidData = UUIDValueClass(_data_2)
          }
          val _tmpNullableLongData: NullableLongValueClass
          val _data_3: Long
          _data_3 = _stmt.getLong(_columnIndexOfNullableLongData)
          _tmpNullableLongData = NullableLongValueClass(_data_3)
          val _tmpDoubleNullableLongData: NullableLongValueClass?
          if (_stmt.isNull(_columnIndexOfDoubleNullableLongData)) {
            _tmpDoubleNullableLongData = null
          } else {
            val _data_4: Long
            _data_4 = _stmt.getLong(_columnIndexOfDoubleNullableLongData)
            _tmpDoubleNullableLongData = NullableLongValueClass(_data_4)
          }
          val _tmpGenericData: GenericValueClass<String>
          val _password: String
          _password = _stmt.getText(_columnIndexOfGenericData)
          _tmpGenericData = GenericValueClass<String>(_password)
          _result = MyEntity(_tmpPk,_tmpUuidData,_tmpNullableUuidData,_tmpNullableLongData,_tmpDoubleNullableLongData,_tmpGenericData)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getEntitySkipVerification(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getValueClass(): UUIDValueClass {
    val _sql: String = "SELECT uuidData FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: UUIDValueClass
        if (_stmt.step()) {
          val _data: UUID
          _data = convertByteToUUID(_stmt.getBlob(0))
          _result = UUIDValueClass(_data)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'UUIDValueClass'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __entityStatementConverter_MyEntity(statement: SQLiteStatement): MyEntity {
    val _entity: MyEntity
    val _columnIndexOfPk: Int = getColumnIndex(statement, "pk")
    val _columnIndexOfUuidData: Int = getColumnIndex(statement, "uuidData")
    val _columnIndexOfNullableUuidData: Int = getColumnIndex(statement, "nullableUuidData")
    val _columnIndexOfNullableLongData: Int = getColumnIndex(statement, "nullableLongData")
    val _columnIndexOfDoubleNullableLongData: Int = getColumnIndex(statement, "doubleNullableLongData")
    val _columnIndexOfGenericData: Int = getColumnIndex(statement, "genericData")
    val _tmpPk: LongValueClass
    if (_columnIndexOfPk == -1) {
      error("Missing column 'pk' for a NON-NULL value, column not found in result.")
    } else {
      val _data: Long
      _data = statement.getLong(_columnIndexOfPk)
      _tmpPk = LongValueClass(_data)
    }
    val _tmpUuidData: UUIDValueClass
    if (_columnIndexOfUuidData == -1) {
      error("Missing column 'uuidData' for a NON-NULL value, column not found in result.")
    } else {
      val _data_1: UUID
      _data_1 = convertByteToUUID(statement.getBlob(_columnIndexOfUuidData))
      _tmpUuidData = UUIDValueClass(_data_1)
    }
    val _tmpNullableUuidData: UUIDValueClass?
    if (_columnIndexOfNullableUuidData == -1) {
      _tmpNullableUuidData = null
    } else {
      if (statement.isNull(_columnIndexOfNullableUuidData)) {
        _tmpNullableUuidData = null
      } else {
        val _data_2: UUID
        _data_2 = convertByteToUUID(statement.getBlob(_columnIndexOfNullableUuidData))
        _tmpNullableUuidData = UUIDValueClass(_data_2)
      }
    }
    val _tmpNullableLongData: NullableLongValueClass
    if (_columnIndexOfNullableLongData == -1) {
      error("Missing column 'nullableLongData' for a NON-NULL value, column not found in result.")
    } else {
      val _data_3: Long
      _data_3 = statement.getLong(_columnIndexOfNullableLongData)
      _tmpNullableLongData = NullableLongValueClass(_data_3)
    }
    val _tmpDoubleNullableLongData: NullableLongValueClass?
    if (_columnIndexOfDoubleNullableLongData == -1) {
      _tmpDoubleNullableLongData = null
    } else {
      if (statement.isNull(_columnIndexOfDoubleNullableLongData)) {
        _tmpDoubleNullableLongData = null
      } else {
        val _data_4: Long
        _data_4 = statement.getLong(_columnIndexOfDoubleNullableLongData)
        _tmpDoubleNullableLongData = NullableLongValueClass(_data_4)
      }
    }
    val _tmpGenericData: GenericValueClass<String>
    if (_columnIndexOfGenericData == -1) {
      error("Missing column 'genericData' for a NON-NULL value, column not found in result.")
    } else {
      val _password: String
      _password = statement.getText(_columnIndexOfGenericData)
      _tmpGenericData = GenericValueClass<String>(_password)
    }
    _entity = MyEntity(_tmpPk,_tmpUuidData,_tmpNullableUuidData,_tmpNullableLongData,_tmpDoubleNullableLongData,_tmpGenericData)
    return _entity
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

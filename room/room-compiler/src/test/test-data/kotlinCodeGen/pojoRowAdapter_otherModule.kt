import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
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
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`primitive`,`string`,`nullableString`,`fieldString`,`nullableFieldString`,`variablePrimitive`,`variableString`,`variableNullableString`,`variableFieldString`,`variableNullableFieldString`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindLong(2, entity.primitive)
        statement.bindText(3, entity.string)
        val _tmpNullableString: String? = entity.nullableString
        if (_tmpNullableString == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpNullableString)
        }
        statement.bindText(5, entity.fieldString)
        val _tmpNullableFieldString: String? = entity.nullableFieldString
        if (_tmpNullableFieldString == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpNullableFieldString)
        }
        statement.bindLong(7, entity.variablePrimitive)
        statement.bindText(8, entity.variableString)
        val _tmpVariableNullableString: String? = entity.variableNullableString
        if (_tmpVariableNullableString == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpVariableNullableString)
        }
        statement.bindText(10, entity.variableFieldString)
        val _tmpVariableNullableFieldString: String? = entity.variableNullableFieldString
        if (_tmpVariableNullableFieldString == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpVariableNullableFieldString)
        }
      }
    }
  }

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) {
      _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _columnIndexOfPrimitive: Int = getColumnIndexOrThrow(_stmt, "primitive")
        val _columnIndexOfString: Int = getColumnIndexOrThrow(_stmt, "string")
        val _columnIndexOfNullableString: Int = getColumnIndexOrThrow(_stmt, "nullableString")
        val _columnIndexOfFieldString: Int = getColumnIndexOrThrow(_stmt, "fieldString")
        val _columnIndexOfNullableFieldString: Int = getColumnIndexOrThrow(_stmt,
            "nullableFieldString")
        val _columnIndexOfVariablePrimitive: Int = getColumnIndexOrThrow(_stmt, "variablePrimitive")
        val _columnIndexOfVariableString: Int = getColumnIndexOrThrow(_stmt, "variableString")
        val _columnIndexOfVariableNullableString: Int = getColumnIndexOrThrow(_stmt,
            "variableNullableString")
        val _columnIndexOfVariableFieldString: Int = getColumnIndexOrThrow(_stmt,
            "variableFieldString")
        val _columnIndexOfVariableNullableFieldString: Int = getColumnIndexOrThrow(_stmt,
            "variableNullableFieldString")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
          val _tmpPrimitive: Long
          _tmpPrimitive = _stmt.getLong(_columnIndexOfPrimitive)
          val _tmpString: String
          _tmpString = _stmt.getText(_columnIndexOfString)
          val _tmpNullableString: String?
          if (_stmt.isNull(_columnIndexOfNullableString)) {
            _tmpNullableString = null
          } else {
            _tmpNullableString = _stmt.getText(_columnIndexOfNullableString)
          }
          val _tmpFieldString: String
          _tmpFieldString = _stmt.getText(_columnIndexOfFieldString)
          val _tmpNullableFieldString: String?
          if (_stmt.isNull(_columnIndexOfNullableFieldString)) {
            _tmpNullableFieldString = null
          } else {
            _tmpNullableFieldString = _stmt.getText(_columnIndexOfNullableFieldString)
          }
          _result =
              MyEntity(_tmpPk,_tmpPrimitive,_tmpString,_tmpNullableString,_tmpFieldString,_tmpNullableFieldString)
          _result.variablePrimitive = _stmt.getLong(_columnIndexOfVariablePrimitive)
          _result.variableString = _stmt.getText(_columnIndexOfVariableString)
          if (_stmt.isNull(_columnIndexOfVariableNullableString)) {
            _result.variableNullableString = null
          } else {
            _result.variableNullableString = _stmt.getText(_columnIndexOfVariableNullableString)
          }
          _result.variableFieldString = _stmt.getText(_columnIndexOfVariableFieldString)
          if (_stmt.isNull(_columnIndexOfVariableNullableFieldString)) {
            _result.variableNullableFieldString = null
          } else {
            _result.variableNullableFieldString =
                _stmt.getText(_columnIndexOfVariableNullableFieldString)
          }
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

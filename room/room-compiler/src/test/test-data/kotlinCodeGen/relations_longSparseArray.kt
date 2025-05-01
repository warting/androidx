import androidx.collection.LongSparseArray
import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getSongsWithArtist(): SongWithArtist {
    val _sql: String = "SELECT * FROM Song"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _columnIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _collectionArtist: LongSparseArray<Artist?> = LongSparseArray<Artist?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfArtistKey)
          _collectionArtist.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipArtistAsArtist(_connection, _collectionArtist)
        val _result: SongWithArtist
        if (_stmt.step()) {
          val _tmpSong: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId)
          val _tmpArtistKey: Long
          _tmpArtistKey = _stmt.getLong(_columnIndexOfArtistKey)
          _tmpSong = Song(_tmpSongId,_tmpArtistKey)
          val _tmpArtist: Artist?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfArtistKey)
          _tmpArtist = _collectionArtist.get(_tmpKey_1)
          if (_tmpArtist == null) {
            error("Relationship item 'artist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'artistKey' and entityColumn named 'artistId'.")
          }
          _result = SongWithArtist(_tmpSong,_tmpArtist)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'SongWithArtist'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getArtistAndSongs(): ArtistAndSongs {
    val _sql: String = "SELECT * FROM Artist"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _collectionSongs: LongSparseArray<MutableList<Song>> = LongSparseArray<MutableList<Song>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfArtistId)
          if (!_collectionSongs.containsKey(_tmpKey)) {
            _collectionSongs.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipSongAsSong(_connection, _collectionSongs)
        val _result: ArtistAndSongs
        if (_stmt.step()) {
          val _tmpArtist: Artist
          val _tmpArtistId: Long
          _tmpArtistId = _stmt.getLong(_columnIndexOfArtistId)
          _tmpArtist = Artist(_tmpArtistId)
          val _tmpSongsCollection: MutableList<Song>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfArtistId)
          _tmpSongsCollection = checkNotNull(_collectionSongs.get(_tmpKey_1))
          _result = ArtistAndSongs(_tmpArtist,_tmpSongsCollection)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'ArtistAndSongs'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPlaylistAndSongs(): PlaylistAndSongs {
    val _sql: String = "SELECT * FROM Playlist"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _collectionSongs: LongSparseArray<MutableList<Song>> = LongSparseArray<MutableList<Song>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          if (!_collectionSongs.containsKey(_tmpKey)) {
            _collectionSongs.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipSongAsSong_1(_connection, _collectionSongs)
        val _result: PlaylistAndSongs
        if (_stmt.step()) {
          val _tmpPlaylist: Playlist
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpPlaylist = Playlist(_tmpPlaylistId)
          val _tmpSongsCollection: MutableList<Song>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpSongsCollection = checkNotNull(_collectionSongs.get(_tmpKey_1))
          _result = PlaylistAndSongs(_tmpPlaylist,_tmpSongsCollection)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'PlaylistAndSongs'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipArtistAsArtist(_connection: SQLiteConnection, _map: LongSparseArray<Artist?>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, false) { _tmpMap ->
        __fetchRelationshipArtistAsArtist(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `artistId` FROM `Artist` WHERE `artistId` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "artistId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfArtistId: Int = 0
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: Artist
          val _tmpArtistId: Long
          _tmpArtistId = _stmt.getLong(_columnIndexOfArtistId)
          _item_1 = Artist(_tmpArtistId)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipSongAsSong(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<Song>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshipSongAsSong(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `songId`,`artistKey` FROM `Song` WHERE `artistKey` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "artistKey")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfSongId: Int = 0
      val _columnIndexOfArtistKey: Int = 1
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<Song>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId)
          val _tmpArtistKey: Long
          _tmpArtistKey = _stmt.getLong(_columnIndexOfArtistKey)
          _item_1 = Song(_tmpSongId,_tmpArtistKey)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipSongAsSong_1(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<Song>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshipSongAsSong_1(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `Song`.`songId` AS `songId`,`Song`.`artistKey` AS `artistKey`,_junction.`playlistKey` FROM `PlaylistSongXRef` AS _junction INNER JOIN `Song` ON (_junction.`songKey` = `Song`.`songId`) WHERE _junction.`playlistKey` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      // _junction.playlistKey
      val _itemKeyIndex: Int = 2
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfSongId: Int = 0
      val _columnIndexOfArtistKey: Int = 1
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<Song>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId)
          val _tmpArtistKey: Long
          _tmpArtistKey = _stmt.getLong(_columnIndexOfArtistKey)
          _item_1 = Song(_tmpSongId,_tmpArtistKey)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

package com.hezi.juyumao.data.local.db.dao

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hezi.juyumao.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.first

class SongPagingSource(
    private val songDao: SongDao,
) : PagingSource<Int, SongEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize

            // For Room, we use the PagingSource directly from DAO
            // This is a fallback implementation
            LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SongEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

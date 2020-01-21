package com.joshtalks.joshskills.repository.service

class NewsRepo(private val apiInterface: ChatNetworkService) : BaseRepository() {
    /* //get latest news using safe api call
     suspend fun getLatestNews() :  MutableList<Article>?{
         return safeApiCall(
             //await the result of deferred type
             call = {apiInterface.fetchLatestNewsAsync("Nigeria", "publishedAt").await()},
             error = "Error fetching news"
             //convert to mutable list
         )?.articles?.toMutableList()
     }*/
}
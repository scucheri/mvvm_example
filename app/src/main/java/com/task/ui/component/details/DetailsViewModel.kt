package com.task.ui.component.details

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.task.data.DataRepositorySource
import com.task.data.Resource
import com.task.data.dto.recipes.RecipesItem
import com.task.ui.base.BaseViewModel
import com.task.utils.wrapEspressoIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Created by AhmedEltaher
 */
@HiltViewModel
open class DetailsViewModel @Inject constructor(private val dataRepository: DataRepositorySource) :
    BaseViewModel() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val recipePrivate = MutableLiveData<RecipesItem>()
    val recipeData: LiveData<RecipesItem> get() = recipePrivate

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val isFavouritePrivate = MutableLiveData<Resource<Boolean>>()
    val isFavourite: LiveData<Resource<Boolean>> get() = isFavouritePrivate

    val sharedFlow= MutableSharedFlow<String>(0)// 一次性事件，默认后面重新订阅不会重新收到
    val stateFlow= MutableStateFlow<String?>(null)// 类似于LiveData，相当于MutableSharedFlow 的relay是1


    init {
        Log.i("testxiaoyu DetailsViewModel DataRepositorySource ", "${dataRepository.hashCode()}")
    }


    fun initIntentData(recipe: RecipesItem) {
        recipePrivate.value = recipe
    }


    open fun testFlow() {
        viewModelScope.launch {
            dataRepository.testFlow().collect {
                Log.i("xiaoyumi detailViewModel ", " $it  thread name: ${Thread.currentThread().name}")
            }
        }

        viewModelScope.launch{
            sharedFlow.emit("Hello")
            sharedFlow.emit("SharedFlow")
            sharedFlow.emit("exception")
            sharedFlow.emit("after exception11")
            sharedFlow.emit("after exception22")

            Log.i("xiaoyumi detailViewModel ", "sharedFlow emit ${Thread.currentThread().name}")
        }

        viewModelScope.launch{
            stateFlow.emit("xiaoyumi stateflow")
            withContext(Dispatchers.IO){
                Log.i("xiaoyumi viewModelScope ", "Dispatchers.IO")
            }
            Log.i("xiaoyumi detailViewModel ", "stateFlow emit ${Thread.currentThread().name}")
        }


        //
//        flowOf(1,2,3).collect({
//
//        })

    }

    open fun addToFavourites() {
        viewModelScope.launch {
            isFavouritePrivate.value = Resource.Loading()
            wrapEspressoIdlingResource {
                recipePrivate.value?.id?.let {
                    dataRepository.addToFavourite(it).collect { isAdded ->
                        isFavouritePrivate.value = isAdded
                    }
                }
            }
        }
    }

    fun removeFromFavourites() {
        viewModelScope.launch {
            isFavouritePrivate.value = Resource.Loading()
            wrapEspressoIdlingResource {
                recipePrivate.value?.id?.let {
                    dataRepository.removeFromFavourite(it).collect { isRemoved ->
                        when (isRemoved) {
                            is Resource.Success -> {
                                isRemoved.data?.let {
                                    isFavouritePrivate.value = Resource.Success(!isRemoved.data)
                                }
                            }
                            is Resource.DataError -> {
                                isFavouritePrivate.value = isRemoved
                            }
                            is Resource.Loading -> {
                                isFavouritePrivate.value = isRemoved
                            }
                        }
                    }
                }
            }
        }
    }

    fun isFavourites() {
        viewModelScope.launch {
            isFavouritePrivate.value = Resource.Loading()
            wrapEspressoIdlingResource {
                recipePrivate.value?.id?.let {
                    dataRepository.isFavourite(it).collect { isFavourites ->
                        isFavouritePrivate.value = isFavourites
                    }
                }
            }
        }
    }
}

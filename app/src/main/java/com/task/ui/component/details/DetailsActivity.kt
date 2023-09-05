package com.task.ui.component.details

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import com.ss.android.lark.calendar.impl.framework.lifecycle.repeatOnLifecycle
import com.task.R
import com.task.RECIPE_ITEM_KEY
import com.task.data.Resource
import com.task.data.dto.recipes.RecipesItem
import com.task.databinding.DetailsLayoutBinding
import com.task.ui.base.BaseActivity
import com.task.utils.observe
import com.task.utils.toGone
import com.task.utils.toVisible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes.Name
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.FlexibleTypeDeserializer.ThrowException

/**
 * Created by AhmedEltaher
 */

@AndroidEntryPoint
class DetailsActivity : BaseActivity() {

    private val viewModel: DetailsViewModel by viewModels()

    private lateinit var binding: DetailsLayoutBinding
    private var menu: Menu? = null


    override fun initViewBinding() {
        binding = DetailsLayoutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initIntentData(intent.getParcelableExtra(RECIPE_ITEM_KEY) ?: RecipesItem())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val scope = CoroutineScope( Dispatchers.Main.immediate + SupervisorJob())

        MainScope()

        viewModel.testFlow()

        //Activity
        lifecycleScope.launch{
            viewModel.sharedFlow.collect { // 这个得在emit 之前，否则会收不到数据； 这个跟MutableSharedFlow 中的relay有关系，默认为0，也就是新的订阅者不会收到以前的数据
                Log.i("xiaoyumi detailactivity sharedFlow", "$it ${Thread.currentThread().name}")
//                if (it == "exception"){// 这里crash了，会导致后面的数据收不到！！而且这个crash会被吃掉！！！
//                    throw  Exception("xiaoyu test exception");                   }
            }

            // 下面这个代码不会执行
            //因为上面launch
            viewModel.testFlow()
            Log.i("xiaoyumi detailactivity lifecycleScope launch testFlow endend ", "$${Thread.currentThread().name}")

        }




        /**
         * 没有repeatOnLifecycle的话，只有第一次调用的时候才会收到事件
         */
        lifecycleScope.launch{
            viewModel.stateFlow.debounce(100).collect({
                Log.i("xiaoyumi detailactivity stateFlow", "$it  ${Thread.currentThread().name}")
            })
        }

        /**
         * 有repeatOnLifecycle的话，就是每次onResume都会重新走到collect里面的逻辑
         */
        lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.stateFlow.debounce(100).collect({
                    Log.i(
                        "xiaoyumi detailactivity stateFlow  repeatOnLifecycle onresumed",
                        "$it  ${Thread.currentThread().name}"
                    )
                })
            }
        }



       viewModel.testFlow()


            // 使用GlobalScope.launch来执行一个协程

            // println(...)最终将运行在kotlin维护的线程池中的某个线程中

            GlobalScope.launch(block = {

                // 我们时刻要记住，suspend仅仅只有有标记的作用，并不会实现挂起操作
                suspend {
                    Log.i("xiaoyumi test launch suspend task",
                        """

                I am in coroutine!

                ${Thread.currentThread().name}

                """.trimIndent()

                    )

                }.invoke()

            })

            Log.i("xiaoyumi test launch suspend task"," Main Thread Name -> ${Thread.currentThread().name}")
            TimeUnit.SECONDS.sleep(3L)


    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.details_menu, menu)
        this.menu = menu
        viewModel.isFavourites()
        return true
    }

    fun onClickFavorite(mi: MenuItem) {
        mi.isCheckable = false
        if (viewModel.isFavourite.value?.data == true) {
            viewModel.removeFromFavourites()
        } else {
            viewModel.addToFavourites()
        }
    }

    override fun observeViewModel() {
        observe(viewModel.recipeData, ::initializeView)
        observe(viewModel.isFavourite, ::handleIsFavourite)
    }

    private fun handleIsFavourite(isFavourite: Resource<Boolean>) {
        when (isFavourite) {
            is Resource.Loading -> {
                binding.pbLoading.toVisible()
            }
            is Resource.Success -> {
                isFavourite.data?.let {
                    handleIsFavouriteUI(it)
                    menu?.findItem(R.id.add_to_favorite)?.isCheckable = true
                    binding.pbLoading.toGone()
                }
            }
            is Resource.DataError -> {
                menu?.findItem(R.id.add_to_favorite)?.isCheckable = true
                binding.pbLoading.toGone()
            }
        }
    }

    private fun handleIsFavouriteUI(isFavourite: Boolean) {
        menu?.let {
            it.findItem(R.id.add_to_favorite)?.icon =
                    if (isFavourite) {
                        ContextCompat.getDrawable(this, R.drawable.ic_star_24)
                    } else {
                        ContextCompat.getDrawable(this, R.drawable.ic_outline_star_border_24)
                    }
        }
    }

    private fun initializeView(recipesItem: RecipesItem) {
        binding.tvName.text = recipesItem.name
        binding.tvHeadline.text = recipesItem.headline
        binding.tvDescription.text = recipesItem.description
        Picasso.get().load(recipesItem.image).placeholder(R.drawable.ic_healthy_food_small)
                .into(binding.ivRecipeImage)

    }
}

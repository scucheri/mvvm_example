package com.task.ui.component.login

import androidx.lifecycle.viewModelScope

/**
 * Created by xiaoxiaoyu on 2023/5/26.
 */
class ChildViewModel(loginViewModel: LoginViewModel) {
    val viewModelScope = loginViewModel.viewModelScope
    val value = "tirtrot"


}
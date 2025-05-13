package com.example.freshbox.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Log

class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Log.w("SingleLiveEvent", "Multiple observers registered but only one will be notified of changes.")
        }

        super.observe(owner, Observer { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    fun call() {
        value = null // 이벤트 발생을 위해 value를 null (또는 아무 값)으로 설정
    }
}
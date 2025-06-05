// File: app/src/main/java/com/example/freshbox/util/SingleLiveEvent.kt
package com.example.freshbox.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean // 동시성 문제를 방지하기 위해 AtomicBoolean 사용
import android.util.Log

/**
 * LiveData를 확장하여 이벤트가 단 한 번만 소비되도록 하는 클래스.
 * 예: 화면 전환, Snackbar 표시, Dialog 표시 등 일회성 이벤트를 처리할 때 유용합니다.
 * 구성 변경(예: 화면 회전) 후에도 이벤트가 중복으로 전달되는 것을 방지합니다.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {

    // 이벤트가 아직 처리되지 않았는지(pending) 여부를 나타내는 플래그입니다.
    // AtomicBoolean을 사용하여 여러 스레드에서 안전하게 접근할 수 있도록 합니다.
    private val pending = AtomicBoolean(false)

    /**
     * LiveData를 관찰(observe)합니다.
     * 일반 LiveData와 달리, 'pending' 상태일 때만 Observer에게 변경 사항을 전달하고,
     * 전달 후에는 'pending' 상태를 false로 변경하여 다음 Observer 호출 시 중복 전달을 막습니다.
     *
     * @param owner Observer의 생명주기를 관리하는 LifecycleOwner.
     * @param observer 변경 사항을 수신할 Observer.
     */
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        // 이미 활성 Observer가 있는지 확인하고, 있다면 경고 로그를 남깁니다.
        // SingleLiveEvent는 보통 단일 Observer에 의해 소비되는 것을 가정합니다.
        if (hasActiveObservers()) {
            Log.w("SingleLiveEvent", "Multiple observers registered but only one will be notified of changes.")
        }

        // 부모 클래스(MutableLiveData)의 observe 메서드를 호출하여 Observer를 등록합니다.
        // 전달되는 데이터(t)에 대해 추가적인 로직을 적용합니다.
        super.observe(owner, Observer { t ->
            // pending 플래그가 true일 경우에만 (즉, 처리되지 않은 새 이벤트가 있을 때만)
            // 해당 값을 false로 변경하고 (compareAndSet), Observer의 onChanged를 호출합니다.
            // compareAndSet은 원자적 연산으로, 현재 값이 expectedValue(첫 번째 인자 true)와 같으면
            // newValue(두 번째 인자 false)로 업데이트하고 true를 반환합니다.
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    /**
     * LiveData의 값을 설정합니다.
     * 새로운 값이 설정되면, 'pending' 플래그를 true로 설정하여 새 이벤트가 발생했음을 알립니다.
     *
     * @param t 설정할 새로운 값.
     */
    override fun setValue(t: T?) {
        pending.set(true) // 새 값이 설정되었으므로, 처리 대기 상태로 만듭니다.
        super.setValue(t) // 부모 클래스의 setValue를 호출하여 실제 값을 변경하고 Observer에게 알립니다.
    }

    /**
     * 파라미터 없이 이벤트를 발생시키고 싶을 때 호출하는 편의 메서드입니다.
     * (주로 Unit 타입의 SingleLiveEvent와 함께 사용됩니다.)
     * 내부적으로 value를 null로 설정하여 setValue를 호출하고, 이를 통해 pending 상태를 true로 만듭니다.
     * Observer는 이 null 값 또는 변경 자체에 반응하게 됩니다.
     */
    fun call() {
        value = null // value를 null (또는 Unit 타입의 경우 Unit 객체 등 의미 없는 값)으로 설정하여 이벤트를 트리거합니다.
    }
}
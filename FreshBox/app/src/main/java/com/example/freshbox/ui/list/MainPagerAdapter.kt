// File: app/src/main/java/com/example/freshbox/ui/list/MainPagerAdapter.kt
package com.example.freshbox.ui.list

import androidx.fragment.app.Fragment // Fragment 클래스 import
import androidx.fragment.app.FragmentActivity // FragmentActivity 클래스 import (생성자에서 사용)
import androidx.viewpager2.adapter.FragmentStateAdapter // ViewPager2에 Fragment를 연결하기 위한 어댑터
import com.example.freshbox.ui.calendar.CalendarFragment // 캘린더 화면을 표시할 Fragment import
// HomeFragment는 같은 패키지에 있으므로 별도 import 불필요 (만약 다른 패키지라면 import 필요)

/**
 * MainActivity의 ViewPager2에 Fragment들을 제공하는 어댑터입니다.
 * FragmentStateAdapter는 Fragment의 생명주기를 관리하며, 화면에 보이지 않는 Fragment의 상태를 저장하고 복원합니다.
 *
 * @param fragmentActivity 이 어댑터가 연결될 ViewPager2를 호스팅하는 FragmentActivity (일반적으로 MainActivity).
 */
class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // ViewPager2가 표시할 총 페이지(Fragment)의 수를 반환합니다.
    // 현재는 HomeFragment와 CalendarFragment 두 개를 사용하므로 2를 반환합니다.
    override fun getItemCount(): Int = 2

    /**
     * 지정된 위치(position)에 해당하는 Fragment의 새 인스턴스를 생성하여 반환합니다.
     * ViewPager2가 특정 페이지를 표시해야 할 때 이 메서드를 호출합니다.
     *
     * @param position 생성할 Fragment의 위치 (0부터 시작).
     * @return 해당 위치에 표시될 Fragment 객체.
     * @throws IllegalArgumentException 만약 유효하지 않은 position이 주어질 경우.
     */
    override fun createFragment(position: Int): Fragment {
        // position 값에 따라 다른 Fragment 인스턴스를 반환합니다.
        return when (position) {
            0 -> HomeFragment() // 첫 번째 탭(position 0)에는 HomeFragment를 표시
            1 -> CalendarFragment() // 두 번째 탭(position 1)에는 CalendarFragment를 표시
            else -> throw IllegalArgumentException("Invalid page index: $position") // 정의되지 않은 위치일 경우 예외 발생
        }
    }
}
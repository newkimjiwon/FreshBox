// File: app/src/main/java/com/example/freshbox/ui/list/MainActivity.kt
package com.example.freshbox.ui.list

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2 // Fragment들을 스와이프 가능한 뷰로 관리
import com.example.freshbox.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding 미사용 시 (현재 보내주신 코드 기반):
        setContentView(R.layout.activity_main) // activity_main.xml 레이아웃을 화면에 설정

        // activity_main.xml에 정의된 ViewPager2 위젯을 ID로 찾아옵니다.
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        // MainPagerAdapter 인스턴스를 생성합니다.
        // MainPagerAdapter는 각 페이지(탭)에 어떤 Fragment를 보여줄지 결정합니다.
        // 'this'는 현재 Activity(FragmentActivity를 상속받음)의 Context를 전달합니다.
        viewPager.adapter = MainPagerAdapter(this)
    }
}
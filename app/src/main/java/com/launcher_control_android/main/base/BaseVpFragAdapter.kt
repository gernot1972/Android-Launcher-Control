package com.launcher_control_android.main.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class BaseVpFragAdapter(fa: FragmentActivity, var arr: List<Fragment>) :
        FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = arr.size

    override fun createFragment(position: Int): Fragment = arr[position]

}






















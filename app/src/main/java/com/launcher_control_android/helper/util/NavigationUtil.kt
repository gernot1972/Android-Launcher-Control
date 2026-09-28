package com.launcher_control_android.helper.util

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.launcher_control_android.R

class NavigationUtil

fun AppCompatActivity.startActivity(
    act: Class<*>,
    bundle: Bundle? = null,
    flags: List<Int>? = null,
    shouldAnimate: Boolean = true
) {
    val intent = Intent(this, act)

    if (bundle != null)
        intent.putExtras(bundle)

    if (!flags.isNullOrEmpty())
        flags.forEach {
            intent.addFlags(it)
        }

    if (shouldAnimate)
        startActivity(
            intent, ActivityOptions.makeCustomAnimation(
                applicationContext,
                R.anim.fade_in,
                R.anim.fade_out
            ).toBundle()
        )
    else
        startActivity(intent)
}

fun AppCompatActivity.startActivityForResult(
    act: Class<*>,
    resultLauncher: ActivityResultLauncher<Intent>,
    bundle: Bundle? = null,
    flags: List<Int>? = null,
    shouldAnimate: Boolean = true
) {
    val intent = Intent(this, act)

    if (bundle != null)
        intent.putExtras(bundle)

    if (!flags.isNullOrEmpty())
        flags.forEach {
            intent.addFlags(it)
        }

    if (shouldAnimate)
        resultLauncher.launch(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(
                applicationContext,
                R.anim.fade_in,
                R.anim.fade_out
            )
        )
    else
        resultLauncher.launch(intent)
}

fun AppCompatActivity.addFrag(
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    shouldAnimate: Boolean = true,
    bundle: Bundle? = null
) {
    supportFragmentManager.commit {
        if (shouldAnimate)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        if (addToBackStack)
            addToBackStack(fragment::class.java.name)
        if (bundle != null)
            fragment.arguments = bundle

        add(container, fragment)
    }
}

fun AppCompatActivity.replaceFrag(
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    shouldAnimate: Boolean = true,
    bundle: Bundle? = null
) {
    supportFragmentManager.commit {
        if (shouldAnimate)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        if (addToBackStack)
            addToBackStack(fragment::class.java.name)
        if (bundle != null)
            fragment.arguments = bundle

        replace(container, fragment)
    }
}

fun AppCompatActivity.showDialogFrag(dialFrag: DialogFragment, bundle: Bundle? = null) {
    dialFrag.arguments = bundle
    dialFrag.show(supportFragmentManager, "")
}


fun Fragment.startActivity(
    act: Class<*>,
    bundle: Bundle? = null,
    flags: List<Int>? = null,
    shouldAnimate: Boolean = true
) {
    val intent = Intent(requireActivity(), act)

    if (bundle != null)
        intent.putExtras(bundle)

    if (!flags.isNullOrEmpty())
        flags.forEach {
            intent.addFlags(it)
        }

    if (shouldAnimate)
        startActivity(
            intent,
            ActivityOptions.makeCustomAnimation(requireContext(), R.anim.fade_in, R.anim.fade_out)
                .toBundle()
        )
    else
        startActivity(intent)
}

fun Fragment.startActivityForResult(
    act: Class<*>,
    resultLauncher: ActivityResultLauncher<Intent>,
    bundle: Bundle? = null,
    flags: List<Int>? = null,
    shouldAnimate: Boolean = true
) {
    val intent = Intent(requireActivity(), act)

    if (bundle != null)
        intent.putExtras(bundle)

    if (!flags.isNullOrEmpty())
        flags.forEach {
            intent.addFlags(it)
        }

    if (shouldAnimate)
        resultLauncher.launch(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                R.anim.fade_in,
                R.anim.fade_out
            )
        )
    else
        resultLauncher.launch(intent)
}

fun Fragment.addFrag(
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    shouldAnimate: Boolean = true,
    bundle: Bundle? = null
) {
    childFragmentManager.commit {
        if (shouldAnimate)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        if (addToBackStack)
            addToBackStack(fragment::class.java.name)
        if (bundle != null)
            fragment.arguments = bundle

        add(container, fragment)
    }
}

fun Fragment.replaceFrag(
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    shouldAnimate: Boolean = true,
    bundle: Bundle? = null
) {
    childFragmentManager.commit {
        if (shouldAnimate)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        if (addToBackStack)
            addToBackStack(fragment::class.java.name)
        if (bundle != null)
            fragment.arguments = bundle

        replace(container, fragment)
    }
}


fun Fragment.addFragInAct(
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    shouldAnimate: Boolean = true,
    bundle: Bundle? = null
) {
    if (requireActivity() is AppCompatActivity)
        (requireActivity() as AppCompatActivity).addFrag(
            fragment,
            container,
            addToBackStack,
            shouldAnimate,
            bundle
        )
}

fun Fragment.replaceFragInAct(
    fragment: Fragment,
    container: Int,
    addToBackStack: Boolean = false,
    shouldAnimate: Boolean = true,
    bundle: Bundle? = null
) {
    if (requireActivity() is AppCompatActivity)
        (requireActivity() as AppCompatActivity).replaceFrag(
            fragment,
            container,
            addToBackStack,
            shouldAnimate,
            bundle
        )
}

fun Fragment.showDialogFrag(dialFrag: DialogFragment, bundle: Bundle? = null) {
    if (isAdded) {
        dialFrag.arguments = bundle
        dialFrag.show(childFragmentManager, "")
    }
}


fun Fragment.canPopBackStack(): Boolean {
    if (childFragmentManager.backStackEntryCount > 0) {
        val topFrag = childFragmentManager.fragments.last()
        if (!topFrag.canPopBackStack()) {
            childFragmentManager.popBackStack()
            return true
        }
    }
    return false
}
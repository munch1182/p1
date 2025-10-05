package com.munch1182.p1.base

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.munch1182.p1.R

class FragmentContainerActivity : BaseActivity() {

    companion object {
        private const val EXTRA_FRAGMENT_CLASS = "fragment_class"
        private const val EXTRA_FRAGMENT_ARGS = "fragment_args"
        private const val EXTRA_CONTAINER_ID = "container_id"
        private val fragmentContainerId: Int = R.id.fragment_container

        /**
         * 启动 Fragment Activity
         * @param context 上下文
         * @param fragmentClass 要显示的 Fragment 类
         * @param args Fragment 参数（可选）
         * @param containerId 容器 ID（可选，默认使用 R.id.fragment_container）
         */
        fun start(
            context: Context,
            fragmentClass: Class<out Fragment>,
            args: Bundle? = null,
            containerId: Int = fragmentContainerId
        ) {
            val intent = Intent(context, FragmentContainerActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_CLASS, fragmentClass)
                putExtra(EXTRA_FRAGMENT_ARGS, args)
                putExtra(EXTRA_CONTAINER_ID, containerId)
            }
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = R.id.fragment_container })

        // 获取容器 ID
        // 如果 Activity 被重建，Fragment 会自动恢复
        if (savedInstanceState == null) {
            val fragment = createFragmentFromIntent()
            fragment?.let {
                supportFragmentManager.beginTransaction().add(fragmentContainerId, it).commit()
            } ?: run {
                finish()
            }
        }
    }

    /**
     * 从 Intent 中创建 Fragment
     */
    @Suppress("unchecked_cast", "DEPRECATION")
    private fun createFragmentFromIntent(): Fragment? {
        return try {
            val fragmentClass: Class<out Fragment>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_FRAGMENT_CLASS, Class::class.java) as? Class<out Fragment>?
            } else {
                intent.getSerializableExtra(EXTRA_FRAGMENT_CLASS) as? Class<out Fragment>?
            }
            val args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS)

            fragmentClass?.let { clazz ->
                val fragment = clazz.newInstance()
                fragment.arguments = args
                fragment
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 替换当前 Fragment
     */
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    /**
     * 获取当前显示的 Fragment
     */
    fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(fragmentContainerId)
    }

    /**
     * 设置 Activity 标题
     */
    fun setActivityTitle(title: String) {
        this.title = title
    }

    /**
     * 设置 Activity 标题（资源 ID）
     */
    fun setActivityTitle(titleResId: Int) {
        this.title = getString(titleResId)
    }
}